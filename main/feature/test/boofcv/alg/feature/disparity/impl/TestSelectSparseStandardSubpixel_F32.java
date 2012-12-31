/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.disparity.impl;

import boofcv.alg.feature.disparity.SelectSparseStandardWta;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSelectSparseStandardSubpixel_F32 extends ChecksSelectSparseStandardWta<float[]>{

	public TestSelectSparseStandardSubpixel_F32() {
		super(float[].class);
	}

	@Override
	protected SelectSparseStandardWta<float[]> createAlg(int maxError, double texture) {
		return new SelectSparseStandardSubpixel.F32(maxError,texture);
	}

	/**
	 * Given different local error values see if it is closer to the value with a smaller error
	 */
	@Test
	public void addSubpixelBias() {

		SelectSparseStandardSubpixel.F32 alg = new SelectSparseStandardSubpixel.F32(-1,-1);

		float scores[] = new float[30];
		Arrays.fill(scores,0,10,500);

		// should be biased towards 4
		scores[4] = 100;
		scores[5] = 50;
		scores[6] = 200;

		assertTrue(alg.select(scores, 10));

		double found = alg.getDisparity();
		assertTrue( found < 5 && found > 4);

		// now biased towards 6
		scores[4] = 200;
		scores[6] = 100;
		assertTrue(alg.select(scores, 10));
		found = alg.getDisparity();

		assertTrue( found < 6 && found > 5);
	}
}
