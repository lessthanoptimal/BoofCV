package boofcv.abst.sfm;

import boofcv.abst.feature.tracker.KeyFramePointTracker;
import boofcv.abst.feature.tracker.PointTrack;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.geo.DistanceModelMonoPixels;
import boofcv.alg.sfm.AccessSfmPointTracks;
import boofcv.alg.sfm.PointPoseTrack;
import boofcv.alg.sfm.StereoSparse3D;
import boofcv.alg.sfm.VisOdomPixelDepthPnP;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class WrapVisOdomPixelDepthPnP<T extends ImageSingleBand>
		implements StereoVisualOdometry<T>, AccessSfmPointTracks {

	// low level algorithm
	VisOdomPixelDepthPnP<T> alg;
	StereoSparse3D<T> stereo;
	KeyFramePointTracker<T,PointPoseTrack> tracker;
	DistanceModelMonoPixels<Se3_F64,Point2D3D> fitError;
	Class<T> imageType;
	boolean failed;


	public WrapVisOdomPixelDepthPnP(VisOdomPixelDepthPnP<T> alg,
									StereoSparse3D<T> stereo,
									KeyFramePointTracker<T, PointPoseTrack> tracker,
									DistanceModelMonoPixels<Se3_F64, Point2D3D> fitError,
									Class<T> imageType) {
		this.alg = alg;
		this.stereo = stereo;
		this.tracker = tracker;
		this.fitError = fitError;
		this.imageType = imageType;
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
			pixels.add(t.getPixel().p2);
		}

		return pixels;
	}

	@Override
	public List<Point2D_F64> getInlierTracks() {
		return alg.getInlierPixels();
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
	public void setCalibration( StereoParameters parameters ) {
		stereo.setCalibration(parameters);

		PointTransform_F64 leftPixelToNorm = LensDistortionOps.transformRadialToNorm_F64(parameters.left);
		tracker.setPixelToNorm(leftPixelToNorm);

		fitError.setIntrinsic(parameters.left.fx, parameters.left.fy, parameters.left.skew);
	}

	@Override
	public boolean process(T leftImage, T rightImage) {
		stereo.setImages(leftImage,rightImage);
		failed = alg.process(leftImage);
		return failed;
	}

	@Override
	public boolean isFault() {
		return false;
	}

	@Override
	public Class<T> getImageType() {
		return imageType;
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
	public Se3_F64 getLeftToWorld() {
		return alg.getCurrToWorld();
	}
}
