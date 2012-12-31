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

import static boofcv.alg.feature.disparity.impl.ChecksSelectRectStandardBase.copyToCorrectType;
import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public abstract class ChecksSelectSparseStandardWta<ArrayData> {

	Class<ArrayData> arrayType;

	protected ChecksSelectSparseStandardWta(Class<ArrayData> arrayType) {
		this.arrayType = arrayType;
	}

	protected abstract SelectSparseStandardWta<ArrayData> createAlg(int maxError, double texture);

	/**
	 * All validation tests are turned off
	 */
	@Test
	public void everythingOff() {
		int maxDisparity = 30;

		int scores[] = new int[50];
		for( int i = 0; i < maxDisparity; i++) {
			scores[i] = Math.abs(i-5)+2;
		}
		// if texture is left on then this will trigger bad stuff
		scores[8]=3;

		SelectSparseStandardWta<ArrayData> alg = createAlg(-1,-1);

		assertTrue(alg.select(copyToCorrectType(scores,arrayType),maxDisparity));

		assertEquals(5,(int)alg.getDisparity());
	}

	/**
	 * Test the confidence in a region with very similar cost score (little texture)
	 */
	@Test
	public void confidenceFlatRegion() {
		int minValue = 3;
		int maxDisparity=10;

		SelectSparseStandardWta<ArrayData> alg = createAlg(-1,3);

		int scores[] = new int[maxDisparity+10];

		for( int d = 0; d < 10; d++ ) {
			scores[d] = minValue + Math.abs(2-d);
		}

		assertFalse(alg.select(copyToCorrectType(scores,arrayType), maxDisparity));
	}

	/**
	 * There are two similar peaks.  Repeated pattern
	 */
	@Test
	public void confidenceMultiplePeak() {
		confidenceMultiplePeak(3);
		confidenceMultiplePeak(0);
	}

	private void confidenceMultiplePeak(int minValue ) {
		int maxDisparity=10;

		SelectSparseStandardWta<ArrayData> alg = createAlg(-1,3);

		int scores[] = new int[maxDisparity+10];

		for( int d = 0; d < 10; d++ ) {
			scores[d] = minValue + (d % 3);
		}

		assertFalse(alg.select(copyToCorrectType(scores,arrayType), maxDisparity));
	}

	/**
	 * See if multiple peak detection works correctly when the first peak is at zero.  There was a bug related to
	 * this at one point.
	 */
	@Test
	public void multiplePeakFirstAtIndexZero() {

		int maxDisparity=10;

		SelectSparseStandardWta<ArrayData> alg = createAlg(-1,3);

		int scores[] = new int[maxDisparity+10];

		for( int d = 0; d < 10; d++ ) {
			scores[d] = d*2+1;
		}

		assertTrue(alg.select(copyToCorrectType(scores,arrayType), maxDisparity));
	}
}
