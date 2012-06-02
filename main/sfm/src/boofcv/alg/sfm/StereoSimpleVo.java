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
import georegression.struct.point.Vector3D_F64;
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
	private ComputePixelTo3D<T> pixelTo3D;

	// triangulate feature's 3D location
	private TriangulateTwoViewsCalibrated triangulate =
			FactoryTriangulate.twoGeometric();

	// estimate and refine motion
	private ModelMatcher<Se3_F64,PointPositionPair> computeMotion;
	private RefinePerspectiveNPoint refineMotion;

	// internal storage
	FastQueue<PointPositionPair> queuePointPose = new FastQueue<PointPositionPair>(200,PointPositionPair.class,true);

	// If the angle between two views is greater than this, triangulate the point's location
	double triangulateAngle = 5*Math.PI/180.0;

	// if the number of tracks is less than this change the keyframe
	int minInlierTracks = 30;

	// transform from the world frame to key frame
	Se3_F64 worldToKey = new Se3_F64();
	// transform from the key frame to the current image frame
	Se3_F64 keyToCurr = new Se3_F64();

	private boolean hadFault;

	int numFaults = -1;

	public StereoSimpleVo(KeyFramePointTracker<T, PointPoseTrack> tracker,
						  ComputePixelTo3D<T> pixelTo3D,
						  ModelMatcher<Se3_F64, PointPositionPair> computeMotion,
						  RefinePerspectiveNPoint refineMotion) {
		this.tracker = tracker;
		this.pixelTo3D = pixelTo3D;
		this.computeMotion = computeMotion;
		this.refineMotion = refineMotion;
	}

	public boolean process( T leftImage , T rightImage ) {
		tracker.process(leftImage);

		hadFault = false;
		boolean reset = false;

		if( updateCameraPose() ) {
			// triangulate feature locations
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

		return !hadFault;
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
		pixelTo3D.setImages(leftImage, rightImage);

		// go through the list of tracks and estimate 3D location, drop tracks
		// whose 3D location cannot be estimated
		List<PointPoseTrack> tracks = tracker.getPairs();

		for( int i = 0; i < tracks.size();  ) {
			PointPoseTrack t = tracks.get(i);
			Point2D_F64 loc = t.getPixel().currLoc;

			if( pixelTo3D.process(loc.x,loc.y) ) {
				t.location.set( pixelTo3D.getX(), pixelTo3D.getY(), pixelTo3D.getZ());
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
		if( !refineMotion.process(computeMotion.getModel(),inliers) )
			return false;

		System.out.println("PnP inlier size "+inliers.size());

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

		Vector3D_F64 XtoC1 = new Vector3D_F64();
		Vector3D_F64 XtoC2 = new Vector3D_F64();

		for( PointPoseTrack t : tracks ) {

			// vector from point to first view camera center
			XtoC1.x = -t.location.x;
			XtoC1.y = -t.location.y;
			XtoC1.z = -t.location.z;

			// vector from point to second view camera center
			XtoC2.x = -keyToCurr.T.x-t.location.x;
			XtoC2.y = -keyToCurr.T.y-t.location.y;
			XtoC2.z = -keyToCurr.T.z-t.location.z;

			double dot = XtoC1.dot(XtoC2);
			double theta = Math.acos( dot / (XtoC1.norm()*XtoC2.norm()));

			if( theta > triangulateAngle && triangulate.triangulate(t.keyLoc,t.currLoc,keyToCurr,location) ) {
				t.location.set(location);
			}
		}
	}

	public KeyFramePointTracker<T, PointPoseTrack> getTracker() {
		return tracker;
	}

	public void setMinInlierTracks(int minInlierTracks) {
		this.minInlierTracks = minInlierTracks;
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
