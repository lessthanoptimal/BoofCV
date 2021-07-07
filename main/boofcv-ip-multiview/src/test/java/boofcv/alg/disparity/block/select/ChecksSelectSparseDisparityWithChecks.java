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

import boofcv.alg.disparity.block.SelectSparseStandardWta;
import boofcv.alg.disparity.block.score.DisparitySparseRectifiedScoreBM;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static boofcv.alg.disparity.block.select.ChecksSelectDisparity.copyToCorrectType;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Base class for sparse disparity checks
 *
 * @author Peter Abeles
 */
@SuppressWarnings("WeakerAccess")
public abstract class ChecksSelectSparseDisparityWithChecks<ArrayData> extends BoofStandardJUnit {

	Class<ArrayData> arrayType;

	protected ChecksSelectSparseDisparityWithChecks( Class<ArrayData> arrayType ) {
		this.arrayType = arrayType;
	}

	protected abstract SelectSparseStandardWta<ArrayData> createAlg( int maxError, double texture, int tolRightToLeft );

	/**
	 * Given an error return a score that's appropriate for the algorithm
	 */
	protected abstract int convertErrorToScore( int error );

	/**
	 * All validation tests are turned off
	 */
	@Test
	void everythingOff() {
		int maxDisparity = 30;

		int[] scores = new int[50];
		for (int i = 0; i < maxDisparity; i++) {
			scores[i] = convertErrorToScore(Math.abs(i - 5) + 2);
		}
		// if texture is left on then this will trigger bad stuff
		scores[8] = convertErrorToScore(3);


		SelectSparseStandardWta<ArrayData> alg = createAlg(-1, -1, -1);

		assertTrue(alg.select(createDummy(scores, maxDisparity), -1, -1));

		assertEquals(5, (int)alg.getDisparity());
	}

	/**
	 * Test the confidence in a region with very similar cost score (little texture)
	 */
	@Test
	void confidenceFlatRegion() {
		int minValue = 3;
		int maxDisparity = 10;

		SelectSparseStandardWta<ArrayData> alg = createAlg(-1, 1.0, -1);

		int[] scores = new int[maxDisparity + 10];

		for (int d = 0; d < 10; d++) {
			scores[d] = convertErrorToScore(minValue + Math.abs(2 - d));
		}

		assertFalse(alg.select(createDummy(scores, maxDisparity), -1, -1));
	}

	/**
	 * There are two similar peaks. Repeated pattern
	 */
	@Test
	void confidenceMultiplePeak() {
		confidenceMultiplePeak(3);
		confidenceMultiplePeak(0);
	}

	private void confidenceMultiplePeak( int minValue ) {
		int maxDisparity = 15;

		SelectSparseStandardWta<ArrayData> alg = createAlg(-1, 0.5, -1);

		int[] scores = new int[maxDisparity + 10];

		for (int d = 0; d < maxDisparity; d++) {
			scores[d] = convertErrorToScore(minValue + (d%5));
		}

		assertFalse(alg.select(createDummy(scores, maxDisparity), -1, -1));
	}

	/**
	 * See if multiple peak detection works correctly when the first peak is at zero. There was a bug related to
	 * this at one point.
	 */
	@Test
	void multiplePeakFirstAtIndexZero() {
		int maxDisparity = 10;
		SelectSparseStandardWta<ArrayData> alg = createAlg(-1, 0.1, -1);
		int[] scores = new int[maxDisparity + 10];

		for (int d = 0; d < 10; d++) {
			scores[d] = convertErrorToScore(d*2 + 1);
		}

		assertTrue(alg.select(createDummy(scores, maxDisparity), -1, -1));
	}

	@Test
	void validateRightToLeft() {
		int best = 7;
		int rangeLtoR = 10;
		int[] scoresLtoR = new int[rangeLtoR + 10];
		for (int d = 0; d < rangeLtoR; d++) {
			scoresLtoR[d] = convertErrorToScore(Math.abs(best - d));
		}

		int rangeRtoL = 10;
		int[] scoresRtoL = new int[rangeRtoL + 10];
		for (int d = 0; d < rangeRtoL; d++) {
			scoresRtoL[d] = convertErrorToScore(Math.abs(best - d));
		}

		// Should be a perfect fit here
		SelectSparseStandardWta<ArrayData> alg = createAlg(-1, -1, 0);
		assertTrue(alg.select(createDummy(scoresLtoR, rangeLtoR, scoresRtoL, rangeRtoL), -1, -1));
		assertEquals(best, alg.getDisparity(), 0.1);

		// Now shift the score so that it's off by one, should fail now
		for (int d = 0; d < rangeRtoL; d++) {
			scoresRtoL[d] = convertErrorToScore(Math.abs(1 + best - d));
		}
		assertFalse(alg.select(createDummy(scoresLtoR, rangeLtoR, scoresRtoL, rangeRtoL), -1, -1));

		// Make it more tolerant and it should pass
		alg = createAlg(-1, -1, 1);
		assertTrue(alg.select(createDummy(scoresLtoR, rangeLtoR, scoresRtoL, rangeRtoL), -1, -1));
		assertEquals(best, alg.getDisparity(), 0.1);

		// now it should fail because the range is too small
		assertFalse(alg.select(createDummy(scoresLtoR, rangeLtoR, scoresRtoL, 5), -1, -1));
	}

	public DisparitySparseRectifiedScoreBM createDummy( int[] scores, int localRange ) {
		if (arrayType == float[].class) {
			return new DummyScore_F32((float[])copyToCorrectType(scores, arrayType), null, localRange, -1);
		} else {
			return new DummyScore_S32(scores, null, localRange, -1);
		}
	}

	public DisparitySparseRectifiedScoreBM createDummy( int[] scoresLtoR, int localRangeLtoR,
														int[] scoresRtoL, int localRangeRtoL ) {
		if (arrayType == float[].class) {
			return new DummyScore_F32((float[])copyToCorrectType(scoresLtoR, arrayType),
					(float[])copyToCorrectType(scoresRtoL, arrayType), localRangeLtoR, localRangeRtoL);
		} else {
			return new DummyScore_S32(scoresLtoR, scoresRtoL, localRangeLtoR, localRangeRtoL);
		}
	}

	public static abstract class CheckError<ArrayData> extends ChecksSelectSparseDisparityWithChecks<ArrayData> {
		protected CheckError( Class<ArrayData> arrayType ) {
			super(arrayType);
		}

		@Override
		protected int convertErrorToScore( int error ) {
			return error;
		}
	}

	public static abstract class CheckCorrelation extends ChecksSelectSparseDisparityWithChecks<float[]> {
		protected CheckCorrelation() {
			super(float[].class);
		}

		@Override
		protected int convertErrorToScore( int error ) {
			return -error;
		}
	}

	public static class DummyScore_F32 extends DisparitySparseRectifiedScoreBM<float[], GrayF32> {
		float[] scoreLeftToRight;
		float[] scoreRightToLeft;

		public DummyScore_F32( float[] scoreLeftToRight, float[] scoreRightToLeft,
							   int localRangeLtoR, int localRangeRtoL ) {
			super(GrayF32.class);
			this.scoreLeftToRight = scoreLeftToRight;
			this.scoreRightToLeft = scoreRightToLeft;
			this.localRangeLtoR = localRangeLtoR;
			this.localRangeRtoL = localRangeRtoL;
		}

		public DummyScore_F32() {
			super(GrayF32.class);
		}

		public void setLocalRangeLtoR( int range ) {this.localRangeLtoR = range;}

		public void setLocalRangeRtoL( int range ) {this.localRangeRtoL = range;}

		@Override
		public boolean processLeftToRight( int x, int y ) { return true; }

		@Override
		public boolean processRightToLeft( int x, int y ) { return true; }

		@Override
		protected void scoreDisparity( int disparityRange, boolean leftToRight ) {}

		@Override
		public float[] getScoreLtoR() {return scoreLeftToRight;}

		@Override
		public float[] getScoreRtoL() {return scoreRightToLeft;}
	}

	public static class DummyScore_S32 extends DisparitySparseRectifiedScoreBM<int[], GrayU8> {
		int[] scoreLeftToRight;
		int[] scoreRightToLeft;

		public DummyScore_S32( int[] scoreLeftToRight, int[] scoreRightToLeft,
							   int localRangeLtoR, int localRangeRtoL ) {
			super(GrayU8.class);
			this.scoreLeftToRight = scoreLeftToRight;
			this.scoreRightToLeft = scoreRightToLeft;
			this.localRangeLtoR = localRangeLtoR;
			this.localRangeRtoL = localRangeRtoL;
		}

		public DummyScore_S32() {
			super(GrayU8.class);
		}

		public void setLocalRangeLtoR( int range ) {this.localRangeLtoR = range;}

		public void setLocalRangeRtoL( int range ) {this.localRangeRtoL = range;}

		@Override
		public boolean processLeftToRight( int x, int y ) {return true;}

		@Override
		public boolean processRightToLeft( int x, int y ) {return true;}

		@Override
		protected void scoreDisparity( int disparityRange, boolean leftToRight ) {}

		@Override
		public int[] getScoreLtoR() {return scoreLeftToRight;}

		@Override
		public int[] getScoreRtoL() {return scoreRightToLeft;}
	}
}
