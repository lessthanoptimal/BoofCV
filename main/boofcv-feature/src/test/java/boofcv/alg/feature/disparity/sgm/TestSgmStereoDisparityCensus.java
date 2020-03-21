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

package boofcv.alg.feature.disparity.sgm;

import boofcv.abst.filter.FilterImageInterface;
import boofcv.alg.feature.disparity.sgm.cost.SgmCostHamming;
import boofcv.factory.transform.census.FactoryCensusTransform;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import org.junit.jupiter.api.Nested;

import static boofcv.factory.transform.census.CensusVariants.BLOCK_3_3;
import static boofcv.factory.transform.census.CensusVariants.BLOCK_5_5;

/**
 * @author Peter Abeles
 */
class TestSgmStereoDisparityCensus
{
	@Nested
	public class U8_U8 extends GenericSgmStereoDisparityChecks<GrayU8,GrayU8>
	{
		protected U8_U8() {
			super(ImageType.SB_U8);
		}

		@Override
		public SgmStereoDisparity<GrayU8, GrayU8> createAlgorithm() {
			FilterImageInterface censusTran = FactoryCensusTransform.variant(BLOCK_3_3, true, GrayU8.class);
			SgmCostHamming<GrayU8> cost = new SgmCostHamming.U8();
			return new SgmStereoDisparityCensus(censusTran,cost,new SgmDisparitySelector());
		}
	}

	@Nested
	public class U8_S32 extends GenericSgmStereoDisparityChecks<GrayU8, GrayS32>
	{
		protected U8_S32() {
			super(ImageType.SB_U8);
		}

		@Override
		public SgmStereoDisparity<GrayU8, GrayS32> createAlgorithm() {
			FilterImageInterface censusTran = FactoryCensusTransform.variant(BLOCK_5_5, true, GrayU8.class);
			SgmCostHamming<GrayS32> cost = new SgmCostHamming.S32();
			return new SgmStereoDisparityCensus(censusTran,cost,new SgmDisparitySelector());
		}
	}
}