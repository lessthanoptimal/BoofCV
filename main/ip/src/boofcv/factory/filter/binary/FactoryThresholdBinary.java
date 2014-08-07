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

package boofcv.factory.filter.binary;

import boofcv.abst.filter.binary.*;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;

/**
 * Factory for creating various filters which convert an input image into a binary one
 *
 * @author Peter Abeles
 */
public class FactoryThresholdBinary {

	/**
	 * @see boofcv.alg.filter.binary.GThresholdImageOps#adaptiveGaussian(boofcv.struct.image.ImageSingleBand, boofcv.struct.image.ImageUInt8, int, double, boolean, boofcv.struct.image.ImageSingleBand, boofcv.struct.image.ImageSingleBand)
	 *
	 * @param radius Radius of square region.
	 * @param bias Bias used to adjust threshold
	 * @param down Should it threshold up or down.
	 * @param inputType Type of input image
	 * @return Filter to binary
	 */
	public static <T extends ImageSingleBand>
	InputToBinary<T> adaptiveGaussian(int radius, double bias, boolean down, Class<T> inputType) {
		return new AdaptiveGaussianBinaryFilter<T>(radius,bias,down,ImageType.single(inputType));
	}

	/**
	 * @see boofcv.alg.filter.binary.GThresholdImageOps#adaptiveSauvola(boofcv.struct.image.ImageSingleBand, boofcv.struct.image.ImageUInt8, int, float, boolean)
	 *
	 * @param radius Radius of local region.  Try 15
	 * @param k Positive parameter used to tune threshold.  Try 0.3
	 * @param down Should it threshold up or down.
	 * @param inputType Type of input image
	 * @return Filter to binary
	 */
	public static <T extends ImageSingleBand>
	InputToBinary<T> adaptiveSauvola(int radius, float k, boolean down, Class<T> inputType) {
		return new AdaptiveSauvolaBinaryFilter<T>(radius,k,down,ImageType.single(inputType));
	}

	/**
	 * @see boofcv.alg.filter.binary.GThresholdImageOps#adaptiveSquare(boofcv.struct.image.ImageSingleBand, boofcv.struct.image.ImageUInt8, int, double, boolean, boofcv.struct.image.ImageSingleBand, boofcv.struct.image.ImageSingleBand)
	 *
	 * @param radius Radius of square region.
	 * @param bias Bias used to adjust threshold
	 * @param down Should it threshold up or down.
	 * @param inputType Type of input image
	 * @return Filter to binary
	 */
	public static <T extends ImageSingleBand>
	InputToBinary<T> adaptiveSquare(int radius, double bias, boolean down, Class<T> inputType) {
		return new AdaptiveSquareBinaryFilter<T>(radius,bias,down,ImageType.single(inputType));
	}

	/**
	 * @see boofcv.alg.filter.binary.GThresholdImageOps#computeEntropy
	 *
	 * @param minValue The minimum value of a pixel in the image.  (inclusive)
	 * @param maxValue The maximum value of a pixel in the image.  (exclusive)
	 * @param down Should it threshold up or down.
	 * @param inputType Type of input image
	 * @return Filter to binary
	 */
	public static <T extends ImageSingleBand>
	InputToBinary<T> globalEntropy(int minValue, int maxValue, boolean down, Class<T> inputType) {
		return new GlobalEntropyBinaryFilter<T>(minValue,maxValue,down,ImageType.single(inputType));
	}

	/**
	 * @see boofcv.alg.filter.binary.GThresholdImageOps#threshold
	 *
	 * @param threshold threshold value.
	 * @param down If true then the inequality <= is used, otherwise if false then >= is used.
	 * @param inputType Type of input image
	 * @return Filter to binary
	 */
	public static <T extends ImageSingleBand>
	InputToBinary<T> globalFixed(double threshold, boolean down, Class<T> inputType) {
		return new GlobalFixedBinaryFilter<T>(threshold,down,ImageType.single(inputType));
	}

	/**
	 * @see boofcv.alg.filter.binary.GThresholdImageOps#computeOtsu
	 *
	 * @param minValue The minimum value of a pixel in the image.  (inclusive)
	 * @param maxValue The maximum value of a pixel in the image.  (exclusive)
	 * @param down Should it threshold up or down.
	 * @param inputType Type of input image
	 * @return Filter to binary
	 */
	public static <T extends ImageSingleBand>
	InputToBinary<T> globalOtsu(int minValue, int maxValue, boolean down, Class<T> inputType) {
		return new GlobalOtsuBinaryFilter<T>(minValue,maxValue,down,ImageType.single(inputType));
	}
}
