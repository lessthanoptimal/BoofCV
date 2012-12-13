package boofcv.alg.sfm;

import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.abst.sfm.ImagePixelTo3D;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.image.ImageSingleBand;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;

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

	// 3D coordinate in rectified left camera
	private Point3D_F64 pointRect = new Point3D_F64();

	// 3D coordinate in the left camera
	private Point3D_F64 pointLeft = new Point3D_F64();

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
		fx = rectK.get(0,0);
		fy = rectK.get(1,1);
		cx = rectK.get(0,2);
		cy = rectK.get(1,2);
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

		// round to the nearest pixel
		if( !disparity.process((int)(pixelRect.x+0.5),(int)(pixelRect.y+0.5)) )
			return false;

		// Coordinate in rectified camera frame
		this.w = disparity.getDisparity();
		pointRect.z = baseline*fx;
		pointRect.x = pointRect.z*(pixelRect.x - cx)/fx;
		pointRect.y = pointRect.z*(pixelRect.y - cy)/fy;

		// rotate into the original left camera frame
		GeometryMath_F64.multTran(rectR,pointRect,pointLeft);

		this.x = pointLeft.x;
		this.y = pointLeft.y;
		this.z = pointLeft.z;

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
