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

package boofcv.factory.filter.binary;

import boofcv.abst.filter.binary.*;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

/**
 * Factory for creating various filters which convert an input image into a binary one
 *
 * @author Peter Abeles
 */
public class FactoryThresholdBinary {

	/**
	 * @see boofcv.alg.filter.binary.GThresholdImageOps#localGaussian(ImageGray, GrayU8, int, double, boolean, ImageGray, ImageGray)
	 *
	 * @param radius Radius of square region.
	 * @param scale Threshold scale adjustment
	 * @param down Should it threshold up or down.
	 * @param inputType Type of input image
	 * @return Filter to binary
	 */
	public static <T extends ImageGray>
	InputToBinary<T> localGaussian(int radius, double scale, boolean down, Class<T> inputType) {
		return new LocalGaussianBinaryFilter<>(radius, scale, down, ImageType.single(inputType));
	}

	/**
	 * @see boofcv.alg.filter.binary.GThresholdImageOps#localSauvola(ImageGray, GrayU8, int, float, boolean)
	 *
	 * @param radius Radius of local region.  Try 15
	 * @param k User specified threshold adjustment factor.  Must be positive. Try 0.3
	 * @param down Should it threshold up or down.
	 * @param inputType Type of input image
	 * @return Filter to binary
	 */
	public static <T extends ImageGray>
	InputToBinary<T> localSauvola(int radius, float k, boolean down, Class<T> inputType) {
		return new LocalSauvolaBinaryFilter<>(radius, k, down, ImageType.single(inputType));
	}

	/**
	 * @see boofcv.alg.filter.binary.GThresholdImageOps#localSquare(ImageGray, GrayU8, int, double, boolean, ImageGray, ImageGray)
	 *
	 * @param radius Radius of square region.
	 * @param scale Scale factor adjust for threshold.  1.0 means no change.
	 * @param down Should it threshold up or down.
	 * @param inputType Type of input image
	 * @return Filter to binary
	 */
	public static <T extends ImageGray>
	InputToBinary<T> localSquare(int radius, double scale, boolean down, Class<T> inputType) {
		return new LocalSquareBinaryFilter<>(radius, scale, down, ImageType.single(inputType));
	}

	public static <T extends ImageGray>
	InputToBinary<T> localSquareBlockMinMax(int regionWidth, double scale , boolean down,
											double minimumSpread, Class<T> inputType) {
		return new LocalSquareBlockMinMaxBinaryFilter<>(minimumSpread, regionWidth, scale, down, inputType);
	}

	/**
	 * @see boofcv.alg.filter.binary.GThresholdImageOps#computeEntropy
	 *
	 * @param minValue The minimum value of a pixel in the image.  (inclusive)
	 * @param maxValue The maximum value of a pixel in the image.  (inclusive)
	 * @param down Should it threshold up or down.
	 * @param inputType Type of input image
	 * @return Filter to binary
	 */
	public static <T extends ImageGray>
	InputToBinary<T> globalEntropy(int minValue, int maxValue, boolean down, Class<T> inputType) {
		return new GlobalEntropyBinaryFilter<>(minValue, maxValue, down, ImageType.single(inputType));
	}

	/**
	 * @see boofcv.alg.filter.binary.GThresholdImageOps#threshold
	 *
	 * @param threshold threshold value.
	 * @param down If true then the inequality &le; is used, otherwise if false then &ge; is used.
	 * @param inputType Type of input image
	 * @return Filter to binary
	 */
	public static <T extends ImageGray>
	InputToBinary<T> globalFixed(double threshold, boolean down, Class<T> inputType) {
		return new GlobalFixedBinaryFilter<>(threshold, down, ImageType.single(inputType));
	}

	/**
	 * @see boofcv.alg.filter.binary.GThresholdImageOps#computeOtsu
	 *
	 * @param minValue The minimum value of a pixel in the image.  (inclusive)
	 * @param maxValue The maximum value of a pixel in the image.  (inclusive)
	 * @param down Should it threshold up or down.
	 * @param inputType Type of input image
	 * @return Filter to binary
	 */
	public static <T extends ImageGray>
	InputToBinary<T> globalOtsu(int minValue, int maxValue, boolean down, Class<T> inputType) {
		return new GlobalOtsuBinaryFilter<>(minValue, maxValue, down, ImageType.single(inputType));
	}

	/**
	 * Creates threshold using a config class
	 *
	 * @param config Configuration
	 * @param inputType Type of input image
	 * @return The thresholder
	 */
	public static <T extends ImageGray>
	InputToBinary<T> threshold( ConfigThreshold config, Class<T> inputType) {

		switch( config.type ) {
			case FIXED:
				return globalFixed(config.fixedThreshold, config.down, inputType);

			case GLOBAL_OTSU:
				return globalOtsu(config.minPixelValue, config.maxPixelValue, config.down, inputType);

			case GLOBAL_ENTROPY:
				return globalEntropy(config.minPixelValue, config.maxPixelValue, config.down, inputType);

			case LOCAL_GAUSSIAN:
				return localGaussian(config.radius, config.scale, config.down, inputType);

			case LOCAL_SAVOLA:
				return localSauvola(config.radius, config.savolaK, config.down, inputType);

			case LOCAL_SQUARE:
				return localSquare(config.radius, config.scale, config.down, inputType);

			case LOCAL_SQUARE_BLOCK_MIN_MAX: {
				ConfigThresholdBlockMinMax c = (ConfigThresholdBlockMinMax) config;
				return localSquareBlockMinMax(c.radius * 2 + 1, c.scale , c.down, c.minimumSpread, inputType);
			}
		}
		throw new IllegalArgumentException("Unknown type "+config.type);
	}
}
