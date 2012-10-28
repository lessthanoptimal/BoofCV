package boofcv.alg.sfm;

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.feature.tracker.KeyFramePointTracker;
import boofcv.abst.geo.BundleAdjustmentCalibrated;
import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.alg.geo.DecomposeEssential;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PositiveDepthConstraintCheck;
import boofcv.alg.geo.bundle.CalibratedPoseAndPoint;
import boofcv.alg.geo.bundle.PointIndexObservation;
import boofcv.alg.geo.bundle.ViewPointObservations;
import boofcv.factory.geo.FactoryTriangulate;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.struct.FastQueue;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.GeoModelRefine;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;
import pja.sorting.QuickSelectArray;

import java.util.ArrayList;
import java.util.Arrays;
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

	double outlierResidualError = 0.5;

	KeyFramePointTracker<T,PointPoseTrack> tracker;

	BundleAdjustmentCalibrated bundle = null;//FactoryMultiView.bundleCalibrated(1e-5, 30);
	CalibratedPoseAndPoint bundleModel = new CalibratedPoseAndPoint();
	List<ViewPointObservations> bundleObs = new ArrayList<ViewPointObservations>();

	ModelMatcher<Se3_F64,AssociatedPair> epipolarMotion;

	DecomposeEssential decomposeE = new DecomposeEssential();
	TriangulateTwoViewsCalibrated triangulateAlg = FactoryTriangulate.twoGeometric();

	PositiveDepthConstraintCheck depthChecker = new PositiveDepthConstraintCheck(triangulateAlg);

	GeoModelRefine<DenseMatrix64F,AssociatedPair> refineE;
	ModelMatcher<Se3_F64,Point2D3D> computeMotion;
	GeoModelRefine<Se3_F64,Point2D3D> refineMotion;

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
	double minPixelChange;
	int minFeatures;
	int setKeyThreshold;
	
	// storage for pixel motion, used to decide if the camera has moved or not
	double distance[];
	
   Se3_F64 temp = new Se3_F64();

	DenseMatrix64F foundE = new DenseMatrix64F(3,3);
	Se3_F64 refinedM = new Se3_F64();
	
	
	FastQueue<Point2D3D> queuePointPose = new FastQueue<Point2D3D>(200,Point2D3D.class,true);

	/**
	 *
	 * @param minFeatures
	 * @param setKeyThreshold
	 * @param minPixelChange
	 * @param tracker
	 * @param pixelToNormalized Pixel to calibrated normalized coordinates.  Right handed (y-axis positive is image up)
	 *                          coordinate system is assumed.
	 * @param epipolarMotion
	 * @param refineE
	 * @param computeMotion
	 * @param refineMotion
	 */
	public MonocularSimpleVo( int minFeatures , int setKeyThreshold,
							  double minPixelChange,
							  ImagePointTracker<T> tracker ,
							  PointTransform_F64 pixelToNormalized ,
							  ModelMatcher<Se3_F64,AssociatedPair> epipolarMotion ,
							  GeoModelRefine<DenseMatrix64F,AssociatedPair> refineE ,
							  ModelMatcher<Se3_F64,Point2D3D> computeMotion ,
							  GeoModelRefine<Se3_F64,Point2D3D> refineMotion )
	{
		this.minFeatures = minFeatures;
		this.setKeyThreshold = setKeyThreshold;
		this.minPixelChange = minPixelChange;
		this.tracker = new KeyFramePointTracker<T,PointPoseTrack>(tracker,pixelToNormalized,PointPoseTrack.class);
		this.epipolarMotion = epipolarMotion;
		this.refineE = refineE;
		this.computeMotion = computeMotion;
		this.refineMotion = refineMotion;

		distance = new double[ minFeatures*2 ];

		// add two observations for the two views being used in bundle adjustment
		bundleObs.add( new ViewPointObservations() );
		bundleObs.add( new ViewPointObservations() );
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
				if( estimateMotionFromEssential(false,false) ) {
					hasSpawned = false;
					mode = 2;
				}
			}
		}
	}

	private boolean estimateMotionFromEssential( boolean hasPrevious , boolean onlyActive ) {
		List<PointPoseTrack> active;

		if( onlyActive ) {
			active = new ArrayList<PointPoseTrack>();
			for( PointPoseTrack p : tracker.getPairs() ) {
				if( p.active )
					active.add(p);
			}
		} else {
			active = tracker.getPairs();
		}
		
		// initial motion estimate
		if( epipolarMotion.process((List)active) ) {
			Se3_F64 found = epipolarMotion.getModel();
			List<AssociatedPair> inliers = epipolarMotion.getMatchSet();

			inlierSize = inliers.size();

			System.out.println("    Essential inliers "+inliers.size()+"  out of "+active.size());

			// TODO hack
			DenseMatrix64F initialE = MultiViewOps.createEssential(found.getR(), found.getT());

			// refine E using non-linear optimization
			if( refineE.process(initialE,inliers,foundE) ) {
				decomposeE.decompose(foundE);

				// select best possible motion from E
				List<Se3_F64> solutions = decomposeE.getSolutions();

				Se3_F64 motionKeyToCurr = selectBestPose(solutions, inliers);
					
				if( hasPrevious ) {
					double ratios[] = new double[inliers.size()];

					int numPositive = 0;
					int priorPositive = 0;
					Point3D_F64 temp = new Point3D_F64();
					for( int i = 0; i < inliers.size(); i++ ) {
						PointPoseTrack t = (PointPoseTrack)inliers.get(i);
						triangulateAlg.triangulate(t.keyLoc,t.currLoc, motionKeyToCurr,temp);
						ratios[i] = t.location.z/temp.z;
						if( temp.z > 0 )
							numPositive++;
						if( t.location.z > 0 )
							priorPositive++;
					}
					Arrays.sort(ratios);
					double r = ratios[ratios.length/2];
					System.out.println("FOUND SCALE RATIO "+r);
					if( r < 0 ) {
						System.out.println("prior "+priorPositive+"  new "+numPositive);
						System.out.println("OH CRAP");
					}
					r = 5e-4;
					keyToCurr.set(motionKeyToCurr);
					keyToCurr.T.x *= r;
					keyToCurr.T.y *= r;
					keyToCurr.T.z *= r;
					for (PointPoseTrack t : active) {
						triangulateAlg.triangulate(t.keyLoc, t.currLoc, keyToCurr, t.location);
					}

				} else {
					keyToCurr.set(motionKeyToCurr);

					// triangular feature points
					// Adjust scale for numerical stability
					double max = 0;
					for( PointPoseTrack t : active ) {
						triangulateAlg.triangulate(t.keyLoc,t.currLoc, keyToCurr,t.location);
						t.active = true;
						if( t.location.z > max ) {
							max = t.location.z;
						}
					}
					for( PointPoseTrack t : active ) {
						t.location.x /= max;
						t.location.y /= max;
						t.location.z /= max;
					}
					keyToCurr.getT().x /= max;
					keyToCurr.getT().y /= max;
					keyToCurr.getT().z /= max;
				}

				System.out.print("   after estimate E:  ");
				computeResidualError((List)inliers);  // todo just compute error on inliers

				// refine using bundle adjustment
				performBundleAdjustment((List)inliers);

				System.out.print("   after bundle    :  ");
				computeResidualError((List)inliers);


				return true;
			}
		}
		
		return false;
	}

	// todo why doesn't numerical improve score at all?!?!
	public void performBundleAdjustment( List<PointPoseTrack> inliers ) {
		if( bundle == null )
			return;

		bundleModel.configure(2,inliers.size());
		bundleModel.setViewKnown(0,true);
		bundleModel.getWorldToCamera(1).set(keyToCurr);

		FastQueue<PointIndexObservation> v0 = bundleObs.get(0).getPoints();
		FastQueue<PointIndexObservation> v1 = bundleObs.get(1).getPoints();

		v0.reset();
		v1.reset();
		
		for( int i = 0; i < inliers.size(); i++ ) {
			PointPoseTrack p = inliers.get(i);
			
			v0.grow().set(i,p.keyLoc);
			v1.grow().set(i,p.currLoc);
			bundleModel.getPoint(i).set(p.location);
		}

		bundle.process(bundleModel,bundleObs);

		// todo remove later on when it becomes references
		keyToCurr.set(bundleModel.getWorldToCamera(1));
		for( int i = 0; i < inliers.size(); i++ ) {
			PointPoseTrack p = inliers.get(i);
			p.location.set(bundleModel.getPoint(i));
		}
	}

	/**
	 * Updates the position estimate using triangulation.
	 *
	 * @return true if successful or false if it failed
	 */
	private boolean updatePosition() {
		queuePointPose.reset();
		List<Point2D3D> active = new ArrayList<Point2D3D>();
		for( PointPoseTrack t : tracker.getPairs() ) {
			if( !t.active )
				continue;
			Point2D3D p = queuePointPose.grow();
			p.location = t.location;
			p.observation = t.currLoc;
			active.add(p);
		}
		if( queuePointPose.size <= 0 )
			return false;
		
		// estimate the camera's motion
		if( !computeMotion.process(active) ) {
			return false;
		}
		
		List<Point2D3D> inliers = computeMotion.getMatchSet();
		inlierSize = inliers.size();
		
		System.out.println("   Motion inliers "+inlierSize+"  out of "+active.size());
		
		refineMotion.process(computeMotion.getModel(), inliers,refinedM);

		keyToCurr.set(refinedM);

		System.out.print("   after PnP Refine:  ");
		if( computeResidualError(tracker.getPairs()) > outlierResidualError ) {
			System.out.println("   EMERGENCY ESSENTIAL 1");
			return estimateMotionFromEssential(true,true);
		}

		// update point positions
		int passPlusZ = 0;
		int numActive = 0;
		for( PointPoseTrack t : tracker.getPairs() ) {
			if( t.active ) {
				numActive++;
				triangulateAlg.triangulate(t.keyLoc,t.currLoc, keyToCurr,t.location);
				if( t.location.z > 0 )
					passPlusZ++;
			}
		}
		System.out.print("   after triangulate: ");
		double error = computeResidualError(tracker.getPairs());
		System.out.println("    +z "+passPlusZ+"  active "+numActive);
		// handle spawning new features
		if( !hasSpawned && inlierSize <= setKeyThreshold ) {
			if( !estimateMotionFromEssential(true,true) )
				return false;

			System.out.println("--- Setting key frame");
			for( PointPoseTrack t : tracker.getPairs() ) {
				if( t.active ) {
					t.spawnLoc.set(t.currLoc);
				}
			}
			tracker.spawnTracks();
			keyToSpawn.set(keyToCurr);
			hasSpawned = true;
		} else if( error > outlierResidualError ) {
			System.out.println("   EMERGENCY ESSENTIAL 2");
			return estimateMotionFromEssential(true,true);
		}
		System.out.println("Local Pose: "+keyToCurr.getT());
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
				t.keyLoc.set(t.spawnLoc); // t.spawnLoc is zero?
			} else {
				t.active = true;
			}
		}
		hasSpawned = false;

		// make the spawn point the new keyframe
		worldToKey.concat(keyToSpawn, temp);
		worldToKey.set(temp);

		keyToCurr.invert(temp);
		keyToSpawn.concat(temp, keyToCurr);

		// now estimate the motion and triangulate points by computing the essential matrix
		estimateMotionFromEssential(true,false);
	}

	/**
	 * When decomposing the essential matrix, several possible motion are created.  Since any
	 * object which is seen by the camera must be in front of the camera the positive depth
	 * constraint can be used to select the most likely motion from the set.
	 *
	 * @param motions List of candidate motions from key to curr
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
	
	private double computeResidualError( List<PointPoseTrack> inliers ) {
		
		Point3D_F64 currentView = new Point3D_F64();
		
		double total = 0;
		int num = 0;
		
		for( PointPoseTrack t : inliers ) {
			if( !t.active )
				continue;

			Point3D_F64 p = t.location;
			Point2D_F64 obs = t.currLoc;
			
			SePointOps_F64.transform(keyToCurr,p,currentView);
			
			double x = currentView.x/currentView.z;
			double y = currentView.y/currentView.z;

			x = x - obs.x;
			y = y - obs.y;

			total += x*x + y*y;
			
			// error in keyframe
			x = p.x/p.z;
			y = p.y/p.z;

			x = x - t.keyLoc.x;
			y = y - t.keyLoc.y;

			total += x*x + y*y;
			num++;
		}

		total /= 2.0;

//		total /= num;
//		System.out.printf("   -- residual error = %05e  N = %d\n",total,num);
		System.out.println("   -- residual error = "+total+"  N = "+num+"  ave  = "+(total/num));

		return total/num;
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
		// todo no need for quick select here, just see how many are within threshold

		double median = QuickSelectArray.select(distance,(int)(count*0.9),count);

		System.out.println("  total inactive "+count+"  total "+tracks.size()+" median "+median);

		return median >= minPixelChange * minPixelChange;
	}

	public Se3_F64 getWorldToCamera() {
		return worldToCurr;
	}

	public boolean isFatal() {
		return fatal;
	}
}
