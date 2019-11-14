/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.misc;

import boofcv.struct.image.ImageGray;

import javax.annotation.Nullable;

/**
 * Functions related to adjusting input pixels to ensure they have a known and fixed range. Can handle the
 * conversion of integer to float images. Output is always float.
 *
 * @author Peter Abeles
 */
public class ImageNormalization {

	/**
	 * Applies the normalization to the image.
	 * @param input Input image.
	 * @param parameter Normalziation parameters
	 * @param output Output image. Can be the same instance as the input image.
	 */
	public static void apply(ImageGray input, NormalizeParameters parameter, ImageGray output) {
		GPixelMath.plus(input,parameter.offset,output);
		GPixelMath.divide(output,parameter.divisor,output);
	}

	/**
	 * Ensures that the output image has a mean zero and a max abs(pixel) of 1
	 * @param input Input image
	 * @param output Scaled output image.
	 */
	public static void zeroMeanMaxOne(ImageGray input , ImageGray output , @Nullable NormalizeParameters parameters ) {
		output.reshape(input);
		if( output.getDataType().isInteger() )
			throw new IllegalArgumentException("Output must be a floating point image");

		double mean = GImageStatistics.mean(input);
		GPixelMath.minus(input,mean,output);
		double scale = GImageStatistics.maxAbs(output);
		GPixelMath.divide(output,scale,output);
	
		if( parameters != null ) {
			parameters.offset = -mean;
			parameters.divisor = scale;
		}
	}

	/**
	 * Ensures that the output image has a mean zero and a standard deviation of 1. This is often the recommended
	 * approach.
	 *
	 * @param input Input image
	 * @param output Scaled output image.
	 */
	public static void zeroMeanStdOne(ImageGray input , ImageGray output , @Nullable NormalizeParameters parameters ) {
		output.reshape(input);
		if( output.getDataType().isInteger() )
			throw new IllegalArgumentException("Output must be a floating point image");

		double mean = GImageStatistics.mean(input);
		double stdev = Math.sqrt(GImageStatistics.variance(input,mean));

		GPixelMath.minus(input,mean,output);
		GPixelMath.divide(output,stdev,output);

		if( parameters != null ) {
			parameters.offset = -mean;
			parameters.divisor = stdev;
		}
	}
}
