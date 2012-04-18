/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.alg.geo.rectify.RectifyFundamental;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PointTransform_F32;
import boofcv.struct.distort.SequencePointTransform_F32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.shapes.Rectangle2D_F32;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.simple.SimpleMatrix;

/**
 * Operations related to rectifying stereo image pairs.
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
	 * for maximum viewing area.  See {@link #fullViewLeft(boofcv.struct.calib.IntrinsicParameters, boolean, org.ejml.data.DenseMatrix64F, org.ejml.data.DenseMatrix64F, org.ejml.data.DenseMatrix64F)}
	 * and {@link #allInsideLeft(boofcv.struct.calib.IntrinsicParameters, boolean, org.ejml.data.DenseMatrix64F, org.ejml.data.DenseMatrix64F, org.ejml.data.DenseMatrix64F)}
	 * for adjusting the rectification.
	 * </p>
	 *
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
	 * Input rectification matrices are overwritten with adjusted values on output.
	 * </p>
	 *
	 * @param paramLeft Intrinsic parameters for left camera
	 * @param applyLeftToRight Has the coordinate system been changed from left to right handed.
	 * @param rectifyLeft Rectification matrix for left image.
	 * @param rectifyRight Rectification matrix for right image.
	 * @param rectifyK Rectification calibration matrix.
	 */
	public static void fullViewLeft(IntrinsicParameters paramLeft,
									boolean applyLeftToRight,
									DenseMatrix64F rectifyLeft, DenseMatrix64F rectifyRight,
									DenseMatrix64F rectifyK)
	{
		PointTransform_F32 tranLeft = rectifyTransform(paramLeft, applyLeftToRight, rectifyLeft);

		Rectangle2D_F32 bound = DistortImageOps.boundBox_F32(paramLeft.width, paramLeft.height,
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
	public static void fullViewLeft(int imageWidth,int imageHeight,
									DenseMatrix64F rectifyLeft, DenseMatrix64F rectifyRight )
	{
		PointTransform_F32 tranLeft = new PointTransformHomography_F32(rectifyLeft);

		Rectangle2D_F32 bound = DistortImageOps.boundBox_F32(imageWidth, imageHeight,
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
	 * @param paramLeft Intrinsic parameters for left camera
	 * @param applyLeftToRight Has the coordinate system been changed from left to right handed.
	 * @param rectifyLeft Rectification matrix for left image. Input and Output. Modified.
	 * @param rectifyRight Rectification matrix for right image. Input and Output. Modified.
	 * @param rectifyK Rectification calibration matrix. Input and Output. Modified.
	 */
	public static void allInsideLeft(IntrinsicParameters paramLeft,
									 boolean applyLeftToRight,
									 DenseMatrix64F rectifyLeft, DenseMatrix64F rectifyRight,
									 DenseMatrix64F rectifyK)
	{
		PointTransform_F32 tranLeft = rectifyTransform(paramLeft, applyLeftToRight, rectifyLeft);

		Rectangle2D_F32 bound = LensDistortionOps.boundBoxInside(paramLeft.width, paramLeft.height,
				new PointToPixelTransform_F32(tranLeft));

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
		PointTransform_F32 tranLeft = new PointTransformHomography_F32(rectifyLeft);

		Rectangle2D_F32 bound = LensDistortionOps.boundBoxInside(imageWidth, imageHeight,
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
										 Rectangle2D_F32 bound, double scale) {
		// translation
		double deltaX = -bound.tl_x*scale;
		double deltaY = -bound.tl_y*scale;

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
										   Rectangle2D_F32 bound, double scale) {
		// translation
		double deltaX = -bound.tl_x*scale;
		double deltaY = -bound.tl_y*scale;

		// adjustment matrix
		SimpleMatrix A = new SimpleMatrix(3,3,true,scale,0,deltaX,0,scale,deltaY,0,0,1);
		SimpleMatrix rL = SimpleMatrix.wrap(rectifyLeft);
		SimpleMatrix rR = SimpleMatrix.wrap(rectifyRight);

		rectifyLeft.set(A.mult(rL).getMatrix());
		rectifyRight.set(A.mult(rR).getMatrix());
	}


	/**
	 * Creates a transform that goes from rectified to original pixel coordinates.
	 * Rectification includes removal of lens distortion.  Used for rendering rectified images.
	 *
	 * @param param Intrinsic parameters.
	 * @param applyLeftToRight Set to true if the image coordinate system was adjusted
	 *                         to right handed during calibration.
	 * @param rectify Transform for rectifying the image.
	 * @return Inverse rectification transform.
	 */
	public static PointTransform_F32 rectifyTransformInv(IntrinsicParameters param,
														 boolean applyLeftToRight ,
														 DenseMatrix64F rectify )
	{
		AddRadialPtoP_F32 radialDistort = new AddRadialPtoP_F32();
		radialDistort.set(param.fx, param.fy, param.skew, param.cx, param.cy, param.radial);

		DenseMatrix64F rectifyInv = new DenseMatrix64F(3,3);
		CommonOps.invert(rectify,rectifyInv);
		PointTransformHomography_F32 rectifyDistort = new PointTransformHomography_F32(rectifyInv);

		if( applyLeftToRight ) {
			PointTransform_F32 l2r = new LeftToRightHanded_F32(param.height);
			return new SequencePointTransform_F32(l2r,rectifyDistort,radialDistort,l2r);
		} else {
			return new SequencePointTransform_F32(rectifyDistort,radialDistort);
		}
	}

	/**
	 * Creates a transform that applies rectification to unrectified distorted pixels.
	 *
	 * @param param Intrinsic parameters.
	 * @param applyLeftToRight Set to true if the image coordinate system was adjusted
	 *                         to right handed during calibration.
	 * @param rectify Transform for rectifying the image.
	 * @return
	 */
	public static PointTransform_F32 rectifyTransform(IntrinsicParameters param,
													  boolean applyLeftToRight ,
													  DenseMatrix64F rectify )
	{
		RemoveRadialPtoP_F32 radialDistort = new RemoveRadialPtoP_F32();
		radialDistort.set(param.fx, param.fy, param.skew, param.cx, param.cy, param.radial);

		PointTransformHomography_F32 rectifyDistort = new PointTransformHomography_F32(rectify);

		if( applyLeftToRight ) {
			PointTransform_F32 l2r = new LeftToRightHanded_F32(param.height);
			return new SequencePointTransform_F32(l2r,radialDistort,rectifyDistort,l2r);
		} else {
			return new SequencePointTransform_F32(radialDistort,rectifyDistort);
		}
	}

	/**
	 * Creates an {@link ImageDistort} for rectifying an image given its rectification matrix.
	 *
	 * @param rectify Transform for rectifying the image.
	 * @param imageType Type of single band image the transform is to be applied to.
	 * @return ImageDistort for rectifying the image.
	 */
	public static <T extends ImageSingleBand> ImageDistort<T>
	rectifyImage( DenseMatrix64F rectify , Class<T> imageType)
	{
		InterpolatePixel<T> interp = FactoryInterpolation.bilinearPixel(imageType);

		DenseMatrix64F rectifyInv = new DenseMatrix64F(3,3);
		CommonOps.invert(rectify,rectifyInv);
		PointTransformHomography_F32 rectifyTran = new PointTransformHomography_F32(rectifyInv);

		// don't bother caching the results since it is likely to only be applied once and is cheap to compute
		ImageDistort<T> ret = FactoryDistort.distort(interp, null, imageType);

		ret.setModel(new PointToPixelTransform_F32(rectifyTran));

		return ret;
	}

	/**
	 * Creates an {@link ImageDistort} for rectifying an image given its radial distortion and
	 * rectification matrix.
	 *
	 * @param param Intrinsic parameters.
	 * @param applyLeftToRight Set to true if the image coordinate system was adjusted
	 *                         to right handed during calibration.
	 * @param rectify Transform for rectifying the image.
	 * @param imageType Type of single band image the transform is to be applied to.
	 * @return ImageDistort for rectifying the image.
	 */
	public static <T extends ImageSingleBand> ImageDistort<T>
	rectifyImage(IntrinsicParameters param,
				 boolean applyLeftToRight ,
				 DenseMatrix64F rectify , Class<T> imageType)
	{
		InterpolatePixel<T> interp = FactoryInterpolation.bilinearPixel(imageType);

		// only compute the transform once
		ImageDistort<T> ret = FactoryDistort.distortCached(interp,null,imageType);

		PointTransform_F32 transform = rectifyTransformInv(param, applyLeftToRight , rectify);

		ret.setModel(new PointToPixelTransform_F32(transform));

		return ret;
	}

}
