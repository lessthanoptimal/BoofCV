/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.processing;

import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;
import processing.core.PConstants;
import processing.core.PImage;

/**
 * High level interface for handling gray scale images
 *
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class SimpleColor extends SimpleImage<MultiSpectral>{

	public SimpleColor(MultiSpectral image) {
		super(image);
	}

	public SimpleColor blurMean( int radius ) {
		return new SimpleColor(GBlurImageOps.mean(image, null, radius, null));
	}

	public SimpleColor blurMedian( int radius ) {
		return new SimpleColor(GBlurImageOps.median(image, null, radius));
	}

	/**
	 * @see boofcv.alg.filter.blur.GBlurImageOps#gaussian
	 */
	public SimpleColor blurGaussian( double sigma, int radius ) {
		return new SimpleColor(GBlurImageOps.gaussian(image, null, sigma, radius, null));
	}

	/**
	 * Converts the color image into a gray scale image by averaged each pixel across the bands
	 */
	public SimpleGray grayMean() {
		ImageSingleBand out =
				GeneralizedImageOps.createSingleBand(image.imageType.getDataType(),image.width,image.height);

		GConvertImage.average(image, out);

		return new SimpleGray(out);
	}

	public SimpleGray getBand( int band ) {
		return new SimpleGray(image.getBand(band));
	}

	public int getNumberOfBands() {
		return image.getNumBands();
	}

	public PImage convert() {
		PImage out = new PImage(image.width,image.height, PConstants.RGB);
		if( image.getType() == ImageFloat32.class) {
			ConvertProcessing.convert_MSF32_RGB(image, out);
		} else if( image.getType() == ImageUInt8.class ) {
			ConvertProcessing.convert_MSU8_RGB(image,out);
		} else {
			throw new RuntimeException("Unknown image type");
		}
		return out;
	}
}
