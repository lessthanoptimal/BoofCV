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


	int mode;

	int inlierSize;
	// minimum distance a pixel needs to move for it to be considered significant motion
	double minDistance;
	int minFeatures;
	int setKeyThreshold;
	
	// storage for pixel motion, used to decide if the camera has moved or not
	double distance[];
	

	
	
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

		System.out.println("Mode "+mode+" tracks "+tracker.getActiveTracks().size());
		
		if( mode == 0 ) {
			tracker.setKeyFrame();
			tracker.spawnTracks();
			mode = 1;
		} else if( mode == 1 ) {
			checkInitialize();
		} else if( mode == 2 ) {
			if( !updatePosition() ) {
				// update failed so reset
				mode = 0;
				keyToCurr.reset();
				tracker.reset();
				tracker.spawnTracks();
				return false;
			} else {
				// check and triangulate new features
				if( hasSpawned && (inlierSize<minFeatures || isSufficientMotion()) ) {
					triangulateNew();
				}
			}
		}

		// compute output position
		worldToKey.concat(keyToCurr,worldToCurr);

		return true;
	}

	private void checkInitialize() {
		List<PointPoseTrack> pairs = tracker.getPairs();

		if( pairs.size() < minFeatures ) {
			tracker.reset();
			tracker.spawnTracks();
			tracker.setKeyFrame();
		} else {
			// see if there has been enough pixel motion to warrant the cost of computing E
			if( isSufficientMotion()) {
				// initial estimate of E
				if( computeE.process((List)pairs) ) {
					DenseMatrix64F initial = computeE.getModel();
					List<AssociatedPair> inliers = computeE.getMatchSet();

					// refine E using non-linear optimization
					if( refineE.process(initial,inliers) ) {
						DenseMatrix64F E = refineE.getRefinement();
						decomposeE.decompose(E);

						// select best possible motion from E
						List<Se3_F64> solutions = decomposeE.getSolutions();
						
						Se3_F64 best = selectBestPose(solutions, inliers);
//						best.invert(worldToCurr); // todo hack
						keyToCurr.set(best);

						// triangular feature points
						double maxZ = 0;
						for( PointPoseTrack t : pairs ) {
							triangulateAlg.triangulate(t.keyLoc,t.currLoc, keyToCurr,t.location);
							t.active = true;
							if( t.location.z > maxZ )
								maxZ = t.location.z;
						}
						// rescale for numerical stability
//						keyToCurr.getT().x /= maxZ;
//						keyToCurr.getT().y /= maxZ;
//						keyToCurr.getT().z /= maxZ;
//
//						for( PointPoseTrack t : pairs ) {
//							Point3D_F64 p = t.location;
//							p.x /= maxZ;
//							p.y /= maxZ;
//							p.z /= maxZ;
//						}

						hasSpawned = false;
						mode = 2;
					}
				}
			}
		}
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
		
		System.out.println("   -- triangulate inliers "+inlierSize+"  out of "+active.size());
		// handle spawning new features
		if( !hasSpawned && inlierSize <= setKeyThreshold ) {
			System.out.println("--- Setting key frame");
			tracker.spawnTracks();
			keyToSpawn.set(keyToCurr);
			hasSpawned = true;
		}

		

		return true;
	}

	/**
	 * Triangulates the location of new features
	 */
	protected void triangulateNew() {
		System.out.println("---- Triangulating new features");
		// change in position from key frame to current frame
//		Se3_F64 worldToCurr = this.worldToCurr.invert(null);
		Se3_F64 spawnToKey = keyToSpawn.invert(null);
		Se3_F64 spawnToCurr = spawnToKey.concat(this.keyToCurr,null);

		
		for( PointPoseTrack t : tracker.getPairs() ) {
			if( t.active ) {
				// project its position to the spawn point
				SePointOps_F64.transform(keyToSpawn,t.location,t.location);

				// now project the observation
				t.keyLoc.x = t.location.x/t.location.z;
				t.keyLoc.y = t.location.y/t.location.z;

			} else {
				triangulateAlg.triangulate(t.keyLoc,t.currLoc,spawnToCurr, t.location);


				t.active = true;
			}
		}
		hasSpawned = false;

		// make the spawn point the new keyframe
		worldToKey.concat(keyToSpawn,spawnToKey);
		worldToKey.set(spawnToKey);
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
}
