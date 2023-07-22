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

package boofcv.alg.disparity.sgm.cost;

import boofcv.alg.disparity.sgm.SgmDisparityCost;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Nested;

class TestSgmCostAbsoluteDifference extends BoofStandardJUnit {
	@Nested
	public class U8 extends ChecksSgmDisparityCost<GrayU8> {
		public U8() {
			super(0, 255, ImageType.single(GrayU8.class));
		}

		@Override
		SgmDisparityCost<GrayU8> createAlg() {
			return new SgmCostAbsoluteDifference.U8();
		}
	}
}
