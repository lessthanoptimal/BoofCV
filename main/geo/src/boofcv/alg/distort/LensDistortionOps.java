/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.core.image.border.BorderType;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.*;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.shapes.Rectangle2D_F32;
import org.ejml.data.DenseMatrix64F;

/**
 * Operations related to manipulating lens distortion in images
 *
 * @author Peter Abeles
 */
public class LensDistortionOps {

	/**
	 * <p>
	 * Transforms the view such that the entire original image is visible after lens distortion has been removed.
	 * The appropriate {@link PointTransform_F32} is returned and a new set of intrinsic camera parameters for
	 * the "virtual" camera that is associated with the returned transformed.
	 * </p>
	 *
	 * <p>
	 * The original image coordinate system is maintained even if the intrinsic parameter flipY is true.
	 * </p>
	 *
	 * @param param Intrinsic camera parameters.
	 * @param paramAdj If not null, the new camera parameters are stored here.
	 * @return New transform that adjusts the view and removes lens distortion..
	 */
	public static PointTransform_F32 fullView( IntrinsicParameters param,
											   IntrinsicParameters paramAdj ) {

		RemoveRadialPtoP_F32 removeDistort = new RemoveRadialPtoP_F32();
		AddRadialPtoP_F32 addDistort = new AddRadialPtoP_F32();
		removeDistort.set(param.fx, param.fy, param.skew, param.cx, param.cy, param.radial);
		addDistort.set(param.fx, param.fy, param.skew, param.cx, param.cy, param.radial);

		Rectangle2D_F32 bound = DistortImageOps.boundBox_F32(param.width, param.height,
				new PointToPixelTransform_F32(removeDistort));

		double scaleX = bound.width/param.width;
		double scaleY = bound.height/param.height;

		double scale = Math.max(scaleX, scaleY);

		// translation
		double deltaX = bound.tl_x;
		double deltaY = bound.tl_y;

		// adjustment matrix
		DenseMatrix64F A = new DenseMatrix64F(3,3,true,scale,0,deltaX,0,scale,deltaY,0,0,1);

		PointTransform_F32 tranAdj = PerspectiveOps.adjustIntrinsic_F32(addDistort, false, param, A, paramAdj);

		if( param.flipY) {
			PointTransform_F32 flip = new FlipVertical_F32(param.height);
			return new SequencePointTransform_F32(flip,tranAdj,flip);
		} else
			return tranAdj;
	}

	/**
	 * <p>
	 * Adjusts the view such that each pixel has a correspondence to the original image while maximizing the
	 * view area. In other words no black regions which can cause problems for some image processing algorithms.
	 * </p>
	 *
	 * <p>
	 * The original image coordinate system is maintained even if the intrinsic parameter flipY is true.
	 * </p>
	 *
	 * @param param Intrinsic camera parameters.
	 * @param paramAdj If not null, the new camera parameters are stored here.
	 * @return New transform that adjusts the view and removes lens distortion..
	 */
	public static PointTransform_F32 allInside( IntrinsicParameters param,
												IntrinsicParameters paramAdj ) {
		RemoveRadialPtoP_F32 removeDistort = new RemoveRadialPtoP_F32();
		AddRadialPtoP_F32 addDistort = new AddRadialPtoP_F32();
		removeDistort.set(param.fx, param.fy, param.skew, param.cx, param.cy, param.radial);
		addDistort.set(param.fx, param.fy, param.skew, param.cx, param.cy, param.radial);

		Rectangle2D_F32 bound = LensDistortionOps.boundBoxInside(param.width, param.height,
				new PointToPixelTransform_F32(removeDistort));

		// ensure there are no strips of black
		LensDistortionOps.roundInside(bound);

		double scaleX = bound.width/param.width;
		double scaleY = bound.height/param.height;

		double scale = Math.min(scaleX, scaleY);

		// translation and shift over so that the small axis is in the middle
		double deltaX = bound.tl_x + (scaleX-scale)*param.width/2.0;
		double deltaY = bound.tl_y + (scaleY-scale)*param.height/2.0;

		// adjustment matrix
		DenseMatrix64F A = new DenseMatrix64F(3,3,true,scale,0,deltaX,0,scale,deltaY,0,0,1);

		PointTransform_F32 tranAdj = PerspectiveOps.adjustIntrinsic_F32(addDistort, false, param, A, paramAdj);

		if( param.flipY) {
			PointTransform_F32 flip = new FlipVertical_F32(param.height);
			return new SequencePointTransform_F32(flip,tranAdj,flip);
		} else
			return tranAdj;
	}

	/**
	 * Removes radial distortion from the image in pixel coordinates and converts it into normalized image coordinates
	 *
	 * @param param Intrinsic camera parameters
	 * @return Distorted pixel to normalized image coordinates
	 */
	public static PointTransform_F64 transformRadialToNorm_F64(IntrinsicParameters param)
	{
		RemoveRadialPtoN_F64 radialDistort = new RemoveRadialPtoN_F64();
		radialDistort.set(param.fx, param.fy, param.skew, param.cx, param.cy, param.radial);

		if( param.flipY) {
			return new FlipVerticalNorm_F64(radialDistort,param.height);
		} else {
			return radialDistort;
		}
	}

	/**
	 * Removes radial distortion from the pixel coordinate.
	 *
	 * @param param Intrinsic camera parameters
	 * @return Transformation into undistorted pixel coordinates
	 */
	public static PointTransform_F64 transformRadialToPixel_F64( IntrinsicParameters param )
	{
		RemoveRadialPtoP_F64 removeRadial = new RemoveRadialPtoP_F64();
		removeRadial.set(param.fx, param.fy, param.skew, param.cx, param.cy, param.radial);

		if( param.flipY) {
			PointTransform_F64 flip = new FlipVertical_F64(param.height);
			return new SequencePointTransform_F64(flip,removeRadial,flip);
		} else {
			return removeRadial;
		}
	}

	/**
	 * Converts normalized image coordinates into distorted pixel coordinates.
	 *
	 * @param param Intrinsic camera parameters
	 * @return Transform from normalized image coordinates into distorted pixel coordinates
	 */
	public static PointTransform_F64 transformNormToRadial_F64(IntrinsicParameters param)
	{
		AddRadialNtoN_F64 addRadial = new AddRadialNtoN_F64();
		addRadial.set(param.radial);

		NormalizedToPixel_F64 toPixel = new NormalizedToPixel_F64();
		toPixel.set(param.fx,param.fy,param.skew,param.cx,param.cy);

		if( param.flipY ) {
			return new SequencePointTransform_F64(addRadial,toPixel,new FlipVertical_F64(param.height));
		} else {
			return new SequencePointTransform_F64(addRadial,toPixel);
		}
	}

	/**
	 * <p>
	 * Transform from undistorted pixel coordinates to distorted with radial pixel coordinates
	 * </p>
	 *
	 * <p>
	 * NOTE: The original image coordinate system is maintained even if the intrinsic parameter flipY is true.
	 * </p>
	 *
	 * @param param Intrinsic camera parameters
	 * @return Transform from undistorted to distorted image.
	 */
	public static PointTransform_F32 transformPixelToRadial_F32(IntrinsicParameters param)
	{
		AddRadialPtoP_F32 radialDistort = new AddRadialPtoP_F32();
		radialDistort.set(param.fx, param.fy, param.skew, param.cx, param.cy, param.radial);

		if( param.flipY) {
			PointTransform_F32 flip = new FlipVertical_F32(param.height);
			return new SequencePointTransform_F32(flip,radialDistort,flip);
		} else {
			return radialDistort;
		}
	}

	/**
	 * Creates an {@link ImageDistort} which removes radial distortion. How pixels outside the image are handled
	 * is specified by the BorderType.  If BorderType.VALUE then pixels outside the image will be filled in with a
	 * value of 0.  For viewing purposes it is recommended that BorderType.VALUE be used and BorderType.EXTENDED
	 * in computer vision applications.  VALUE creates harsh edges which can cause false positives
	 * when detecting features, which EXTENDED minimizes.
	 *
	 * @param param Intrinsic camera parameters
	 * @param imageType Type of single band image being processed
	 * @param borderType Specifies how the image border is handled.
	 * @return Image distort that removes radial distortion
	 */
	public static <T extends ImageSingleBand> ImageDistort<T>
	removeRadialImage(IntrinsicParameters param, BorderType borderType, Class<T> imageType)
	{
		InterpolatePixelS<T> interp = FactoryInterpolation.bilinearPixelS(imageType);
		ImageBorder<T> border;
		if( borderType == BorderType.VALUE )
			border = FactoryImageBorder.value(imageType, 0);
		else
			border = FactoryImageBorder.general(imageType,borderType);

		// only compute the transform once
		ImageDistort<T> ret = FactoryDistort.distortCached(interp, border, imageType);

		PointTransform_F32 transform = transformPixelToRadial_F32(param);

		ret.setModel(new PointToPixelTransform_F32(transform));

		return ret;
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
	public static Rectangle2D_F32 boundBoxInside(int srcWidth, int srcHeight,
												 PixelTransform_F32 transform) {

		float x0,y0,x1,y1;

		transform.compute(0,0);
		x0 = transform.distX;
		y0 = transform.distY;

		transform.compute(srcWidth,0);
		x1=transform.distX;
		transform.compute(0,srcHeight);
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

		return new Rectangle2D_F32(x0,y0,x1-x0,y1-y0);
	}

	/**
	 * Adjust bound to ensure the entire image is contained inside, otherwise there might be
	 * single pixel wide black regions
	 */
	public static void roundInside( Rectangle2D_F32 bound ) {
		float x0 = (float)Math.ceil(bound.tl_x);
		float y0 = (float)Math.ceil(bound.tl_y);
		float x1 = (float)Math.floor(bound.tl_x+bound.width);
		float y1 = (float)Math.floor(bound.tl_y+bound.height);

		bound.tl_x = x0;
		bound.tl_y = y0;
		bound.width = x1-x0;
		bound.height = y1-y0;
	}
}
