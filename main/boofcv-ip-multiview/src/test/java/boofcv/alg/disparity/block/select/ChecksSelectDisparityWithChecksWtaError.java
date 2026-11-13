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

import boofcv.alg.disparity.block.SelectDisparityWithChecksWta;
import boofcv.struct.image.ImageGray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Unit tests for implementers of [SelectDisparityWithChecksWta]
@SuppressWarnings("WeakerAccess")
public abstract class ChecksSelectDisparityWithChecksWtaError<ArrayData, D extends ImageGray<D>>
		extends ChecksSelectDisparityWithChecksWta<ArrayData, D> {
	protected ChecksSelectDisparityWithChecksWtaError( Class<ArrayData> arrayType, Class<D> disparityType ) {
		super(arrayType, disparityType);
	}

	@Override
	public SelectDisparityWithChecksWta<ArrayData, D> createSelector( int rightToLeftTolerance, double texture ) {
		return createSelector(-1, rightToLeftTolerance, texture);
	}

	public abstract SelectDisparityWithChecksWta<ArrayData, D>
	createSelector( int maxError, int rightToLeftTolerance, double texture );

	@Test
	void maxError() {
		init(0, 10);

		int y = 3;

		SelectDisparityWithChecksWta<ArrayData, D> alg = createSelector(2, -1, -1);
		alg.configure(disparity, null, 0, maxDisparity, 2);

		int[] scores = createScores(( d, x ) -> d == 0 ? 5 : x);

		alg.process(y, copyToCorrectType(scores));

		// Below error threshold and disparity of 1 should be optimal
		assertEquals(1, getDisparity(1, y), 1e-8);
		assertEquals(1, getDisparity(2, y), 1);
		// At this point the error should become too high
		assertEquals(reject, getDisparity(3, y), 1e-8);
		assertEquals(reject, getDisparity(4, y), 1e-8);

		// Sanity check, much higher error threshold
		alg = createSelector(20, -1, -1);
		alg.configure(disparity, null, 0, maxDisparity, 2);
		alg.process(y, copyToCorrectType(scores));
		assertEquals(1, getDisparity(3, y), 1);
		assertEquals(1, getDisparity(4, y), 1);
	}

	/// Range of 3 with the best in the middle excludes every other candidate from the texture test. There is no
	/// competing disparity, so the pixel is kept.
	@Test void textureNoOtherCandidate() {
		init(0, 2);
		int y = 3;

		SelectDisparityWithChecksWta<ArrayData, D> alg = createSelector(-1, -1, 0.15);
		alg.configure(disparity, null, 0, maxDisparity, 2);

		int[] scores = createScores(( d, x ) -> d == 1 ? 10 : 20);

		alg.process(y, copyToCorrectType(scores));

		for (int x = 2; x < w; x++) {
			assertEquals(1, getDisparity(x, y), "col=" + x);
		}
	}

	/// Scores as large as SSD with a large region must not overflow in the texture test
	@Test void textureLargeScores() {
		// second best is twice the best, so it's a unique match
		textureLargeScores(1_000_000, 5);
		// second best is only 10% larger, so it's ambiguous
		textureLargeScores(550_000, reject);
	}

	private void textureLargeScores( int secondBest, int expected ) {
		init(0, 10);
		int y = 3;

		SelectDisparityWithChecksWta<ArrayData, D> alg = createSelector(-1, -1, 0.15);
		alg.configure(disparity, null, 0, maxDisparity, 2);

		int[] scores = createScores(( d, x ) -> d == 5 ? 500_000 : d == 2 ? secondBest : 2_000_000);

		alg.process(y, copyToCorrectType(scores));

		// Columns that can see disparities 2 to 6
		for (int x = 6; x < w; x++) {
			assertEquals(expected, getDisparity(x, y), "col=" + x);
		}
	}

	@Override
	public int convertErrorToScore( int d ) {
		return d;
	}
}
