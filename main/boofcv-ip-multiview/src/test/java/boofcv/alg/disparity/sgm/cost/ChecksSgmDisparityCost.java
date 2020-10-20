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

package boofcv.alg.disparity.sgm.cost;

import boofcv.BoofTesting;
import boofcv.alg.disparity.sgm.SgmDisparityCost;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Common unit tests for implementations of {@link SgmDisparityCost}
 *
 * @author Peter Abeles
 */
abstract class ChecksSgmDisparityCost<T extends ImageGray<T>> extends BoofStandardJUnit {

	Random rand = new Random(2345);
	ImageType<T> imageType;

	// Image shape
	int width = 25;
	int height = 20;

	// minimum and maximum pixel values, inclusive
	double minValue, maxValue;

	T left;
	T right;

	protected ChecksSgmDisparityCost( double minValue, double maxValue, ImageType<T> imageType ) {
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.imageType = imageType;

		left = imageType.createImage(width, height);
		right = imageType.createImage(width, height);
	}

	abstract SgmDisparityCost<T> createAlg();

	/**
	 * Give it a simple problem that's composed of a horizontal gradient. See if cost matches expectation
	 */
	@Test
	void disparityBounds() {
		// see if min and max disparity are both respected
		disparityBounds(5, 0, 20, true);
		disparityBounds(10, 0, 6, false);
		disparityBounds(5, 4, 16, true);
		disparityBounds(5, 6, 1, false);
	}

	/**
	 * It should output the same answer when called multiple times
	 */
	@Test
	void multipleCalls() {
		int disparity = 5;
		fillRandom(disparity);

		SgmDisparityCost<T> alg = createAlg();
		Planar<GrayU16> cost1 = new Planar<>(GrayU16.class, 1, 1, 1);
		Planar<GrayU16> cost2 = new Planar<>(GrayU16.class, 1, 1, 1);
		alg.configure(0, 14);
		alg.process(left, right, cost1);
		alg.process(left, right, cost2);

		BoofTesting.assertEquals(cost1, cost2, 0);
	}

	private void disparityBounds( int disparity, int minDisparity, int disparityRange, boolean shouldSucceed ) {

		// Set each image to a gradient that has a simple known solution
		fillRandom(disparity);

		// This has a known solution. See if it worked
		Planar<GrayU16> costYXD = new Planar<>(GrayU16.class, 1, 1, 1);
		SgmDisparityCost<T> alg = createAlg();
		alg.configure(minDisparity, disparityRange);
		alg.process(left, right, costYXD);

		// Check outside
		checkInvalidRangeIsMax(minDisparity, disparityRange, costYXD);

		// For each pixel, find the best disparity and see if it matches up with the input image
		int failure = 0;
		int total = 0;
		for (int y = 0; y < height; y++) {
			GrayU16 costXD = costYXD.getBand(y);
			for (int x = minDisparity; x < width; x++) {
				int localRange = Math.min(disparityRange, x - minDisparity + 1);

				// If it can't find the correct solution due to local limitations skip
				if (shouldSucceed && disparity >= minDisparity + localRange)
					continue;

				int bestD = 0;
				int bestCost = costXD.get(0, x - minDisparity);
				for (int d = 1; d < localRange; d++) {
					int c = costXD.get(d, x - minDisparity);
					if (bestCost > c) {
						bestCost = c;
						bestD = d;
					}
				}

				if (bestD + minDisparity != disparity) {
					failure++;
				}
				total++;
			}
		}

		if (shouldSucceed)
			assertTrue(failure/(double)total < 0.05, failure + " vs " + total);
		else
			assertTrue(failure/(double)total > 0.90, failure + " vs " + total);
	}

	/**
	 * Randomly fills in the left image and copies it by a fixed amount into the right
	 */
	private void fillRandom( int disparity ) {
		GImageMiscOps.fillUniform(left, rand, minValue, maxValue);
		GImageMiscOps.fill(right, 0);
		GImageMiscOps.copy(disparity, 0, 0, 0, left.width - disparity, left.height, left, right);
	}

	/**
	 * Makes sure that if a range can't be reached it is set to the max possible cost
	 */
	private void checkInvalidRangeIsMax( int minD, int rangeD, Planar<GrayU16> costYXD ) {
		for (int y = 0; y < height; y++) {
			for (int x = minD; x < width; x++) {
				int localMaxRange = Math.min(rangeD, x - minD + 1);
				for (int d = localMaxRange; d < rangeD; d++) {
					assertEquals(SgmDisparityCost.MAX_COST, lookup(costYXD, x - minD, y, d), y + " " + x + " " + d);
				}
			}
		}
	}

	private static int lookup( Planar<GrayU16> cost, int x, int y, int d ) {
		return cost.getBand(y).get(d, x);
	}

	/**
	 * Does it reshape the output?
	 */
	@Test
	void reshape() {
		SgmDisparityCost<T> alg = createAlg();

		int rangeD = 6;
		Planar<GrayU16> cost = new Planar<>(GrayU16.class, 1, 1, 1);
		alg.configure(5, rangeD);
		alg.process(left, right, cost);

		// cost is Y,X,D order
		assertEquals(height, cost.getNumBands());
		assertEquals(width, cost.getHeight());
		assertEquals(rangeD, cost.getWidth());
	}
}
