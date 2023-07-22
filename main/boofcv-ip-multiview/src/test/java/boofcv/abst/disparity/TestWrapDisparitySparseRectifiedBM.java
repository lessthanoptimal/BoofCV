/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.disparity;

import boofcv.abst.transform.census.FilterCensusTransform;
import boofcv.alg.disparity.DisparityBlockMatchRowFormat;
import boofcv.alg.disparity.block.BlockRowScore;
import boofcv.alg.disparity.block.DisparitySelect;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.factory.disparity.ConfigDisparityBM;
import boofcv.factory.disparity.DisparityError;
import boofcv.factory.transform.census.FactoryCensusTransform;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.*;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Nested;

import static boofcv.factory.disparity.FactoryStereoDisparity.*;
import static boofcv.factory.transform.census.FactoryCensusTransform.CENSUS_BORDER;

public class TestWrapDisparitySparseRectifiedBM extends BoofStandardJUnit {
	@Nested
	class SAD_U8 extends CompareSparseToDenseDisparityChecks<GrayU8> {
		public SAD_U8() {
			super(DisparityError.SAD, ImageType.SB_U8);
		}
	}

	@Nested
	class SAD_F32 extends CompareSparseToDenseDisparityChecks<GrayF32> {
		public SAD_F32() {
			super(DisparityError.SAD, ImageType.SB_F32);
		}
	}

	@Nested
	class NCC_U8 extends CompareSparseToDenseDisparityChecks<GrayU8> {
		public NCC_U8() {
			super(DisparityError.NCC, ImageType.SB_U8);
		}
	}

	@Nested
	class NCC_F32 extends CompareSparseToDenseDisparityChecks<GrayF32> {
		public NCC_F32() {
			super(DisparityError.NCC, ImageType.SB_F32);
		}
	}

	@Nested
	class CENSUS_U8 extends CompareSparseToDenseDisparityChecks<GrayU8> {
		public CENSUS_U8() {
			super(DisparityError.CENSUS, ImageType.SB_U8);
		}

		@Override
		public <D extends ImageGray<D>> StereoDisparity<GrayU8, D> createDense( ConfigDisparityBM config ) {
			double maxError = (config.regionRadiusX*2 + 1)*(config.regionRadiusY*2 + 1)*config.maxPerPixelError;
			DisparitySelect select = createDisparitySelect(config, imageType.getImageClass(), (int)maxError);
			FilterCensusTransform censusTran = FactoryCensusTransform.variant(config.configCensus.variant, true, imageType.getImageClass());
			BlockRowScore rowScore = createCensusRowScore(config, censusTran);

			DisparityBlockMatchRowFormat alg = createBlockMatching(config,
					censusTran.getOutputType().getImageClass(), select, rowScore);
			alg.setBorder(FactoryImageBorder.generic(config.border, censusTran.getOutputType()));
			ImageBorder censusBorder = FactoryImageBorder.single(CENSUS_BORDER, imageType.getImageClass());
			return new MakeCensusDenseLikeSparse<>(censusTran, censusBorder, alg);
		}
	}

	@Nested
	class CENSUS_U16 extends CompareSparseToDenseDisparityChecks<GrayU16> {
		public CENSUS_U16() {
			super(DisparityError.CENSUS, ImageType.SB_U16);
		}

		@Override
		public <D extends ImageGray<D>> StereoDisparity<GrayU16, D> createDense( ConfigDisparityBM config ) {
			double maxError = (config.regionRadiusX*2 + 1)*(config.regionRadiusY*2 + 1)*config.maxPerPixelError;
			DisparitySelect select = createDisparitySelect(config, imageType.getImageClass(), (int)maxError);
			FilterCensusTransform censusTran = FactoryCensusTransform.variant(config.configCensus.variant, true, imageType.getImageClass());
			BlockRowScore rowScore = createCensusRowScore(config, censusTran);

			DisparityBlockMatchRowFormat alg = createBlockMatching(config,
					censusTran.getOutputType().getImageClass(), select, rowScore);
			alg.setBorder(FactoryImageBorder.generic(config.border, censusTran.getOutputType()));
			ImageBorder censusBorder = FactoryImageBorder.single(CENSUS_BORDER, imageType.getImageClass());
			return new MakeCensusDenseLikeSparse<>(censusTran, censusBorder, alg);
		}
	}

	@Nested
	class CENSUS_F32 extends CompareSparseToDenseDisparityChecks<GrayF32> {
		public CENSUS_F32() {
			super(DisparityError.CENSUS, ImageType.SB_F32);
		}

		@Override
		public <D extends ImageGray<D>> StereoDisparity<GrayF32, D> createDense( ConfigDisparityBM config ) {
			double maxError = (config.regionRadiusX*2 + 1)*(config.regionRadiusY*2 + 1)*config.maxPerPixelError;
			DisparitySelect select = createDisparitySelect(config, imageType.getImageClass(), (int)maxError);
			FilterCensusTransform censusTran = FactoryCensusTransform.variant(config.configCensus.variant, true, imageType.getImageClass());
			BlockRowScore rowScore = createCensusRowScore(config, censusTran);

			DisparityBlockMatchRowFormat alg = createBlockMatching(config,
					censusTran.getOutputType().getImageClass(), select, rowScore);
			alg.setBorder(FactoryImageBorder.generic(config.border, censusTran.getOutputType()));
			ImageBorder censusBorder = FactoryImageBorder.single(CENSUS_BORDER, imageType.getImageClass());
			return new MakeCensusDenseLikeSparse<>(censusTran, censusBorder, alg);
		}
	}
}
