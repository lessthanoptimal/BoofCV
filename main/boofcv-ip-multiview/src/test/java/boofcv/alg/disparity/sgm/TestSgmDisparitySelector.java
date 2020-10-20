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

import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
@SuppressWarnings({"SuspiciousNameCombination", "SameParameterValue"})
class TestSgmDisparitySelector extends BoofStandardJUnit {

	int width = 30;
	int height = 20;
	int rangeD = 10;

	/**
	 * Provide it a specially constructed cost tensor with a known easily computed solution
	 */
	@Test
	void simple() {
		simple(0);
		simple(5);
	}

	private void simple( int disparityMin ) {
		Planar<GrayU16> aggregatedYXD = createCostSimple(disparityMin);

		GrayU8 disparity = new GrayU8(width, height);
		SgmDisparitySelector alg = new SgmDisparitySelector();
		alg.setDisparityMin(disparityMin);
		alg.setRightToLeftTolerance(-1); // disable right to left
		alg.select(null, aggregatedYXD, disparity);

		// Check the solution
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < disparityMin; x++) {
				assertEquals(rangeD, disparity.get(x, y), x + " " + y);
			}
			for (int x = disparityMin; x < width; x++) {
				// the max possible disparity that it can consider
				int localRangeD = alg.helper.localDisparityRangeLeft(x);
				int expectedRange = (x + y)%rangeD;
				int foundRange = disparity.get(x, y);

				// The expected disparity is within the range it can compare
				if (expectedRange > disparityMin && expectedRange < disparityMin + localRangeD) {
					assertEquals(expectedRange - disparityMin, foundRange, x + " " + y);
				} else {
					// the cost for d=0 is the same as all other cost so it will be selected
					assertEquals(0, foundRange, x + " " + y);
				}
			}
		}
	}

	private Planar<GrayU16> createCostSimple( int disparityMin ) {
		Planar<GrayU16> aggregatedYXD = new Planar<>(GrayU16.class, rangeD, width, height);

		// Fill in the cost with known and obvious minimums
		for (int y = 0; y < height; y++) {
			GrayU16 costXD = aggregatedYXD.getBand(y);
			for (int x = disparityMin; x < width; x++) {
				int localRangeMax = Math.min(x - disparityMin + 1, rangeD);
				for (int d = 0; d < localRangeMax; d++) {
					int c = (disparityMin + d) == (x + y)%rangeD ? 0 : 200;
					costXD.set(d, x - disparityMin, c);
				}
			}
		}
		return aggregatedYXD;
	}

	private Planar<GrayU16> createCostConst( int disparityMin, int selectedD ) {
		Planar<GrayU16> aggregatedYXD = new Planar<>(GrayU16.class, rangeD, width, height);

		// Fill in the cost with known and obvious minimums
		for (int y = 0; y < height; y++) {
			GrayU16 costXD = aggregatedYXD.getBand(y);
			for (int x = disparityMin; x < width; x++) {
				int localRangeMax = Math.min(x - disparityMin + 1, rangeD);
				for (int d = 0; d < localRangeMax; d++) {
					int c = (disparityMin + d) == selectedD ? 0 : 200;
					costXD.set(d, x, c);
				}
			}
		}
		return aggregatedYXD;
	}

	/**
	 * Checks to see if the left to right test actually works by giving it a scenario where
	 * the solution would be accepted if it was not turned on. Then adjust the threshold and make it
	 * accepted again.
	 */
	@Test
	void rightToLeft() {
		rightToLeft(0);
		rightToLeft(1); // this won't run into border issues
		rightToLeft(2);
	}

	void rightToLeft( int disparityMin ) {
		int d = 5;
		Planar<GrayU16> aggregatedYXD = createCostConst(disparityMin, d);

		int tx = 7, ty = 6;
		aggregatedYXD.getBand(ty).set(d - disparityMin, tx - disparityMin, 100);
		// when right to left is considered it will find this lower cost
		aggregatedYXD.getBand(ty).set(3 - disparityMin, tx - d + 3 - disparityMin, 50);

		SgmDisparitySelector alg = new SgmDisparitySelector();
		GrayU16 aggregatedXD = aggregatedYXD.getBand(ty);
		alg.setDisparityMin(disparityMin);
		alg.setup(aggregatedYXD);

		for (int tol = 0; tol < 3; tol++) {
			alg.setRightToLeftTolerance(tol);
			int found = alg.findBestDisparity(tx, aggregatedXD);
			// see the match will be within tolerance
			// minDisparity offsets the estimated disparity so take that in account
			if (tol < d - 3 + disparityMin)
				assertEquals(alg.getInvalidDisparity(), found, "tol = " + tol);
			else
				assertEquals(d - disparityMin, found, "tol = " + tol);
		}
	}

	/**
	 * Check to see if the right border is handled correctly
	 */
	@Test
	void rightToLeft_RightBorder() {
		rightToLeft_RightBorder(0);
		rightToLeft_RightBorder(1);
	}

	void rightToLeft_RightBorder( int disparityMin ) {
		int tx = width - 1, ty = 6, d = 5;
		Planar<GrayU16> aggregatedYXD = createCostConst(disparityMin, d);
		GrayU16 aggregatedXD = aggregatedYXD.getBand(ty);
		SgmDisparitySelector alg = new SgmDisparitySelector();
		alg.setDisparityMin(disparityMin);
		alg.setup(aggregatedYXD);
		alg.setRightToLeftTolerance(0);
		assertEquals(d - disparityMin, alg.findBestDisparity(tx, aggregatedXD));
	}

	@Test
	void maxError() {
		maxError(0);
		maxError(1);
	}

	void maxError( int disparityMin ) {
		int tx = width - 1, ty = 6, d = 5;
		Planar<GrayU16> aggregatedYXD = createCostConst(disparityMin, d);
		GrayU16 aggregatedXD = aggregatedYXD.getBand(ty);

		// set the cost for the target to a non-zero value so it can be filtered
		aggregatedXD.set(d - disparityMin, tx - disparityMin, 100);

		SgmDisparitySelector alg = new SgmDisparitySelector();
		alg.setDisparityMin(disparityMin);
		alg.setup(aggregatedYXD);
		alg.setRightToLeftTolerance(-1);

		// test obvious scenarios
		alg.setMaxError(150);
		assertEquals(d - disparityMin, alg.findBestDisparity(tx, aggregatedXD));
		alg.setMaxError(50);
		assertEquals(alg.getInvalidDisparity(), alg.findBestDisparity(tx, aggregatedXD));

		// test cases on the threshold
		alg.setMaxError(100);
		assertEquals(d - disparityMin, alg.findBestDisparity(tx, aggregatedXD));
		alg.setMaxError(99);
		assertEquals(alg.getInvalidDisparity(), alg.findBestDisparity(tx, aggregatedXD));
	}

	@Test
	void texture() {
		texture(0);
		texture(1);
	}

	void texture( int disparityMin ) {
		int tx = width - 1, ty = 6, d = 5;
		Planar<GrayU16> aggregatedYXD = createCostConst(disparityMin, d);
		GrayU16 aggregatedXD = aggregatedYXD.getBand(ty);

		// Set another cost along the disparity that will have a value close to the optimal
		aggregatedXD.set(d - disparityMin, tx - disparityMin, 100);
		aggregatedXD.set(d - disparityMin, tx - disparityMin, 120);

		SgmDisparitySelector alg = new SgmDisparitySelector();
		alg.setDisparityMin(disparityMin);
		alg.setup(aggregatedYXD);
		alg.setRightToLeftTolerance(-1);

		// Turn off texture validation
		alg.setTextureThreshold(0.0);
		assertEquals(d - disparityMin, alg.findBestDisparity(tx, aggregatedXD));
		// should still be accepted
		alg.setTextureThreshold(0.05);
		assertEquals(d - disparityMin, alg.findBestDisparity(tx, aggregatedXD));
		// now it should reject everything
		alg.setTextureThreshold(1.0);
		assertEquals(alg.getInvalidDisparity(), alg.findBestDisparity(tx, aggregatedXD));
	}
}
