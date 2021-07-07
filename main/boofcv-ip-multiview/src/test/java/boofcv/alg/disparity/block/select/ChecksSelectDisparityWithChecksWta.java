/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.disparity.block.DisparitySelect;
import boofcv.alg.disparity.block.SelectDisparityWithChecksWta;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public abstract class ChecksSelectDisparityWithChecksWta<ArrayData, D extends ImageGray<D>>
		extends ChecksSelectDisparity<ArrayData, D> {
	int w = 20;
	int h = 25;
	int minDisparity = -1;
	int maxDisparity = -1;
	int rangeDisparity;
	int reject;

	protected ChecksSelectDisparityWithChecksWta( Class<ArrayData> arrayType, Class<D> disparityType ) {
		super(arrayType, disparityType);
		disparity = GeneralizedImageOps.createSingleBand(disparityType, w, h);
	}

	void init( int min, int max ) {
		this.minDisparity = min;
		this.maxDisparity = max;
		this.rangeDisparity = maxDisparity - minDisparity + 1;
		this.reject = rangeDisparity;
		GImageMiscOps.fill(disparity, reject);
	}

	public abstract SelectDisparityWithChecksWta<ArrayData, D> createSelector( int rightToLeftTolerance, double texture );

	@Override
	public DisparitySelect<ArrayData, D> createAlg() {
		return createSelector(-1, -1);
	}

	/**
	 * Similar to simpleTest but takes in account the effects of right to left validation
	 */
	@Test
	void testRightToLeftValidation() {
		rightToLeftValidation(0);
		rightToLeftValidation(2);
	}

	private void rightToLeftValidation( int minDisparity ) {
		init(minDisparity, 10);

		int y = 3;
		int r = 2;

		SelectDisparityWithChecksWta<ArrayData, D> alg = createSelector(1, -1);
		alg.configure(disparity, minDisparity, maxDisparity, r);

		int[] scores = new int[w*rangeDisparity];

		for (int d = 0; d < rangeDisparity; d++) {
			for (int x = 0; x < w; x++) {
				scores[w*d + x] = convertErrorToScore(Math.abs(d - 5));
			}
		}

		alg.process(y, copyToCorrectType(scores));

		// Less than the minimum disparity should be reject
		for (int i = 0; i < minDisparity; i++)
			assertEquals(reject, getDisparity(i + r, y), 1e-8);

		// These should all be zero since other pixels will have lower scores
		for (int i = minDisparity; i < 4 + minDisparity; i++)
			assertEquals(reject, getDisparity(i, y), 1e-8);

		// the tolerance is one, so this should be 4
		assertEquals(4, getDisparity(4 + minDisparity, y), 1e-8);
		// should be at 5 for the remainder
		for (int i = minDisparity + 5; i < w; i++)
			assertEquals(5, getDisparity(i, y), 1e-8);

		// sanity check, I now set the tolerance to zero
		alg = createSelector(0, -1);
		alg.configure(disparity, minDisparity, maxDisparity, 2);
		alg.process(y, copyToCorrectType(scores));
		assertEquals(reject, getDisparity(4 + minDisparity, y), 1e-8);
	}

	/**
	 * Test the confidence in a region with very similar cost score (little texture)
	 */
	@Test
	void confidenceFlatRegion() {
		init(0, 10);
		int minValue = 5;
		int y = 3;

		SelectDisparityWithChecksWta<ArrayData, D> alg = createSelector(-1, 0.25);
		alg.configure(disparity, minDisparity, maxDisparity, 2);

		int[] scores = new int[w*rangeDisparity];

		for (int d = 0; d < rangeDisparity; d++) {
			for (int x = 0; x < w; x++) {
				if (x == w/2) {
					scores[w*d + x] = convertErrorToScore(minValue - 1);
				} else {
					scores[w*d + x] = convertErrorToScore(minValue);
				}
			}
		}

		alg.process(y, copyToCorrectType(scores));

		// it should reject the solution
		assertEquals(reject, getDisparity(4 + 2, y), 1e-8);
	}

	/**
	 * There are two similar peaks. Repeated pattern
	 */
	@Test
	void confidenceMultiplePeak() {
		confidenceMultiplePeak(3, 0);
		confidenceMultiplePeak(0, 0);
		confidenceMultiplePeak(3, 2);
		confidenceMultiplePeak(0, 2);
	}

	private void confidenceMultiplePeak( int minValue, int minDisparity ) {
		init(minDisparity, 10);
		int y = 3;
		int r = 2;

		SelectDisparityWithChecksWta<ArrayData, D> alg = createSelector(-1, 0.25);
		alg.configure(disparity, minDisparity, maxDisparity, r);

		int[] scores = new int[w*rangeDisparity];

		for (int d = 0; d < rangeDisparity; d++) {
			for (int x = 0; x < w; x++) {
				scores[w*d + x] = convertErrorToScore(minValue + (d%3));
			}
		}

		alg.process(y, copyToCorrectType(scores));

		// it should reject the solution
		for (int i = r + minDisparity + 3; i < w - r; i++)
			assertEquals(reject, getDisparity(i, y), 1e-8);
	}

	/**
	 * Could potentially return a sub-pixel accuracy but tests are only for pixel accuracy.
	 *
	 * Will not work in all situations since the movement could be farther than 0.5 from
	 * the "correct" value
	 */
	protected int getDisparity( int x, int y ) {
		double value = GeneralizedImageOps.get(disparity, x, y);
		return (int)Math.round(value);
	}
}
