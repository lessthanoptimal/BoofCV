/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

	/// Makes sure everything works correctly when larger disparity values are computed and there's an offset.
	/// Created after a bug was found due to type casting.
	@Test void largestDisparityWithOffset() {
		rightToLeftValidation(4, 200);
	}

	/// Similar to simpleTest but takes in account the effects of right to left validation
	@Test
	void testRightToLeftValidation() {
		rightToLeftValidation(0, 10);
		rightToLeftValidation(4, 10);
		// ranges that include negative disparities
		rightToLeftValidation(-5, 5);
		rightToLeftValidation(-8, -2);
	}

	private void rightToLeftValidation( int minDisparity, int maxDisparity ) {
		init(minDisparity, maxDisparity);

		int y = 3;
		int r = 2;

		// Error is minimized at disparity index 5 in every column
		int[] scores = new int[w*rangeDisparity];
		for (int d = 0; d < rangeDisparity; d++) {
			for (int x = 0; x < w; x++) {
				scores[w*d + x] = convertErrorToScore(Math.abs(d - 5));
			}
		}

		// Compare against a model of the expected result for a couple of tolerances
		for (int tol : new int[]{1, 0}) {
			SelectDisparityWithChecksWta<ArrayData, D> alg = createSelector(tol, -1);
			alg.configure(disparity, null, minDisparity, this.maxDisparity, r);
			alg.process(y, copyToCorrectType(scores));

			for (int x = 0; x < w; x++) {
				assertEquals(expectedRightToLeft(x, tol), getDisparity(x, y), 1e-8, "tol=" + tol + " col=" + x);
			}
		}
	}

	/// Models the expected disparity for the score above (index 5 is best), including the right-to-left
	/// consistency check. Returns the disparity index relative to minDisparity, or [#reject].
	private int expectedRightToLeft( int x, int tolRightToLeft ) {
		// Left-to-right disparity window (indices relative to minDisparity), clamped to both image borders
		int lo = Math.max(0, x - (w - 1) - minDisparity);
		int hi = Math.min(maxDisparity, x) - minDisparity;
		if (hi < lo)
			return reject;
		int bestLtoR = Math.max(lo, Math.min(hi, 5));

		// Right column matched by that disparity, and the right-to-left window there
		int rightCol = x - (bestLtoR + minDisparity);
		int loR = Math.max(0, -rightCol - minDisparity);
		int hiR = Math.min(rangeDisparity, w - rightCol - minDisparity) - 1;
		int bestRtoL = Math.max(loR, Math.min(hiR, 5));

		return Math.abs(bestRtoL - bestLtoR) <= tolRightToLeft ? bestLtoR : reject;
	}

	/// Number of valid disparities at column x, clamped against both image borders.
	private int validCount( int x ) {
		int hi = Math.min(maxDisparity, x);
		int lo = Math.max(minDisparity, x - (w - 1));
		return Math.max(0, hi - lo + 1);
	}

	/// Test the confidence in a region with very similar cost score (little texture)
	@Test
	void confidenceFlatRegion() {
		confidenceFlatRegion(0);
		confidenceFlatRegion(2);
		// ranges that include negative disparities
		confidenceFlatRegion(-4);
		confidenceFlatRegion(-8);
	}

	private void confidenceFlatRegion( int minDisparity ) {
		init(minDisparity, 10);
		int minValue = 5;
		int y = 3;

		SelectDisparityWithChecksWta<ArrayData, D> alg = createSelector(-1, 0.25);
		alg.configure(disparity, null, minDisparity, maxDisparity, 2);

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

		// The score is flat across disparity, so texture rejects every column with enough disparities to evaluate
		for (int x = 0; x < w; x++) {
			if (validCount(x) >= 3)
				assertEquals(reject, getDisparity(x, y), 1e-8, "col=" + x);
		}
	}

	/// There are two similar peaks. Repeated pattern
	@Test
	void confidenceMultiplePeak() {
		confidenceMultiplePeak(3, 0);
		confidenceMultiplePeak(0, 0);
		confidenceMultiplePeak(3, 2);
		confidenceMultiplePeak(0, 2);
		// ranges that include negative disparities
		confidenceMultiplePeak(3, -4);
		confidenceMultiplePeak(0, -8);
	}

	private void confidenceMultiplePeak( int minValue, int minDisparity ) {
		init(minDisparity, 10);
		int y = 3;
		int r = 2;

		SelectDisparityWithChecksWta<ArrayData, D> alg = createSelector(-1, 0.25);
		alg.configure(disparity, null, minDisparity, maxDisparity, r);

		int[] scores = new int[w*rangeDisparity];

		for (int d = 0; d < rangeDisparity; d++) {
			for (int x = 0; x < w; x++) {
				scores[w*d + x] = convertErrorToScore(minValue + (d%3));
			}
		}

		alg.process(y, copyToCorrectType(scores));

		// Repeated pattern is ambiguous, so texture rejects every column with enough disparities to evaluate
		for (int x = 0; x < w; x++) {
			if (validCount(x) >= 6)
				assertEquals(reject, getDisparity(x, y), 1e-8, "col=" + x);
		}
	}

	/// Could potentially return a sub-pixel accuracy but tests are only for pixel accuracy.
	/// Will not work in all situations since the movement could be farther than 0.5 from
	/// the "correct" value
	protected int getDisparity( int x, int y ) {
		double value = GeneralizedImageOps.get(disparity, x, y);
		return (int)Math.round(value);
	}
}
