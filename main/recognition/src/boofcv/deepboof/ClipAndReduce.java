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

package boofcv.deepboof;

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.PixelTransformAffine_F32;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.core.image.border.BorderType;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.affine.Affine2D_F32;

/**
 * Transforms an image in an attempt to not change the information contained inside of it for processing by
 * a classification algorithm that requires an image of fixed size.  This is done by clipping the image to
 * ensure that it has the same aspect ratio then scales it using bilinear interpolation.
 *
 * @author Peter Abeles
 */
public class ClipAndReduce<T extends ImageBase<T>> {


	// storage for clipped sub-region
	T clipped;

	// affine transform that specifies how to scale the image
	Affine2D_F32 transform = new Affine2D_F32();
	// class used to distort the image/shrink it
	ImageDistort<T,T> distort;

	boolean clip;

	/**
	 * Configuration constructor
	 * @param clip If true it will first clip the image to make it square before reducing it's size.  Otherwise both
	 *             sides are scaled independently to make it square
	 * @param imageType Type of image it iwll be processing
	 */
	public ClipAndReduce( boolean clip , ImageType<T> imageType ) {
		this.clip = clip;
		InterpolatePixel<T> interp =
				FactoryInterpolation.createPixel(0,255, InterpolationType.BILINEAR, BorderType.EXTENDED,imageType);
		distort = FactoryDistort.distort(false,interp,imageType);
		distort.setModel(new PixelTransformAffine_F32(transform));
	}

	/**
	 * Clipps and scales the input iamge as neccisary
	 * @param input Input image.  Typically larger than output
	 * @param output Output image
	 */
	public void massage( T input , T output ) {
		if( clip ) {
			T inputAdjusted = clipInput(input, output);

			// configure a simple change in scale for both axises
			transform.a11 = input.width / (float) output.width;
			transform.a22 = input.height / (float) output.height;
			// this change is automatically reflected in the distortion class.  It is configured to cache nothing

			distort.apply(inputAdjusted, output);
		} else {
			// scale each axis independently.  It will have the whole image but it will be distorted
			transform.a11 = input.width / (float) output.width;
			transform.a22 = input.height / (float) output.height;

			distort.apply(input, output);
		}

	}

	/**
	 * Clip the input image to ensure a constant aspect ratio
	 */
	T clipInput(T input, T output) {
		double ratioInput = input.width/(double)input.height;
		double ratioOutput = output.width/(double)output.height;

		T a = input;

		if( ratioInput > ratioOutput ) { // clip the width
			int width = input.height*output.width/output.height;
			int x0 = (input.width-width)/2;
			int x1 = x0 + width;

			clipped = input.subimage(x0,0,x1,input.height, clipped);
			a = clipped;
		} else if( ratioInput < ratioOutput ) { // clip the height
			int height = input.width*output.height/output.width;
			int y0 = (input.height-height)/2;
			int y1 = y0 + height;

			clipped = input.subimage(0,y0,input.width,y1, clipped);
			a = clipped;
		}

		return a;
	}
}
