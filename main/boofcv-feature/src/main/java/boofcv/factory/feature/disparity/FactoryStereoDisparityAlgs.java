/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.feature.disparity;

import boofcv.abst.filter.FilterImageInterface;
import boofcv.alg.feature.disparity.DisparityBlockMatchRowFormat;
import boofcv.alg.feature.disparity.block.BlockRowScore;
import boofcv.alg.feature.disparity.block.BlockRowScoreMutualInformation;
import boofcv.alg.feature.disparity.block.DisparitySelect;
import boofcv.alg.feature.disparity.block.DisparitySparseSelect;
import boofcv.alg.feature.disparity.block.select.*;
import boofcv.alg.feature.disparity.sgm.*;
import boofcv.alg.feature.disparity.sgm.cost.SgmCostAbsoluteDifference;
import boofcv.alg.feature.disparity.sgm.cost.SgmCostFromBlocks;
import boofcv.alg.feature.disparity.sgm.cost.SgmCostHamming;
import boofcv.alg.feature.disparity.sgm.cost.StereoMutualInformation;
import boofcv.concurrency.BoofConcurrency;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.factory.transform.census.FactoryCensusTransform;
import boofcv.struct.image.*;

import javax.annotation.Nullable;

import static boofcv.factory.feature.disparity.FactoryStereoDisparity.*;

/**
 * Algorithms related to computing the disparity between two rectified stereo images.
 *
 * @author Peter Abeles
 */
public class FactoryStereoDisparityAlgs {

	/**
	 * Creates SGM stereo using HMI.
	 */
	public static SgmStereoDisparity createSgm(@Nullable ConfigDisparitySGM config ) {
		if( config == null )
			config = new ConfigDisparitySGM();

		int maxError = config.maxError < 0 ? Integer.MAX_VALUE : config.maxError;

		SgmDisparitySelector selector = BoofConcurrency.USE_CONCURRENT ?
				new SgmDisparitySelector_MT() : new SgmDisparitySelector();
		selector.setRightToLeftTolerance(config.validateRtoL);
		selector.setMaxError(maxError);
		selector.setTextureThreshold(config.texture);

		SgmStereoDisparity sgm;

		// There's currently no block variant of MI
		if( !config.useBlocks )
			sgm = createSgmNativeCost(config, selector);
		else
			sgm = createSgmBlockCost(config, selector, GrayU8.class);

		sgm.setDisparityMin(config.disparityMin);
		sgm.setDisparityRange(config.disparityRange);
		sgm.getAggregation().setPathsConsidered(config.paths.getCount());
		sgm.getAggregation().setPenalty1(config.penaltySmallChange);
		sgm.getAggregation().setPenalty2(config.penaltyLargeChange);

		return sgm;
	}

	private static SgmStereoDisparity createSgmNativeCost( ConfigDisparitySGM config, SgmDisparitySelector selector) {
		SgmStereoDisparity sgm;

		switch( config.errorType) {
			case MUTUAL_INFORMATION: {
				StereoMutualInformation stereoMI = createStereoMutualInformation(config);
				sgm = new SgmStereoDisparityHmi(config.configHMI.pyramidLayers,stereoMI,selector);
				((SgmStereoDisparityHmi)sgm).setExtraIterations(config.configHMI.extraIterations);
			} break;

			case ABSOLUTE_DIFFERENCE: {
				sgm = new SgmStereoDisparityError(new SgmCostAbsoluteDifference.U8(),selector);
			} break;

			case CENSUS: {
				FilterImageInterface censusTran = FactoryCensusTransform.variant(config.configCensus.variant, true, GrayU8.class);
				Class censusType = censusTran.getOutputType().getImageClass();
				SgmCostHamming cost;
				if (censusType == GrayU8.class) {
					cost = new SgmCostHamming.U8();
				} else if (censusType == GrayS32.class) {
					cost = new SgmCostHamming.S32();
				} else if (censusType == GrayS64.class) {
					cost = new SgmCostHamming.S64();
				} else {
					throw new IllegalArgumentException("Unsupported image type");
				}
				sgm = new SgmStereoDisparityCensus(censusTran,cost,selector);
			} break;

			default:
				throw new IllegalArgumentException("Unknown error type "+config.errorType);
		}
		return sgm;
	}

	private static <T extends ImageGray<T>>
	SgmStereoDisparity createSgmBlockCost(ConfigDisparitySGM config, SgmDisparitySelector selector, Class<T> imageType)
	{
		SgmStereoDisparity sgm;
		ConfigDisparityBM configBM = new ConfigDisparityBM();
		configBM.regionRadiusX = config.configBlockMatch.radiusX;
		configBM.regionRadiusY = config.configBlockMatch.radiusY;
		configBM.disparityMin = config.disparityMin;
		configBM.disparityRange = config.disparityRange;
		configBM.border = config.border;

		SgmCostFromBlocks<T> blockCost = new SgmCostFromBlocks<T>();
		DisparityBlockMatchRowFormat<T, GrayU8> blockScore;

		switch( config.errorType) {
			case MUTUAL_INFORMATION: {
				if (imageType != GrayU8.class) {
					throw new IllegalArgumentException("Only GrayU8 supported at this time for Mutual Information");
				}
				StereoMutualInformation stereoMI = createStereoMutualInformation(config);
				BlockRowScore rowScore = new BlockRowScoreMutualInformation.U8(stereoMI);
				rowScore.setBorder(FactoryImageBorder.generic(config.border,rowScore.getImageType()));
				blockScore = createSgmBlockMatch(config, imageType, configBM, blockCost, rowScore);
				blockScore.setBorder(FactoryImageBorder.generic(config.border,rowScore.getImageType()));
				sgm = new SgmStereoDisparityHmi(config.configHMI.pyramidLayers,stereoMI,selector,(SgmCostFromBlocks)blockCost);
				((SgmStereoDisparityHmi)sgm).setExtraIterations(config.configHMI.extraIterations);
			} break;

			case ABSOLUTE_DIFFERENCE: {
				BlockRowScore rowScore = createScoreRowSad(configBM,imageType);
				blockScore = createSgmBlockMatch(config, (Class<T>) imageType, configBM, (SgmCostFromBlocks<T>) blockCost, rowScore);
				blockScore.setBorder(FactoryImageBorder.generic(config.border,rowScore.getImageType()));
				sgm = new SgmStereoDisparityError(blockCost,selector);
			} break;

			case CENSUS: {
				FilterImageInterface censusTran = FactoryCensusTransform.variant(config.configCensus.variant, true, imageType);
				BlockRowScore rowScore = createCensusRowScore(configBM, censusTran);
				blockScore = createSgmBlockMatch(config, censusTran.getOutputType().getImageClass(),
						configBM, blockCost, rowScore);
				blockScore.setBorder(FactoryImageBorder.generic(config.border,censusTran.getOutputType()));
				sgm = new SgmStereoDisparityCensus(censusTran,blockCost,selector);
			} break;

			default:
				throw new IllegalArgumentException("Unknown error type "+config.errorType);
		}
		blockCost.setBlockScore(blockScore);
		return sgm;
	}

	private static StereoMutualInformation createStereoMutualInformation(ConfigDisparitySGM config) {
		StereoMutualInformation stereoMI = new StereoMutualInformation();
		stereoMI.configureSmoothing(config.configHMI.smoothingRadius);
		stereoMI.configureHistogram(config.configHMI.totalGrayLevels);
		return stereoMI;
	}

	private static <T extends ImageGray<T>> DisparityBlockMatchRowFormat<T, GrayU8> createSgmBlockMatch(ConfigDisparitySGM config, Class<T> imageType, ConfigDisparityBM configBM, SgmCostFromBlocks<T> blockCost, BlockRowScore rowScore) {
		DisparityBlockMatchRowFormat<T, GrayU8> blockScore;
		switch (config.configBlockMatch.approach) {
			case BASIC:
				blockScore = createBlockMatching(configBM, imageType, blockCost, rowScore);
				break;

			case BEST5:
				blockScore = createBestFive(configBM, imageType, blockCost, rowScore);
				break;

			default:
				throw new IllegalArgumentException("Unknown type " + config.configBlockMatch.approach);
		}
		return blockScore;
	}

	public static DisparitySelect<int[],GrayU8> selectDisparity_S32(int maxError , int tolR2L , double texture) {
		if( maxError < 0 && tolR2L < 0  & texture <= 0 )
			return new SelectErrorBasicWta_S32_U8();
		else
			return new SelectErrorWithChecks_S32.DispU8(maxError,tolR2L,texture);
	}

	public static DisparitySelect<float[],GrayU8> selectDisparity_F32(int maxError , int tolR2L , double texture) {
		if( maxError < 0 && tolR2L < 0  & texture <= 0 )
			return new SelectErrorBasicWta_F32_U8();
		else
			return new SelectErrorWithChecks_F32.DispU8(maxError,tolR2L,texture);
	}

	public static <D extends ImageGray<D>> DisparitySelect<float[],D> selectCorrelation_F32(int tolR2L , double texture, boolean subpixel) {
		if( !subpixel &&  tolR2L < 0 && texture <= 0 )
			return (DisparitySelect)new SelectCorrelationWta_F32_U8();
		else if( !subpixel )
			return (DisparitySelect)new SelectCorrelationWithChecks_F32.DispU8(tolR2L, texture);
		else
			return (DisparitySelect)new SelectCorrelationSubpixel.F32_F32(tolR2L,texture);
	}

	public static DisparitySelect<int[],GrayF32>
	selectDisparitySubpixel_S32( int maxError , int tolR2L , double texture) {
		return new SelectErrorSubpixel.S32_F32(maxError,tolR2L,texture);
	}

	public static DisparitySelect<float[],GrayF32>
	selectDisparitySubpixel_F32( int maxError , int tolR2L , double texture) {
		return new SelectErrorSubpixel.F32_F32(maxError,tolR2L,texture);
	}

	public static DisparitySparseSelect<int[]>
	selectDisparitySparse_S32( int maxError , double texture) {
		if( maxError < 0 && texture <= 0 )
			return new SelectSparseErrorBasicWta_S32();
		else
			return new SelectSparseErrorWithChecksWta_S32(maxError,texture);
	}

	public static DisparitySparseSelect<float[]>
	selectDisparitySparse_F32( int maxError , double texture) {
		if( maxError < 0 && texture <= 0 )
			return new SelectSparseErrorBasicWta_F32();
		else
			return new SelectSparseErrorWithChecksWta_F32(maxError,texture);
	}

	public static DisparitySparseSelect<int[]>
	selectDisparitySparseSubpixel_S32( int maxError , double texture) {
		return new SelectSparseErrorSubpixel.S32(maxError,texture);
	}

	public static DisparitySparseSelect<float[]>
	selectDisparitySparseSubpixel_F32( int maxError , double texture) {
		return new SelectSparseErrorSubpixel.F32(maxError,texture);
	}
}
