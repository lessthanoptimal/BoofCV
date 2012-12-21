package boofcv.alg.sfm.d3;

import boofcv.abst.feature.tracker.ModelAssistedTracker;
import boofcv.abst.feature.tracker.PointTrack;
import boofcv.abst.feature.tracker.TrackGeometryManager;
import boofcv.abst.sfm.ImagePixelTo3D;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.ImageBase;
import boofcv.struct.sfm.Point2D3DTrack;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * Full 6-DOF visual odometry where a ranging device is assumed for pixels in the primary view and the motion is estimated
 * using a {@link boofcv.abst.geo.Estimate1ofPnP}.  Range is usually estimated using stereo cameras, structured
 * light or time of flight sensors.  New features are added and removed as needed.  Features are removed
 * if they are not part of the inlier feature set for some number of consecutive frames.  New features are detected
 * and added if the inlier set falls below a threshold or every turn.
 *
 * Non-linear refinement is optional and appears to provide a very modest improvement in performance.  It is recommended
 * that motion is estimated using a P3P algorithm, which is the minimal case.  Adding features every frame can be
 * computationally expensive, but having too few features being tracked will degrade accuracy. The algorithm was
 * designed to minimize magic numbers and to be insensitive to small changes in their values.
 *
 * Due to the level of abstraction, it can't take full advantage of the sensors used to estimate 3D feature locations.
 * For example if a stereo camera is used then 3-view geometry can't be used to improve performance.
 *
 * @author Peter Abeles
 */
// TODO remove PnP from name and comments above
public class VisOdomPixelDepthPnP<T extends ImageBase>
		implements TrackGeometryManager<Se3_F64,Point2D3D>
{

	// when the inlier set is less than this number new features are detected
	private int thresholdAdd;

	// discard tracks after they have not been in the inlier set for this many updates in a row
	private int thresholdRetire;

	// tracks features and estimates camera motion
	private ModelAssistedTracker<T, Se3_F64,Point2D3D> tracker;
	// used to estimate a feature's 3D position from image range data
	private ImagePixelTo3D pixelTo3D;
	// converts from pixel to normalized image coordinates
	private PointTransform_F64 pixelToNorm;
	// convert from normalized image coordinates to pixel
	private PointTransform_F64 normToPixel;

	// location of tracks in the image that are included in the inlier set
	private List<Point2D3DTrack> inlierTracks = new ArrayList<Point2D3DTrack>();

	// transform from key frame to world frame
	private Se3_F64 keyToWorld = new Se3_F64();
	// transform from the current camera view to the key frame
	private Se3_F64 currToKey = new Se3_F64();
	// transform from the current camera view to the world frame
	private Se3_F64 currToWorld = new Se3_F64();

	// is this the first camera view being processed?
	private boolean first = true;
	// number of frames processed.
	private long tick;

	/**
	 * Configures magic numbers and estimation algorithms.
	 *
	 * @param thresholdAdd Add new tracks when less than this number are in the inlier set.  Tracker dependent. Set to
	 *                     a value <= 0 to add features every frame.
	 * @param thresholdRetire Discard a track if it is not in the inlier set after this many updates.  Try 2
	 * @param tracker Tracks point features and estimates the camera's ego motion.
	 * @param pixelTo3D Computes the 3D location of pixels.
	 * @param pixelToNorm Converts from raw image pixels into normalized image coordinates.
	 * @param normToPixel Converts from normalized image coordinates into raw pixels
	 */
	public VisOdomPixelDepthPnP(int thresholdAdd,
								int thresholdRetire ,
								ModelAssistedTracker<T, Se3_F64,Point2D3D> tracker,
								ImagePixelTo3D pixelTo3D,
								PointTransform_F64 pixelToNorm ,
								PointTransform_F64 normToPixel )
	{
		this.thresholdAdd = thresholdAdd;
		this.thresholdRetire = thresholdRetire;
		this.pixelTo3D = pixelTo3D;
		this.tracker = tracker;
		this.pixelToNorm = pixelToNorm;
		this.normToPixel = normToPixel;

		tracker.setTrackGeometry(this);
	}

	/**
	 * Resets the algorithm into its original state
	 */
	public void reset() {
		tracker.reset();
		keyToWorld.reset();
		currToKey.reset();
		first = true;
		tick = 0;
	}

	/**
	 * Estimates the motion given the left camera image.  The latest information required by ImagePixelTo3D
	 * should be passed to the class before invoking this function.
	 *
	 * @param image Camera image.
	 * @return true if successful or false if it failed
	 */
	public boolean process( T image ) {
		tracker.process(image);

		inlierTracks.clear();

		if( first ) {
			addNewTracks();
			first = false;
		} else {
			if( !estimateMotion() ) {
				return false;
			}

			dropUnusedTracks();
			int N = tracker.getMatchSet().size();

			if( thresholdAdd <= 0 || N < thresholdAdd ) {
				changePoseToReference();
				addNewTracks();
			}

//			System.out.println("  num inliers = "+N+"  num dropped "+numDropped+" total active "+tracker.getActivePairs().size());
		}
		tick++;

		return true;
	}


	/**
	 * Updates the relative position of all points so that the current frame is the reference frame.  Mathematically
	 * this is not needed, but should help keep numbers from getting too large.
	 */
	private void changePoseToReference() {
		Se3_F64 keyToCurr = currToKey.invert(null);

		List<PointTrack> all = tracker.getAllTracks(null);

		for( PointTrack t : all ) {
			Point2D3DTrack p = t.getCookie();
			SePointOps_F64.transform(keyToCurr,p.location,p.location);
		}

		concatMotion();
	}

	/**
	 * Removes tracks which have not been included in the inlier set recently
	 *
	 * @return Number of dropped tracks
	 */
	private int dropUnusedTracks() {

		List<PointTrack> all = tracker.getAllTracks(null);
		int num = 0;

		for( PointTrack t : all ) {
			Point2D3DTrack p = t.getCookie();
			if( tick - p.lastInlier >= thresholdRetire ) {
				tracker.dropTrack(t);
				num++;
			}
		}

		return num;
	}

	/**
	 * Detects new features and computes their 3D coordinates
	 */
	private void addNewTracks() {
		pixelTo3D.initialize();
		tracker.spawnTracks();
	}

	/**
	 * Estimates motion from the set of tracks and their 3D location
	 *
	 * @return true if successful.
	 */
	private boolean estimateMotion() {
		if( !tracker.foundModel() )
			return false;

		Se3_F64 keyToCurr = tracker.getModel();
		keyToCurr.invert(currToKey);

		// mark tracks as being inliers and add to inlier list
		List<PointTrack> active = tracker.getActiveTracks(null);
		int N = tracker.getMatchSet().size();
		for( int i = 0; i < N; i++ ) {
			int index = tracker.convertMatchToActiveIndex(i);
			Point2D3DTrack t = active.get(index).getCookie();
			t.lastInlier = tick;
			inlierTracks.add( t );
		}

		return true;
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

	public ModelAssistedTracker<T, Se3_F64,Point2D3D> getTracker() {
		return tracker;
	}

	public List<Point2D3DTrack> getInlierTracks() {
		return inlierTracks;
	}

	public void setPixelToNorm(PointTransform_F64 pixelToNorm) {
		this.pixelToNorm = pixelToNorm;
	}

	public void setNormToPixel(PointTransform_F64 normToPixel) {
		this.normToPixel = normToPixel;
	}

	@Override
	public boolean handleSpawnedTrack(PointTrack track) {
		Point2D3DTrack p = track.getCookie();
		if( p == null) {
			track.cookie = p = new Point2D3DTrack();
		}

		// discard point if it can't localized
		if( !pixelTo3D.process(track.x,track.y) || pixelTo3D.getW() == 0 ) {
			return false;
		} else {
			Point3D_F64 X = p.getLocation();

			double w = pixelTo3D.getW();
			X.set(pixelTo3D.getX() / w, pixelTo3D.getY() / w, pixelTo3D.getZ() / w);

			// translate the point into the key frame
			// SePointOps_F64.transform(currToKey,X,X);
			// not needed since the current frame was just set to be the key frame

			p.lastInlier = tick;
			pixelToNorm.compute(track.x, track.y, p.observation);
			return true;
		}
	}

	@Override
	public Point2D3D extractGeometry(PointTrack track) {
		Point2D3D p = track.getCookie();

		pixelToNorm.compute( track.x , track.y , p.observation );

		return p;
	}

	@Override
	public void predict(Se3_F64 worldToCurr, PointTrack track, Point2D_F64 prediction ) {
		Point2D3D info = track.getCookie();

		Point3D_F64 curr = new Point3D_F64();
		SePointOps_F64.transform(worldToCurr,info.location,curr);

		normToPixel.compute( curr.x/curr.z , curr.y/curr.z ,prediction );
	}
}
