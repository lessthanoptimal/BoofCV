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

import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSelectErrorSubpixel extends BoofStandardJUnit {

	@Nested
	public class F32_F32 extends ChecksSelectDisparityWithChecksWtaError<float[], GrayF32> {
		public F32_F32() {
			super(float[].class, GrayF32.class);
		}

		@Override
		public SelectErrorWithChecks_F32<GrayF32> createSelector( int maxError, int rightToLeftTolerance, double texture ) {
			return new SelectErrorSubpixel.F32_F32(maxError, rightToLeftTolerance, texture);
		}

		/**
		 * Given different local error values see if it is closer to the value with a smaller error
		 */
		@Test
		public void addSubpixelBias() {

			GrayF32 img = new GrayF32(w, h);

			SelectErrorSubpixel.F32_F32 alg = new SelectErrorSubpixel.F32_F32(-1, -1, -1);

			alg.configure(img, 0, 20, 2);
			alg.setLocalDisparityMax(20);

			// should be biased towards 4
			alg.columnScore[4] = 100;
			alg.columnScore[5] = 50;
			alg.columnScore[6] = 200;

			alg.setDisparity(4, 5);
			assertTrue(img.data[4] < 5 && img.data[4] > 4);

			// now biased towards 6
			alg.columnScore[4] = 200;
			alg.columnScore[6] = 100;
			alg.setDisparity(4, 5);
			assertTrue(img.data[4] < 6 && img.data[4] > 5);
		}
	}

	@Nested
	public class S32_F32 extends ChecksSelectDisparityWithChecksWtaError<int[], GrayF32> {
		public S32_F32() {
			super(int[].class, GrayF32.class);
		}

		@Override
		public SelectErrorWithChecks_S32<GrayF32> createSelector( int maxError, int rightToLeftTolerance, double texture ) {
			return new SelectErrorSubpixel.S32_F32(maxError, rightToLeftTolerance, texture);
		}

		/**
		 * Given different local error values see if it is closer to the value with a smaller error
		 */
		@Test
		public void addSubpixelBias() {

			GrayF32 img = new GrayF32(w, h);

			SelectErrorSubpixel.S32_F32 alg = new SelectErrorSubpixel.S32_F32(-1, -1, -1);

			alg.configure(img, 0, 20, 2);
			alg.setLocalDisparityMax(20);

			// should be biased towards 4
			alg.columnScore[4] = 100;
			alg.columnScore[5] = 50;
			alg.columnScore[6] = 200;

			alg.setDisparity(4, 5);
			assertTrue(img.data[4] < 5 && img.data[4] > 4);

			// now biased towards 6
			alg.columnScore[4] = 200;
			alg.columnScore[6] = 100;
			alg.setDisparity(4, 5);
			assertTrue(img.data[4] < 6 && img.data[4] > 5);
		}
	}
}
