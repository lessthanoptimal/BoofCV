package boofcv.alg.sfm;

import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;

/**
 * Computes stereo disparity on a per pixel basis as requested.
 *
 * @author Peter Abeles
 */
public class StereoSparse3D<T extends ImageSingleBand>
		extends StereoProcessingBase<T> implements ImagePixelTo3D {

	// computes spare disparity
	private StereoDisparitySparse<T> disparity;

	// convert from left camera pixel coordinates into rectified coordinates
	private PointTransform_F64 leftPixelToRect;

	// storage for rectified pixel coordinate
	private Point2D_F64 pixelRect = new Point2D_F64();

	// output 3D coordinate
	private double x,y,z,w;

	// --------- Camera Calibration parameters
	// stereo baseline
	private double baseline;
	// intrinsic parameters for rectified camera
	// skew is always set to zero in rectified camera
	private double cx,cy,fx,fy;

	/**
	 * Configures and declares internal data
	 *
	 * @param imageType   Input image type
	 */
	public StereoSparse3D(StereoDisparitySparse<T> disparity, Class<T> imageType) {
		super(imageType);
		this.disparity = disparity;
	}

	@Override
	public void setCalibration(StereoParameters stereoParam) {
		super.setCalibration(stereoParam);

		leftPixelToRect = RectifyImageOps.transformPixelToRect_F64(stereoParam.left,rect1);

		baseline = stereoParam.getBaseline();
		cx = rectK.get(0, 0);
		cy = rectK.get(1,1);
		fx = rectK.get(0,2);
		fy = rectK.get(1,2);
	}

	@Override
	public void initialize() {
		super.initialize();

		disparity.setImages(imageLeftRect,imageRightRect);
	}

	/**
	 * Takes in pixel coordinates from the left camera in the original image coordinate system
	 * @param x x-coordinate of the pixel
	 * @param y y-coordinate of the pixel
	 * @return true if successful
	 */
	@Override
	public boolean process(double x, double y) {

		leftPixelToRect.compute(x,y,pixelRect);

		if( !disparity.process((int)pixelRect.x,(int)pixelRect.y) )
			return false;

		double d = disparity.getDisparity();

		this.w = d;
		this.z = baseline*fx;
		this.x = z*(x - cx)/fx;
		this.y = z*(y - cy)/fy;

		return true;
	}

	@Override
	public double getX() {
		return x;
	}

	@Override
	public double getY() {
		return y;
	}

	@Override
	public double getZ() {
		return z;
	}

	@Override
	public double getW() {
		return w;
	}
}
