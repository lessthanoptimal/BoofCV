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

import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestSgmHelper extends BoofStandardJUnit {
	@Test
	void localDisparityRangeLeft() {
		SgmHelper helper = new SgmHelper();
		helper.width = 100;
		helper.disparityMin = 0;
		helper.disparityRange = 10;

		for (int x = 0; x < 10; x++) {
			assertEquals(x + 1, helper.localDisparityRangeLeft(x));
		}
		assertEquals(10, helper.localDisparityRangeLeft(10));
		assertEquals(10, helper.localDisparityRangeLeft(20));

		helper.disparityMin = 5;
		for (int x = 5; x < 15; x++) {
			assertEquals(x + 1 - 5, helper.localDisparityRangeLeft(x));
		}
		assertEquals(10, helper.localDisparityRangeLeft(15));
		assertEquals(10, helper.localDisparityRangeLeft(25));
	}

	@Test
	void localDisparityRangeRight() {
		SgmHelper helper = new SgmHelper();
		helper.width = 100;
		helper.disparityMin = 0;
		helper.disparityRange = 10;

		for (int x = 0; x < 10; x++) {
			assertEquals(10, helper.localDisparityRangeRight(x));
		}
		assertEquals(10, helper.localDisparityRangeRight(helper.width - 10));
		for (int i = 0; i < 10; i++) {
			assertEquals(9 - i, helper.localDisparityRangeRight(helper.width - 9 + i));
		}

		helper.disparityMin = 5;
		for (int x = 0; x < 10; x++) {
			assertEquals(10, helper.localDisparityRangeRight(x));
		}
		assertEquals(10, helper.localDisparityRangeRight(helper.width - 15));
		for (int i = 0; i < 10; i++) {
			assertEquals(9 - i, helper.localDisparityRangeRight(helper.width - 14 + i));
		}
		for (int i = 0; i < 5; i++) {
			assertTrue(helper.localDisparityRangeRight(helper.width - 4 + i) <= 0);
		}
	}
}
