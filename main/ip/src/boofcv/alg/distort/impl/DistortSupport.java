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

package boofcv.alg.distort.impl;

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.PixelTransformAffine_F32;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.factory.distort.FactoryDistort;
import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.Planar;
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
	public static PixelTransformAffine_F32 transformScale(ImageBase from, ImageBase to,
														  PixelTransformAffine_F32 distort)
	{
		if( distort == null )
			distort = new PixelTransformAffine_F32();

		float scaleX = (float)(to.width)/(float)(from.width);
		float scaleY = (float)(to.height)/(float)(from.height);

		Affine2D_F32 affine = distort.getModel();
		affine.set(scaleX,0,0,scaleY,0,0);

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
	 * Creates a {@link boofcv.alg.distort.ImageDistort} for the planar images of the specified image type, transformation
	 * and interpolation instance.
	 *
	 * @param dstToSrc Transform from dst to src image.
	 * @param interp Which interpolation algorithm should be used.
	 */
	public static <Input extends ImageGray,Output extends ImageGray>
	ImageDistort<Planar<Input>,Planar<Output>>
	createDistortPL(Class<Output> outputType, PixelTransform2_F32 dstToSrc,
					InterpolatePixelS<Input> interp, boolean cached )
	{
		ImageDistort<Input,Output> bandDistort = FactoryDistort.distortSB(cached, interp, outputType);
		bandDistort.setModel(dstToSrc);
		return new ImplImageDistort_PL<>(bandDistort);
	}
}
