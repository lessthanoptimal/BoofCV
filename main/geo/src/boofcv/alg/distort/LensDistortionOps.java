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

package boofcv.alg.distort;

import boofcv.alg.geo.UtilIntrinsic;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.core.image.border.BorderType;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.distort.PointTransform_F32;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.distort.SequencePointTransform_F32;
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
	 * Transforms the view such that the entire original image is visible after lens distortion has been removed.
	 * The appropriate {@link PointTransform_F32} is returned and a new set of intrinsic camera parameters for
	 * the "virtual" camera that is associated with the returned transformed.
	 *
	 * @param param Intrinsic camera parameters.
	 * @param applyLeftToRight True if the intrinsic parameters were computed by adjusting the image coordinate system
	 *                         to be right handed.
	 * @param paramAdj If not null, the new camera parameters are stored here.
	 * @return New transform that adjusts the view and removes lens distortion..
	 */
	public static PointTransform_F32 fullView( IntrinsicParameters param,
											   boolean applyLeftToRight ,
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

		PointTransform_F32 tranAdj = UtilIntrinsic.adjustIntrinsic_F32(addDistort, false, param, A, paramAdj);

		if( applyLeftToRight) {
			PointTransform_F32 l2r = new LeftToRightHanded_F32(param.height);
			return new SequencePointTransform_F32(l2r,tranAdj,l2r);
		} else
			return tranAdj;
	}

	/**
	 * Adjusts the view such that each pixel has a correspondence to the original image while maximizing the
	 * view area. In other words no black regions which can cause problems for some image processing algorithms.
	 *
	 * @param param Intrinsic camera parameters.
	 * @param applyLeftToRight True if the intrinsic parameters were computed by adjusting the image coordinate system
	 *                         to be right handed.
	 * @param paramAdj If not null, the new camera parameters are stored here.
	 * @return New transform that adjusts the view and removes lens distortion..
	 */
	public static PointTransform_F32 allInside( IntrinsicParameters param,
												boolean applyLeftToRight ,
												IntrinsicParameters paramAdj ) {
		RemoveRadialPtoP_F32 removeDistort = new RemoveRadialPtoP_F32();
		AddRadialPtoP_F32 addDistort = new AddRadialPtoP_F32();
		removeDistort.set(param.fx, param.fy, param.skew, param.cx, param.cy, param.radial);
		addDistort.set(param.fx, param.fy, param.skew, param.cx, param.cy, param.radial);

		Rectangle2D_F32 bound = LensDistortionOps.boundBoxInside(param.width, param.height,
				new PointToPixelTransform_F32(removeDistort));

		double scaleX = bound.width/param.width;
		double scaleY = bound.height/param.height;

		double scale = Math.min(scaleX, scaleY);

		// translation and shift over so that the small axis is in the middle
		double deltaX = bound.tl_x + (scaleX-scale)*param.width/2.0;
		double deltaY = bound.tl_y + (scaleY-scale)*param.height/2.0;

		// adjustment matrix
		DenseMatrix64F A = new DenseMatrix64F(3,3,true,scale,0,deltaX,0,scale,deltaY,0,0,1);

		PointTransform_F32 tranAdj = UtilIntrinsic.adjustIntrinsic_F32(addDistort, false, param, A, paramAdj);

		if( applyLeftToRight) {
			PointTransform_F32 l2r = new LeftToRightHanded_F32(param.height);
			return new SequencePointTransform_F32(l2r,tranAdj,l2r);
		} else
			return tranAdj;
	}

	/**
	 * Removes radial distortion from the image and converts it into normalized image coordinates
	 *
	 * @param param Intrinsic camera parameters
	 * @param applyLeftToRight Set to true if the image coordinate system was adjusted
	 *                         to right handed during calibration
	 * @return Distorted pixel to normalized image coordinates
	 */
	public static PointTransform_F64 removeRadialToNorm( IntrinsicParameters param,
														 boolean applyLeftToRight )
	{
		RemoveRadialPtoN_F64 radialDistort = new RemoveRadialPtoN_F64();
		radialDistort.set(param.fx, param.fy, param.skew, param.cx, param.cy, param.radial);

		if( applyLeftToRight ) {
			return new LeftToRightHandedNorm_F64(radialDistort,param.height);
		} else {
			return radialDistort;
		}
	}

	/**
	 * Transform from undistorted image to an image with radial distortion.
	 * TODO MAKE IT MATCH
	 *
	 * @param param Intrinsic camera parameters
	 * @param applyLeftToRight Set to true if the image coordinate system was adjusted
	 *                         to right handed during calibration.
	 * @return Transform from undistorted to distorted image.
	 */
	public static PointTransform_F32 radialTransform_F32(IntrinsicParameters param,
														 boolean applyLeftToRight )
	{
		RemoveRadialPtoP_F32 radialDistort = new RemoveRadialPtoP_F32();
		radialDistort.set(param.fx, param.fy, param.skew, param.cx, param.cy, param.radial);

		if( applyLeftToRight ) {
			PointTransform_F32 l2r = new LeftToRightHanded_F32(param.height);
			return new SequencePointTransform_F32(l2r,radialDistort,l2r);
		} else {
			return radialDistort;
		}
	}


	/**
	 * Transform from undistorted image to an image with radial distortion.
	 *
	 * @param param Intrinsic camera parameters
	 * @param applyLeftToRight Set to true if the image coordinate system was adjusted
	 *                         to right handed during calibration.
	 * @return Transform from undistorted to distorted image.
	 */
	public static PointTransform_F32 radialTransformInv(IntrinsicParameters param,
														boolean applyLeftToRight )
	{
		AddRadialPtoP_F32 radialDistort = new AddRadialPtoP_F32();
		radialDistort.set(param.fx, param.fy, param.skew, param.cx, param.cy, param.radial);

		if( applyLeftToRight ) {
			PointTransform_F32 l2r = new LeftToRightHanded_F32(param.height);
			return new SequencePointTransform_F32(l2r,radialDistort,l2r);
		} else {
			return radialDistort;
		}
	}

	/**
	 * Creates an {@Link ImageDistort} which removes radial distortion.
	 *
	 * @param param Intrinsic camera parameters
	 * @param applyLeftToRight Set to true if the image coordinate system was adjusted
	 *                         to right handed during calibration.
	 * @param imageType Type of single band image being processed
	 * @return Image distort that removes radial distortion
	 */
	public static <T extends ImageSingleBand> ImageDistort<T>
	removeRadialImage(IntrinsicParameters param,
					  boolean applyLeftToRight ,
					  Class<T> imageType)
	{
		InterpolatePixel<T> interp = FactoryInterpolation.bilinearPixel(imageType);
		ImageBorder<T> border = FactoryImageBorder.general(imageType, BorderType.EXTENDED);

		// only compute the transform once
		ImageDistort<T> ret = FactoryDistort.distortCached(interp, border, imageType);

		PointTransform_F32 transform = radialTransformInv(param, applyLeftToRight );

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
}
