package boofcv.alg.sfm;

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.feature.tracker.KeyFramePointTracker;
import boofcv.abst.geo.RefineEpipolarMatrix;
import boofcv.abst.geo.RefinePerspectiveNPoint;
import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.geo.DecomposeEssential;
import boofcv.alg.geo.PointPositionPair;
import boofcv.alg.geo.PositiveDepthConstraintCheck;
import boofcv.factory.geo.FactoryTriangulate;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.struct.FastQueue;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;
import pja.sorting.QuickSelectArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Very basic and straight forward visual odometry algorithm. Because of its simplicity it is more
 * prone to errors caused by a single event.  A high level summary of each processing step is shown
 * below.  Key frames are identified based on the number of frames processed and track motion.  Fixed position
 * updates are performed at key frames while temporary ones are done as each frame is processed to provide
 * a continuous update.  Updating motion from two consecutive frames with little motion greatly increases
 * the amount of noise.
 *
 * <ol>
 * <li> Compute Essential matrix. </li>
 * <li> Select best motion solution. </li>
 * <li> Triangular point location</li>
 * <li> Update camera position</li>
 * <li> Bundle adjustment</li>
 * <li> Repeat last 3 steps</li>
 * </ol>
 *
 * @author Peter Abeles
 */
// TODO handle active/not active better
public class MonocularSimpleVo<T extends ImageBase> {
	KeyFramePointTracker<T,PointPoseTrack> tracker;

	ModelMatcher<DenseMatrix64F,AssociatedPair> computeE;
	RefineEpipolarMatrix refineE;
	
	DecomposeEssential decomposeE = new DecomposeEssential();
	PositiveDepthConstraintCheck depthChecker = new PositiveDepthConstraintCheck(true);

	TriangulateTwoViewsCalibrated triangulateAlg = FactoryTriangulate.twoDLT();

	ModelMatcher<Se3_F64,PointPositionPair> computeMotion;
	RefinePerspectiveNPoint refineMotion;

	// transform from work to current image.  Only used for output purposes
	Se3_F64 worldToCurr = new Se3_F64();
	// transform from the world frame to the keyframe
	Se3_F64 worldToKey = new Se3_F64();
	// transform from the keyframe to the current frame
	Se3_F64 keyToCurr = new Se3_F64();
	// transform from the keyframe to the most recent spawn point
	Se3_F64 keyToSpawn = new Se3_F64();
	// has a key frame been set?
	boolean hasSpawned = false;
	// true if there was a fatal error
	boolean fatal;

	int mode;

	int inlierSize;
	// minimum distance a pixel needs to move for it to be considered significant motion
	double minDistance;
	int minFeatures;
	int setKeyThreshold;
	
	// storage for pixel motion, used to decide if the camera has moved or not
	double distance[];
	
   Se3_F64 temp = new Se3_F64();
	
	
	FastQueue<PointPositionPair> queuePointPose = new FastQueue<PointPositionPair>(200,PointPositionPair.class,true);

	/**
	 *
	 * @param minFeatures
	 * @param setKeyThreshold
	 * @param minDistance
	 * @param tracker
	 * @param pixelToNormalized Pixel to calibrated normalized coordinates.  Right handed (y-axis positive is image up)
	 *                          coordinate system is assumed.
	 * @param computeE
	 * @param refineE
	 * @param computeMotion
	 * @param refineMotion
	 */
	public MonocularSimpleVo( int minFeatures , int setKeyThreshold,
							  double minDistance ,
							  ImagePointTracker<T> tracker ,
							  PointTransform_F64 pixelToNormalized ,
							  ModelMatcher<DenseMatrix64F,AssociatedPair> computeE ,
							  RefineEpipolarMatrix refineE ,
							  ModelMatcher<Se3_F64,PointPositionPair> computeMotion ,
							  RefinePerspectiveNPoint refineMotion )
	{
		this.minFeatures = minFeatures;
		this.setKeyThreshold = setKeyThreshold;
		this.minDistance = minDistance;
		this.tracker = new KeyFramePointTracker<T,PointPoseTrack>(tracker,pixelToNormalized,PointPoseTrack.class);
		this.computeE = computeE;
		this.refineE = refineE;
		this.computeMotion = computeMotion;
		this.refineMotion = refineMotion;
		this.fatal = fatal;

		distance = new double[ minFeatures*2 ];
	}

	/**
	 * Estimates the camera's ego motion by processing the image.  If true is returned then
	 * the position was estimated.  If false is returned then the position was not estimated
	 * and any past history has been discarded.
	 *
	 * @param image Image being processed.
	 *
	 * @return True if the motion was estimated and false if it was not.
	 */
	public boolean process( T image )
	{
		tracker.process(image);

		System.out.println("***************** Mode "+mode+" tracks "+tracker.getActiveTracks().size());
		
		if( mode == 0 ) {
			tracker.setKeyFrame();
			tracker.spawnTracks();
			mode = 1;
			return false;
		} else if( mode == 1 ) {
			checkInitialize();
		} else if( mode == 2 ) {
			if( !updatePosition() ) {
				System.out.println(" *** UPDATED FAILED ****");
				// update failed so reset
				mode = 1;
				worldToKey.reset();
				keyToCurr.reset();
				tracker.reset();
				tracker.spawnTracks();
				hasSpawned = false;
				fatal = true;
				return false;
			} else {
				// check and triangulate new features
				if( hasSpawned && inlierSize < minFeatures && isSufficientMotion() ) {
					setSpawnToKeyFrame();
				}
			}
		}

		// compute output position
		worldToKey.concat(keyToCurr,worldToCurr);

		return true;
	}

	private void checkInitialize() {
		fatal = false;
		List<PointPoseTrack> pairs = tracker.getPairs();

		if( pairs.size() < minFeatures ) {
			tracker.reset();
			tracker.spawnTracks();
			tracker.setKeyFrame();
		} else {
			// see if there has been enough pixel motion to warrant the cost of computing E
			if( isSufficientMotion()) {
				if( estimateMotionFromEssential(false) ) {
					hasSpawned = false;
					mode = 2;
				}
			}
		}
	}

	private boolean estimateMotionFromEssential( boolean hasPrevious) {
		List<PointPoseTrack> pairs = tracker.getPairs();

		// initial estimate of E
		if( computeE.process((List)pairs) ) {
			DenseMatrix64F initial = computeE.getModel();
			List<AssociatedPair> inliers = computeE.getMatchSet();

			inlierSize = inliers.size();

			System.out.println("    Essential inliers "+inliers.size()+"  out of "+pairs.size());
			
			// refine E using non-linear optimization
			if( refineE.process(initial,inliers) ) {
				DenseMatrix64F E = refineE.getRefinement();
				decomposeE.decompose(E);

				// select best possible motion from E
				List<Se3_F64> solutions = decomposeE.getSolutions();

				Se3_F64 best = selectBestPose(solutions, inliers);
					
				if( hasPrevious ) {
					double scale = keyToCurr.getT().norm();
					double foundScale = best.getT().norm();
					best.getT().x *= scale/foundScale;
					best.getT().y *= scale/foundScale;
					best.getT().z *= scale/foundScale;

//					System.out.println("  SCALE RATIO "+(scale/foundScale));
					
					keyToCurr.set(best);
					for( PointPoseTrack t : pairs ) {
						if( !t.active )
							throw new RuntimeException("CRAP INACTIVE HERE?!?!");
						triangulateAlg.triangulate(t.keyLoc,t.currLoc, keyToCurr,t.location);
					}
				} else {
					keyToCurr.set(best);

					// triangular feature points
					// Adjust scale for numerical stability
					double max = 0;
					for( PointPoseTrack t : pairs ) {
						triangulateAlg.triangulate(t.keyLoc,t.currLoc, keyToCurr,t.location);
						t.active = true;
						if( t.location.z > max ) {
							max = t.location.z;
						}
					}
					for( PointPoseTrack t : pairs ) {
						t.location.x /= max;
						t.location.y /= max;
						t.location.z /= max;
					}
					keyToCurr.getT().x /= max;
					keyToCurr.getT().y /= max;
					keyToCurr.getT().z /= max;
				}
				
				System.out.print("   after estimate E:  ");
				computeResidualError();
				
				return true;
			}
		}
		
		return false;
	}

	/**
	 * Updates the position estimate using triangulation.
	 *
	 * @return true if successful or false if it failed
	 */
	private boolean updatePosition() {
		queuePointPose.reset();
		List<PointPositionPair> active = new ArrayList<PointPositionPair>();
		for( PointPoseTrack t : tracker.getPairs() ) {
			if( !t.active )
				continue;
			PointPositionPair p = queuePointPose.pop();
			p.location = t.location;
			p.observed = t.currLoc;
			active.add(p);
		}
		if( queuePointPose.size <= 0 )
			return false;
		
		// estimate the camera's motion
		if( !computeMotion.process(active) ) {
			return false;
		}
		
		List<PointPositionPair> inliers = computeMotion.getMatchSet();
		inlierSize = inliers.size();
		
		System.out.println("   Motion inliers "+inlierSize+"  out of "+active.size());
		
		refineMotion.process(computeMotion.getModel(), inliers);

		keyToCurr.set(refineMotion.getRefinement());

		System.out.print("   after PnP Refine:  ");
		computeResidualError();

		// update point positions
		for( PointPoseTrack t : tracker.getPairs() ) {
			if( t.active )
				triangulateAlg.triangulate(t.keyLoc,t.currLoc, keyToCurr,t.location);
		}
		System.out.print("   after triangulate: ");
		computeResidualError();

		// handle spawning new features
		if( !hasSpawned && inlierSize <= setKeyThreshold ) {
			estimateMotionFromEssential(true);

			System.out.println("--- Setting key frame");
			for( PointPoseTrack t : tracker.getPairs() ) {
				if( t.active ) {
					t.spawnLoc.set(t.currLoc);
				}
			}
			tracker.spawnTracks();
			keyToSpawn.set(keyToCurr);
			hasSpawned = true;
		}

		return true;
	}

	/**
	 * Changes the last spawn point into the new keyframe.  All points have keyLoc
	 * set to the observation at the spawn point and set active to true.  Then
	 * the camera position and points are triangulated using motion from essential
	 *
	 */
	protected void setSpawnToKeyFrame() {

		// update the key location and set inactive tracks to true which where
		// spawned at the spane point
		for( PointPoseTrack t : tracker.getPairs() ) {
			if( t.active ) {
				t.keyLoc.set(t.spawnLoc);
			} else {
				t.active = true;
			}
		}
		hasSpawned = false;

		// make the spawn point the new keyframe
		worldToKey.concat(keyToSpawn, temp);
		worldToKey.set(temp);

		keyToSpawn.invert(temp);
		// s2k * k2c = s2c
		keyToCurr.concat(temp,keyToSpawn);
		keyToCurr.set(keyToSpawn);
		

		// now estimate the motion and triangulate points by computing the essential matrix
		estimateMotionFromEssential(true);
	}

	/**
	 * When decomposing the essential matrix, several possible motion are created.  Since any
	 * object which is seen by the camera must be in front of the camera the positive depth
	 * constraint can be used to select the most likely motion from the set.
	 */
	private Se3_F64 selectBestPose(List<Se3_F64> motions, List<AssociatedPair> observations) {
		int bestCount = 0;
		Se3_F64 bestModel = null;

		for( Se3_F64 s : motions ) {
			int count = 0;
			for( AssociatedPair p : observations ) {
				if( depthChecker.checkConstraint(p.currLoc,p.keyLoc,s) ) {
					count++;
				}
			}
			
			if( count > bestCount ) {
				bestCount = count;
				bestModel = s;
			}
		}
		
		return bestModel;
	}
	
	private void computeResidualError() {
		
		Point3D_F64 currentView = new Point3D_F64();
		
		double total = 0;
		int num = 0;
		
		for( PointPoseTrack t : tracker.getPairs() ) {
			if( !t.active )
				continue;

			Point3D_F64 p = t.location;
			Point2D_F64 obs = t.currLoc;
			
			SePointOps_F64.transform(keyToCurr,p,currentView);
			
			double x = currentView.x/currentView.z;
			double y = currentView.y/currentView.z;

			x = x - obs.x;
			y = y - obs.y;

			total += Math.sqrt(x*x + y*y);
			num++;
		}
		total /= num;
		System.out.printf("   -- residual error = %05e  N = %d\n",total,num);
	}
	
	/**
	 * Looks at inactive points and decides if there is enough motion to estimate their 
	 * position.
	 * 
	 * @return true if there is sufficient motion
	 */
	private boolean isSufficientMotion() {
		
		List<PointPoseTrack> tracks = tracker.getPairs();
		
		if( distance.length < tracks.size() ) {
			distance = new double[ tracks.size()*4/3 ];
		}
		
		int count = 0;
		for( int i = 0; i < tracks.size(); i++ ) {
			PointPoseTrack t = tracks.get(i);
			AssociatedPair p = t.getPixel();
			if( !t.active ) {
				distance[count++] = p.currLoc.distance2(p.keyLoc);
			}
		}
		System.out.println("  total inactive "+count+"  total "+tracks.size());

		double median = QuickSelectArray.select(distance,count/2,count);
	
		return median >= minDistance*minDistance;
	}

	public Se3_F64 getCameraLocation() {
		return worldToCurr;
	}

	public boolean isFatal() {
		return fatal;
	}
}
