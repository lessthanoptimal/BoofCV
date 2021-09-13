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

package boofcv.alg.geo;

import boofcv.alg.geo.impl.ImplRectifyImageOps_F32;
import boofcv.alg.geo.impl.ImplRectifyImageOps_F64;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.alg.geo.rectify.RectifyFundamental;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageDimension;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.FMatrixRMaj;
import org.jetbrains.annotations.Nullable;

/**
 * <p>
 * Operations related to rectifying stereo image pairs. Provides functions for 1) creating rectification calculation
 * algorithms, 2) rectification transforms, and 3) image distortion for rectification.
 * </p>
 *
 * <p>
 * Definition of transformed coordinate systems:
 * <dl>
 *     <dt>Pixel<dd>Original image coordinates in pixels.
 *     <dt>Rect<dd>Rectified image coordinates in pixels. Lens distortion has been removed.
 *     <dt>RectNorm<dd>Rectified image coordinates in normalized coordinates.
 * </dl>
 * </p>
 *
 * @author Peter Abeles
 */
public class RectifyImageOps {

	/**
	 * <p>
	 * Rectification for calibrated stereo pairs. Two stereo camera are considered calibrated if
	 * their baseline is known.
	 * </p>
	 *
	 * <p>
	 * After the rectification has been found it might still need to be adjusted
	 * for maximum viewing area. See fullViewLeft and allInsideLeft for adjusting the rectification.
	 * </p>
	 *
	 * @return {@link RectifyCalibrated}
	 */
	public static RectifyCalibrated createCalibrated() {
		return new RectifyCalibrated();
	}

	/**
	 * <p>
	 * Rectification for uncalibrated stereo pairs using the fundamental matrix. Uncalibrated refers
	 * to the stereo baseline being unknown. For this technique to work the fundamental matrix needs
	 * to be known very accurately. See comments in {@link RectifyFundamental} for more details.
	 * </p>
	 *
	 * <p>
	 * After the rectification has been found it might still need to be adjusted
	 * for maximum viewing area. See {@link #fullViewLeft(int, int, DMatrixRMaj, DMatrixRMaj)}
	 * and {@link #allInsideLeft(int, int, DMatrixRMaj, DMatrixRMaj)}.
	 * </p>
	 *
	 * @return {@link RectifyFundamental}
	 */
	public static RectifyFundamental createUncalibrated() {
		return new RectifyFundamental();
	}

	/**
	 * <p>
	 * Adjust the rectification based on the provided rule for filling the view.
	 * </p>
	 *
	 * <p>
	 * WARNING: There are pathological conditions where this will fail. If the new rotated image view
	 * and a pixel are parallel it will require infinite area.
	 * </p>
	 *
	 * @param paramLeft (Input) Left camera intrinsic parameters
	 * @param rectifyLeft (Input, Output) Left image transform from pixels to rectified pixels.
	 * @param rectifyRight (Input, Output) Right image transform from pixels to rectified pixels.
	 * @param rectifyK (Input, Output) Rectified intrinsic calibration matrix.
	 * @param rectifiedSize (Output, Optional) Rectified image size that maximizes usable pixels at native resolution.
	 */
	public static void adjustView( RectifyFillType approach,
								   CameraPinholeBrown paramLeft,
								   DMatrixRMaj rectifyLeft, DMatrixRMaj rectifyRight,
								   DMatrixRMaj rectifyK, @Nullable ImageDimension rectifiedSize ) {
		if (rectifiedSize == null)
			rectifiedSize = new ImageDimension();

		switch (approach) {
			case NONE -> {}
			case ALL_INSIDE_LEFT -> allInsideLeft(paramLeft, rectifyLeft, rectifyRight, rectifyK, rectifiedSize);
			case FULL_VIEW_LEFT -> fullViewLeft(paramLeft, rectifyLeft, rectifyRight, rectifyK, rectifiedSize);
		}
	}

	/**
	 * <p>
	 * Adjust the rectification such that the entire original left image can be seen and adjusts the shape
	 * of the rectified image to maximize it's area. For use with calibrated stereo images having a known baseline.
	 * Due to lens distortions it is possible for large parts of the
	 * rectified image to have no overlap with the original and will appear to be black. This can cause
	 * issues when processing the image
	 * </p>
	 *
	 * <p>
	 * WARNING: There are pathological conditions where this will fail. If the new rotated image view
	 * and a pixel are parallel it will require infinite area.
	 * </p>
	 *
	 * @param paramLeft (Input) Left camera intrinsic parameters
	 * @param rectifyLeft (Input, Output) Left image transform from pixels to rectified pixels.
	 * @param rectifyRight (Input, Output) Right image transform from pixels to rectified pixels.
	 * @param rectifyK (Input, Output) Rectified intrinsic calibration matrix.
	 * @param rectifiedSize (Output, Optional) Rectified image size that maximizes usable pixels at native resolution.
	 */
	public static void fullViewLeft( CameraPinholeBrown paramLeft,
									 DMatrixRMaj rectifyLeft, DMatrixRMaj rectifyRight,
									 DMatrixRMaj rectifyK, @Nullable ImageDimension rectifiedSize ) {
		if (rectifiedSize == null)
			rectifiedSize = new ImageDimension();

		ImplRectifyImageOps_F64.fullViewLeft(paramLeft, rectifyLeft, rectifyRight, rectifyK, rectifiedSize);
	}

	/**
	 * <p>
	 * Adjust the rectification such that the entire original left image can be seen. For use with
	 * calibrated stereo images having a known baseline. Due to lens distortions it is possible for large parts of the
	 * rectified image to have no overlap with the original and will appear to be black. This can cause
	 * issues when processing the image
	 * </p>
	 *
	 * <p>
	 * WARNING: There are pathological conditions where this will fail. If the new rotated image view
	 * and a pixel are parallel it will require infinite area.
	 * </p>
	 *
	 * @param paramLeft (Input) Left camera intrinsic parameters
	 * @param rectifyLeft (Input, Output) Left image transform from pixels to rectified pixels.
	 * @param rectifyRight (Input, Output) Right image transform from pixels to rectified pixels.
	 * @param rectifyK (Input, Output) Rectified intrinsic calibration matrix.
	 * @param rectifiedSize (Output, Optional) Rectified image size that maximizes usable pixels at native resolution.
	 */
	public static void fullViewLeft( CameraPinholeBrown paramLeft,
									 FMatrixRMaj rectifyLeft, FMatrixRMaj rectifyRight,
									 FMatrixRMaj rectifyK, @Nullable ImageDimension rectifiedSize ) {
		if (rectifiedSize == null)
			rectifiedSize = new ImageDimension();

		ImplRectifyImageOps_F32.fullViewLeft(paramLeft, rectifyLeft, rectifyRight, rectifyK, rectifiedSize);
	}

	/**
	 * <p>
	 * Adjust the rectification such that the entire original left image can be seen. For use with
	 * uncalibrated stereo images with unknown baseline.
	 * </p>
	 *
	 * <p>
	 * Input rectification matrices are overwritten with adjusted values on output.
	 * </p>
	 *
	 * @param imageWidth (Input) Width of left image.
	 * @param imageHeight (Input) Height of left image.
	 * @param rectifyLeft (Input, Output) Left image transform from pixels to rectified pixels.
	 * @param rectifyRight (Input, Output) Right image transform from pixels to rectified pixels.
	 */
	// TODO Delete this function?  It should reasonably fill the old view in most non-pathological cases
	public static void fullViewLeft( int imageWidth, int imageHeight,
									 DMatrixRMaj rectifyLeft, DMatrixRMaj rectifyRight ) {
		ImplRectifyImageOps_F64.fullViewLeft(imageWidth, imageHeight, rectifyLeft, rectifyRight);
	}

	/**
	 * <p>
	 * Adjust the rectification such that the entire original left image can be seen. For use with
	 * uncalibrated stereo images with unknown baseline.
	 * </p>
	 *
	 * <p>
	 * Input rectification matrices are overwritten with adjusted values on output.
	 * </p>
	 *
	 * @param imageWidth (Input) Width of left image.
	 * @param imageHeight (Input) Height of left image.
	 * @param rectifyLeft (Input, Output) Left image transform from pixels to rectified pixels.
	 * @param rectifyRight (Input, Output) Right image transform from pixels to rectified pixels.
	 */
	// TODO Delete this function?  It should reasonably fill the old view in most non-pathological cases
	public static void fullViewLeft( int imageWidth, int imageHeight,
									 FMatrixRMaj rectifyLeft, FMatrixRMaj rectifyRight ) {
		ImplRectifyImageOps_F32.fullViewLeft(imageWidth, imageHeight, rectifyLeft, rectifyRight);
	}

	/**
	 * <p>
	 * Adjust the rectification such that only pixels which overlap the original left image can be seen. For use with
	 * calibrated stereo images having a known baseline. Image processing is easier since only the "true" image pixels
	 * are visible, but information along the image border has been discarded. The rectification matrices are
	 * overwritten with adjusted values on output.
	 * </p>
	 *
	 * @param paramLeft (Input) Left camera intrinsic parameters
	 * @param rectifyLeft (Input, Output) Left image transform from pixels to rectified pixels.
	 * @param rectifyRight (Input, Output) Right image transform from pixels to rectified pixels.
	 * @param rectifyK (Input, Output) Rectified intrinsic calibration matrix.
	 * @param rectifiedSize (Output, Optional) Rectified image size that maximizes usable pixels at native resolution.
	 */
	public static void allInsideLeft( CameraPinholeBrown paramLeft,
									  DMatrixRMaj rectifyLeft, DMatrixRMaj rectifyRight,
									  DMatrixRMaj rectifyK,
									  @Nullable ImageDimension rectifiedSize ) {
		if (rectifiedSize == null)
			rectifiedSize = new ImageDimension();

		ImplRectifyImageOps_F64.allInsideLeft(paramLeft, rectifyLeft, rectifyRight, rectifyK, rectifiedSize);
	}

	/**
	 * <p>
	 * Adjust the rectification such that only pixels which overlap the original left image can be seen. For use with
	 * calibrated stereo images having a known baseline. Image processing is easier since only the "true" image pixels
	 * are visible, but information along the image border has been discarded. The rectification matrices are
	 * overwritten with adjusted values on output.
	 * </p>
	 *
	 * @param paramLeft (Input) Left camera intrinsic parameters
	 * @param rectifyLeft (Input, Output) Left image transform from pixels to rectified pixels.
	 * @param rectifyRight (Input, Output) Right image transform from pixels to rectified pixels.
	 * @param rectifyK (Input, Output) Rectified intrinsic calibration matrix.
	 * @param rectifiedSize (Output, Optional) Rectified image size that maximizes usable pixels at native resolution. Modified.
	 */
	public static void allInsideLeft( CameraPinholeBrown paramLeft,
									  FMatrixRMaj rectifyLeft, FMatrixRMaj rectifyRight,
									  FMatrixRMaj rectifyK,
									  @Nullable ImageDimension rectifiedSize ) {
		if (rectifiedSize == null)
			rectifiedSize = new ImageDimension();
		ImplRectifyImageOps_F32.allInsideLeft(paramLeft, rectifyLeft, rectifyRight, rectifyK, rectifiedSize);
	}

	/**
	 * <p>
	 * Adjust the rectification such that only pixels which overlap the original left image can be seen. For use with
	 * uncalibrated images with unknown baselines. Image processing is easier since only the "true" image pixels
	 * are visible, but information along the image border has been discarded. The rectification matrices are
	 * overwritten with adjusted values on output.
	 * </p>
	 *
	 * @param imageWidth (Input) Width of left image.
	 * @param imageHeight (Input) Height of left image.
	 * @param rectifyLeft (Input, Output) Left image transform from pixels to rectified pixels.
	 * @param rectifyRight (Input, Output) Right image transform from pixels to rectified pixels.
	 */
	public static void allInsideLeft( int imageWidth, int imageHeight,
									  DMatrixRMaj rectifyLeft, DMatrixRMaj rectifyRight ) {
		ImplRectifyImageOps_F64.allInsideLeft(imageWidth, imageHeight, rectifyLeft, rectifyRight);
	}

	/**
	 * <p>
	 * Adjust the rectification such that only pixels which overlap the original left image can be seen. For use with
	 * uncalibrated images with unknown baselines. Image processing is easier since only the "true" image pixels
	 * are visible, but information along the image border has been discarded. The rectification matrices are
	 * overwritten with adjusted values on output.
	 * </p>
	 *
	 * @param imageWidth (Input) Width of left image.
	 * @param imageHeight (Input) Height of left image.
	 * @param rectifyLeft (Input, Output) Left image transform from pixels to rectified pixels.
	 * @param rectifyRight (Input, Output) Right image transform from pixels to rectified pixels.
	 */
	public static void allInsideLeft( int imageWidth, int imageHeight,
									  FMatrixRMaj rectifyLeft, FMatrixRMaj rectifyRight ) {
		ImplRectifyImageOps_F32.allInsideLeft(imageWidth, imageHeight, rectifyLeft, rectifyRight);
	}

	/**
	 * <p>
	 * Creates a transform that goes from rectified to original distorted pixel coordinates.
	 * Rectification includes removal of lens distortion. Used for rendering rectified images.
	 * </p>
	 *
	 * @param param (Input) Intrinsic parameters.
	 * @param rectify (Input) Transform from pixels to rectified pixels
	 * @return Transform from rectified to unrectified pixels
	 */
	public static Point2Transform2_F64 transformRectToPixel( CameraPinholeBrown param,
															 DMatrixRMaj rectify ) {
		return ImplRectifyImageOps_F64.transformRectToPixel(param, rectify);
	}

	/**
	 * <p>
	 * Creates a transform that goes from rectified to original distorted pixel coordinates.
	 * Rectification includes removal of lens distortion. Used for rendering rectified images.
	 * </p>
	 *
	 * @param param (Input) Intrinsic parameters.
	 * @param rectify (Input) Transform from pixels to rectified pixels
	 * @return Transform from rectified to unrectified pixels
	 */
	public static Point2Transform2_F32 transformRectToPixel( CameraPinholeBrown param,
															 FMatrixRMaj rectify ) {
		return ImplRectifyImageOps_F32.transformRectToPixel(param, rectify);
	}

	/**
	 * <p>
	 * Creates a transform that applies rectification to unrectified distorted pixels.
	 * </p>
	 *
	 * @param param (Input) Intrinsic parameters.
	 * @param rectify (Input) Transform from pixels to rectified pixels
	 * @return Transform from distorted pixel to rectified pixels
	 */
	public static Point2Transform2_F64 transformPixelToRect( CameraPinholeBrown param,
															 DMatrixRMaj rectify ) {
		return ImplRectifyImageOps_F64.transformPixelToRect(param, rectify);
	}

	/**
	 * <p>
	 * Creates a transform that applies rectification to unrectified distorted pixels.
	 * </p>
	 *
	 * @param param (Input) Intrinsic parameters.
	 * @param rectify (Input) Transform from pixels to rectified pixels
	 * @return Transform from distorted pixel to rectified pixels
	 */
	public static Point2Transform2_F32 transformPixelToRect( CameraPinholeBrown param,
															 FMatrixRMaj rectify ) {
		return ImplRectifyImageOps_F32.transformPixelToRect(param, rectify);
	}

	/**
	 * <p>
	 * Creates a transform that applies rectification to unrectified distorted pixels and outputs
	 * normalized pixel coordinates.
	 * </p>
	 *
	 * @param param (Input) Intrinsic parameters.
	 * @param rectify (Input) Transform from pixels to rectified pixels
	 * @param rectifyK (Input) Camera calibration matrix after rectification
	 * @return Transform from unrectified to rectified normalized pixels
	 */
	public static Point2Transform2_F64 transformPixelToRectNorm( CameraPinholeBrown param,
																 DMatrixRMaj rectify,
																 DMatrixRMaj rectifyK ) {
		return ImplRectifyImageOps_F64.transformPixelToRectNorm(param, rectify, rectifyK);
	}

	/**
	 * <p>
	 * Creates a transform that applies rectification to unrectified distorted pixels and outputs
	 * normalized pixel coordinates.
	 * </p>
	 *
	 * @param param (Input) Intrinsic parameters.
	 * @param rectify (Input) Transform from pixels to rectified pixels
	 * @param rectifyK Camera calibration matrix after rectification
	 * @return Transform from unrectified to rectified normalized pixels
	 */
	public static Point2Transform2_F32 transformPixelToRectNorm( CameraPinholeBrown param,
																 FMatrixRMaj rectify,
																 FMatrixRMaj rectifyK ) {
		return ImplRectifyImageOps_F32.transformPixelToRectNorm(param, rectify, rectifyK);
	}

	/**
	 * Applies a mask which indicates which pixels had mappings to the unrectified image. Pixels which were
	 * outside of the original image will be set to 255. The border is extended because the sharp edge
	 * in the rectified image can cause in incorrect match between image features.
	 *
	 * @param disparity (Input) disparity
	 * @param mask (Input) mask. 1 = mapping to unrectified. 0 = no mapping
	 * @param radius How much the border is extended by
	 */
	public static void applyMask( GrayF32 disparity, GrayU8 mask, int radius ) {
		if (disparity.isSubimage() || mask.isSubimage())
			throw new RuntimeException("Input is subimage. Currently not support but no reason why it can't be. Ask for it");

		int N = disparity.width*disparity.height;
		for (int i = 0; i < N; i++) {
			if (mask.data[i] == 0) {
				disparity.data[i] = 255;
			}
		}

		// TODO make this more efficient and correct. Update unit test
		if (radius > 0) {
			int r = radius;
			for (int y = r; y < mask.height - r - 1; y++) {
				int indexMsk = y*mask.stride + r;
				for (int x = r; x < mask.width - r - 1; x++, indexMsk++) {
					int deltaX = mask.data[indexMsk] - mask.data[indexMsk + 1];
					int deltaY = mask.data[indexMsk] - mask.data[indexMsk + mask.stride];

					if (deltaX != 0 || deltaY != 0) {
						// because of how the border is detected it has a bias when going from up to down
						if (deltaX < 0)
							deltaX = 0;
						if (deltaY < 0)
							deltaY = 0;
						for (int i = -r; i <= r; i++) {
							for (int j = -r; j <= r; j++) {
								disparity.set(deltaX + x + j, deltaY + y + i, 255);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Applies a mask which indicates which pixels had mappings to the unrectified image. Pixels which were
	 * outside of the original image will be set to 255. The border is extended because the sharp edge
	 * can confuse disparity algorithms.
	 *
	 * @param disparity (Input) disparity
	 * @param mask (Input) mask. 1 = mapping to unrectified. 0 = no mapping
	 * @param radius How much the border is extended by
	 */
	public static void applyMask( GrayU8 disparity, GrayU8 mask, int radius ) {
		if (disparity.isSubimage() || mask.isSubimage())
			throw new RuntimeException("Input is subimage. Currently not support but no reason why it can't be. Ask for it");

		int N = disparity.width*disparity.height;
		for (int i = 0; i < N; i++) {
			if (mask.data[i] == 0) {
				disparity.data[i] = (byte)255;
			}
		}

		// TODO make this more efficient and correct. Update unit test
		if (radius > 0) {
			int r = radius;
			for (int y = r; y < mask.height - r - 1; y++) {
				int indexMsk = y*mask.stride + r;
				for (int x = r; x < mask.width - r - 1; x++, indexMsk++) {
					int deltaX = mask.data[indexMsk] - mask.data[indexMsk + 1];
					int deltaY = mask.data[indexMsk] - mask.data[indexMsk + mask.stride];

					if (deltaX != 0 || deltaY != 0) {
						// because of how the border is detected it has a bias when going from up to down
						if (deltaX < 0)
							deltaX = 0;
						if (deltaY < 0)
							deltaY = 0;
						for (int i = -r; i <= r; i++) {
							for (int j = -r; j <= r; j++) {
								disparity.set(deltaX + x + j, deltaY + y + i, 255);
							}
						}
					}
				}
			}
		}
	}
}
