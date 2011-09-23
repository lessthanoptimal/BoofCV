/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.distort.impl;

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.PixelTransformAffine;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.image.*;
import georegression.struct.affine.Affine2D_F32;
import georegression.struct.se.InvertibleTransformSequence;
import georegression.struct.se.Se2_F32;
import georegression.transform.ConvertTransform_F32;


/**
 * Provides low level functions that {@link boofcv.alg.distort.DistortImageOps} can call.
 *
 * @author Peter Abeles
 */
public class DistortSupport {
	/**
	 * Computes a transform which is used to rescale an image.  The scale is computed
	 * directly from the size of the two input images and independently scales
	 * the x and y axises.
	 */
	public static PixelTransformAffine transformScale(ImageBase from, ImageBase to)
	{
		float scaleX = (float)to.width/(float)from.width;
		float scaleY = (float)to.height/(float)from.height;

		Affine2D_F32 affine = new Affine2D_F32(scaleX,0,0,scaleY,0,0);
		PixelTransformAffine distort = new PixelTransformAffine();
		distort.set(affine);

		return distort;
	}

	/**
	 * Rotates the input image using the specified coordinate as the center of rotation.
	 *
	 * @param centerX Center of rotation in input image coordinates.
	 * @param centerY Center of rotation in input image coordinates.
	 * @param angle Angle of rotation.
	 */
	public static PixelTransformAffine transformRotate( float centerX , float centerY , float angle )
	{
		// make the coordinate system's origin the image center
		Se2_F32 imageToCenter = new Se2_F32(-centerX,-centerY,0);
		Se2_F32 rotate = new Se2_F32(0,0,angle);
		Se2_F32 centerToImage = new Se2_F32(centerX,centerY,0);

		InvertibleTransformSequence sequence = new InvertibleTransformSequence();
		sequence.addTransform(true,imageToCenter);
		sequence.addTransform(true,rotate);
		sequence.addTransform(true,centerToImage);

		Se2_F32 total = new Se2_F32();
		sequence.computeTransform(total);

		Affine2D_F32 affine = ConvertTransform_F32.convert(total,null);
		PixelTransformAffine distort = new PixelTransformAffine();
		distort.set(affine);

		return distort;
	}

	/**
	 * Creates a {@link boofcv.alg.distort.ImageDistort} for the specified image type, transformation
	 * and interpolation type.
	 *
	 * @param dstToSrc Transform from dst to src image.
	 */
	public static <T extends ImageBase>
	ImageDistort<T> createDistort( Class<T> imageType , PixelTransform dstToSrc , TypeInterpolate interpType)
	{
		InterpolatePixel<T> interp = FactoryInterpolation.createPixel(0, 255, interpType, imageType);
		return createDistort(imageType,dstToSrc,interp);
	}

	/**
	 * Creates a {@link boofcv.alg.distort.ImageDistort} for the specified image type, transformation
	 * and interpolation instance.
	 * 
	 * @param dstToSrc Transform from dst to src image.
	 */
	public static <T extends ImageBase>
	ImageDistort<T> createDistort( Class<T> imageType ,
								   PixelTransform dstToSrc ,
								   InterpolatePixel<T> interp )
	{
		if( imageType == ImageFloat32.class ) {
			return (ImageDistort<T>)new ImplImageDistort_F32(dstToSrc,(InterpolatePixel<ImageFloat32>)interp);
		} else if( ImageSInt32.class.isAssignableFrom(imageType) ) {
			return (ImageDistort<T>)new ImplImageDistort_S32(dstToSrc,(InterpolatePixel<ImageSInt32>)interp);
		} else if( ImageInt16.class.isAssignableFrom(imageType) ) {
			return (ImageDistort<T>)new ImplImageDistort_I16(dstToSrc,(InterpolatePixel<ImageInt16>)interp);
		} else if( ImageInt8.class.isAssignableFrom(imageType) ) {
			return (ImageDistort<T>)new ImplImageDistort_I8(dstToSrc,(InterpolatePixel<ImageInt8>)interp);
		} else {
			throw new IllegalArgumentException("Image type not supported: "+imageType.getSimpleName());
		}
	}
}
