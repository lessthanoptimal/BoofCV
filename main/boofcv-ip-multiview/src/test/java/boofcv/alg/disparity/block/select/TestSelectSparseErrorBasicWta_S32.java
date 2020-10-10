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

import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestSelectSparseErrorBasicWta_S32 extends BoofStandardJUnit {

	@Test
	void simple() {
		int maxDisparity = 30;

		int[] scores = new int[50];
		for (int i = 0; i < maxDisparity; i++) {
			scores[i] = Math.abs(i - 5) + 2;
		}

		var dummy = new ChecksSelectSparseDisparityWithChecks.DummyScore_S32();
		dummy.scoreLeftToRight = scores;
		dummy.setLocalRangeLtoR(maxDisparity);

		SelectSparseErrorBasicWta_S32 alg = new SelectSparseErrorBasicWta_S32();

		// (x,y) is ignored and any value will work
		assertTrue(alg.select(dummy, 0, 0));
		assertEquals(5, (int)alg.getDisparity());
	}
}
