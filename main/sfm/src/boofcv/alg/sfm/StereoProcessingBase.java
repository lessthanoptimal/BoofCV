package boofcv.alg.sfm;

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;

/**
 * Base class that configures stereo processing.  Created distortion for converting image from its input image
 * into an undistorted rectified image ready for stereo processing.
 *
 * @author Peter Abeles
 */
public class StereoProcessingBase<T extends ImageSingleBand> {

	// applied rectification to input images
	private ImageDistort<T> distortLeftRect;
	private ImageDistort<T> distortRightRect;

	// references to input images
	private T imageLeftInput;
	private T imageRightInput;

	// rectified images
	protected T imageLeftRect;
	protected T imageRightRect;

	// rectification matrices for left and right image
	protected DenseMatrix64F rect1;
	protected DenseMatrix64F rect2;

	// calibration matrix for both cameras after rectification
	protected DenseMatrix64F rectK;

	// rotation matrix for both rectified cameras
	protected DenseMatrix64F rectR;

	/**
	 * Declares internal data structures
	 *
	 * @param imageType Input image type
	 */
	public StereoProcessingBase( Class<T> imageType ) {

		// pre-declare input images
		imageLeftRect = GeneralizedImageOps.createSingleBand(imageType, 1,1);
		imageRightRect = GeneralizedImageOps.createSingleBand(imageType,1,1);
	}

	/**
	 * Specifies stereo parameters
	 *
	 * @param stereoParam stereo parameters
	 */
	public void setCalibration(StereoParameters stereoParam) {
		IntrinsicParameters left = stereoParam.getLeft();
		IntrinsicParameters right = stereoParam.getRight();

		// adjust image size
		imageLeftRect.reshape(left.getWidth(), left.getHeight());
		imageRightRect.reshape(right.getWidth(), right.getHeight());

		// compute rectification
		RectifyCalibrated rectifyAlg = RectifyImageOps.createCalibrated();
		Se3_F64 leftToRight = stereoParam.getRightToLeft().invert(null);

		// original camera calibration matrices
		DenseMatrix64F K1 = PerspectiveOps.calibrationMatrix(left, null);
		DenseMatrix64F K2 = PerspectiveOps.calibrationMatrix(right, null);

		rectifyAlg.process(K1,new Se3_F64(),K2,leftToRight);

		// rectification matrix for each image
		rect1 = rectifyAlg.getRect1();
		rect2 = rectifyAlg.getRect2();
		// New calibration and rotation matrix, Both cameras are the same after rectification.
		rectK = rectifyAlg.getCalibrationMatrix();
		rectR = rectifyAlg.getRectifiedRotation();

		Class<T> imageType = imageLeftRect.getTypeInfo().getImageClass();
		distortLeftRect = RectifyImageOps.rectifyImage(stereoParam.left, rect1, imageType);
		distortRightRect = RectifyImageOps.rectifyImage(stereoParam.right, rect2, imageType);
	}

	/**
	 * Sets the input images.  Processing is delayed until {@link #initialize()} has been called.
	 *
	 * @param leftImage Left image
	 * @param rightImage Right image
	 */
	public void setImages( T leftImage , T rightImage ) {
		this.imageLeftInput = leftImage;
		this.imageRightInput = rightImage;
	}

	/**
	 * Initializes stereo processing.
	 */
	public void initialize() {
		// rectify input images
		distortLeftRect.apply(imageLeftInput, imageLeftRect);
		distortRightRect.apply(imageRightInput, imageRightRect);
	}

	/**
	 * Rectified left image
	 */
	public T getImageLeftRect() {
		return imageLeftRect;
	}

	/**
	 * Rectified right image
	 */
	public T getImageRightRect() {
		return imageRightRect;
	}

	/**
	 * Intrinsic camera calibration matrix for both cameras after rectification
	 *
	 * @return camera calibration matrix
	 */
	public DenseMatrix64F getRectK() {
		return rectK;
	}
}
