package boofcv.alg.sfm;

import boofcv.abst.feature.tracker.KeyFramePointTracker;
import boofcv.abst.geo.BundleAdjustmentCalibrated;
import boofcv.abst.geo.RefinePnP;
import boofcv.alg.geo.bundle.CalibratedPoseAndPoint;
import boofcv.alg.geo.bundle.ViewPointObservations;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.ImageBase;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;

import java.util.ArrayList;
import java.util.List;

/**
 * Visual odometry where a ranging device is assumed for pixels in the primary view.  Typical
 * inputs would include a stereo or depth camera.
 *
 * Technote discussion:
 * - Better than keyframe based approach because that technique forces a hard decision and good tracks are dropped
 *   while its hard to detect false positives just after a new keyframe has been created.  A feature can have an
 *   incorrect range/associated pair and that not be found until there has been more significant motion
 * - Before the above change MIN_PIXEL_CHANGE significantly effected performance
 *
 * @author Peter Abeles
 */

// TODO Dynamically select add tracks thresholds
//      I think during some turns it should immediately spawn new tracks.  so check the 2D distribution of the points
//      and see if it has degraded significantly since being spawned?
//      only target depletion caused by rotation

public class VisOdomPixelDepthPnP<T extends ImageBase> {
	// TODO Make relative to the last update or remove?
	double MIN_PIXEL_CHANGE = 100;  // nominal = 100

	double TOL_TRIANGULATE = 3*Math.PI/180.0;

	double MIN_FRACTION = 0.6;
	int thresholdAdd;
	int absoluteMinimum;

	int RETIRE_TRACKS = 2;

	// tracks features in the image
	private KeyFramePointTracker<T,PointPoseTrack> tracker;
	// used to estimate a feature's 3D position from image range data
	private ImagePixelTo3D pixelTo3D;

	TrackDistributionCheck trackDistribution = new TrackGaussianCheck(1);

	RefinePnP refine = null;//FactoryMultiView.refinePnP(1e-6, 100);
	BundleAdjustmentCalibrated bundle = null;//FactoryMultiView.bundleCalibrated(1e-8,300);

	// estimate the camera motion up to a scale factor from two sets of point correspondences
	private ModelMatcher<Se3_F64, Point2D3D> motionEstimator;

	private List<Point2D_F64> inlierPixels = new ArrayList<Point2D_F64>();

	ComputeObservationAcuteAngle computeObsAngle = new ComputeObservationAcuteAngle();

	Se3_F64 keyToWorld = new Se3_F64();
	Se3_F64 currToKey = new Se3_F64();
	Se3_F64 currToWorld = new Se3_F64();

	int numTracksUsed;
	int numOriginalUsed;

	boolean hasSignificantChange;

	int motionFailed;
	boolean inliersValid;

	boolean firstEstimate;
	boolean first = true;
	long tick;

	public VisOdomPixelDepthPnP(int thresholdAdd, ModelMatcher<Se3_F64, Point2D3D> motionEstimator,
								ImagePixelTo3D pixelTo3D,
								KeyFramePointTracker<T, PointPoseTrack> tracker )
	{
//		this.thresholdAdd = thresholdAdd;
		this.motionEstimator = motionEstimator;
		this.pixelTo3D = pixelTo3D;
		this.tracker = tracker;

		absoluteMinimum = thresholdAdd;//motionEstimator.getMinimumSize()*2;
	}

	public void reset() {
		tracker.reset();
		keyToWorld.reset();
		currToKey.reset();
		motionFailed = 0;
		first = true;
		tick = 0;
		firstEstimate = false;
	}

	// TODO indicate FULL_MOTION, ANGLE_ONLY,NO_MOTION,FAULT
	public boolean process( T leftImage ) {
		tracker.process(leftImage);

		inliersValid = false;
		inlierPixels.clear();

		if( !hasSignificantChange ) {
			if( !checkSignificantMotion() ) {
				return false;
			} else
				hasSignificantChange = true;
		}

		if( first ) {
			setNewKeyFrame();
			first = false;
		} else {
			if( !estimateMotion() ) {
				motionFailed++;
				return false;
			}

			if( firstEstimate ) {
//				thresholdAdd = (int)(motionEstimator.getMatchSet().size()*MIN_FRACTION);
//				if( thresholdAdd < absoluteMinimum )
					thresholdAdd = absoluteMinimum;
				firstEstimate = false;
				trackDistribution.setInitialLocation(inlierPixels);
			}

			List<PointPoseTrack> drop = dropUnusedTracks();

			int N = motionEstimator.getMatchSet().size();

			// todo on first trackDist is pointless
//			if( N < thresholdAdd || computeRotationAngle() > 0.01 || trackDistribution.checkDistribution(inlierPixels) ) {
			if( N < thresholdAdd ) {
//				if( computeRotationAngle() > 0.01 )
//					System.out.println("****** LARGE ROTATION");

				changePoseToReference();
				addNewTracks();
			}

			System.out.println("  num inliers = "+N+"  num dropped "+drop.size()+" total "+tracker.getPairs().size());

			inliersValid = false;
			tick++;
		}

		return true;
	}

	private double computeRotationAngle() {
		Se3_F64 m = motionEstimator.getModel();

		DenseMatrix64F R = m.getR();
		double euler[] = RotationMatrixGenerator.matrixToEulerXYZ(R);

		return Math.sqrt( euler[0]*euler[0] + euler[1]*euler[1]+ euler[2]*euler[2] );
	}

	/**
	 * Updates the relative position of all points so that the current frame is the reference frame.  Mathematically
	 * this is not needed, but should help keep numbers from getting too large.
	 */
	private void changePoseToReference() {
		Se3_F64 keyToCurr = currToKey.invert(null);

		List<PointPoseTrack> all = tracker.getPairs();

		for( PointPoseTrack t : all ) {
			SePointOps_F64.transform(keyToCurr,t.location,t.location);
		}

		concatMotion();
	}

	private List<PointPoseTrack> dropUnusedTracks() {
		int N = motionEstimator.getMatchSet().size();

		List<PointPoseTrack> all = tracker.getPairs();

		for( int i = 0; i < N; i++ ) {
			PointPoseTrack t = all.get( motionEstimator.getInputIndex(i));
			t.lastInlier = tick;
		}

		List<PointPoseTrack> drop = new ArrayList<PointPoseTrack>();
		for( PointPoseTrack t : all ) {
			if( tick - t.lastInlier >= RETIRE_TRACKS ) {
				drop.add(t);
			}
		}

		for( PointPoseTrack t : drop ) {
			tracker.dropTrack(t);
		}
		return drop;
	}

	private void setNewKeyFrame() {
		pixelTo3D.initialize();

		if( !first && bundle != null )
			bundleAdjustment();

		System.out.println("----------- CHANGE KEY FRAME ---------------");
		concatMotion();

		tracker.setKeyFrame();
		tracker.spawnTracks();

		List<PointPoseTrack> tracks = tracker.getPairs();
		List<PointPoseTrack> drop = new ArrayList<PointPoseTrack>();

		// estimate 3D coordinate using stereo vision
		for( PointPoseTrack p : tracks ) {
			Point2D_F64 pixel = p.getPixel().keyLoc;
			// discard point if it can't triangulate
			if( !pixelTo3D.process(pixel.x,pixel.y) || pixelTo3D.getW() == 0 ) {
				drop.add(p);
			} else {
				double w = pixelTo3D.getW();
				p.getLocation().set( pixelTo3D.getX()/w , pixelTo3D.getY()/w, pixelTo3D.getZ()/w);
				p.original = true;
				p.lastInlier = tick;

//					System.out.println("Stereo z = "+p.getLocation().getZ());
//					if( p.getLocation().z < 100 )
//						System.out.println("   * ");
			}
		}

		// drop tracks which couldn't be triangulated
		for( PointPoseTrack p : drop ) {
			tracker.dropTrack(p);
		}

		hasSignificantChange = false;
		firstEstimate = true;
	}

	private void addNewTracks() {
		System.out.println("----------- Adding new tracks ---------------");

		pixelTo3D.initialize();
		List<PointPoseTrack> spawned = tracker.spawnTracks();

		List<PointPoseTrack> drop = new ArrayList<PointPoseTrack>();

		// estimate 3D coordinate using stereo vision
		for( PointPoseTrack p : spawned ) {
			Point2D_F64 pixel = p.getPixel().keyLoc;

			// discard point if it can't triangulate
			if( !pixelTo3D.process(pixel.x,pixel.y) || pixelTo3D.getW() == 0 ) {
				drop.add(p);
			} else {
				Point3D_F64 X = p.getLocation();

				double w = pixelTo3D.getW();
				X.set(pixelTo3D.getX() / w, pixelTo3D.getY() / w, pixelTo3D.getZ() / w);

				// translate the point into the key frame
				SePointOps_F64.transform(currToKey,X,X); // todo technically not needed now

				p.original = false;
				p.lastInlier = tick;

				// create a synthetic observation in the key frame
//				Point2D_F64 s = p.getSpawnLoc();
//				s.x = X.x/X.z;
//				s.y = X.y/X.z;
			}
		}

		// drop tracks which couldn't be triangulated
		for( PointPoseTrack p : drop ) {
			tracker.dropTrack(p);
		}

		firstEstimate = true;
	}


	private boolean estimateMotion() {

		List<Point2D3D> obs = new ArrayList<Point2D3D>();

		for( PointPoseTrack t : tracker.getPairs() ) {
			Point2D3D p = new Point2D3D();

			p.location = t.getLocation();
			p.observation = t.currLoc;

			obs.add(p);
		}

		// estimate the motion up to a scale factor in translation
		if( !motionEstimator.process( obs ) )
			return false;

		Se3_F64 keyToCurr;

		if( refine != null ) {
			keyToCurr = new Se3_F64();
			if( !refine.process(motionEstimator.getModel(),motionEstimator.getMatchSet(),keyToCurr) )
				return false;
		} else {
			keyToCurr = motionEstimator.getModel();
		}

		keyToCurr.invert(currToKey);

		// update feature locations using triangulation
//		computeObsAngle.setFromAtoB(currToKey);
//		for( PointPoseTrack t : tracker.getPairs() ) {
//			if( computeObsAngle.computeAcuteAngle(t.currLoc,t.keyLoc) >= TOL_TRIANGULATE ) {
//				triangulate.triangulate(t.currLoc,t.keyLoc,currToKey,t.location);
//			}
//		}

		numOriginalUsed = 0;
		int N = motionEstimator.getMatchSet().size();
		for( int i = 0; i < N; i++ ) {
			int index = motionEstimator.getInputIndex(i);
			PointPoseTrack t = tracker.getPairs().get(index);

			inlierPixels.add( t.getPixel().currLoc );
			if( t.original )
				numOriginalUsed++;
		}

		numTracksUsed = N;
		inliersValid = true;

		return true;
	}

	// TODO only do bundle adjustment if X points have an angle greater than Y?
	private boolean bundleAdjustment() {
		List<Point2D3D> inliers = motionEstimator.getMatchSet();
		List<PointPoseTrack> all = tracker.getPairs();

		CalibratedPoseAndPoint model = new CalibratedPoseAndPoint();
		ViewPointObservations view1 = new ViewPointObservations();
		ViewPointObservations view2 = new ViewPointObservations();

		model.configure(2,inliers.size());
		model.getWorldToCamera(0).reset();
		model.getWorldToCamera(1).set(motionEstimator.getModel());
		model.setViewKnown(0,true);
		model.setViewKnown(1,false);

		int numGoodAngle = 0;

		computeObsAngle.setFromAtoB(model.getWorldToCamera(1));

		for( int i = 0; i < inliers.size(); i++ ) {
			PointPoseTrack t = all.get( motionEstimator.getInputIndex(i));

			view1.getPoints().grow().set(i,t.keyLoc);
			view2.getPoints().grow().set(i,t.currLoc);

			model.getPoint(i).set(t.getLocation());

			double acute =  computeObsAngle.computeAcuteAngle(t.keyLoc,t.currLoc);
			if( acute > TOL_TRIANGULATE )
				numGoodAngle++;
		}

		if( numGoodAngle < 5 ) {
			System.out.println("  NOOOOOO UPDATE BUNDLE");
			return true;
		}

		List<ViewPointObservations> l = new ArrayList<ViewPointObservations>();
		l.add(view1);
		l.add(view2);

		if( !bundle.process(model,l) ) {
			return false;
		}

//		for( int i = 0; i < inliers.size(); i++ ) {
//			PointPoseTrack t = all.get( motionEstimator.getInputIndex(i));
//
////			System.out.println("Before Z = "+t.getLocation().z+"  after "+model.getPoint(i).z);
//
//			t.getLocation().set( model.getPoint(i) );
//		}

		motionEstimator.getModel().getT().print();
		model.getWorldToCamera(1).getT().print();

		model.getWorldToCamera(1).invert(currToKey);

		return false;
	}

	private boolean checkSignificantMotion() {
		List<PointPoseTrack> tracks = tracker.getPairs();

		int numOver = 0;

		for( int i = 0; i < tracks.size(); i++ ) {
			AssociatedPair p = tracks.get(i).getPixel();

			if( p.keyLoc.distance2(p.currLoc) > MIN_PIXEL_CHANGE )
				numOver++;
		}
		return numOver >= tracks.size()/2;
	}

	private void concatMotion() {
		Se3_F64 temp = new Se3_F64();
		currToKey.concat(keyToWorld,temp);
		keyToWorld.set(temp);
		currToKey.reset();
	}

	public Se3_F64 getCurrToWorld() {
		currToKey.concat(keyToWorld,currToWorld);
		return currToWorld;
	}

	public KeyFramePointTracker<T, PointPoseTrack> getTracker() {
		return tracker;
	}

	public ModelMatcher<Se3_F64, Point2D3D> getMotionEstimator() {
		return motionEstimator;
	}

	public boolean isInliersValid() {
		return inliersValid;
	}

	public List<Point2D_F64> getInlierPixels() {
		return inlierPixels;
	}
}
