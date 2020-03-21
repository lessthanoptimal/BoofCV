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

import boofcv.abst.feature.disparity.*;
import boofcv.abst.filter.FilterImageInterface;
import boofcv.abst.transform.census.FilterCensusTransform;
import boofcv.alg.feature.disparity.DisparityBlockMatchRowFormat;
import boofcv.alg.feature.disparity.block.*;
import boofcv.alg.feature.disparity.block.score.*;
import boofcv.alg.feature.disparity.sgm.SgmStereoDisparity;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.factory.transform.census.FactoryCensusTransform;
import boofcv.struct.image.*;

import javax.annotation.Nullable;

import static boofcv.factory.feature.disparity.FactoryStereoDisparityAlgs.*;

/**
 * <p>
 * Creates high level interfaces for computing the disparity between two rectified stereo images.
 * Algorithms which select the best disparity for each region independent of all the others are
 * referred to as Winner Takes All (WTA) in the literature.  Dense algorithms compute the disparity for the
 * whole image while sparse algorithms do it in a per pixel basis as requested.
 * </p>
 *
 * <p>
 * Typically disparity calculations with regions will produce less erratic results, but their precision will
 * be decreased.  This is especially evident along the border of objects.  Computing a wider range of disparities
 * can better results, but is very computationally expensive.
 * </p>
 *
 * <p>
 * Dense vs Sparse.  Here dense refers to computing the disparity across the whole image at once.  Sparse refers
 * to computing the disparity for a single pixel at a time as requested by the user,
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class FactoryStereoDisparity {

	public static <T extends ImageGray<T>, DI extends ImageGray<DI>> StereoDisparity<T,DI>
	blockMatch(@Nullable ConfigDisparityBM config , Class<T> imageType , Class<DI> dispType ) {
		if( config == null )
			config = new ConfigDisparityBM();
		config.checkValidity();

		if( config.subpixel ) {
			if( dispType != GrayF32.class )
				throw new IllegalArgumentException("With subpixel on, disparity image must be GrayF32");
		} else {
			if( dispType != GrayU8.class )
				throw new IllegalArgumentException("With subpixel on, disparity image must be GrayU8");
		}

		double maxError = (config.regionRadiusX*2+1)*(config.regionRadiusY*2+1)*config.maxPerPixelError;

		switch( config.errorType) {
			case SAD: {
				DisparitySelect select = createDisparitySelect(config, imageType, (int) maxError);
				BlockRowScore rowScore = createScoreRowSad(config,imageType);
				DisparityBlockMatchRowFormat alg = createBlockMatching(config, imageType, select, rowScore);
				alg.setBorder(FactoryImageBorder.generic(config.border,rowScore.getImageType()));
				return new WrapDisparityBlockMatchRowFormat(alg);
			}

			case CENSUS: {
				DisparitySelect select = createDisparitySelect(config, imageType, (int) maxError);
				FilterImageInterface censusTran = FactoryCensusTransform.variant(config.configCensus.variant, true, imageType);
				BlockRowScore rowScore = createCensusRowScore(config, censusTran);

				DisparityBlockMatchRowFormat alg = createBlockMatching(config,
						censusTran.getOutputType().getImageClass(), select, rowScore);
				alg.setBorder(FactoryImageBorder.generic(config.border,censusTran.getOutputType()));
				return new WrapDisparityBlockMatchCensus<>(censusTran, alg);
			}

			case NCC: {
				DisparitySelect select = createDisparitySelect(config, GrayF32.class, (int) maxError);
				BlockRowScore rowScore = createScoreRowNcc(config,GrayF32.class);
				DisparityBlockMatchRowFormat alg = createBlockMatching(config, GrayF32.class, select, rowScore);
				alg.setBorder(FactoryImageBorder.generic(config.border,rowScore.getImageType()));
				return new DisparityBlockMatchCorrelation(alg,imageType);
			}

			default:
				throw new IllegalArgumentException("Unsupported error type "+config.errorType);
		}
	}

	public static BlockRowScore createCensusRowScore( ConfigDisparityBM config, FilterImageInterface censusTran) {
		Class censusType = censusTran.getOutputType().getImageClass();
		int bits = config.configCensus.variant.getBits();
		BlockRowScore rowScore;
		if (censusType == GrayU8.class) {
			rowScore = new BlockRowScoreCensus.U8(bits);
		} else if (censusType == GrayS32.class) {
			rowScore = new BlockRowScoreCensus.S32(bits);
		} else if (censusType == GrayS64.class) {
			rowScore = new BlockRowScoreCensus.S64(bits);
		} else {
			throw new IllegalArgumentException("Unsupported image type");
		}

		return rowScore;
	}

	public static <T extends ImageGray<T>> DisparitySelect
	createDisparitySelect(ConfigDisparityBM config, Class<T> imageType, int maxError) {
		DisparitySelect select;
		if( !GeneralizedImageOps.isFloatingPoint(imageType) ) {
			if( config.errorType.isCorrelation() )
				throw new IllegalArgumentException("Can't do correlation scores for integer image types");
			if( config.subpixel ) {
				select = selectDisparitySubpixel_S32(maxError, config.validateRtoL, config.texture);
			} else {
				select = selectDisparity_S32(maxError, config.validateRtoL, config.texture);
			}
		} else if( imageType == GrayF32.class ) {
			if( config.subpixel ) {
				if( config.errorType.isCorrelation() ) {
					select = selectCorrelation_F32(config.validateRtoL, config.texture, true);
				} else {
					select = selectDisparitySubpixel_F32(maxError, config.validateRtoL, config.texture);
				}
			} else {
				if( config.errorType.isCorrelation() )
					select = selectCorrelation_F32(config.validateRtoL, config.texture, false);
				else
					select = selectDisparity_F32(maxError, config.validateRtoL, config.texture);
			}
		} else {
			throw new IllegalArgumentException("Unknown image type");
		}
		return select;
	}

	public static <T extends ImageGray<T>, DI extends ImageGray<DI>> StereoDisparity<T,DI>
	blockMatchBest5(@Nullable ConfigDisparityBMBest5 config , Class<T> imageType , Class<DI> dispType ) {
		if( config == null )
			config = new ConfigDisparityBMBest5();
		config.checkValidity();

		if( config.subpixel ) {
			if( dispType != GrayF32.class )
				throw new IllegalArgumentException("With subpixel on, disparity image must be GrayF32");
		} else {
			if( dispType != GrayU8.class )
				throw new IllegalArgumentException("With subpixel on, disparity image must be GrayU8");
		}

		double maxError = (config.regionRadiusX*2+1)*(config.regionRadiusY*2+1)*config.maxPerPixelError;

		// 3 regions are used not just one in this case
		maxError *= 3;

		switch( config.errorType) {
			case SAD: {
				DisparitySelect select = createDisparitySelect(config, imageType, (int) maxError);
				BlockRowScore rowScore = createScoreRowSad(config,imageType);
				DisparityBlockMatchRowFormat alg = createBestFive(config, imageType, select, rowScore);
				alg.setBorder(FactoryImageBorder.generic(config.border,rowScore.getImageType()));
				return new WrapDisparityBlockMatchRowFormat(alg);
			}

			case CENSUS: {
				DisparitySelect select = createDisparitySelect(config, imageType, (int) maxError);
				FilterImageInterface censusTran = FactoryCensusTransform.variant(config.configCensus.variant, true, imageType);
				BlockRowScore rowScore = createCensusRowScore(config, censusTran);
				DisparityBlockMatchRowFormat alg = createBestFive(config,
						censusTran.getOutputType().getImageClass(), select, rowScore);
				alg.setBorder(FactoryImageBorder.generic(config.border,censusTran.getOutputType()));
				return new WrapDisparityBlockMatchCensus<>(censusTran, alg);
			}

			case NCC: {
				DisparitySelect select = createDisparitySelect(config, GrayF32.class, (int) maxError);
				BlockRowScore rowScore = createScoreRowNcc(config,GrayF32.class);
				DisparityBlockMatchRowFormat alg = createBestFive(config, GrayF32.class, select, rowScore);
				alg.setBorder(FactoryImageBorder.generic(config.border,rowScore.getImageType()));
				return new DisparityBlockMatchCorrelation(alg,imageType);
			}

			default:
				throw new IllegalArgumentException("Unsupported error type "+config.errorType);
		}
	}

	public static <T extends ImageGray<T>> BlockRowScore createScoreRowSad(ConfigDisparityBM config, Class<T> imageType) {
		BlockRowScore rowScore;
		if (imageType == GrayU8.class) {
			rowScore = new BlockRowScoreSad.U8();
		} else if (imageType == GrayU16.class) {
			rowScore = new BlockRowScoreSad.U16();
		} else if (imageType == GrayS16.class) {
			rowScore = new BlockRowScoreSad.S16();
		} else if (imageType == GrayF32.class) {
			rowScore = new BlockRowScoreSad.F32();
		} else {
			throw new IllegalArgumentException("Unsupported image type "+imageType.getSimpleName());
		}
		return rowScore;
	}

	public static <T extends ImageGray<T>> BlockRowScore createScoreRowNcc( ConfigDisparityBM config, Class<T> imageType) {
		BlockRowScore rowScore;
		if (imageType == GrayF32.class) {
			rowScore = new BlockRowScoreNcc.F32(config.regionRadiusX,config.regionRadiusY);
			((BlockRowScoreNcc.F32)rowScore).eps = (float)config.configNCC.eps;
		} else {
			throw new IllegalArgumentException("Unsupported image type "+imageType.getSimpleName());
		}
		return rowScore;
	}

	public static <T extends ImageGray<T>> DisparityBlockMatchRowFormat
	createBlockMatching(ConfigDisparityBM config, Class<T> imageType, DisparitySelect select, BlockRowScore rowScore) {
		DisparityBlockMatchRowFormat alg;
		if (GeneralizedImageOps.isFloatingPoint(imageType)) {
			alg = new DisparityScoreBM_F32<>( config.regionRadiusX, config.regionRadiusY, rowScore, select);
		} else {
			alg = new DisparityScoreBM_S32( config.regionRadiusX, config.regionRadiusY, rowScore, select,
					ImageType.single(imageType));
		}
		alg.configure(config.disparityMin,config.disparityRange);
		return alg;
	}

	public static <T extends ImageGray<T>> DisparityBlockMatchRowFormat
	createBestFive(ConfigDisparityBM config, Class<T> imageType, DisparitySelect select, BlockRowScore rowScore) {
		DisparityBlockMatchRowFormat alg;
		if (GeneralizedImageOps.isFloatingPoint(imageType)) {
			alg = new DisparityScoreBMBestFive_F32(config.regionRadiusX, config.regionRadiusY, rowScore, select);
		} else {
			alg = new DisparityScoreBMBestFive_S32(config.regionRadiusX, config.regionRadiusY, rowScore, select,
					ImageType.single(imageType));
		}
		alg.configure(config.disparityMin,config.disparityRange);
		return alg;
	}

	public static <T extends ImageGray<T>> StereoDisparitySparse<T>
	sparseRectifiedBM(ConfigDisparityBM config, final Class<T> imageType) {

		if( config.validateRtoL >= 0 )
			throw new IllegalArgumentException("validateRtoL must be set to a value < 0. Not supported, yet.");

		double maxError = (config.regionRadiusX*2+1)*(config.regionRadiusY*2+1)*config.maxPerPixelError;

		// TODO support error besides SAD
		DisparitySparseSelect select;
		if( GeneralizedImageOps.isFloatingPoint(imageType)) {
			if (config.subpixel)
				select = selectDisparitySparseSubpixel_F32((int) maxError, config.texture);
			else
				select = selectDisparitySparse_F32((int) maxError, config.texture);
		} else {
			if (config.subpixel)
				select = selectDisparitySparseSubpixel_S32((int) maxError, config.texture);
			else
				select = selectDisparitySparse_S32((int) maxError, config.texture);
		}

		DisparitySparseRectifiedScoreBM score = null;
		switch( config.errorType ) {
			case SAD: {
				if( imageType == GrayU8.class ) {
					score = new SparseScoreRectifiedSad.U8(config.regionRadiusX, config.regionRadiusY);
				} else if( imageType == GrayF32.class ) {
					score = new SparseScoreRectifiedSad.F32(config.regionRadiusX, config.regionRadiusY);
				} else
					throw new RuntimeException("Image type not supported: "+imageType.getSimpleName() );
			} break;

			case CENSUS: {
				FilterCensusTransform censusTran = FactoryCensusTransform.variant(config.configCensus.variant, false, imageType);

				final Class censusType = censusTran.getOutputType().getImageClass();

				if( censusType == GrayU8.class ) {
					score = new SparseScoreRectifiedCensus.U8(config.regionRadiusX, config.regionRadiusY, censusTran,imageType);
				} else if( censusType == GrayS32.class ) {
					score = new SparseScoreRectifiedCensus.S32(config.regionRadiusX, config.regionRadiusY, censusTran,imageType);
				} else if( censusType == GrayS64.class ) {
					score = new SparseScoreRectifiedCensus.S64(config.regionRadiusX, config.regionRadiusY, censusTran,imageType);
				} else {
					throw new RuntimeException("Image type not supported census: " + censusType.getSimpleName());
				}
			} break;
		}

		if( score == null ) {
			throw new RuntimeException("Selected score type and image type is not supported at this time");
		}

		score.configure(config.disparityMin,config.disparityRange);
		score.setBorder(FactoryImageBorder.generic(config.border, ImageType.single(imageType)));
		return new WrapDisparitySparseRectifiedBM(score,select);
	}

	/**
	 * Disparity computed using Semi Global Matching (SGM)
	 * @param config Configuration for SGM
	 * @param imageType Type of input image
	 * @param dispType Type of disparity image. F32 is sub-pixel is turned on, U8 otherwise
	 * @return The algorithm.
	 */
	public static <T extends ImageGray<T>, DI extends ImageGray<DI>> StereoDisparity<T,DI>
	sgm(@Nullable ConfigDisparitySGM config , Class<T> imageType , Class<DI> dispType ) {
		if( config == null )
			config = new ConfigDisparitySGM();

		if( config.subpixel ){
			if( dispType != GrayF32.class ) {
				throw new IllegalArgumentException("Disparity must be F32 for sub-pixel precision");
			}
		} else {
			if( dispType != GrayU8.class ) {
				throw new IllegalArgumentException("Disparity must be U8 for pixel precision");
			}
		}

		if( imageType == GrayU8.class ) {
			SgmStereoDisparity alg = FactoryStereoDisparityAlgs.createSgm(config);
			return (StereoDisparity)new WrapDisparitySgm(alg,config.subpixel);
		} else {
			throw new IllegalArgumentException("Only U8 input supported");
		}
	}
}