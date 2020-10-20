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

import boofcv.BoofTesting;
import boofcv.alg.disparity.sgm.cost.SgmCostAbsoluteDifference;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static java.lang.Math.min;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("SuspiciousNameCombination")
class TestSgmCostAggregation  extends BoofStandardJUnit {

	Random rand = new Random(234);
	int width = 40, height = 30, rangeD = 5;

	@BeforeEach
	void setup() {
		// Turn off concurrency since it's easier to debug without it
		BoofConcurrency.USE_CONCURRENT = false;
	}

	@Test
	void process_basic() {
		int expectedD = 5;
		Planar<GrayU16> costYXD = createCostWithStep(22, 5, 0);

		// This simple test only works well for paths that are aligned along the axis
		// diagonal paths yield incorrect solutions near the border depending on the path's length it seems
		// Don't think it's a bug, but maybe there's room to improve the algorithm?
		for (int paths : new int[]{2, 4}) {
			SgmCostAggregation alg = new SgmCostAggregation();
			alg.configure(0);
			alg.setPenalty1(200);
			alg.setPenalty2(2000);
			alg.setPathsConsidered(paths);
			alg.process(costYXD);
			Planar<GrayU16> aggregatedYXD = alg.getAggregated();

			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					int localRangeD = alg.helper.localDisparityRangeLeft(x);
					int bestD = -1;
					int bestScore = Integer.MAX_VALUE;
					for (int d = 0; d < localRangeD; d++) {
						int score = aggregatedYXD.getBand(y).get(d, x);
						if (score < bestScore) {
							bestScore = score;
							bestD = d;
						}
					}
//					System.out.println(x+" "+y+"  bestD "+bestD);
					// the outside border's will pick a bad disparity because there is no corresponding pixel
					// in the other one
					if (x >= expectedD && x < width - expectedD) {
						assertEquals(expectedD, bestD);
					}
				}
			}
		}
	}

	/**
	 * Compute the cost using AD. The image has two pixel values and there's a step at stepX.
	 *
	 * @param stepX where the values change
	 */
	private Planar<GrayU16> createCostWithStep( int stepX, int disparity, int minDisparity ) {
		Planar<GrayU16> costYXD = new Planar<>(GrayU16.class, 1, 1, 1);
		GrayU8 left = new GrayU8(width, height);
		GrayU8 right = new GrayU8(width, height);

		ImageMiscOps.fillRectangle(left, 100, stepX, 0, width - stepX, height);
		ImageMiscOps.fillRectangle(right, 100, stepX - disparity, 0, width - stepX - disparity, height);

		SgmCostAbsoluteDifference<GrayU8> cost = new SgmCostAbsoluteDifference.U8();
		cost.configure(minDisparity, 20);
		cost.process(left, right, costYXD);

		return costYXD;
	}

	/**
	 * Does it produce the same results when called multiple times with the same input?
	 */
	@Test
	void process_MultipleCalls() {
		Planar<GrayU16> costYXD = new Planar<>(GrayU16.class, width, height, 12);
		GImageMiscOps.fillUniform(costYXD, rand, 0, 100);

		SgmCostAggregation alg = new SgmCostAggregation();

		for (int paths : new int[]{1, 2, 4, 8, 16}) {
			alg.setPathsConsidered(paths);
			alg.process(costYXD);
			Planar<GrayU16> expected = alg.getAggregated().clone();
			alg.process(costYXD);
			Planar<GrayU16> found = alg.getAggregated();

			BoofTesting.assertEquals(expected, found, 0.0);
		}
	}

	/**
	 * See if it properly handles and respects the requested disparity search bounds?
	 */
	@Test
	void process_DisparitySearch() {
		SgmCostAggregation alg = new SgmCostAggregation();
		alg.setPathsConsidered(4);

		process_DisparitySearch(alg, 0, 5);
		process_DisparitySearch(alg, 4, 5);
		process_DisparitySearch(alg, 6, 5);
		process_DisparitySearch(alg, 8, 5);
	}

	void process_DisparitySearch( SgmCostAggregation alg, int minDisparity, int expectedD ) {
		Planar<GrayU16> costYXD = createCostWithStep(22, expectedD, minDisparity);
		alg.configure(minDisparity);
		alg.process(costYXD);

		double successRate = fractionSuccess(alg, expectedD);
		if (minDisparity <= expectedD)
			assertTrue(successRate > 0.95, "rate = " + successRate);
		else
			assertTrue(successRate < 0.10, "rate = " + successRate);
	}

	private double fractionSuccess( SgmCostAggregation alg, int expectedD ) {
		int correct = 0;
		int total = 0;
		Planar<GrayU16> aggregatedYXD = alg.getAggregated();

		final int disparityMin = alg.disparityMin;

		for (int y = 0; y < height; y++) {
			for (int x = disparityMin; x < width; x++) {
				int localRangeD = alg.helper.localDisparityRangeLeft(x);
				int bestD = -1;
				int bestScore = Integer.MAX_VALUE;
				for (int d = 0; d < localRangeD; d++) {
					int score = aggregatedYXD.getBand(y).get(d, x - disparityMin);
					if (score < bestScore) {
						bestScore = score;
						bestD = d;
					}
				}
//					System.out.println(x+" "+y+"  bestD "+bestD);
				// the outside border's will pick a bad disparity because there is no corresponding pixel
				// in the other one
				if (x >= expectedD && x < width - expectedD) {
					if (expectedD == bestD + disparityMin)
						correct++;
					total++;
				}
			}
		}

		return correct/(double)total;
	}

	/**
	 * Compare concurrent to non-concurrent results
	 */
	@Test
	void compareConcurrent() {
		// larger image to give the threads more time to mess stuff up
		Planar<GrayU16> costYXD = new Planar<>(GrayU16.class, 120, 60, 20);
		GImageMiscOps.fillUniform(costYXD, rand, 0, 100);

		Planar<GrayU16> expected;
		BoofConcurrency.USE_CONCURRENT = false;
		{
			SgmCostAggregation alg = new SgmCostAggregation();
			alg.process(costYXD);
			expected = alg.getAggregated();
		}

		BoofConcurrency.USE_CONCURRENT = true;
		{
			SgmCostAggregation alg = new SgmCostAggregation();
			alg.process(costYXD);
			BoofTesting.assertEquals(expected, alg.getAggregated(), 0.0);
		}
	}

	/**
	 * Makes sure all possible paths are scored once and only once
	 */
	@Test
	void scoreDirection() {
		scoreDirection(1, 0);
		scoreDirection(0, 1);
		scoreDirection(1, 1);
		scoreDirection(-1, 0);
		scoreDirection(0, -1);
		scoreDirection(-1, -1);
		scoreDirection(1, 2);
		scoreDirection(2, 1);
		scoreDirection(1, -2);
		scoreDirection(-2, 1);
		scoreDirection(-1, -2);
		scoreDirection(-2, -1);
	}

	void scoreDirection( int dx, int dy ) {
		Planar<GrayU16> costYXD = new Planar<>(GrayU16.class, rangeD, width, height);

		// The helper will increment score every time it's accessed
		ScorePathHelper alg = new ScorePathHelper();
		alg.init(costYXD);
		alg.scoreDirection(dx, dy);

		// Create a matrix with all the elements that should have been scored set to 1
		GrayU8 expected = alg.scored.createSameShape();
		for (int x = 0; x < alg.effectiveLengthX; x++) {
			if (dy > 0)
				expected.set(x, 0, 1);
			else if (dy < 0)
				expected.set(x, height - 1, 1);
		}
		for (int y = 0; y < height; y++) {
			if (dx > 0)
				expected.set(0, y, 1);
			else if (dx < 0)
				expected.set(alg.effectiveLengthX - 1, y, 1);
		}

		// expected and found scores should be identical
		BoofTesting.assertEquals(expected, alg.scored, 0);
	}

	/**
	 * Compute the score along a direction with an obvious minimum and see if that minimum is found.
	 * This test was created as a sanity check when hunting down a bug.
	 */
	@Test
	void scoreDirection_minimum() {
		// Construct the cost tensor with an obvious minimum
		int targetD = rangeD/2;
		Planar<GrayU16> costYXD = new Planar<>(GrayU16.class, rangeD, width, height);
		for (int y = 0; y < height; y++) {
			GrayU16 costXD = costYXD.getBand(y);
			for (int x = 0; x < width; x++) {
				for (int d = 0; d < rangeD; d++) {
					costXD.set(d, x, targetD == d ? 100 : 1000);
				}
			}
		}

		SgmCostAggregation alg = new SgmCostAggregation();
		alg.init(costYXD);
		alg.scoreDirection(1, 0);
		alg.scoreDirection(-1, 0);

		Planar<GrayU16> aggregatedYXD = alg.getAggregated();

		// see if the minimum in the aggregated cost is at the expected location
		for (int y = 0; y < height; y++) {
			GrayU16 aggregatedXD = aggregatedYXD.getBand(y);
			for (int x = 0; x < width; x++) {
				int localRangeD = alg.helper.localDisparityRangeLeft(x);
				int bestD = -1;
				int bestCost = Integer.MAX_VALUE;
				for (int d = 0; d < localRangeD; d++) {
					int cost = aggregatedXD.get(d, x);
					if (cost < bestCost) {
						bestCost = aggregatedXD.get(d, x);
						bestD = d;
					}
				}
				if (localRangeD > targetD)
					assertEquals(targetD, bestD);
				else
					assertTrue(bestD < targetD || bestD >= 0);
			}
		}
	}

	/**
	 * Checks the fill in pattern when a single path is scored
	 */
	@Test
	void scorePath() {
		int x0 = 0, y0 = 0, dx = 1, dy = 1;

		Planar<GrayU16> costYXD = new Planar<>(GrayU16.class, rangeD, width, height);
		GImageMiscOps.fillUniform(costYXD, rand, 1, SgmDisparityCost.MAX_COST);

		SgmCostAggregation alg = new SgmCostAggregation();

		alg.init(costYXD);
		GImageMiscOps.fill(alg.aggregated, 0xBEEF);
		short[] workCostlr = new short[width*rangeD];
		alg.scorePath(x0, y0, dx, dy, workCostlr);

		// the length is the number of elements to expect
		int foundCount = countNotValue(alg.aggregated, 0xBEEF);

		// Find the number of non-zero elements
		int length = alg.computePathLength(x0, y0, dx, dy);
		int expected = 0;
		for (int i = 0, x = x0; i < length; i++, x += dx) {
			expected += alg.helper.localDisparityRangeLeft(x);
		}
		expected -= length; // at least one element will be zero for each element in the path

		// for each point in the path it computed the aggregated cost
		assertEquals(expected, foundCount);
	}

	private int countNotValue( Planar<GrayU16> aggregated, int value ) {
		int count = 0;
		for (int band = 0; band < aggregated.getNumBands(); band++) {
			GrayU16 b = aggregated.getBand(band);
			for (int row = 0; row < b.height; row++) {
				for (int col = 0; col < b.width; col++) {
					if (b.unsafe_get(col, row) != value)
						count++;
				}
			}
		}
		return count;
	}

	/**
	 * Compute the cost using inner and border methods then compare against a brute force implementationcomputeCostBorderD
	 */
	@Test
	void computeCost_inner_and_border() {
		Planar<GrayU16> costYXD = new Planar<>(GrayU16.class, rangeD, width, height);
		GImageMiscOps.fillUniform(costYXD, rand, 0, SgmDisparityCost.MAX_COST);

		SgmCostAggregation alg = new SgmCostAggregation();
		alg.init(costYXD);
		alg.workspace.get(0).checkSize();
		short[] workCostLr = alg.workspace.get(0).workCostLr;
		for (int i = 0; i < workCostLr.length; i++) {
			workCostLr[i] = (short)rand.nextInt(SgmDisparityCost.MAX_COST);
		}

		int y = 2;
		int x = rangeD + 2;  // x-value away from the image border

		GrayU16 costXD = costYXD.getBand(y);

		int pathI = 3;       // location along the path

		int idxCost = costXD.getIndex(0, x); // x=row, d=col
		int idxLrPrev = alg.lengthD*pathI;

		// Compute the cost using this algorithm
		alg.computeCostInnerD(costXD.data, idxCost, idxLrPrev, rangeD, workCostLr);
		alg.computeCostBorderD(idxCost, idxLrPrev, 0, costXD, rangeD, workCostLr);
		alg.computeCostBorderD(idxCost, idxLrPrev, rangeD - 1, costXD, rangeD, workCostLr);

		// Now compare it to a brute force solution
		bruteForceCost(alg, x, y, pathI, rangeD);
	}

	private void bruteForceCost( SgmCostAggregation alg, int x, int y, int pathI, int localRangeD ) {
		GrayU16 costXD = alg.costYXD.getBand(y);
		final int MC = SgmDisparityCost.MAX_COST;

		for (int d = 0; d < localRangeD; d++) {
			int cost_p_d = costXD.get(d, x);

			int l0 = workArray(alg, pathI, d);
			int l1 = (d > 0 ? workArray(alg, pathI, d - 1) : MC) + alg.penalty1;
			int l2 = (d < localRangeD - 1 ? workArray(alg, pathI, d + 1) : MC) + alg.penalty1;
			int l3 = alg.penalty2;

			int v = min(min(min(l0, l1), l2), l3);

			int expected = cost_p_d + v;
			int found = workArray(alg, pathI + 1, d);

			assertEquals(expected, found);
		}
	}

	int workArray( SgmCostAggregation alg, int pathIdx, int d ) {
		short[] workCostLr = alg.workspace.get(0).workCostLr;
		return workCostLr[alg.lengthD*pathIdx + d] & 0xFFFF;
	}

	@Test
	void saveWorkToAggregated() {
		saveWorkToAggregated(0);
		saveWorkToAggregated(2);
	}

	void saveWorkToAggregated( int disparityMin ) {
		SgmCostAggregation alg = new SgmCostAggregation();
		alg.configure(disparityMin);
		alg.init(new Planar<>(GrayU16.class, rangeD, width, height));

		// set the aggregated cost to 1 so that we can tell the difference between assign and add
		GImageMiscOps.fill(alg.aggregated, 1);

		int x0 = 2, y0 = 2;
		int dx = 1, dy = 2;
		int length = alg.computePathLength(x0, y0, dx, dy);
		short[] workCostLr = new short[length*rangeD];

		// fill in the work cost with a fixed known set of values
		for (int i = 0, x = x0; i < length; i++, x += dx) {
			int localRange = alg.helper.localDisparityRangeLeft(x + disparityMin);
			for (int d = 0; d < localRange; d++) {
				workCostLr[i*rangeD + d] = (short)(1 + i + d);
			}
		}

		// Add the data to the aggregated cost
		alg.saveWorkToAggregated(x0, y0, dx, dy, length, workCostLr);

		// Check to aggregated cost's value along the path
		for (int i = 0, x = x0, y = y0; i < length; i++, x += dx, y += dy) {
			int localRange = alg.helper.localDisparityRangeLeft(x + disparityMin);
			for (int d = 0; d < localRange; d++) {
				short expected = (short)(2 + i + d);
				assertEquals(expected, alg.aggregated.getBand(y).get(d, x), i + " " + d);
				// set the value back to one so that other values can be easily tested
				alg.aggregated.getBand(y).set(d, x, 1);
			}
		}

		// Aggregated should be all ones now
		Planar<GrayU16> ones = alg.aggregated.createSameShape();
		GImageMiscOps.fill(ones, 1);
		BoofTesting.assertEquals(ones, alg.aggregated, 0);
	}

	@Test
	void computePathLength() {
		SgmCostAggregation alg = new SgmCostAggregation();
		// The effective length should be used instead of the actual length to take in account min disparity
		alg.effectiveLengthX = width;
		alg.lengthY = height;

		checkComputePathLength(alg, 0, 0, 1, 0);
		checkComputePathLength(alg, 0, 0, 0, 1);
		checkComputePathLength(alg, 0, 0, 1, 1);
		checkComputePathLength(alg, 0, 0, 1, 2);
		checkComputePathLength(alg, 0, 0, 2, 1);

		checkComputePathLength(alg, width - 1, height - 1, -1, 0);
		checkComputePathLength(alg, width - 1, height - 1, 0, -1);
		checkComputePathLength(alg, width - 1, height - 1, -1, -1);
		checkComputePathLength(alg, width - 1, height - 1, -2, -1);
		checkComputePathLength(alg, width - 1, height - 1, -1, -2);

		checkComputePathLength(alg, 5, 0, 1, 1);
		checkComputePathLength(alg, 0, 5, 1, 1);
		checkComputePathLength(alg, width - 6, height - 1, -1, -1);
		checkComputePathLength(alg, width - 1, height - 6, -1, -1);
		checkComputePathLength(alg, width - 6, height - 1, -2, -1);
		checkComputePathLength(alg, width - 1, height - 6, -1, -2);

		checkComputePathLength(alg, 5, 0, 1, 0);
		checkComputePathLength(alg, 5, 0, 1, 2);
		checkComputePathLength(alg, 5, 0, 2, 1);

		checkComputePathLength(alg, 0, 5, 0, 1);
		checkComputePathLength(alg, 0, 5, 2, 1);
		checkComputePathLength(alg, 0, 5, 1, 2);
	}

	void checkComputePathLength( SgmCostAggregation alg, int x0, int y0, int dx, int dy ) {
		int expected = bruteForceLength(x0, y0, dx, dy);
		int found = alg.computePathLength(x0, y0, dx, dy);
		assertEquals(expected, found);
	}

	int bruteForceLength( int x0, int y0, int dx, int dy ) {
		int x = x0, y = y0;

		int count = 0;
		while (BoofMiscOps.isInside(width, height, x, y)) {
			count++;
			x += dx;
			y += dy;
		}

		return count;
	}

	private static class ScorePathHelper extends SgmCostAggregation {
		GrayU8 scored = new GrayU8(1, 1);

		@Override
		void init( Planar<GrayU16> costYXD ) {
			super.init(costYXD);
			scored.reshape(costYXD.height, costYXD.getNumBands());
		}

		@Override
		void scorePath( int x0, int y0, int dx, int dy, short[] work ) {
			// increment scored every time it's accessed
			scored.set(x0, y0, scored.get(x0, y0) + 1);
		}
	}
}
