/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.disparity.sgm.cost;

import boofcv.alg.disparity.block.BlockRowScore;
import boofcv.alg.disparity.block.BlockRowScoreSad;
import boofcv.alg.disparity.block.score.DisparityScoreBM_S32;
import boofcv.alg.disparity.sgm.SgmDisparityCost;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Nested;

/**
 * @author Peter Abeles
 */
class TestSgmCostFromBlocks extends BoofStandardJUnit {
	@Nested
	public class U8 extends ChecksSgmDisparityCost<GrayU8> {
		public U8() {
			super(0, 255, ImageType.single(GrayU8.class));
		}

		@Override
		SgmDisparityCost<GrayU8> createAlg() {
			SgmCostFromBlocks<GrayU8> blockCost = new SgmCostFromBlocks<>();
			BlockRowScore<GrayU8, int[], byte[]> rowScore = new BlockRowScoreSad.U8();
			blockCost.blockScore = new DisparityScoreBM_S32(1, 1, rowScore, blockCost, imageType);
			blockCost.blockScore.setBorder(FactoryImageBorder.generic(BorderType.REFLECT, ImageType.single(GrayU8.class)));
			return blockCost;
		}
	}
}
