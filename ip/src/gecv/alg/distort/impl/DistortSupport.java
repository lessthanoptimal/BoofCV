/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.distort.impl;

import gecv.alg.distort.ImageDistort;
import gecv.alg.distort.PixelTransformAffine;
import gecv.alg.interpolate.FactoryInterpolation;
import gecv.alg.interpolate.InterpolatePixel;
import gecv.alg.interpolate.TypeInterpolate;
import gecv.struct.distort.PixelTransform;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt16;
import gecv.struct.image.ImageInt8;
import jgrl.struct.affine.Affine2D_F32;
import jgrl.struct.se.InvertibleTransformSequence;
import jgrl.struct.se.Se2_F32;
import jgrl.struct.se.SpecialEuclideanOps_F32;


/**
 * Provides low level functions that {@link gecv.alg.distort.DistortImageOps} can call.
 *
 * @author Peter Abeles
 */
public class DistortSupport {
	/**
	 * Computes a transform which is used to rescale an image.  The scale is computed
	 * directly from the size of the two input images and independently scales
	 * the x and y axises.
	 */
	public static PixelTransform transformScale(ImageBase from, ImageBase to)
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
	public static PixelTransform transformRotate( float centerX , float centerY , float angle )
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

		Affine2D_F32 affine = SpecialEuclideanOps_F32.toAffine(total,null);
		PixelTransformAffine distort = new PixelTransformAffine();
		distort.set(affine);

		return distort;
	}

	/**
	 * Creates a {@link gecv.alg.distort.ImageDistort} for the specified image type, transformation
	 * and interpolation type.
	 */
	public static <T extends ImageBase>
	ImageDistort<T> createDistort( Class<T> imageType , PixelTransform model , TypeInterpolate interpType)
	{
		InterpolatePixel<T> interp = FactoryInterpolation.createPixel(imageType, interpType);
		return createDistort(imageType,model,interp);
	}

	/**
	 * Creates a {@link gecv.alg.distort.ImageDistort} for the specified image type, transformation
	 * and interpolation instance.
	 */
	public static <T extends ImageBase>
	ImageDistort<T> createDistort( Class<T> imageType ,
								   PixelTransform dstToSrc ,
								   InterpolatePixel<T> interp )
	{
		if( imageType == ImageFloat32.class ) {
			return (ImageDistort<T>)new ImplImageDistort_F32(dstToSrc,(InterpolatePixel<ImageFloat32>)interp);
		} else if( ImageInt16.class.isAssignableFrom(imageType) ) {
			return (ImageDistort<T>)new ImplImageDistort_I16(dstToSrc,(InterpolatePixel<ImageInt16>)interp);
		} else if( ImageInt8.class.isAssignableFrom(imageType) ) {
			return (ImageDistort<T>)new ImplImageDistort_I8(dstToSrc,(InterpolatePixel<ImageInt8>)interp);
		} else {
			throw new IllegalArgumentException("Image type not supported: "+imageType.getSimpleName());
		}
	}
}
