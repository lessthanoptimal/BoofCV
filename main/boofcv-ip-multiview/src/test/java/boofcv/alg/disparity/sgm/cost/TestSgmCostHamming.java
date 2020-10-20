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

import boofcv.alg.disparity.sgm.SgmDisparityCost;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayS64;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Nested;

/**
 * @author Peter Abeles
 */
class TestSgmCostHamming extends BoofStandardJUnit {

	@Nested
	public class U8 extends ChecksSgmDisparityCost<GrayU8> {
		public U8() {
			super(0, 255, ImageType.single(GrayU8.class));
		}

		@Override
		SgmDisparityCost<GrayU8> createAlg() {
			return new SgmCostHamming.U8();
		}
	}

	@Nested
	public class S32 extends ChecksSgmDisparityCost<GrayS32> {
		public S32() {
			super(0, 2000, ImageType.single(GrayS32.class));
		}

		@Override
		SgmDisparityCost<GrayS32> createAlg() {
			return new SgmCostHamming.S32();
		}
	}

	@Nested
	public class S64 extends ChecksSgmDisparityCost<GrayS64> {
		public S64() {
			super(0, 40000, ImageType.single(GrayS64.class));
		}

		@Override
		SgmDisparityCost<GrayS64> createAlg() {
			return new SgmCostHamming.S64();
		}
	}
}
