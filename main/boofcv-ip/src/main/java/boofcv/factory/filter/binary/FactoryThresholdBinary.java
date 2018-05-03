/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.filter.binary.*;
import boofcv.alg.filter.binary.impl.*;
import boofcv.struct.ConfigLength;
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
	 * @param regionWidth Width of square region.
	 * @param scale Threshold scale adjustment
	 * @param down Should it threshold up or down.
	 * @param inputType Type of input image
	 * @return Filter to binary
	 */
	public static <T extends ImageGray<T>>
	InputToBinary<T> localGaussian(ConfigLength regionWidth, double scale, boolean down, Class<T> inputType) {
		if( BOverrideFactoryThresholdBinary.localGaussian != null )
			return BOverrideFactoryThresholdBinary.localGaussian.handle(regionWidth, scale, down, inputType);
		return new LocalGaussianBinaryFilter<>(regionWidth, scale, down, ImageType.single(inputType));
	}

	/**
	 * @see boofcv.alg.filter.binary.GThresholdImageOps#localSauvola(ImageGray, GrayU8, ConfigLength, float, boolean)
	 *
	 * @param width Width of square region.
	 * @param k User specified threshold adjustment factor.  Must be positive. Try 0.3
	 * @param down Should it threshold up or down.
	 * @param inputType Type of input image
	 * @return Filter to binary
	 */
	public static <T extends ImageGray<T>>
	InputToBinary<T> localSauvola(ConfigLength width, float k, boolean down, Class<T> inputType)
	{
		if( BOverrideFactoryThresholdBinary.localSauvola != null )
			return BOverrideFactoryThresholdBinary.localSauvola.handle(width, k, down, inputType);
		return new InputToBinarySwitch<T>(new ThresholdSauvola(width, k, down),inputType);
	}

	/**
	 * @see boofcv.alg.filter.binary.GThresholdImageOps#localMean(ImageGray, GrayU8, ConfigLength, double, boolean, ImageGray, ImageGray)
	 *
	 * @param width Width of square region.
	 * @param scale Scale factor adjust for threshold.  1.0 means no change.
	 * @param down Should it threshold up or down.
	 * @param inputType Type of input image
	 * @return Filter to binary
	 */
	public static <T extends ImageGray<T>>
	InputToBinary<T> localMean(ConfigLength width, double scale, boolean down, Class<T> inputType) {
		if( BOverrideFactoryThresholdBinary.localMean != null )
			return BOverrideFactoryThresholdBinary.localMean.handle(width, scale, down, inputType);
		return new LocalMeanBinaryFilter<>(width, scale, down, ImageType.single(inputType));
	}

	/**
	 * Applies a local Otsu threshold
	 *
	 * @see ThresholdLocalOtsu
	 *
	 * @param scale Scale factor adjust for threshold.  1.0 means no change.
	 * @param down Should it threshold up or down.
	 * @param regionWidth About how wide and tall you wish a block to be in pixels.
	 * @param inputType Type of input image
	 * @return Filter to binary
	 */
	public static <T extends ImageGray<T>>
	InputToBinary<T> localOtsu(boolean otsu2, ConfigLength regionWidth , double tuning, double scale, boolean down, Class<T> inputType) {
		if( BOverrideFactoryThresholdBinary.localOtsu != null )
			return BOverrideFactoryThresholdBinary.localOtsu.handle(otsu2,regionWidth, tuning, scale, down, inputType);
		return new InputToBinarySwitch<>(new ThresholdLocalOtsu(otsu2,regionWidth,tuning,scale,down),inputType);
	}

	/**
	 * Applies a very fast non-overlapping block thresholding algorithm which uses min/max statistics.
	 *
	 * @see ThresholdBlockMinMax
	 *
	 * @param scale Scale factor adjust for threshold.  1.0 means no change.
	 * @param down Should it threshold up or down.
	 * @param minimumSpread If the difference between min max is less than or equal to this
	 *                         value then it is considered textureless.  Set to &le; -1 to disable.
	 * @param regionWidth Approximate size of block region
	 * @param inputType Type of input image
	 * @return Filter to binary
	 */
	public static <T extends ImageGray<T>>
	InputToBinary<T> blockMinMax(ConfigLength regionWidth, double scale , boolean down,
								 double minimumSpread, boolean thresholdFromLocalBlocks, Class<T> inputType) {
		if( BOverrideFactoryThresholdBinary.blockMinMax != null )
			return BOverrideFactoryThresholdBinary.blockMinMax.handle(regionWidth, scale, down,
					minimumSpread,thresholdFromLocalBlocks, inputType);
		if( inputType == GrayU8.class )
			return (InputToBinary<T>)new ThresholdBlockMinMax_U8(minimumSpread,regionWidth,scale,down,thresholdFromLocalBlocks);
		else
			return (InputToBinary<T>)new ThresholdBlockMinMax_F32((float)minimumSpread,regionWidth,(float)scale,down,thresholdFromLocalBlocks);
	}

	/**
	 * Applies a non-overlapping block mean threshold
	 *
	 * @see ThresholdBlockMean
	 *
	 * @param scale Scale factor adjust for threshold.  1.0 means no change.
	 * @param down Should it threshold up or down.
	 * @param regionWidth Approximate size of block region
	 * @param inputType Type of input image
	 * @return Filter to binary
	 */
	public static <T extends ImageGray<T>>
	InputToBinary<T> blockMean(ConfigLength regionWidth, double scale , boolean down, boolean thresholdFromLocalBlocks,
							   Class<T> inputType) {
		if( BOverrideFactoryThresholdBinary.blockMean != null )
			return BOverrideFactoryThresholdBinary.blockMean.handle(regionWidth, scale, down,
					thresholdFromLocalBlocks, inputType);
		if( inputType == GrayU8.class )
			return (InputToBinary<T>)new ThresholdBlockMean_U8(regionWidth,scale,down,thresholdFromLocalBlocks);
		else
			return (InputToBinary<T>)new ThresholdBlockMean_F32(regionWidth,scale,down,thresholdFromLocalBlocks);
	}

	/**
	 * Applies a non-overlapping block Otsu threshold
	 *
	 * @see ThresholdBlockOtsu
	 *
	 * @param scale Scale factor adjust for threshold.  1.0 means no change.
	 * @param down Should it threshold up or down.
	 * @param regionWidth Approximate size of block region
	 * @param inputType Type of input image
	 * @return Filter to binary
	 */
	public static <T extends ImageGray<T>>
	InputToBinary<T> blockOtsu( boolean otsu2, ConfigLength regionWidth , double tuning, double scale, boolean down,
							   boolean thresholdFromLocalBlocks, Class<T> inputType) {
		if( BOverrideFactoryThresholdBinary.blockOtsu != null )
			return BOverrideFactoryThresholdBinary.blockOtsu.handle(otsu2,regionWidth, tuning, scale, down,
					thresholdFromLocalBlocks, inputType);
		return new InputToBinarySwitch<>(new ThresholdBlockOtsu(otsu2,regionWidth,tuning,scale,down,
				thresholdFromLocalBlocks),inputType);
	}

	/**
	 * @see GThresholdImageOps#computeEntropy
	 *
	 * @param minValue The minimum value of a pixel in the image.  (inclusive)
	 * @param maxValue The maximum value of a pixel in the image.  (inclusive)
	 * @param down Should it threshold up or down.
	 * @param inputType Type of input image
	 * @return Filter to binary
	 */
	public static <T extends ImageGray<T>>
	InputToBinary<T> globalEntropy(int minValue, int maxValue, boolean down, Class<T> inputType) {
		if( BOverrideFactoryThresholdBinary.globalEntropy != null )
			return BOverrideFactoryThresholdBinary.globalEntropy.handle(minValue, maxValue, down, inputType);
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
	public static <T extends ImageGray<T>>
	InputToBinary<T> globalFixed(double threshold, boolean down, Class<T> inputType) {
		if( BOverrideFactoryThresholdBinary.globalFixed != null )
			return BOverrideFactoryThresholdBinary.globalFixed.handle(threshold, down, inputType);
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
	public static <T extends ImageGray<T>>
	InputToBinary<T> globalOtsu(double minValue, double maxValue, boolean down, Class<T> inputType) {
		if( BOverrideFactoryThresholdBinary.globalOtsu != null )
			return BOverrideFactoryThresholdBinary.globalOtsu.handle(minValue, maxValue, down, inputType);
		return new GlobalOtsuBinaryFilter<>(minValue, maxValue, down, ImageType.single(inputType));
	}

	/**
	 * Creates threshold using a config class
	 *
	 * @param config Configuration
	 * @param inputType Type of input image
	 * @return The thresholder
	 */
	public static <T extends ImageGray<T>>
	InputToBinary<T> threshold( ConfigThreshold config, Class<T> inputType) {

		switch( config.type ) {
			case FIXED:
				return globalFixed(config.fixedThreshold, config.down, inputType);

			case GLOBAL_OTSU:
				return globalOtsu(config.minPixelValue, config.maxPixelValue, config.down, inputType);

			case GLOBAL_ENTROPY:
				return globalEntropy(config.minPixelValue, config.maxPixelValue, config.down, inputType);

			case LOCAL_GAUSSIAN:
				return localGaussian(config.width, config.scale, config.down, inputType);

			case LOCAL_SAVOLA:
				return localSauvola(config.width, config.savolaK, config.down, inputType);

			case LOCAL_MEAN:
				return localMean(config.width, config.scale, config.down, inputType);

			case LOCAL_OTSU: {
				ConfigThresholdLocalOtsu c = (ConfigThresholdLocalOtsu) config;
				return localOtsu(c.useOtsu2,config.width, c.tuning, config.scale,config.down, inputType);
			}

			case BLOCK_MIN_MAX: {
				ConfigThresholdBlockMinMax c = (ConfigThresholdBlockMinMax) config;
				return blockMinMax(c.width, c.scale , c.down, c.minimumSpread,
						c.thresholdFromLocalBlocks, inputType);
			}

			case BLOCK_MEAN:
				return blockMean(config.width, config.scale , config.down,
						config.thresholdFromLocalBlocks, inputType);

			case BLOCK_OTSU: {
				ConfigThresholdLocalOtsu c = (ConfigThresholdLocalOtsu) config;
				return blockOtsu(c.useOtsu2,c.width, c.tuning, c.scale, c.down,
						c.thresholdFromLocalBlocks, inputType);
			}

		}
		throw new IllegalArgumentException("Unknown type "+config.type);
	}
}
