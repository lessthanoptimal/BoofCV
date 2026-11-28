/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.filter.binary.ThresholdBlock.BlockProcessor;
import boofcv.alg.filter.binary.impl.ThresholdBlockMean_F32;
import boofcv.alg.filter.binary.impl.ThresholdBlockMean_U8;
import boofcv.alg.filter.binary.impl.ThresholdBlockMinMax_F32;
import boofcv.alg.filter.binary.impl.ThresholdBlockMinMax_U8;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.ConfigLength;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/// Factory for creating various filters which convert an input image into a binary one
@SuppressWarnings({"unchecked", "rawtypes"})
public class FactoryThresholdBinary {

	/// [LocalGaussianBinaryFilter]
	///
	/// @param regionWidth Width of square region.
	/// @param scale Threshold scale adjustment
	/// @param down Should it threshold up or down.
	/// @param inputType Type of input image
	/// @return Filter to binary
	public static <T extends ImageGray<T>>
	InputToBinary<T> localGaussian( ConfigLength regionWidth, double scale, boolean down, Class<T> inputType ) {
		InputToBinary<T> filter = override(BOverrideFactoryThresholdBinary.localGaussian,
				hook -> hook.handle(regionWidth, scale, down, inputType));
		if (filter != null)
			return filter;
		return new LocalGaussianBinaryFilter<>(regionWidth, scale, down, ImageType.single(inputType));
	}

	/// [ThresholdNiblackFamily]
	///
	/// @param width Width of square region.
	/// @param down Should it threshold up or down.
	/// @param k User specified threshold adjustment factor. Must be positive. Try 0.3
	/// @param inputType Type of input image
	/// @return Filter to binary
	public static <T extends ImageGray<T>>
	InputToBinary<T> localSauvola( ConfigLength width, boolean down, float k, Class<T> inputType ) {
		return localNiblackFamily(ThresholdNiblackFamily.Variant.SAUVOLA, width, down, k, inputType);
	}

	/// [ThresholdNiblackFamily]
	///
	/// @param width Width of square region.
	/// @param down Should it threshold up or down.
	/// @param k User specified threshold adjustment factor. Must be positive. Try 0.3
	/// @param inputType Type of input image
	/// @return Filter to binary
	public static <T extends ImageGray<T>>
	InputToBinary<T> localWolf( ConfigLength width, boolean down, float k, Class<T> inputType ) {
		return localNiblackFamily(ThresholdNiblackFamily.Variant.WOLF_JOLION, width, down, k, inputType);
	}

	/// [ThresholdNiblackFamily]
	///
	/// @param width Width of square region.
	/// @param down Should it threshold up or down.
	/// @param k User specified threshold adjustment factor. Must be positive. Try 0.3
	/// @param inputType Type of input image
	/// @return Filter to binary
	public static <T extends ImageGray<T>>
	InputToBinary<T> localNiblack( ConfigLength width, boolean down, float k, Class<T> inputType ) {
		return localNiblackFamily(ThresholdNiblackFamily.Variant.NIBLACK, width, down, k, inputType);
	}

	/// [ThresholdNiblackFamily]
	///
	/// @param variant Which variant in the family
	/// @param width Width of square region.
	/// @param down Should it threshold up or down.
	/// @param k User specified threshold adjustment factor. Must be positive. Try 0.3
	/// @param inputType Type of input image
	/// @return Filter to binary
	protected static <T extends ImageGray<T>>
	InputToBinary<T> localNiblackFamily( ThresholdNiblackFamily.Variant variant,
										 ConfigLength width, boolean down, float k, Class<T> inputType ) {
		InputToBinary<T> filter = override(BOverrideFactoryThresholdBinary.localNiblackFamily,
				hook -> hook.handle(variant, width, k, down, inputType));
		if (filter != null)
			return filter;

		InputToBinary<GrayF32> sauvola;
		if (BoofConcurrency.USE_CONCURRENT) {
			sauvola = new ThresholdNiblackFamily_MT(width, k, down, variant);
		} else {
			sauvola = new ThresholdNiblackFamily(width, k, down, variant);
		}
		return new InputToBinarySwitch<>(sauvola, inputType);
	}

	/// [ThresholdNick]
	///
	/// @param width size of local region. Try 31
	/// @param down Should it threshold up or down.
	/// @param k The Niblack factor. Recommend -0.1 to -0.2
	/// @param inputType Type of input image
	/// @return Filter to binary
	public static <T extends ImageGray<T>>
	InputToBinary<T> localNick( ConfigLength width, boolean down, float k, Class<T> inputType ) {
		InputToBinary<T> filter = override(BOverrideFactoryThresholdBinary.localNick,
				hook -> hook.handle(width, k, down, inputType));
		if (filter != null)
			return filter;
		return new InputToBinarySwitch<>(new ThresholdNick(width, k, down), inputType);
	}

	/// [boofcv.alg.filter.binary.GThresholdImageOps#localMean]
	///
	/// @param width Width of square region.
	/// @param scale Scale factor adjust for threshold. 1.0 means no change.
	/// @param down Should it threshold up or down.
	/// @param inputType Type of input image
	/// @return Filter to binary
	public static <T extends ImageGray<T>>
	InputToBinary<T> localMean( ConfigLength width, double scale, boolean down, Class<T> inputType ) {
		InputToBinary<T> filter = override(BOverrideFactoryThresholdBinary.localMean,
				hook -> hook.handle(width, scale, down, inputType));
		if (filter != null)
			return filter;
		return new LocalMeanBinaryFilter<>(width, scale, down, ImageType.single(inputType));
	}

	/// Applies a local Otsu threshold. [ThresholdLocalOtsu]
	///
	/// @param regionWidth About how wide and tall you wish a block to be in pixels.
	/// @param scale Scale factor adjust for threshold. 1.0 means no change.
	/// @param down Should it threshold up or down.
	/// @param tuning Tuning parameter. 0 = standard Otsu. Greater than 0 will penalize zero texture.
	/// @param inputType Type of input image
	/// @return Filter to binary
	public static <T extends ImageGray<T>>
	InputToBinary<T> localOtsu( ConfigLength regionWidth, double scale, boolean down, boolean otsu2, double tuning, Class<T> inputType ) {
		InputToBinary<T> filter = override(BOverrideFactoryThresholdBinary.localOtsu,
				hook -> hook.handle(otsu2, regionWidth, tuning, scale, down, inputType));
		if (filter != null)
			return filter;

		ThresholdLocalOtsu otsu;
		if (BoofConcurrency.USE_CONCURRENT) {
			otsu = new ThresholdLocalOtsu_MT(otsu2, regionWidth, tuning, scale, down);
		} else {
			otsu = new ThresholdLocalOtsu(otsu2, regionWidth, tuning, scale, down);
		}
		return new InputToBinarySwitch<>(otsu, inputType);
	}

	/// Applies a very fast non-overlapping block thresholding algorithm which uses min/max statistics.
	///
	/// @param regionWidth Approximate size of block region
	/// @param scale Scale factor adjust for threshold. 1.0 means no change.
	/// @param down Should it threshold up or down.
	/// @param minimumSpread See [ConfigThresholdBlockMinMax#minimumSpread]
	/// @param inputType Type of input image
	/// @return Filter to binary
	/// @see ThresholdBlockMinMax
	public static <T extends ImageGray<T>>
	InputToBinary<T> blockMinMax( ConfigLength regionWidth, double scale, boolean down,
								  boolean thresholdFromLocalBlocks, double minimumSpread, Class<T> inputType ) {
		InputToBinary<T> filter = override(BOverrideFactoryThresholdBinary.blockMinMax,
				hook -> hook.handle(regionWidth, scale, down, minimumSpread, thresholdFromLocalBlocks, inputType));
		if (filter != null)
			return filter;

		BlockProcessor processor;
		if (inputType == GrayU8.class)
			processor = new ThresholdBlockMinMax_U8(minimumSpread, scale, down);
		else
			processor = new ThresholdBlockMinMax_F32((float)minimumSpread, (float)scale, down);

		if (BoofConcurrency.USE_CONCURRENT) {
			return new ThresholdBlock_MT(processor, regionWidth, thresholdFromLocalBlocks, inputType);
		} else {
			return new ThresholdBlock(processor, regionWidth, thresholdFromLocalBlocks, inputType);
		}
	}

	/// Applies a non-overlapping block mean threshold
	///
	/// @param scale Scale factor adjust for threshold. 1.0 means no change.
	/// @param down Should it threshold up or down.
	/// @param regionWidth Approximate size of block region
	/// @param inputType Type of input image
	/// @return Filter to binary
	/// @see ThresholdBlockMean
	public static <T extends ImageGray<T>>
	InputToBinary<T> blockMean( ConfigLength regionWidth, double scale, boolean down, boolean thresholdFromLocalBlocks,
								Class<T> inputType ) {
		InputToBinary<T> filter = override(BOverrideFactoryThresholdBinary.blockMean,
				hook -> hook.handle(regionWidth, scale, down, thresholdFromLocalBlocks, inputType));
		if (filter != null)
			return filter;

		BlockProcessor processor;
		if (inputType == GrayU8.class)
			processor = new ThresholdBlockMean_U8(scale, down);
		else
			processor = new ThresholdBlockMean_F32((float)scale, down);

		if (BoofConcurrency.USE_CONCURRENT) {
			return new ThresholdBlock_MT(processor, regionWidth, thresholdFromLocalBlocks, inputType);
		} else {
			return new ThresholdBlock(processor, regionWidth, thresholdFromLocalBlocks, inputType);
		}
	}

	/// Applies a non-overlapping block Otsu threshold. [ThresholdBlockOtsu]
	///
	/// @param regionWidth Approximate size of block region
	/// @param scale Scale factor adjust for threshold. 1.0 means no change.
	/// @param down Should it threshold up or down.
	/// @param inputType Type of input image
	/// @return Filter to binary
	public static <T extends ImageGray<T>>
	InputToBinary<T> blockOtsu( ConfigLength regionWidth, double scale, boolean down, boolean thresholdFromLocalBlocks,
								boolean otsu2, double tuning, Class<T> inputType ) {
		InputToBinary<T> filter = override(BOverrideFactoryThresholdBinary.blockOtsu,
				hook -> hook.handle(otsu2, regionWidth, tuning, scale, down, thresholdFromLocalBlocks, inputType));
		if (filter != null)
			return filter;

		BlockProcessor processor = new ThresholdBlockOtsu(otsu2, tuning, scale, down);
		InputToBinary<GrayU8> otsu;

		if (BoofConcurrency.USE_CONCURRENT) {
			otsu = new ThresholdBlock_MT<>(processor, regionWidth, thresholdFromLocalBlocks, GrayU8.class);
		} else {
			otsu = new ThresholdBlock<>(processor, regionWidth, thresholdFromLocalBlocks, GrayU8.class);
		}

		return new InputToBinarySwitch<>(otsu, inputType);
	}

	/// [GThresholdImageOps#computeEntropy]
	///
	/// @param minValue The minimum value of a pixel in the image. (inclusive)
	/// @param maxValue The maximum value of a pixel in the image. (inclusive)
	/// @param down Should it threshold up or down.
	/// @param inputType Type of input image
	/// @return Filter to binary
	public static <T extends ImageGray<T>>
	InputToBinary<T> globalEntropy( int minValue, int maxValue, double scale, boolean down, Class<T> inputType ) {
		InputToBinary<T> filter = override(BOverrideFactoryThresholdBinary.globalEntropy,
				hook -> hook.handle(minValue, maxValue, scale, down, inputType));
		if (filter != null)
			return filter;
		return new GlobalBinaryFilter.Entropy<>(minValue, maxValue, scale, down, ImageType.single(inputType));
	}

	/// [boofcv.alg.filter.binary.GThresholdImageOps#threshold]
	///
	/// @param threshold threshold value.
	/// @param down If true then the inequality ≤ is used, otherwise if false then ≥ is used.
	/// @param inputType Type of input image
	/// @return Filter to binary
	public static <T extends ImageGray<T>>
	InputToBinary<T> globalFixed( double threshold, boolean down, Class<T> inputType ) {
		InputToBinary<T> filter = override(BOverrideFactoryThresholdBinary.globalFixed,
				hook -> hook.handle(threshold, down, inputType));
		if (filter != null)
			return filter;
		return new GlobalFixedBinaryFilter<>(threshold, down, ImageType.single(inputType));
	}

	/// [boofcv.alg.filter.binary.GThresholdImageOps#computeOtsu]
	///
	/// @param minValue The minimum value of a pixel in the image. (inclusive)
	/// @param maxValue The maximum value of a pixel in the image. (inclusive)
	/// @param down Should it threshold up or down.
	/// @param inputType Type of input image
	/// @return Filter to binary
	public static <T extends ImageGray<T>>
	InputToBinary<T> globalOtsu( double minValue, double maxValue, double scale, boolean down, Class<T> inputType ) {
		InputToBinary<T> filter = override(BOverrideFactoryThresholdBinary.globalOtsu,
				hook -> hook.handle(minValue, maxValue, scale, down, inputType));
		if (filter != null)
			return filter;
		return new GlobalBinaryFilter.Otsu<>(minValue, maxValue, scale, down, ImageType.single(inputType));
	}

	/// [boofcv.alg.filter.binary.GThresholdImageOps#computeLi(int\[\], int)]
	///
	/// @param minValue The minimum value of a pixel in the image. (inclusive)
	/// @param maxValue The maximum value of a pixel in the image. (inclusive)
	/// @param down Should it threshold up or down.
	/// @param inputType Type of input image
	/// @return Filter to binary
	public static <T extends ImageGray<T>>
	InputToBinary<T> globalLi( double minValue, double maxValue, double scale, boolean down, Class<T> inputType ) {
		return new GlobalBinaryFilter.Li<>(minValue, maxValue, scale, down, ImageType.single(inputType));
	}

	/// [boofcv.alg.filter.binary.GThresholdImageOps#computeHuang(int\[\], int)].
	///
	/// @param minValue The minimum value of a pixel in the image. (inclusive)
	/// @param maxValue The maximum value of a pixel in the image. (inclusive)
	/// @param down Should it threshold up or down.
	/// @param inputType Type of input image
	/// @return Filter to binary
	public static <T extends ImageGray<T>>
	InputToBinary<T> globalHuang( double minValue, double maxValue, double scale, boolean down, Class<T> inputType ) {
		return new GlobalBinaryFilter.Huang<>(minValue, maxValue, scale, down, ImageType.single(inputType));
	}

	/// Creates threshold using a config class
	///
	/// @param config Configuration
	/// @param inputType Type of input image
	/// @return The thresholder
	public static <T extends ImageGray<T>>
	InputToBinary<T> threshold( ConfigThreshold config, Class<T> inputType ) {
		return switch (config.type) {
			case FIXED -> globalFixed(config.fixedThreshold, config.down, inputType);
			case GLOBAL_OTSU -> globalOtsu(config.minPixelValue, config.maxPixelValue, config.scale, config.down, inputType);
			case GLOBAL_ENTROPY -> globalEntropy(config.minPixelValue, config.maxPixelValue, config.scale, config.down, inputType);
			case GLOBAL_LI -> globalLi(config.minPixelValue, config.maxPixelValue, config.scale, config.down, inputType);
			case GLOBAL_HUANG -> globalHuang(config.minPixelValue, config.maxPixelValue, config.scale, config.down, inputType);
			case LOCAL_GAUSSIAN -> localGaussian(config.width, config.scale, config.down, inputType);
			case LOCAL_NIBLACK -> localNiblack(config.width, config.down, config.niblackK, inputType);
			case LOCAL_SAVOLA -> localSauvola(config.width, config.down, config.niblackK, inputType);
			case LOCAL_WOLF -> localWolf(config.width, config.down, config.niblackK, inputType);
			case LOCAL_NICK -> localNick(config.width, config.down, config.nickK, inputType);
			case LOCAL_MEAN -> localMean(config.width, config.scale, config.down, inputType);

			case LOCAL_OTSU -> {
				ConfigThresholdLocalOtsu c = (ConfigThresholdLocalOtsu)config;
				yield localOtsu(config.width, config.scale, config.down, c.useOtsu2, c.tuning, inputType);
			}

			case BLOCK_MIN_MAX -> {
				ConfigThresholdBlockMinMax c = (ConfigThresholdBlockMinMax)config;
				yield blockMinMax(c.width, c.scale, c.down, c.thresholdFromLocalBlocks, c.minimumSpread,
						inputType);
			}

			case BLOCK_MEAN -> blockMean(config.width, config.scale, config.down, config.thresholdFromLocalBlocks, inputType);

			case BLOCK_OTSU -> {
				ConfigThresholdLocalOtsu c = (ConfigThresholdLocalOtsu)config;
				yield blockOtsu(c.width, c.scale, c.down, c.thresholdFromLocalBlocks, c.useOtsu2, c.tuning,
						inputType);
			}
		};
	}

	/// Filter created by the override. Null if the override isn't set, returns null, or throws a RuntimeException,
	/// which means it doesn't support the input
	private static <H, T extends ImageGray<T>>
	@Nullable InputToBinary<T> override( @Nullable H hook, Function<H, InputToBinary<T>> create ) {
		if (hook == null)
			return null;
		try {
			return create.apply(hook);
		} catch (RuntimeException ignore) {
			return null;
		}
	}
}
