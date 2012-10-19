package boofcv.alg.sfm;

import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;

/**
 * Given a point in the left camera in a stereo pair find the corresponding point in the right
 * camera.  Stereo rectification is applied to input images and the match found using a sparse algorithm.
 *
 * @author Peter Abeles
 */
public class AssociateStereoPoint<T extends ImageSingleBand> extends StereoProcessingBase<T>{

	// computes disparity between left and right images
	protected StereoDisparitySparse<T> sparseDisparity;

	// Left Image: pixel into rectified pixel coordinates
	protected PointTransform_F64 leftPixelToRect;
	// Right Image: rectified pixel into pixel coordinates
	protected PointTransform_F64 rightRectToPixel;

	// temporary storage
	private Point2D_F64 rectified = new Point2D_F64();

	/**
	 * Configures the algorithm.
	 *
	 * @param sparseDisparity Computes disparity for a single point.
	 * @param imageType Input image type
	 */
	public AssociateStereoPoint(StereoDisparitySparse<T> sparseDisparity,
								Class<T> imageType ) {
		super(imageType);
		this.sparseDisparity = sparseDisparity;
	}

	@Override
	public void setCalibration(StereoParameters stereoParam) {
		super.setCalibration(stereoParam);
		leftPixelToRect = RectifyImageOps.transformPixelToRect_F64(stereoParam.left, rect1);
		rightRectToPixel = RectifyImageOps.transformRectToPixel_F64(stereoParam.right, rect2);
	}

	@Override
	public void initialize() {
		super.initialize();
		sparseDisparity.setImages(imageLeftRect,imageRightRect);
	}

	/**
	 * Given a pixel coordinate in the left image find a matching pixel in the right image.
	 * Input and output coordinates are both in the original image coordinate system.
	 *
	 * @param x X-coordinate of pixel in left image
	 * @param y Y-coordinate of pixel in left image
	 * @param rightPt Output: Pixel coordinate of match in right camera
	 * @return true if an association was found in the right image or false if it failed
	 */
	public boolean associate( double x , double y , Point2D_F64 rightPt ) {
		leftPixelToRect.compute((float)x,(float)y,rectified);

		if( !sparseDisparity.process((int)rectified.x,(int)rectified.y))
			return false;

		// location in right image is found by adding the disparity
		double rightRectX = (int)rectified.x + sparseDisparity.getDisparity();

		// convert back into regular pixel coordinates
		rightRectToPixel.compute(rightRectX,rectified.y,rectified);

		rightPt.x = rectified.x;
		rightPt.y = rectified.y;

		return true;
	}
}
