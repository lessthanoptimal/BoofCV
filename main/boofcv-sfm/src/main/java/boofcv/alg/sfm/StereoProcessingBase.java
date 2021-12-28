/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.alg.sfm;

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyDistortImageOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.border.BorderType;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.FMatrixRMaj;
import org.ejml.ops.ConvertMatrixData;

/**
 * Base class that configures stereo processing. Created distortion for converting image from its input image
 * into an undistorted rectified image ready for stereo processing.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class StereoProcessingBase<T extends ImageGray<T>> {

	// applied rectification to input images
	private ImageDistort<T, T> distortLeftRect;
	private ImageDistort<T, T> distortRightRect;

	// references to input images
	private T imageLeftInput;
	private T imageRightInput;

	// rectified images
	protected T imageLeftRect;
	protected T imageRightRect;

	// rectification matrices for left and right image
	protected DMatrixRMaj rect1;
	protected DMatrixRMaj rect2;

	// calibration matrix for both cameras after rectification
	protected DMatrixRMaj rectK;

	// rotation matrix for both rectified cameras
	protected DMatrixRMaj rectR;

	// storage for 3D coordinate of point in rectified reference frame
	protected Point3D_F64 pointRect = new Point3D_F64();

	// --------- Camera Calibration parameters
	// stereo baseline
	protected double baseline;
	// intrinsic parameters for rectified camera
	// skew is always set to zero in rectified camera
	protected double cx, cy, fx, fy;

	/**
	 * Declares internal data structures
	 *
	 * @param imageType Input image type
	 */
	public StereoProcessingBase( Class<T> imageType ) {

		// pre-declare input images
		imageLeftRect = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		imageRightRect = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
	}

	/**
	 * Specifies stereo parameters
	 *
	 * @param stereoParam stereo parameters
	 */
	public void setCalibration( StereoParameters stereoParam ) {
		CameraPinholeBrown left = stereoParam.getLeft();
		CameraPinholeBrown right = stereoParam.getRight();

		// adjust image size
		imageLeftRect.reshape(left.getWidth(), left.getHeight());
		imageRightRect.reshape(right.getWidth(), right.getHeight());

		// compute rectification
		RectifyCalibrated rectifyAlg = RectifyImageOps.createCalibrated();
		Se3_F64 leftToRight = stereoParam.getRightToLeft().invert(null);

		// original camera calibration matrices
		DMatrixRMaj K1 = PerspectiveOps.pinholeToMatrix(left, (DMatrixRMaj)null);
		DMatrixRMaj K2 = PerspectiveOps.pinholeToMatrix(right, (DMatrixRMaj)null);

		rectifyAlg.process(K1, new Se3_F64(), K2, leftToRight);

		// rectification matrix for each image
		rect1 = rectifyAlg.getUndistToRectPixels1();
		rect2 = rectifyAlg.getUndistToRectPixels2();
		// New calibration and rotation matrix, Both cameras are the same after rectification.
		rectK = rectifyAlg.getCalibrationMatrix();
		rectR = rectifyAlg.getRectifiedRotation();

		FMatrixRMaj rect1_F32 = new FMatrixRMaj(3, 3);
		FMatrixRMaj rect2_F32 = new FMatrixRMaj(3, 3);

		ConvertMatrixData.convert(rect1, rect1_F32);
		ConvertMatrixData.convert(rect2, rect2_F32);

		ImageType<T> imageType = imageLeftRect.getImageType();
		distortLeftRect = RectifyDistortImageOps.rectifyImage(stereoParam.left, rect1_F32, BorderType.SKIP, imageType);
		distortRightRect = RectifyDistortImageOps.rectifyImage(stereoParam.right, rect2_F32, BorderType.SKIP, imageType);

		// Compute parameters that are needed when converting to 3D
		baseline = stereoParam.getBaseline();
		fx = rectK.get(0, 0);
		fy = rectK.get(1, 1);
		cx = rectK.get(0, 2);
		cy = rectK.get(1, 2);
	}

	/**
	 * Given a coordinate of a point in the left rectified frame, compute the point's 3D
	 * coordinate in the camera's reference frame in homogeneous coordinates. To convert the coordinate
	 * into normal 3D, divide each element by the disparity.
	 *
	 * @param x x-coordinate of pixel in rectified left image
	 * @param y y-coordinate of pixel in rectified left image
	 * @param pointLeft Storage for 3D coordinate of point in homogeneous coordinates. w = disparity
	 */
	public void computeHomo3D( double x, double y, Point3D_F64 pointLeft ) {
		// Coordinate in rectified camera frame
		pointRect.z = baseline*fx;
		pointRect.x = pointRect.z*(x - cx)/fx;
		pointRect.y = pointRect.z*(y - cy)/fy;

		// rotate into the original left camera frame
		GeometryMath_F64.multTran(rectR, pointRect, pointLeft);
	}

	/**
	 * Sets the input images. Processing is delayed until {@link #initialize()} has been called.
	 *
	 * @param leftImage Left image
	 * @param rightImage Right image
	 */
	public void setImages( T leftImage, T rightImage ) {
		this.imageLeftInput = leftImage;
		this.imageRightInput = rightImage;

		// rectify input images
		distortLeftRect.apply(imageLeftInput, imageLeftRect);
		distortRightRect.apply(imageRightInput, imageRightRect);
	}

	/**
	 * Initializes stereo processing.
	 */
	public void initialize() {

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
	public DMatrixRMaj getRectK() {
		return rectK;
	}

	public DMatrixRMaj getRect1() {
		return rect1;
	}

	public DMatrixRMaj getRect2() {
		return rect2;
	}
}
