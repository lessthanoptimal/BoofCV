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

package boofcv.alg.disparity.block.select;

import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Nested;

/**
 * @author Peter Abeles
 */
public class TestSelectErrorWithChecks extends BoofStandardJUnit {

	@Nested
	public class F32_U8 extends ChecksSelectDisparityWithChecksWtaError<float[], GrayU8> {
		F32_U8() {
			super(float[].class, GrayU8.class);
		}

		@Override
		public SelectErrorWithChecks_F32<GrayU8> createSelector( int maxError, int rightToLeftTolerance, double texture ) {
			return new SelectErrorWithChecks_F32.DispU8(maxError, rightToLeftTolerance, texture);
		}
	}

	@Nested
	public class S32_U8 extends ChecksSelectDisparityWithChecksWtaError<int[], GrayU8> {
		S32_U8() {
			super(int[].class, GrayU8.class);
		}

		@Override
		public SelectErrorWithChecks_S32<GrayU8> createSelector( int maxError, int rightToLeftTolerance, double texture ) {
			return new SelectErrorWithChecks_S32.DispU8(maxError, rightToLeftTolerance, texture);
		}
	}
}
