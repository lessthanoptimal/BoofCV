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

package boofcv.alg.distort;

import boofcv.alg.distort.pinhole.LensDistortionPinhole;
import boofcv.alg.distort.radtan.LensDistortionRadialTangential;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.core.image.border.BorderType;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.calib.CameraModel;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.*;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.shapes.RectangleLength2D_F32;
import georegression.struct.shapes.RectangleLength2D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

/**
 * Operations related to manipulating lens distortion in images
 *
 * @author Peter Abeles
 */
public class LensDistortionOps {

	/**
	 * Creates a distortion for modifying the input image from one camera model into another camera model.  If
	 * requested the camera model can be further modified to ensure certain visibility requirements are meet
	 * and the adjusted camera model will be returned.
	 * @param type How it should modify the image model to ensure visibility of pixels.
	 * @param borderType How the image border is handled
	 * @param original The original camera model
	 * @param desired The desired camera model
	 * @param modified (Optional) The desired camera model after being rescaled.  Can be null.
	 * @param imageType Type of image.
	 * @return Image distortion from original camera model to the modified one.
	 */
	public static <T extends ImageBase<T>,O extends CameraPinhole, D extends CameraPinhole>
	ImageDistort<T,T> changeCameraModel(AdjustmentType type, BorderType borderType,
										O original,
										D desired,
										D modified,
										ImageType<T> imageType)
	{
		Class bandType = imageType.getImageClass();
		boolean skip = borderType == BorderType.SKIP;

		// it has to process the border at some point, so if skip is requested just skip stuff truly outside the image
		if( skip )
			borderType = BorderType.EXTENDED;

		InterpolatePixelS interp = FactoryInterpolation.createPixelS(0, 255, InterpolationType.BILINEAR,borderType, bandType);

		Point2Transform2_F32 undistToDist = transformChangeModel_F32(type, original, desired, true, modified);

		ImageDistort<T,T> distort = FactoryDistort.distort(true, interp, imageType);

		distort.setModel(new PointToPixelTransform_F32(undistToDist));
		distort.setRenderAll(!skip );

		return distort;
	}

	/**
	 * <p>
	 * Creates an {@link ImageDistort} class which will remove the lens distortion.  The user
	 * can select how the view is adjusted.
	 * </p>
	 *
	 * @param type The type of adjustment it will do
	 * @param borderType Specifies how the image border is handled. Null means borders are ignored.  Border of {@link BorderType#ZERO}
	 *                   is recommended.
	 * @param param Original intrinsic parameters.
	 * @param paramAdj (output) Intrinsic parameters which reflect the undistorted image. Can be null.
	 * @param imageType Type of image it will undistort
	 * @return ImageDistort which removes lens distortion
	 */
	public static <T extends ImageBase<T>>
	ImageDistort<T,T> imageRemoveDistortion(AdjustmentType type, BorderType borderType,
											CameraPinholeRadial param, CameraPinholeRadial paramAdj,
											ImageType<T> imageType)
	{
		Class bandType = imageType.getImageClass();
		boolean skip = borderType == BorderType.SKIP;

		// it has to process the border at some point, so if skip is requested just skip stuff truly outside the image
		if( skip )
			borderType = BorderType.EXTENDED;

		InterpolatePixelS interp = FactoryInterpolation.createPixelS(0, 255, InterpolationType.BILINEAR,borderType, bandType);

		Point2Transform2_F32 undistToDist = null;
		switch( type ) {
			case EXPAND:
			case FULL_VIEW:
				undistToDist = transform_F32(type, param, paramAdj, true);
				break;

			case NONE:
				if( paramAdj != null ) {
					// remove lens distortion
					paramAdj.set(param);
					paramAdj.radial = null;
					paramAdj.t1 = param.t1 = 0;
				}
				undistToDist = transformPoint(param).distort_F32(true, true);
				break;
		}

		ImageDistort<T,T> distort = FactoryDistort.distort(true, interp, imageType);

		distort.setModel(new PointToPixelTransform_F32(undistToDist));
		distort.setRenderAll(!skip );

		return distort;
	}

	/**
	 * Creates a {@link Point2Transform2_F32} for adding and removing lens distortion.
	 *
	 * @param type The type of adjustment it will apply to the transform
	 * @param param Intrinsic camera parameters.
	 * @param paramAdj If not null, the new camera parameters for the undistorted view are stored here.
	 * @param undistortedToDistorted If true then the transform's input is assumed to be pixels in the adjusted undistorted
	 *                       image and the output will be in distorted image, if false then the reverse transform
	 *                       is returned.
	 * @return The requested transform
	 */
	public static Point2Transform2_F32 transform_F32(AdjustmentType type,
													 CameraPinholeRadial param,
													 CameraPinholeRadial paramAdj,
													 boolean undistortedToDistorted)
	{
		Point2Transform2_F32 remove_p_to_p = transformPoint(param).undistort_F32(true, true);

		RectangleLength2D_F32 bound;
		if( type == AdjustmentType.FULL_VIEW ) {
			bound = DistortImageOps.boundBox_F32(param.width, param.height,
					new PointToPixelTransform_F32(remove_p_to_p));
		} else if( type == AdjustmentType.EXPAND) {
			bound = LensDistortionOps.boundBoxInside(param.width, param.height,
					new PointToPixelTransform_F32(remove_p_to_p));

			// ensure there are no strips of black
			LensDistortionOps.roundInside(bound);
		} else {
			throw new IllegalArgumentException("Unsupported type "+type);
		}

		double scaleX = bound.width/param.width;
		double scaleY = bound.height/param.height;

		double scale;

		if( type == AdjustmentType.FULL_VIEW ) {
			scale = Math.max(scaleX, scaleY);
		} else {
			scale = Math.min(scaleX, scaleY);
		}

		double deltaX = bound.x0 + (scaleX-scale)*param.width/2.0;
		double deltaY = bound.y0 + (scaleY-scale)*param.height/2.0;

		// adjustment matrix
		DenseMatrix64F A = new DenseMatrix64F(3,3,true,scale,0,deltaX,0,scale,deltaY,0,0,1);

		return adjustmentTransform_F32(param, paramAdj, undistortedToDistorted, remove_p_to_p, A);
	}

	/**
	 * Creates a {@link Point2Transform2_F32} for converting pixels from original camera model into a new synthetic
	 * model.  The scaling of the image can be adjusted to ensure certain visibility requirements.
	 *
	 * @param type The type of adjustment it will apply to the transform
	 * @param paramOriginal Camera model for the current image
	 * @param paramDesired Desired camera model for the distorted image
	 * @param desiredToOriginal If true then the transform's input is assumed to be pixels in the desired
	 *                       image and the output will be in original image, if false then the reverse transform
	 *                       is returned.
	 * @param paramMod The modified camera model to meet the requested visibility requirements.  Null if you don't want it.
	 * @return The requested transform
	 */
	public static <O extends CameraPinhole, D extends CameraPinhole>
	Point2Transform2_F32 transformChangeModel_F32(AdjustmentType type,
												  O paramOriginal,
												  D paramDesired,
												  boolean desiredToOriginal,
												  D paramMod)
	{
		LensDistortionNarrowFOV original = LensDistortionOps.transformPoint(paramOriginal);
		LensDistortionNarrowFOV desired = LensDistortionOps.transformPoint(paramDesired);

		Point2Transform2_F32 ori_p_to_n = original.undistort_F32(true, false);
		Point2Transform2_F32 des_n_to_p = desired.distort_F32(false, true);

		Point2Transform2_F32 ori_to_des = new SequencePoint2Transform2_F32(ori_p_to_n,des_n_to_p);

		RectangleLength2D_F32 bound;
		if( type == AdjustmentType.FULL_VIEW ) {
			bound = DistortImageOps.boundBox_F32(paramOriginal.width, paramOriginal.height, new PointToPixelTransform_F32(ori_to_des));
		} else if( type == AdjustmentType.EXPAND) {
			bound = LensDistortionOps.boundBoxInside(paramOriginal.width, paramOriginal.height, new PointToPixelTransform_F32(ori_to_des));

			// ensure there are no strips of black
			LensDistortionOps.roundInside(bound);
		} else if( type == AdjustmentType.NONE ) {
			bound = new RectangleLength2D_F32(0,0,paramDesired.width, paramDesired.height);
		} else {
			throw new IllegalArgumentException("Unsupported type "+type);
		}

		double scaleX = bound.width/paramDesired.width;
		double scaleY = bound.height/paramDesired.height;

		double scale;

		if( type == AdjustmentType.FULL_VIEW ) {
			scale = Math.max(scaleX, scaleY);
		} else if( type == AdjustmentType.EXPAND) {
			scale = Math.min(scaleX, scaleY);
		} else {
			scale = 1.0;
		}

		double deltaX = bound.x0 + (scaleX-scale)*paramDesired.width/2.0;
		double deltaY = bound.y0 + (scaleY-scale)*paramDesired.height/2.0;

		// adjustment matrix
		DenseMatrix64F A = new DenseMatrix64F(3,3,true,scale,0,deltaX,0,scale,deltaY,0,0,1);
		DenseMatrix64F A_inv = new DenseMatrix64F(3, 3);
		if (!CommonOps.invert(A, A_inv)) {
			throw new RuntimeException("Failed to invert adjustment matrix.  Probably bad.");
		}

		if( paramMod != null ) {
			PerspectiveOps.adjustIntrinsic(paramDesired, A_inv, paramMod);
		}

		if( desiredToOriginal ) {
			Point2Transform2_F32 des_p_to_n = desired.undistort_F32(true, false);
			Point2Transform2_F32 ori_n_to_p = original.distort_F32(false, true);
			PointTransformHomography_F32 adjust = new PointTransformHomography_F32(A);
			return new SequencePoint2Transform2_F32(adjust,des_p_to_n,ori_n_to_p);
		} else {
			PointTransformHomography_F32 adjust = new PointTransformHomography_F32(A_inv);
			return new SequencePoint2Transform2_F32(ori_to_des, adjust);
		}
	}

	/**
	 * Given the lens distortion and the intrinsic adjustment matrix compute the new intrinsic parameters
	 * and {@link Point2Transform2_F32}
	 */
	private static Point2Transform2_F32 adjustmentTransform_F32(CameraPinholeRadial param,
																CameraPinhole paramAdj,
																boolean undistToDist,
																Point2Transform2_F32 remove_p_to_p,
																DenseMatrix64F A) {
		DenseMatrix64F A_inv = null;

		if( !undistToDist || paramAdj != null ) {
			A_inv = new DenseMatrix64F(3, 3);
			if (!CommonOps.invert(A, A_inv)) {
				throw new RuntimeException("Failed to invert adjustment matrix.  Probably bad.");
			}
		}

		if( paramAdj != null ) {
			PerspectiveOps.adjustIntrinsic(param, A_inv, paramAdj);
		}

		if( undistToDist ) {
			Point2Transform2_F32 add_p_to_p = transformPoint(param).distort_F32(true, true);
			PointTransformHomography_F32 adjust = new PointTransformHomography_F32(A);

			return new SequencePoint2Transform2_F32(adjust,add_p_to_p);
		} else {
			PointTransformHomography_F32 adjust = new PointTransformHomography_F32(A_inv);

			return new SequencePoint2Transform2_F32(remove_p_to_p,adjust);
		}
	}

	/**
	 * Creates a {@link Point2Transform2_F64} for adding and removing lens distortion.
	 *
	 * @param type The type of adjustment it will apply to the transform
	 * @param param Intrinsic camera parameters.
	 * @param paramAdj If not null, the new camera parameters for the undistorted view are stored here.
	 * @param undistortedToDistorted If true then the transform's input is assumed to be pixels in the adjusted undistorted
	 *                       image and the output will be in distorted image, if false then the reverse transform
	 *                       is returned.
	 * @return The requested transform
	 */
	public static Point2Transform2_F64 transform_F64(AdjustmentType type,
													 CameraPinholeRadial param,
													 CameraPinholeRadial paramAdj,
													 boolean undistortedToDistorted)
	{
		Point2Transform2_F64 remove_p_to_p = transformPoint(param).undistort_F64(true, true);

		RectangleLength2D_F64 bound;
		if( type == AdjustmentType.FULL_VIEW ) {
			bound = DistortImageOps.boundBox_F64(param.width, param.height,
					new PointToPixelTransform_F64(remove_p_to_p));
		} else if( type == AdjustmentType.EXPAND) {
			bound = LensDistortionOps.boundBoxInside(param.width, param.height,
					new PointToPixelTransform_F64(remove_p_to_p));

			// ensure there are no strips of black
			LensDistortionOps.roundInside(bound);
		} else {
			throw new IllegalArgumentException("If you don't want to adjust the view just call transformPoint()");
		}

		double scaleX = bound.width/param.width;
		double scaleY = bound.height/param.height;

		double scale;

		if( type == AdjustmentType.FULL_VIEW ) {
			scale = Math.max(scaleX, scaleY);
		} else {
			scale = Math.min(scaleX, scaleY);
		}

		double deltaX = bound.x0 + (scaleX-scale)*param.width/2.0;
		double deltaY = bound.y0 + (scaleY-scale)*param.height/2.0;

		// adjustment matrix
		DenseMatrix64F A = new DenseMatrix64F(3,3,true,scale,0,deltaX,0,scale,deltaY,0,0,1);

		return adjustmentTransform_F64(param, paramAdj, undistortedToDistorted, remove_p_to_p, A);
	}

	/**
	 * Given the lens distortion and the intrinsic adjustment matrix compute the new intrinsic parameters
	 * and {@link Point2Transform2_F32}
	 */
	private static Point2Transform2_F64 adjustmentTransform_F64(CameraPinholeRadial param,
																CameraPinholeRadial paramAdj,
																boolean adjToDistorted,
																Point2Transform2_F64 remove_p_to_p,
																DenseMatrix64F A) {
		DenseMatrix64F A_inv = null;

		if( !adjToDistorted || paramAdj != null ) {
			A_inv = new DenseMatrix64F(3, 3);
			if (!CommonOps.invert(A, A_inv)) {
				throw new RuntimeException("Failed to invert adjustment matrix.  Probably bad.");
			}
		}

		if( paramAdj != null ) {
			PerspectiveOps.adjustIntrinsic(param, A_inv, paramAdj);
		}

		if( adjToDistorted ) {
			Point2Transform2_F64 add_p_to_p = transformPoint(param).distort_F64(true, true);
			PointTransformHomography_F64 adjust = new PointTransformHomography_F64(A);

			return new SequencePoint2Transform2_F64(adjust,add_p_to_p);
		} else {
			PointTransformHomography_F64 adjust = new PointTransformHomography_F64(A_inv);

			return new SequencePoint2Transform2_F64(remove_p_to_p,adjust);
		}
	}

	/**
	 * <p>
	 * Creates the {@link LensDistortionNarrowFOV lens distortion} for the specified camera parameters.
	 * Call this to create transforms to and from pixel and normalized image coordinates with and without
	 * lens distortion.  Automatically switches algorithm depending on the type of distortion or lack thereof.
	 * </p>
	 *
	 * <p>
	 * Example:<br>
	 * <pre>PointTransform_F64 normToPixel = LensDistortionOps.distortTransform(param).distort_F64(false,true);</pre>
	 * Creates a transform from normalized image coordinates into pixel coordinates.
	 * </p>
	 *
	 */
	public static LensDistortionNarrowFOV transformPoint(CameraModel param) {
		if( param instanceof CameraPinholeRadial ) {
			CameraPinholeRadial c = (CameraPinholeRadial)param;

			if (c.isDistorted())
				return new LensDistortionRadialTangential(c);
			else
				return new LensDistortionPinhole(c);
		} else if( param instanceof CameraPinhole ) {
			CameraPinhole c = (CameraPinhole)param;

			return new LensDistortionPinhole(c);
		} else {
			throw new IllegalArgumentException("Unknown camera model "+param.getClass().getSimpleName());
		}
	}

	/**
	 * Finds the maximum area axis-aligned rectangle contained inside the transformed image which
	 * does not include any pixels outside the sources border.  Assumes that the coordinates are not
	 * flipped and some other stuff too.
	 *
	 * @param srcWidth Width of the source image
	 * @param srcHeight Height of the source image
	 * @param transform Transform being applied to the image
	 * @return Bounding box
	 */
	public static RectangleLength2D_F32 boundBoxInside(int srcWidth, int srcHeight,
												 PixelTransform2_F32 transform) {

		float x0,y0,x1,y1;

		transform.compute(0,0);
		x0 = transform.distX;
		y0 = transform.distY;

		transform.compute(srcWidth,0);
		x1=transform.distX;
		transform.compute(0, srcHeight);
		y1=transform.distY;

		for( int x = 0; x < srcWidth; x++ ) {
			transform.compute(x, 0);
			if( transform.distY > y0 )
				y0 = transform.distY;
			transform.compute(x,srcHeight);
			if( transform.distY < y1 )
				y1 = transform.distY;
		}

		for( int y = 0; y < srcHeight; y++ ) {
			transform.compute(0,y);
			if( transform.distX > x0 )
				x0 = transform.distX;
			transform.compute(srcWidth,y);
			if( transform.distX < x1 )
				x1 = transform.distX;
		}

		return new RectangleLength2D_F32(x0,y0,x1-x0,y1-y0);
	}

	/**
	 * Finds the maximum area axis-aligned rectangle contained inside the transformed image which
	 * does not include any pixels outside the sources border.  Assumes that the coordinates are not
	 * flipped and some other stuff too.
	 *
	 * @param srcWidth Width of the source image
	 * @param srcHeight Height of the source image
	 * @param transform Transform being applied to the image
	 * @return Bounding box
	 */
	public static RectangleLength2D_F64 boundBoxInside(int srcWidth, int srcHeight,
													   PixelTransform2_F64 transform) {

		double x0,y0,x1,y1;

		transform.compute(0,0);
		x0 = transform.distX;
		y0 = transform.distY;

		transform.compute(srcWidth,0);
		x1=transform.distX;
		transform.compute(0, srcHeight);
		y1=transform.distY;

		for( int x = 0; x < srcWidth; x++ ) {
			transform.compute(x, 0);
			if( transform.distY > y0 )
				y0 = transform.distY;
			transform.compute(x,srcHeight);
			if( transform.distY < y1 )
				y1 = transform.distY;
		}

		for( int y = 0; y < srcHeight; y++ ) {
			transform.compute(0,y);
			if( transform.distX > x0 )
				x0 = transform.distX;
			transform.compute(srcWidth,y);
			if( transform.distX < x1 )
				x1 = transform.distX;
		}

		return new RectangleLength2D_F64(x0,y0,x1-x0,y1-y0);
	}

	/**
	 * Adjust bound to ensure the entire image is contained inside, otherwise there might be
	 * single pixel wide black regions
	 */
	public static void roundInside( RectangleLength2D_F32 bound ) {
		float x0 = (float)Math.ceil(bound.x0);
		float y0 = (float)Math.ceil(bound.y0);
		float x1 = (float)Math.floor(bound.x0+bound.width);
		float y1 = (float)Math.floor(bound.y0+bound.height);

		bound.x0 = x0;
		bound.y0 = y0;
		bound.width = x1-x0;
		bound.height = y1-y0;
	}

	/**
	 * Adjust bound to ensure the entire image is contained inside, otherwise there might be
	 * single pixel wide black regions
	 */
	public static void roundInside( RectangleLength2D_F64 bound ) {
		double x0 = Math.ceil(bound.x0);
		double y0 = Math.ceil(bound.y0);
		double x1 = Math.floor(bound.x0+bound.width);
		double y1 = Math.floor(bound.y0+bound.height);

		bound.x0 = x0;
		bound.y0 = y0;
		bound.width = x1-x0;
		bound.height = y1-y0;
	}
}
