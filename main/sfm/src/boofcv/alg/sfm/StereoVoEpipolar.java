package boofcv.alg.sfm;

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.feature.tracker.PointTrack;
import boofcv.numerics.fitting.modelset.ModelGenerator;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.struct.FastQueue;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes the camera ego motion without triangulation, eliminating a major source of error. The ego motion is
 * estimated by computing the camera motion (up to a scale factor) between the current left camera view and
 * the left + right camera view in the keyframe.  The known stereo geometry is used to eliminate the scale ambiguity.
 *
 * PROBLEMS:
 * - can't initially prune points using stereo matches since all matches are along the epipole
 * - Found transform from C to R appears to be bad
 *   * Caused by bad feature localization in right image?
 *
 *
 * @author Peter Abeles
 */
public class StereoVoEpipolar<T extends ImageSingleBand> {

	// Left Camera: Converts a pixel from pixel coordinates to rectified in the normalized image coordinates
	private PointTransform_F64 leftPixelToNorm;
	// Right Camera: Converts a pixel from rectified coordinate to normalized image coordinates
	private PointTransform_F64 rightPixelToNorm;

	// tracks point image features
	private ImagePointTracker<T> tracker;

	// Finds associated points between left and right stereo cameras
	private AssociateStereoPoint<T> stereoAssociate;

	// ------ Estimate the camera motion up to a scale factor
	// Estimates motion while removing outliers between two views of the left camera
	private ModelMatcher<Se3_F64, AssociatedPair> robustLeft;
	// Estimates motion while removing outliers between a view of the left and right camera
	private ModelMatcher<Se3_F64, AssociatedPair> robustLeft2Right;
	// Estimates motion without removing outliers
	private ModelGenerator<Se3_F64, AssociatedPair> brittleMotion;

	// resolves the unknown scale factor
	private ScaleFactorTwoRelative resolveScale;

	// minimum number of tracks used before it spawns a new keyframe
	private int minTracks;

	// Temporary storage for the location of associated image features between different views
	private FastQueue<AssociatedPair> pairs = new FastQueue<AssociatedPair>(100,AssociatedPair.class,true);

	// estimated motion from current frame to left + right key frames (up to a scale factor)
	protected Se3_F64 leftKeyToCurr = new Se3_F64();
	protected Se3_F64 rightKeyToCurr = new Se3_F64();

	// estimate camera motion from the current frame to the key frame
	private Se3_F64 currToKey = new Se3_F64();
	// estimate camera motion from the key frame to the global frame
	private Se3_F64 keyToWorld = new Se3_F64();
	// output motion found by concatenating
	private Se3_F64 currToWorld = new Se3_F64();

	// is this the first camera frame being processed?
	private boolean initialFrame = true;
	// number of tracks used to estimate the camera's motion
	private int numTracksUsed;

	Se3_F64 stereoRightToLeft;

	public StereoVoEpipolar(ModelMatcher<Se3_F64, AssociatedPair> robustLeft,
							ModelMatcher<Se3_F64, AssociatedPair> robustLeft2Right ,
							ModelGenerator<Se3_F64, AssociatedPair> brittleMotion,
							PointTransform_F64 leftPixelToNorm,
							PointTransform_F64 rightPixelToNorm,
							ImagePointTracker<T> tracker,
							AssociateStereoPoint<T> stereoAssociate,
							Se3_F64 stereoRightToLeft ,
							int minTracks )
	{
		this.robustLeft = robustLeft;
		this.robustLeft2Right = robustLeft2Right;
		this.brittleMotion = brittleMotion;
		this.leftPixelToNorm = leftPixelToNorm;
		this.rightPixelToNorm = rightPixelToNorm;
		this.tracker = tracker;
		this.stereoAssociate = stereoAssociate;
		this.stereoRightToLeft = stereoRightToLeft;
		this.minTracks = minTracks;

		resolveScale = new ScaleFactorTwoRelative(stereoRightToLeft);
	}

	public void reset() {
		initialFrame = true;
		tracker.dropTracks();
		currToKey.reset();
		keyToWorld.reset();
	}

	public boolean process( T leftImage , T rightImage ) {
		tracker.process(leftImage);

		if( initialFrame ) {
			initialFrame = false;
			spawnAssociateTracks(leftImage, rightImage);
		} else {
			// estimate motion between left keyframe and the current frame
		    if( !estimateRelativeMotion() )
				return false;

			// resolve for the scale
			if( !resolveScale.computeScaleFactor(leftKeyToCurr,rightKeyToCurr) ) {
				System.err.println("WTF scale can't be resolved?");
				return false;
			}

			leftKeyToCurr.invert(currToKey);

			// change keyframe if needed
			if( numTracksUsed < minTracks ) {
				concatMotion();
				spawnAssociateTracks(leftImage, rightImage);
			}
		}

		return true;
	}

	/**
	 * Creates a set of valid tracks.  A track are initially detected in the left image, but then matched to
	 * points in the right image.  Tracks which are not matched are dropped.
	 */
	public void spawnAssociateTracks(T leftImage, T rightImage) {
		stereoAssociate.setImages(leftImage,rightImage);
		stereoAssociate.initialize();
		tracker.spawnTracks();

		List<PointTrack> tracks = tracker.getActiveTracks();

		// list of tracks which are to be dropped
		List<PointTrack> dropList = new ArrayList<PointTrack>();

		// temporary storage for matching pixel in right image
		Point2D_F64 rightPixel = new Point2D_F64();

		for( PointTrack t : tracks ) {
			// declare track data
			STrack s;
			if( t.cookie == null ) {
				t.cookie = s = new STrack();
			} else {
				s = t.getCookie();
			}

			// see if the image can be matched to the right camera
			if( !stereoAssociate.associate(t.x,t.y,rightPixel) ) {
				// can't use the feature unless it has a known location in the right camera
				dropList.add(t);
				continue;
			}

			// update feature location in each of the views
			rightPixelToNorm.compute(rightPixel.x,rightPixel.y,s.keyRightN);
			leftPixelToNorm.compute(t.x,t.y,s.currLeftN);
			s.keyLeftN.set(s.currLeftN);
		}

		// drop all the unusable tracks
		for( PointTrack t : dropList ) {
			tracker.dropTrack(t);
		}
	}

	/**
	 * Estimate camera motion using epipolar geometry.  Scale ambiguity is resolved using the known
	 * baseline between the left and right camera.
	 *
	 * @return true if successful or false if it was not able to estimate the motion
	 */
	protected boolean estimateRelativeMotion() {
		List<PointTrack> tracks = tracker.getActiveTracks();

		pairs.reset();
		for( PointTrack t : tracks ) {
			// update normalized image coordinate of the track
			STrack s = t.getCookie();
			leftPixelToNorm.compute(t.x,t.y,s.currLeftN);

			// create an associated pair between the key frame and the current frame
			AssociatedPair p = pairs.pop();
			p.keyLoc.set(s.keyLeftN);
			p.currLoc.set(s.currLeftN);
		}

		// Estimate camera motion from key frame to current frame in left camera, up to scale
		if( !robustLeft.process(pairs.toList()) )
			return false;

		leftKeyToCurr.set(robustLeft.getModel());

		// Create a set of associated pairs from current frame to right key frame
		// Assume all features in the inlier set are good so robust estimation to remove outliers
		// will not be needed.
		pairs.reset();
		int N = numTracksUsed = robustLeft.getMatchSet().size();
		for( int i = 0; i < N; i++ ) {
			STrack s = tracks.get( robustLeft.getInputIndex(i)).getCookie();

			// create an associated pair between the key frame and the current frame
			AssociatedPair p = pairs.pop();
			p.keyLoc.set(s.keyRightN);
			p.currLoc.set(s.currLeftN);
//			p.keyLoc.set(s.currLeftN);
//			p.currLoc.set(s.keyRightN);
		}

		// estimate camera motion the right key frame to left current frame, up to scale
//		return brittleMotion.generate(pairs.toList(), rightKeyToCurr);

		if( !robustLeft2Right.process(pairs.toList()) )
			return false;

		System.out.println(" left to curr = "+numTracksUsed+" Curr to right inliers "+robustLeft2Right.getMatchSet().size()+"  total tracks "+tracks.size());

//		robustLeft2Right.getModel().invert(rightKeyToCurr);
		rightKeyToCurr.set(robustLeft2Right.getModel());

		numTracksUsed = Math.min(numTracksUsed,robustLeft2Right.getMatchSet().size());

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

	private static class STrack {
		// track location, keyframe, left camera, normalized coordinates
		Point2D_F64 keyLeftN = new Point2D_F64();
		// track location, keyframe, right camera, normalized coordinates
		Point2D_F64 keyRightN = new Point2D_F64();
		// track location, current, frame left camera, normalized coordinates
		Point2D_F64 currLeftN = new Point2D_F64();
	}
}
