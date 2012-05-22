package boofcv.alg.sfm;

import boofcv.abst.feature.tracker.KeyFramePointTracker;
import boofcv.abst.geo.RefinePerspectiveNPoint;
import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.alg.geo.PointPositionPair;
import boofcv.factory.geo.FactoryTriangulate;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.struct.FastQueue;
import boofcv.struct.feature.ComputePixelTo3D;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

import java.util.List;

/**
 * TODO Comment
 *
 *
 *
 * @author Peter Abeles
 */
public class StereoSimpleVo<T extends ImageBase> {

	// tracks features in the image
	private KeyFramePointTracker<T,PointPoseTrack> tracker;
	// used to estimate a feature's 3D position from image range data
	private ComputePixelTo3D<T> pixelToCoordinate;

	// triangulate feature's 3D location
	private TriangulateTwoViewsCalibrated triangulate =
			FactoryTriangulate.twoGeometric();

	// estimate and refine motion
	private ModelMatcher<Se3_F64,PointPositionPair> computeMotion;
	private RefinePerspectiveNPoint refineMotion;

	// internal storage
	FastQueue<PointPositionPair> queuePointPose = new FastQueue<PointPositionPair>(200,PointPositionPair.class,true);

	// if the camera's translational motion since the last keyframe is less than this do not triangulate
	// the feature's location.  Too small of an angle will result in a noisy estimate
	double triangulateMotionThreshold;

	// if the number of tracks is less than this change the keyframe
	int minInlierTracks;

	// transform from the world frame to key frame
	Se3_F64 worldToKey;
	// transform from the key frame to the current image frame
	Se3_F64 keyToCurr;

	private boolean hadFault;

	int numFaults = -1;

	public StereoSimpleVo(KeyFramePointTracker<T, PointPoseTrack> tracker,
						  ComputePixelTo3D<T> pixelToCoordinate,
						  ModelMatcher<Se3_F64, PointPositionPair> computeMotion,
						  RefinePerspectiveNPoint refineMotion) {
		this.tracker = tracker;
		this.pixelToCoordinate = pixelToCoordinate;
		this.computeMotion = computeMotion;
		this.refineMotion = refineMotion;
	}

	public boolean process( T leftImage , T rightImage ) {
		tracker.process(leftImage);

		hadFault = false;
		boolean reset = false;

		if( updateCameraPose() ) {
			// TODO use the angle instead
			// only update if it is numerically stable
			if( keyToCurr.getT().norm() >= triangulateMotionThreshold )
				updateFeatureLocation();

			if( computeMotion.getMatchSet().size() < minInlierTracks ) {
				reset = true;
			}
		} else {
			reset = true;
			numFaults++;
		}

		if( reset ) {
			// add motion from key frame to global motion
			worldToKey = worldToKey.concat(keyToCurr,null);
			keyToCurr.reset();

			// initialize new tracks and estimate their 3D coordinates
			initializeNewTracks(leftImage,rightImage);
		}

		return hadFault;
	}

	/**
	 * Resets the 3D estimate of all features and spawns new tracks.  If a track
	 * cannot have its 3D location estimated drop it.
	 */
	protected void initializeNewTracks( T leftImage , T rightImage ) {
		// spawn new tracks and make this the key frame
		tracker.spawnTracks();
		tracker.setKeyFrame();
		// setup algorithm for estimating point 3D location
		pixelToCoordinate.setImages(leftImage,rightImage);

		// go through the list of tracks and estimate 3D location, drop tracks
		// whose 3D location cannot be estimated
		List<PointPoseTrack> tracks = tracker.getPairs();

		for( int i = 0; i < tracks.size();  ) {
			PointPoseTrack t = tracks.get(i);
			Point2D_F64 loc = t.getPixel().currLoc;

			if( pixelToCoordinate.process(loc.x,loc.y) ) {
				t.location.set( pixelToCoordinate.getX(),
						pixelToCoordinate.getY(),pixelToCoordinate.getZ());
				i++;
			} else {
				// drop tracks that it can't triangulate
				tracker.dropTrack(t);
			}
		}
	}

	/**
	 * Given the current
	 * @return
	 */
	protected boolean updateCameraPose() {
		// convert tracks into PointPositionPair
		List<PointPoseTrack> tracks = tracker.getPairs();

		queuePointPose.reset();
		for( PointPoseTrack t : tracks ) {
			PointPositionPair p = queuePointPose.pop();
			p.setLocation(t.location);
			p.setObserved(t.currLoc);
		}

		// estimate the camera's new position
		if( !computeMotion.process(queuePointPose.toList()) )
			return false;

		// refine the estimate using non-linear optimization
		List<PointPositionPair> inliers = computeMotion.getMatchSet();
		refineMotion.process(computeMotion.getModel(),inliers);

		// save the result
		keyToCurr.set(refineMotion.getRefinement());

		return true;
	}

	/**
	 * Given the new pose estimate triangulate the feature's location.
	 */
	protected void updateFeatureLocation() {
		List<PointPoseTrack> tracks = tracker.getPairs();

		Point3D_F64 location = new Point3D_F64();

		for( PointPoseTrack t : tracks ) {
			if( triangulate.triangulate(t.keyLoc,t.currLoc,keyToCurr,location) ) {
				t.location.set(location);
			}
		}
	}

	public void setMinInlierTracks(int minInlierTracks) {
		this.minInlierTracks = minInlierTracks;
	}

	public void setTriangulateMotionThreshold(double triangulateMotionThreshold) {
		this.triangulateMotionThreshold = triangulateMotionThreshold;
	}

	public int getNumFaults() {
		return numFaults;
	}

	public boolean hadFault() {
		return hadFault;
	}

	public Se3_F64 getWorldToCamera() {
		return worldToKey.concat(keyToCurr,null);
	}
}
