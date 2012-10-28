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
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * Visual odometry where a ranging device is assumed for pixels in the primary view.  Typical
 * inputs would include a stereo or depth camera.
 *
 * @author Peter Abeles
 */

// TODO Why does changing MIN_PIXEL_CHANGE affect accuracy?
// TODO Dynamically select keyframe thresholds

public class VisOdomPixelDepthPnP<T extends ImageBase> {
	// TODO Make relative to the last update or remove?
	double MIN_PIXEL_CHANGE = 100;  // nominal = 100

	double TOL_TRIANGULATE = 3*Math.PI/180.0;

	int MIN_TRACKS = 100;

	// tracks features in the image
	private KeyFramePointTracker<T,PointPoseTrack> tracker;
	// used to estimate a feature's 3D position from image range data
	private ImagePixelTo3D pixelTo3D;

	RefinePnP refine = null;//FactoryMultiView.refinePnP(1e-6, 100);
	BundleAdjustmentCalibrated bundle = null;//FactoryMultiView.bundleCalibrated(1e-8,300);

	// estimate the camera motion up to a scale factor from two sets of point correspondences
	private ModelMatcher<Se3_F64, Point2D3D> motionEstimator;

	ComputeObservationAcuteAngle computeObsAngle = new ComputeObservationAcuteAngle();

	Se3_F64 keyToWorld = new Se3_F64();
	Se3_F64 currToKey = new Se3_F64();
	Se3_F64 currToWorld = new Se3_F64();

	int numTracksUsed;
	int numOriginalUsed;

	boolean hasSignificantChange;

	int motionFailed;
	boolean inliersValid;

	boolean first = true;

	public VisOdomPixelDepthPnP(int MIN_TRACKS, ModelMatcher<Se3_F64, Point2D3D> motionEstimator,
								ImagePixelTo3D pixelTo3D,
								KeyFramePointTracker<T, PointPoseTrack> tracker )
	{
		this.MIN_TRACKS = MIN_TRACKS;
		this.motionEstimator = motionEstimator;
		this.pixelTo3D = pixelTo3D;
		this.tracker = tracker;
	}

	public void reset() {
		tracker.reset();
		keyToWorld.reset();
		currToKey.reset();
		motionFailed = 0;
		first = true;
	}

	// TODO indicate FULL_MOTION, ANGLE_ONLY,NO_MOTION,FAULT
	public boolean process( T leftImage ) {
		tracker.process(leftImage);

		inliersValid = false;

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

			System.out.println(" numTracksUsed = "+numTracksUsed+"  original "+numOriginalUsed+" total "+tracker.getPairs().size());
			if( numOriginalUsed < MIN_TRACKS/2 ) {
				setNewKeyFrame();
				inliersValid = false;
			} else if( numTracksUsed < MIN_TRACKS ) {
				addNewTracks();
			}
		}

		return true;
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
				SePointOps_F64.transform(currToKey,X,X);

				p.original = false;

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
			if( tracker.getPairs().get(index).original )
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
}
