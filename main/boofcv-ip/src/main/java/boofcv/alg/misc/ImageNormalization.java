/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import org.jetbrains.annotations.Nullable;

/**
 * Functions related to adjusting input pixels to ensure they have a known and fixed range. Can handle the
 * conversion of integer to float images. Output is always float.
 *
 * @author Peter Abeles
 */
public class ImageNormalization {

	/**
	 * Applies the normalization to the image.
	 *
	 * @param input Input image.
	 * @param parameter Normalziation parameters
	 * @param output Output image. Can be the same instance as the input image.
	 */
	public static void apply( ImageGray input, NormalizeParameters parameter, ImageGray output ) {
		GPixelMath.plus(input, parameter.offset, output);
		GPixelMath.multiply(output, 1.0f/parameter.divisor, output);
	}

	/**
	 * Normalizes the image so that the max abs of the image is 1.
	 *
	 * @param input Input image
	 * @param output Scaled output image.
	 * @param parameters the parameters
	 */
	public static void maxAbsOfOne( ImageGray input, ImageGray output, @Nullable NormalizeParameters parameters ) {
		output.reshape(input);
		if (output.getDataType().isInteger())
			throw new IllegalArgumentException("Output must be a floating point image");
		double scale = GImageStatistics.maxAbs(input);

		if (scale == 0.0) {
			scale = 1.0;
		} else {
			GPixelMath.multiply(input, 1.0f/scale, output);
		}
		if (parameters != null) {
			parameters.offset = 0.0;
			parameters.divisor = scale;
		}
	}

	/**
	 * Ensures that the output image has a mean zero and a max abs(pixel) of 1
	 *
	 * @param input Input image
	 * @param output Scaled output image.
	 */
	public static void zeroMeanMaxOne( ImageGray input, ImageGray output, @Nullable NormalizeParameters parameters ) {
		output.reshape(input);
		if (output.getDataType().isInteger())
			throw new IllegalArgumentException("Output must be a floating point image");

		// Numerical errors is a concern and if you sum up the input it could overflow
		double scale = GImageStatistics.maxAbs(input);
		if (scale != 0.0) {
			GPixelMath.multiply(input, 1.0f/scale, output);
			// Work with this scaled image
			double mean = GImageStatistics.mean(output);
			GPixelMath.minus(output, mean, output);
			double scale2;
			if (input.getDataType().isSigned()) {
				scale2 = GImageStatistics.maxAbs(output);
			} else {
				// image is scaled from 0 to 1.0
				scale2 = mean < 0.5 ? 1.0 - mean : mean;
			}
			if (scale2 != 0.0)
				GPixelMath.multiply(output, 1.0f/scale2, output);
			else
				scale2 = 1.0;

			if (parameters != null) {
				parameters.offset = -mean*scale;
				parameters.divisor = scale*scale2;
			}
		} else {
			if (parameters != null) {
				parameters.offset = 0.0;
				parameters.divisor = 1.0;
			}
		}
	}

	/**
	 * Ensures that the output image has a mean zero and a standard deviation of 1. This is often the recommended
	 * approach.
	 *
	 * @param input Input image
	 * @param output Scaled output image.
	 */
	public static void zeroMeanStdOne( ImageGray input, ImageGray output, @Nullable NormalizeParameters parameters ) {
		output.reshape(input);
		if (output.getDataType().isInteger())
			throw new IllegalArgumentException("Output must be a floating point image");

		// avoid overflow
		double scale = GImageStatistics.maxAbs(input);
		if (scale != 0.0) {
			GPixelMath.multiply(input, 1.0f/scale, output);
			double mean = GImageStatistics.mean(output);
			double stdev = Math.sqrt(GImageStatistics.variance(output, mean));

			GPixelMath.minus(output, mean, output);
			if (stdev != 0.0) {
				GPixelMath.multiply(output, 1.0f/stdev, output);
			} else {
				stdev = 1.0;
			}

			if (parameters != null) {
				parameters.offset = -mean*scale;
				parameters.divisor = stdev*scale;
			}
		} else {
			if (parameters != null) {
				parameters.offset = 0.0;
				parameters.divisor = 1.0;
			}
		}
	}
}
