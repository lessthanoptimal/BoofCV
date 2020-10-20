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

package boofcv.alg.disparity.sgm;

import boofcv.alg.disparity.sgm.cost.SgmCostAbsoluteDifference;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Nested;

/**
 * @author Peter Abeles
 */
class TestSgmStereoDisparityError extends BoofStandardJUnit {
	@Nested
	public class U8_U8 extends GenericSgmStereoDisparityChecks<GrayU8, GrayU8> {
		protected U8_U8() {
			super(ImageType.SB_U8);
		}

		@Override
		public SgmStereoDisparity<GrayU8, GrayU8> createAlgorithm() {
			SgmDisparitySelector selector = new SgmDisparitySelector();
			return new SgmStereoDisparityError<>(new SgmCostAbsoluteDifference.U8(), selector);
		}
	}
}
