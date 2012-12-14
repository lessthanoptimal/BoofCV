package boofcv.abst.sfm;

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.feature.tracker.PointTrack;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.geo.DistanceModelMonoPixels;
import boofcv.alg.sfm.StereoSparse3D;
import boofcv.alg.sfm.d3.VisOdomPixelDepthPnP;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.sfm.Point2D3DTrack;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class WrapVisOdomPixelDepthPnP<T extends ImageSingleBand>
		implements StereoVisualOdometry<T>, AccessPointTracks3D {

	// low level algorithm
	VisOdomPixelDepthPnP<T> alg;
	StereoSparse3D<T> stereo;
	ImagePointTracker<T> tracker;
	DistanceModelMonoPixels<Se3_F64,Point2D3D> fitError;
	Class<T> imageType;
	boolean failed;


	public WrapVisOdomPixelDepthPnP(VisOdomPixelDepthPnP<T> alg,
									StereoSparse3D<T> stereo,
									ImagePointTracker<T> tracker,
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
		PointTrack t = alg.getTracker().getActiveTracks(null).get(index);
		return ((Point2D3D)t.getCookie()).getLocation();
	}

	@Override
	public long getTrackId(int index) {
		PointTrack t = alg.getTracker().getActiveTracks(null).get(index);
		return t.featureId;
	}

	@Override
	public List<Point2D_F64> getAllTracks() {
		return (List)alg.getTracker().getActiveTracks(null);
	}

	@Override
	public boolean isInlier(int index) {
		Point2D3DTrack t = alg.getTracker().getActiveTracks(null).get(index).getCookie();
		return alg.getInlierTracks().contains(t);
	}

	@Override
	public boolean isNew(int index) {
		PointTrack t = alg.getTracker().getActiveTracks(null).get(index);
		return alg.getTracker().getNewTracks(null).contains(t);
	}

	@Override
	public void setCalibration( StereoParameters parameters ) {
		stereo.setCalibration(parameters);

		PointTransform_F64 leftPixelToNorm = LensDistortionOps.transformRadialToNorm_F64(parameters.left);
		PointTransform_F64 leftNormToPixel = LensDistortionOps.transformNormToRadial_F64(parameters.left);

		alg.setPixelToNorm(leftPixelToNorm);
		alg.setNormToPixel(leftNormToPixel);

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
