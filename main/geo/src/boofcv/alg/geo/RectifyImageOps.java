/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.distort.*;
import boofcv.alg.distort.pinhole.PinholePtoN_F64;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.alg.geo.rectify.RectifyFundamental;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.core.image.border.BorderType;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.distort.SequencePoint2Transform2_F32;
import boofcv.struct.distort.SequencePoint2Transform2_F64;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.shapes.RectangleLength2D_F32;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.simple.SimpleMatrix;

import static boofcv.alg.distort.LensDistortionOps.transformPoint;

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
 *     <dt>Rect<dd>Rectified image coordinates in pixels.  Lens distortion has been removed.
 *     <dt>RectNorm<dd>Rectified image coordinates in normalized coordinates.
 * </dl>
 * </p>
 *
 * @author Peter Abeles
 */
public class RectifyImageOps {

	/**
	 * <p>
	 * Rectification for calibrated stereo pairs.  Two stereo camera care considered calibrated if
	 * their baseline is known.
	 * </p>
	 *
	 * <p>
	 * After the rectification has been found it might still need to be adjusted
	 * for maximum viewing area.  See fullViewLeft and allInsideLeft for adjusting the rectification.
	 * </p>
	 *
	 * @return {@link RectifyCalibrated}
	 */
	public static RectifyCalibrated createCalibrated() {
		return new RectifyCalibrated();
	}

	/**
	 * <p>
	 * Rectification for uncalibrated stereo pairs using the fundamental matrix.  Uncalibrated refers
	 * to the stereo baseline being unknown.  For this technique to work the fundamental matrix needs
	 * to be known very accurately.  See comments in {@link RectifyFundamental} for more details.
	 * </p>
	 *
	 <p>
	 * After the rectification has been found it might still need to be adjusted
	 * for maximum viewing area.  See {@link #fullViewLeft(int, int, org.ejml.data.DenseMatrix64F, org.ejml.data.DenseMatrix64F)}
	 * and {@link #allInsideLeft(int, int, org.ejml.data.DenseMatrix64F, org.ejml.data.DenseMatrix64F)}.
	 * </p>
	 *
	 * @return {@link RectifyFundamental}
	 */
	public static RectifyFundamental createUncalibrated() {
		return new RectifyFundamental();
	}

	/**
	 * <p>
	 * Adjust the rectification such that the entire original left image can be seen.  For use with
	 * calibrated stereo images having a known baseline.  Due to lens distortions it is possible for large parts of the
	 * rectified image to have no overlap with the original and will appear to be black.  This can cause
	 * issues when processing the image
	 * </p>
	 *
	 * <p>
	 * WARNING: There are pathological conditions where this will fail.  If the new rotated image view
	 * and a pixel are parallel it will require infinite area.
	 * </p>
	 *
	 * @param paramLeft Intrinsic parameters for left camera. Not modified.
	 * @param rectifyLeft Rectification matrix for left image. Input and Output. Modified.
	 * @param rectifyRight Rectification matrix for right image. Input and Output. Modified.
	 * @param rectifyK Rectification calibration matrix. Input and Output. Modified.
	 */
	// TODO Delete this function?  It should reasonably fill the old view in most non-pathological cases
	public static void fullViewLeft(CameraPinholeRadial paramLeft,
									DenseMatrix64F rectifyLeft, DenseMatrix64F rectifyRight,
									DenseMatrix64F rectifyK)
	{
		// need to take in account the order in which image distort will remove rectification later on
		paramLeft = new CameraPinholeRadial(paramLeft);

		Point2Transform2_F32 tranLeft = transformPixelToRect_F32(paramLeft, rectifyLeft);

		RectangleLength2D_F32 bound = DistortImageOps.boundBox_F32(paramLeft.width, paramLeft.height,
				new PointToPixelTransform_F32(tranLeft));

		double scaleX = paramLeft.width/bound.width;
		double scaleY = paramLeft.height/bound.height;

		double scale = Math.min(scaleX, scaleY);

		adjustCalibrated(rectifyLeft, rectifyRight, rectifyK, bound, scale);
	}

	/**
	 * <p>
	 * Adjust the rectification such that the entire original left image can be seen.  For use with
	 * uncalibrated stereo images with unknown baseline.
	 * </p>
	 *
	 * <p>
	 * Input rectification matrices are overwritten with adjusted values on output.
	 * </p>
	 *
	 * @param imageWidth Width of left image.
	 * @param imageHeight Height of left image.
	 * @param rectifyLeft Rectification matrix for left image. Input and Output. Modified.
	 * @param rectifyRight Rectification matrix for right image. Input and Output. Modified.
	 */
	// TODO Delete this function?  It should reasonably fill the old view in most non-pathological cases
	public static void fullViewLeft(int imageWidth,int imageHeight,
									DenseMatrix64F rectifyLeft, DenseMatrix64F rectifyRight )
	{
		Point2Transform2_F32 tranLeft = new PointTransformHomography_F32(rectifyLeft);

		RectangleLength2D_F32 bound = DistortImageOps.boundBox_F32(imageWidth, imageHeight,
				new PointToPixelTransform_F32(tranLeft));

		double scaleX = imageWidth/bound.width;
		double scaleY = imageHeight/bound.height;

		double scale = Math.min(scaleX,scaleY);

		adjustUncalibrated(rectifyLeft, rectifyRight, bound, scale);
	}

	/**
	 * <p>
	 * Adjust the rectification such that only pixels which overlap the original left image can be seen.  For use with
	 * calibrated stereo images having a known baseline. Image processing is easier since only the "true" image pixels
	 * are visible, but information along the image border has been discarded.  The rectification matrices are
	 * overwritten with adjusted values on output.
	 * </p>
	 *
	 * @param paramLeft Intrinsic parameters for left camera. Not modified.
	 * @param rectifyLeft Rectification matrix for left image. Input and Output. Modified.
	 * @param rectifyRight Rectification matrix for right image. Input and Output. Modified.
	 * @param rectifyK Rectification calibration matrix. Input and Output. Modified.
	 */
	public static void allInsideLeft(CameraPinholeRadial paramLeft,
									 DenseMatrix64F rectifyLeft, DenseMatrix64F rectifyRight,
									 DenseMatrix64F rectifyK)
	{
		// need to take in account the order in which image distort will remove rectification later on
		paramLeft = new CameraPinholeRadial(paramLeft);

		Point2Transform2_F32 tranLeft = transformPixelToRect_F32(paramLeft, rectifyLeft);

		RectangleLength2D_F32 bound = LensDistortionOps.boundBoxInside(paramLeft.width, paramLeft.height,
				new PointToPixelTransform_F32(tranLeft));

		LensDistortionOps.roundInside(bound);

		double scaleX = paramLeft.width/(double)bound.width;
		double scaleY = paramLeft.height/(double)bound.height;

		double scale = Math.max(scaleX, scaleY);

		adjustCalibrated(rectifyLeft, rectifyRight, rectifyK, bound, scale);
	}

	/**
	 * <p>
	 * Adjust the rectification such that only pixels which overlap the original left image can be seen.  For use with
	 * uncalibrated images with unknown baselines.  Image processing is easier since only the "true" image pixels
	 * are visible, but information along the image border has been discarded.  The rectification matrices are
	 * overwritten with adjusted values on output.
	 * </p>
	 *
	 * @param imageWidth Width of left image.
	 * @param imageHeight Height of left image.
	 * @param rectifyLeft Rectification matrix for left image. Input and Output. Modified.
	 * @param rectifyRight Rectification matrix for right image. Input and Output. Modified.
	 */
	public static void allInsideLeft( int imageWidth,int imageHeight,
									  DenseMatrix64F rectifyLeft, DenseMatrix64F rectifyRight )
	{
		Point2Transform2_F32 tranLeft = new PointTransformHomography_F32(rectifyLeft);

		RectangleLength2D_F32 bound = LensDistortionOps.boundBoxInside(imageWidth, imageHeight,
				new PointToPixelTransform_F32(tranLeft));

		double scaleX = imageWidth/(double)bound.width;
		double scaleY = imageHeight/(double)bound.height;

		double scale = Math.max(scaleX, scaleY);

		adjustUncalibrated(rectifyLeft, rectifyRight, bound, scale);
	}

	/**
	 * Internal function which applies the rectification adjustment to a calibrated stereo pair
	 */
	private static void adjustCalibrated(DenseMatrix64F rectifyLeft, DenseMatrix64F rectifyRight,
										 DenseMatrix64F rectifyK,
										 RectangleLength2D_F32 bound, double scale) {
		// translation
		double deltaX = -bound.x0*scale;
		double deltaY = -bound.y0*scale;

		// adjustment matrix
		SimpleMatrix A = new SimpleMatrix(3,3,true,scale,0,deltaX,0,scale,deltaY,0,0,1);
		SimpleMatrix rL = SimpleMatrix.wrap(rectifyLeft);
		SimpleMatrix rR = SimpleMatrix.wrap(rectifyRight);
		SimpleMatrix K = SimpleMatrix.wrap(rectifyK);

		// remove previous calibration matrix
		SimpleMatrix K_inv = K.invert();
		rL = K_inv.mult(rL);
		rR = K_inv.mult(rR);

		// compute new calibration matrix and apply it
		K = A.mult(K);

		rectifyK.set(K.getMatrix());
		rectifyLeft.set(K.mult(rL).getMatrix());
		rectifyRight.set(K.mult(rR).getMatrix());
	}

	/**
	 * Internal function which applies the rectification adjustment to an uncalibrated stereo pair
	 */
	private static void adjustUncalibrated(DenseMatrix64F rectifyLeft, DenseMatrix64F rectifyRight,
										   RectangleLength2D_F32 bound, double scale) {
		// translation
		double deltaX = -bound.x0*scale;
		double deltaY = -bound.y0*scale;

		// adjustment matrix
		SimpleMatrix A = new SimpleMatrix(3,3,true,scale,0,deltaX,0,scale,deltaY,0,0,1);
		SimpleMatrix rL = SimpleMatrix.wrap(rectifyLeft);
		SimpleMatrix rR = SimpleMatrix.wrap(rectifyRight);

		rectifyLeft.set(A.mult(rL).getMatrix());
		rectifyRight.set(A.mult(rR).getMatrix());
	}


	/**
	 * <p>
	 * Creates a transform that goes from rectified to original distorted pixel coordinates.
	 * Rectification includes removal of lens distortion.  Used for rendering rectified images.
	 * </p>
	 *
	 * @param param Intrinsic parameters.
	 * @param rectify Transform for rectifying the image.
	 * @return Transform from rectified to unrectified pixels
	 */
	public static Point2Transform2_F32 transformRectToPixel_F32(CameraPinholeRadial param,
																DenseMatrix64F rectify)
	{
		Point2Transform2_F32 add_p_to_p = transformPoint(param).distort_F32(true, true);

		DenseMatrix64F rectifyInv = new DenseMatrix64F(3,3);
		CommonOps.invert(rectify,rectifyInv);
		PointTransformHomography_F32 removeRect = new PointTransformHomography_F32(rectifyInv);

		return new SequencePoint2Transform2_F32(removeRect,add_p_to_p);
	}

	/**
	 * <p>
	 * Creates a transform that goes from rectified to original distorted pixel coordinates.
	 * Rectification includes removal of lens distortion.  Used for rendering rectified images.
	 * </p>
	 *
	 * @param param Intrinsic parameters.
	 * @param rectify Transform for rectifying the image.
	 * @return Transform from rectified to unrectified pixels
	 */
	public static Point2Transform2_F64 transformRectToPixel_F64(CameraPinholeRadial param,
																DenseMatrix64F rectify)
	{
		Point2Transform2_F64 add_p_to_p = transformPoint(param).distort_F64(true, true);

		DenseMatrix64F rectifyInv = new DenseMatrix64F(3,3);
		CommonOps.invert(rectify,rectifyInv);
		PointTransformHomography_F64 removeRect = new PointTransformHomography_F64(rectifyInv);

		return new SequencePoint2Transform2_F64(removeRect,add_p_to_p);
	}

	/**
	 * <p>
	 * Creates a transform that applies rectification to unrectified distorted pixels.
	 * </p>
	 *
	 * @param param Intrinsic parameters. Not modified.
	 * @param rectify Transform for rectifying the image. Not modified.
	 * @return Transform from unrectified to rectified pixels
	 */
	public static Point2Transform2_F32 transformPixelToRect_F32(CameraPinholeRadial param,
																DenseMatrix64F rectify)
	{
		Point2Transform2_F32 remove_p_to_p = transformPoint(param).undistort_F32(true, true);

		PointTransformHomography_F32 rectifyPixel = new PointTransformHomography_F32(rectify);

		return new SequencePoint2Transform2_F32(remove_p_to_p,rectifyPixel);
	}

	/**
	 * <p>
	 * Creates a transform that applies rectification to unrectified distorted pixels.
	 * </p>
	 *
	 * @param param Intrinsic parameters. Not modified.
	 * @param rectify Transform for rectifying the image. Not modified.
	 * @return Transform from distorted pixel to rectified pixels
	 */
	public static Point2Transform2_F64 transformPixelToRect_F64(CameraPinholeRadial param,
																DenseMatrix64F rectify)
	{
		Point2Transform2_F64 remove_p_to_p = transformPoint(param).undistort_F64(true, true);

		PointTransformHomography_F64 rectifyDistort = new PointTransformHomography_F64(rectify);

		return new SequencePoint2Transform2_F64(remove_p_to_p,rectifyDistort);
	}

	/**
	 * <p>
	 * Creates a transform that applies rectification to unrectified distorted pixels and outputs
	 * normalized pixel coordinates.
	 * </p>
	 *
	 * @param param Intrinsic parameters.
	 * @param rectify Transform for rectifying the image.
	 * @param rectifyK Camera calibration matrix after rectification
	 * @return Transform from unrectified to rectified normalized pixels
	 */
	public static Point2Transform2_F64 transformPixelToRectNorm_F64(CameraPinholeRadial param,
																	DenseMatrix64F rectify,
																	DenseMatrix64F rectifyK) {
		if (rectifyK.get(0, 1) != 0)
			throw new IllegalArgumentException("Skew should be zero in rectified images");

		Point2Transform2_F64 remove_p_to_p = transformPoint(param).undistort_F64(true, true);

		PointTransformHomography_F64 rectifyDistort = new PointTransformHomography_F64(rectify);

		PinholePtoN_F64 pixelToNorm = new PinholePtoN_F64();
		pixelToNorm.set(rectifyK.get(0, 0), rectifyK.get(1, 1),
				rectifyK.get(0, 1),
				rectifyK.get(0, 2), rectifyK.get(1, 2));

		return new SequencePoint2Transform2_F64(remove_p_to_p, rectifyDistort, pixelToNorm);
	}

	/**
	 * Creates an {@link ImageDistort} for rectifying an image given its rectification matrix.
	 * Lens distortion is assumed to have been previously removed.
	 *
	 * @param rectify Transform for rectifying the image.
	 * @param imageType Type of single band image the transform is to be applied to.
	 * @return ImageDistort for rectifying the image.
	 */
	public static <T extends ImageGray> ImageDistort<T,T>
	rectifyImage( DenseMatrix64F rectify , BorderType borderType, Class<T> imageType)
	{
		boolean skip = borderType == BorderType.SKIP;
		if( skip ) {
			borderType = BorderType.EXTENDED;
		}
		InterpolatePixelS<T> interp = FactoryInterpolation.bilinearPixelS(imageType, borderType);

		DenseMatrix64F rectifyInv = new DenseMatrix64F(3,3);
		CommonOps.invert(rectify,rectifyInv);
		PointTransformHomography_F32 rectifyTran = new PointTransformHomography_F32(rectifyInv);

		// don't bother caching the results since it is likely to only be applied once and is cheap to compute
		ImageDistort<T,T> ret = FactoryDistort.distortSB(false, interp, imageType);
		ret.setRenderAll(!skip);

		ret.setModel(new PointToPixelTransform_F32(rectifyTran));

		return ret;
	}

	/**
	 * Creates an {@link ImageDistort} for rectifying an image given its radial distortion and
	 * rectification matrix.
	 *
	 * @param param Intrinsic parameters.
	 * @param rectify Transform for rectifying the image.
	 * @param imageType Type of single band image the transform is to be applied to.
	 * @return ImageDistort for rectifying the image.
	 */
	public static <T extends ImageBase> ImageDistort<T,T>
	rectifyImage(CameraPinholeRadial param, DenseMatrix64F rectify , BorderType borderType, ImageType<T> imageType)
	{
		boolean skip = borderType == BorderType.SKIP;
		if( skip ) {
			borderType = BorderType.EXTENDED;
		}
		InterpolatePixel<T> interp =
				FactoryInterpolation.createPixel(0,255, InterpolationType.BILINEAR,borderType,imageType);

		// only compute the transform once
		ImageDistort<T,T> ret = FactoryDistort.distort(true, interp, imageType);
		ret.setRenderAll(!skip);

		Point2Transform2_F32 transform = transformRectToPixel_F32(param, rectify);

		ret.setModel(new PointToPixelTransform_F32(transform));

		return ret;
	}

}
