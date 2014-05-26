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

package boofcv.alg.distort.impl;

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.PixelTransformAffine_F32;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.core.image.border.ImageBorder;
import boofcv.factory.distort.FactoryDistort;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.affine.Affine2D_F32;
import georegression.struct.se.Se2_F32;
import georegression.transform.ConvertTransform_F32;
import georegression.transform.InvertibleTransformSequence;


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
	public static PixelTransformAffine_F32 transformScale(ImageBase from, ImageBase to)
	{
		float scaleX = (float)(to.width-1)/(float)(from.width-1);
		float scaleY = (float)(to.height-1)/(float)(from.height-1);

		Affine2D_F32 affine = new Affine2D_F32(scaleX,0,0,scaleY,0,0);
		PixelTransformAffine_F32 distort = new PixelTransformAffine_F32();
		distort.set(affine);

		return distort;
	}

	/**
	 * Creates a {@link boofcv.alg.distort.PixelTransformAffine_F32} from the dst image into the src image.
	 *
	 * @param x0 Center of rotation in input image coordinates.
	 * @param y0 Center of rotation in input image coordinates.
	 * @param x1 Center of rotation in output image coordinates.
	 * @param y1 Center of rotation in output image coordinates.
	 * @param angle Angle of rotation.
	 */
	public static PixelTransformAffine_F32 transformRotate( float x0 , float y0 , float x1 , float y1 , float angle )
	{
		// make the coordinate system's origin the image center
		Se2_F32 imageToCenter = new Se2_F32(-x0,-y0,0);
		Se2_F32 rotate = new Se2_F32(0,0,angle);
		Se2_F32 centerToImage = new Se2_F32(x1,y1,0);

		InvertibleTransformSequence sequence = new InvertibleTransformSequence();
		sequence.addTransform(true,imageToCenter);
		sequence.addTransform(true,rotate);
		sequence.addTransform(true,centerToImage);

		Se2_F32 total = new Se2_F32();
		sequence.computeTransform(total);
		Se2_F32 inv = total.invert(null);

		Affine2D_F32 affine = ConvertTransform_F32.convert(inv,(Affine2D_F32)null);
		PixelTransformAffine_F32 distort = new PixelTransformAffine_F32();
		distort.set(affine);

		return distort;
	}

	/**
	 * Creates a {@link boofcv.alg.distort.ImageDistort} for the multi-spectral images of the specified image type, transformation
	 * and interpolation instance.
	 *
	 * @param dstToSrc Transform from dst to src image.
	 * @param interp Which interpolation algorithm should be used.
	 * @param border Specifies how requests to pixels outside the image should be handled.  If null then no change
	 *               happens to pixels which have a source pixel outside the image.
	 */
	public static <T extends ImageSingleBand>
	ImageDistort<MultiSpectral<T>> createDistortMS(Class<T> imageType,
												   PixelTransform_F32 dstToSrc,
												   InterpolatePixelS<T> interp, ImageBorder border)
	{
		ImageDistort<T> bandDistort = FactoryDistort.distort(interp, border, imageType);
		bandDistort.setModel(dstToSrc);
		return new ImplImageDistort_MS<T>(bandDistort);
	}
}
