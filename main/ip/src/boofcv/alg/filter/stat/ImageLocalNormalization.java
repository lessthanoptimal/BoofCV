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

package boofcv.alg.filter.stat;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.alg.filter.convolve.GConvolveImageOps;
import boofcv.alg.misc.GImageStatistics;
import boofcv.alg.misc.GPixelMath;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.BorderType;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.struct.convolve.Kernel1D;
import boofcv.struct.image.GrayF;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayF64;
import boofcv.struct.image.ImageType;

/**
 * Provides different functions for normalizing the spatially local statics of an image.
 *
 * @author Peter Abeles
 */
public class ImageLocalNormalization<T extends GrayF> {

	// storage for the adjusted input which has a max pixel value of 1
	protected T adjusted;
	// storage for the local image mean
	protected T localMean;
	// storage for input pixel elements to the power of 2
	protected T pow2;
	// storage for locally weighted power of 2
	protected T localPow2;

	protected Class<T> imageType;

	// handle the image border.  If null then normalization is used
	ImageBorder<T> border;

	/**
	 * Configures normalization
	 *
	 * @param imageType Type of input image
	 * @param borderType How image borders are handled.  {@link BorderType#NORMALIZED} is recommended
	 */
	public ImageLocalNormalization( Class<T> imageType , BorderType borderType ) {
		this.imageType = imageType;

		if( borderType != BorderType.NORMALIZED )
			border = FactoryImageBorder.generic(borderType, ImageType.single(imageType));

		adjusted = GeneralizedImageOps.createSingleBand(imageType,1,1);
		localMean = GeneralizedImageOps.createSingleBand(imageType,1,1);
		pow2 = GeneralizedImageOps.createSingleBand(imageType,1,1);
		localPow2 = GeneralizedImageOps.createSingleBand(imageType,1,1);
	}

	/*
		 * <p>Normalizes the input image such that local weighted statics are a zero mean and with standard deviation
		 * of 1.  The image border is handled by truncating the kernel and renormalizing it so that it's sum is
		 * still one.</p>
		 *
		 * <p>output[x,y] = (input[x,y]-mean[x,y])/(stdev[x,y] + delta)</p>
		 *
		 * @param kernel Separable kernel.  Typically Gaussian
		 * @param input Input image
		 * @param maxPixelValue maximum value of a pixel element in the input image. -1 = compute max value.
		 * Typically this is 255 or 1.
		 * @param delta A small value used to avoid divide by zero errors.  Typical 1e-4f for 32 bit and 1e-8 for 64bit
		 * @param output Storage for output
		 */
	public void zeroMeanStdOne(Kernel1D kernel, T input , double maxPixelValue , double delta , T output ) {
		// check preconditions and initialize data structures
		initialize(input, output);

		// avoid overflow issues by ensuring that the max pixel value is 1
		T adjusted = ensureMaxValueOfOne(input, maxPixelValue);

		// take advantage of 2D gaussian kernels being separable
		if( border == null ) {
			GConvolveImageOps.horizontalNormalized(kernel, adjusted, output);
			GConvolveImageOps.verticalNormalized(kernel, output, localMean);
			GPixelMath.pow2(adjusted, pow2);
			GConvolveImageOps.horizontalNormalized(kernel, pow2, output);
			GConvolveImageOps.verticalNormalized(kernel, output, localPow2);
		} else {
			GConvolveImageOps.horizontal(kernel, adjusted, output, border);
			GConvolveImageOps.vertical(kernel, output, localMean, border);
			GPixelMath.pow2(adjusted, pow2);
			GConvolveImageOps.horizontal(kernel, pow2, output, border);
			GConvolveImageOps.vertical(kernel, output, localPow2, border);
		}

		// Compute the final output
		if( imageType == GrayF32.class )
			computeOutput((GrayF32)input, (float)delta, (GrayF32)output, (GrayF32)adjusted);
		else
			computeOutput((GrayF64)input, delta, (GrayF64)output, (GrayF64)adjusted);
	}

	/*
	 * <p>Normalizes the input image such that local statics are a zero mean and with standard deviation
	 * of 1.  The image border is handled by truncating the kernel and renormalizing it so that it's sum is
	 * still one.</p>
	 *
	 * <p>output[x,y] = (input[x,y]-mean[x,y])/(stdev[x,y] + delta)</p>
	 *
	 * @param input Input image
	 * @param maxPixelValue maximum value of a pixel element in the input image. -1 = compute max value.  Typically
	 * this is 255 or 1.
	 * @param delta A small value used to avoid divide by zero errors.  Typical 1e-4f for 32 bit and 1e-8 for 64bit
	 * @param output Storage for output
	 */
	public void zeroMeanStdOne( int radius , T input , double maxPixelValue , double delta , T output ) {
		// check preconditions and initialize data structures
		initialize(input, output);

		// avoid overflow issues by ensuring that the max pixel value is 1
		T adjusted = ensureMaxValueOfOne(input, maxPixelValue);

		// take advantage of 2D gaussian kernels being separable
		if( border == null ) {
			GBlurImageOps.mean(adjusted, localMean, radius, output);
			GPixelMath.pow2(adjusted, pow2);
			GBlurImageOps.mean(pow2, localPow2, radius, output);
		} else {
			throw new IllegalArgumentException("Only renormalize border supported here so far.  This can be changed...");
		}

		// Compute the final output
		if( imageType == GrayF32.class )
			computeOutput((GrayF32)input, (float)delta, (GrayF32)output, (GrayF32)adjusted);
		else
			computeOutput((GrayF64)input, delta, (GrayF64)output, (GrayF64)adjusted);
	}

	private void initialize(T input, T output) {
		InputSanityCheck.checkSameShape(input,output);

		adjusted.reshape(input.width,input.height);
		localMean.reshape(input.width,input.height);
		pow2.reshape(input.width,input.height);
		localPow2.reshape(input.width,input.height);
	}

	private void computeOutput(GrayF32 input, float delta, GrayF32 output, GrayF32 adjusted) {
		GrayF32 localMean = (GrayF32)this.localMean;
		GrayF32 localPow2 = (GrayF32)this.localPow2;

		for (int y = 0; y < input.height; y++) {
			int indexIn = y*input.width;
			int indexEnd = indexIn+input.width;
			int indexOut = output.startIndex + y*output.stride;

			while( indexIn < indexEnd ) {

				float ave = localMean.data[indexIn];
				float std = (float)Math.sqrt(localPow2.data[indexIn] - ave*ave + delta);

				output.data[indexOut++] = (adjusted.data[indexIn]-ave)/std;
				indexIn++;
			}
		}
	}

	private void computeOutput(GrayF64 input, double delta, GrayF64 output, GrayF64 adjusted) {
		GrayF64 localMean = (GrayF64)this.localMean;
		GrayF64 localPow2 = (GrayF64)this.localPow2;

		for (int y = 0; y < input.height; y++) {
			int indexIn = y*input.width;
			int indexEnd = indexIn+input.width;
			int indexOut = output.startIndex + y*output.stride;

			while( indexIn < indexEnd ) {

				double ave = localMean.data[indexIn];
				double std = Math.sqrt(localPow2.data[indexIn] - ave*ave + delta);

				output.data[indexOut++] = (adjusted.data[indexIn]-ave)/std;
				indexIn++;
			}
		}
	}

	private T ensureMaxValueOfOne(T input, double maxPixelValue) {
		T adjusted;
		if( maxPixelValue < 0 ) {
			maxPixelValue = GImageStatistics.max(input);
		}
		if( maxPixelValue != 1.0f ) {
			adjusted = this.adjusted;
			GPixelMath.divide(input, maxPixelValue, adjusted);
		} else {
			adjusted = input;
		}
		return adjusted;
	}

	public Class<T> getImageType() {
		return imageType;
	}
}
