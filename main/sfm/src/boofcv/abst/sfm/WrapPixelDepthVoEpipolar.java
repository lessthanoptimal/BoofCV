package boofcv.abst.sfm;

import boofcv.abst.feature.tracker.PointTrack;
import boofcv.alg.sfm.AccessSfmPointTracks;
import boofcv.alg.sfm.PixelDepthVoEpipolar;
import boofcv.alg.sfm.PointPoseTrack;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class WrapPixelDepthVoEpipolar<T extends ImageSingleBand>
		implements StereoVisualOdometry<T>, AccessSfmPointTracks {

	// low level algorithm
	PixelDepthVoEpipolar<T> alg;

	public WrapPixelDepthVoEpipolar(PixelDepthVoEpipolar<T> alg) {
		this.alg = alg;
	}

	@Override
	public Point3D_F64 getTrackLocation(int index) {
		return alg.getTracker().getPairs().get(index).getLocation();
	}

	@Override
	public long getTrackId(int index) {
		return alg.getTracker().getPairs().get(index).getTrackID();
	}

	@Override
	public List<Point2D_F64> getAllTracks() {
		List<Point2D_F64> pixels = new ArrayList<Point2D_F64>();

		for(PointPoseTrack t : alg.getTracker().getPairs() ) {
			pixels.add(t.getPixel().currLoc);
		}

		return pixels;
	}

	@Override
	public List<Point2D_F64> getInlierTracks() {
		List<Point2D_F64> pixels = new ArrayList<Point2D_F64>();

		List<PointPoseTrack> tracks = alg.getTracker().getPairs();

		int N = alg.getMotionEstimator().getMatchSet().size();
		for( int i = 0; i < N; i++ ) {
			int index = alg.getMotionEstimator().getInputIndex(i);

			pixels.add( tracks.get(index).getPixel().currLoc );
		}

		return pixels;
	}

	@Override
	public int fromInlierToAllIndex(int inlierIndex) {
		return alg.getMotionEstimator().getInputIndex(inlierIndex);
	}

	@Override
	public List<Point2D_F64> getNewTracks() {
		List<Point2D_F64> pixels = new ArrayList<Point2D_F64>();

		for(PointTrack t : alg.getTracker().getTracker().getNewTracks() ) {
			pixels.add(t);
		}

		return pixels;
	}

	@Override
	public boolean process(T leftImage, T rightImage) {
		return alg.process(leftImage,rightImage);
	}

	@Override
	public void reset() {
		alg.reset();
	}

	@Override
	public boolean isFatal() {
		return false;
	}

	@Override
	public Se3_F64 getCameraToWorld() {
		return alg.getCurrToWorld();
	}
}
