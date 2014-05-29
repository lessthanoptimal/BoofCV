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

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.alg.misc.GImageStatistics;
import boofcv.alg.misc.ImageStatistics;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageInteger;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import processing.core.PConstants;
import processing.core.PImage;

/**
 * High level interface for handling gray scale images
 *
 * @author Peter Abeles
 */
// TODO gradient Abs and Sign
@SuppressWarnings("unchecked")
public class SimpleGray {
	ImageSingleBand image;

	public SimpleGray(ImageSingleBand image) {
		this.image = image;
	}

	public SimpleGray blurMean( int radius ) {
		return new SimpleGray(GBlurImageOps.mean(image, null, radius, null));
	}

	public SimpleGray blurMedian( int radius ) {
		return new SimpleGray(GBlurImageOps.median(image, null, radius));
	}

	/**
	 * @see GBlurImageOps#gaussian
	 */
	public SimpleGray blurGaussian( double sigma, int radius ) {
		return new SimpleGray(GBlurImageOps.gaussian(image, null, sigma, radius, null));
	}

	/**
	 * @see GThresholdImageOps#threshold
	 */
	public SimpleBinary threshold(double threshold, boolean down ) {
		return new SimpleBinary(GThresholdImageOps.threshold(image, null, threshold, down));
	}

	/**
	 * @see GThresholdImageOps#adaptiveSquare
	 */
	public SimpleBinary thresholdSquare( int radius, double bias, boolean down ) {
		return new SimpleBinary(GThresholdImageOps.adaptiveSquare(image, null,radius,bias,down,null,null));
	}

	/**
	 * @see GThresholdImageOps#adaptiveGaussian
	 */
	public SimpleBinary thresholdGaussian( int radius, double bias, boolean down ) {
		return new SimpleBinary(GThresholdImageOps.adaptiveGaussian(image, null,radius,bias,down,null,null));
	}

	public SimpleGradient gradientSobel() {
		return gradient(FactoryDerivative.sobel(image.getClass(),null));
	}

	public SimpleGradient gradientPrewitt() {
		return gradient(FactoryDerivative.prewitt(image.getClass(), null));
	}

	public SimpleGradient gradientThree() {
		return gradient(FactoryDerivative.three(image.getClass(), null));
	}

	public SimpleGradient gradientTwo() {
		return gradient(FactoryDerivative.two(image.getClass(), null));
	}

	/**
	 * @see GImageStatistics#mean
	 */
	public double mean() {
		return GImageStatistics.mean(image);
	}

	/**
	 * @see GImageStatistics#max
	 */
	public double max() {
		return GImageStatistics.max(image);
	}

	/**
	 * @see GImageStatistics#maxAbs
	 */
	public double maxAbs() {
		return GImageStatistics.maxAbs(image);
	}

	/**
	 * @see GImageStatistics#sum
	 */
	public double sum() {
		return GImageStatistics.sum(image);
	}


	private SimpleGradient gradient(ImageGradient gradient) {
		SimpleGradient ret = new SimpleGradient(gradient.getDerivType(),image.width,image.height);
		gradient.process(image,ret.dx,ret.dy);

		return ret;
	}

	public PImage visualizeSign() {
		if( image instanceof ImageFloat32) {
			float max = ImageStatistics.maxAbs((ImageFloat32) image);
			return VisualizeProcessing.colorizeSign((ImageFloat32)image,max);
		} else if( image instanceof ImageInteger) {
			int max = (int)GImageStatistics.maxAbs(image);
			return VisualizeProcessing.colorizeSign((ImageInteger) image, max);
		} else {
			throw new RuntimeException("Unknown image type");
		}
	}

	public PImage convert() {
		PImage out = new PImage(image.width,image.height, PConstants.RGB);
		if( image instanceof ImageFloat32) {
			ConvertProcessing.convert_F32_RGB((ImageFloat32)image,out);
		} else if( image instanceof ImageUInt8 ) {
			ConvertProcessing.convert_U8_RGB((ImageUInt8) image, out);
		} else {
			throw new RuntimeException("Unknown image type");
		}
		return out;
	}

	public ImageSingleBand getImage() {
		return image;
	}
}
