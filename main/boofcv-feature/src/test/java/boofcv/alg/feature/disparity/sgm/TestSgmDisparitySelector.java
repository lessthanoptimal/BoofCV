/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.disparity.sgm;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Peter Abeles
 */
class TestSgmDisparitySelector {

	int width=30;
	int height=20;
	int rangeD=10;

	/**
	 * Provide it a specially constructed cost tensor with a known easily computed solution
	 */
	@Test
	void simple() {
		simple(0);
		simple(5);
	}

	private void simple(int minDisparity) {
		Planar<GrayU16> aggregatedYXD = createCostSimple();

		GrayU8 disparity = new GrayU8(width,height);
		SgmDisparitySelector alg = new SgmDisparitySelector();
		alg.setMinDisparity(minDisparity);
		alg.setRightToLeftTolerance(-1); // disable right to left
		alg.select(aggregatedYXD,disparity);

		// Check the solution
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				// the max possible disparity that it can consider
				int maxRangePlus1 = Math.min(x-minDisparity+1,rangeD);
				int expected = (x+y)%rangeD;

				// The expected disparity is within the range it can compare
				if( maxRangePlus1 > expected )
					assertEquals(expected,disparity.get(x,y),x+" "+y);
				else if( maxRangePlus1 > 0 )
					// the cost for d=0 is the same as all other cost so it will be selected
					assertEquals(0,disparity.get(x,y),x+" "+y);
				else
					// can't sample within the allowed disparity range
					assertEquals(alg.getInvalidDisparity(),disparity.get(x,y));
			}
		}
	}

	private Planar<GrayU16> createCostSimple() {
		Planar<GrayU16> aggregatedYXD = new Planar<>(GrayU16.class,rangeD,width,height);

		// Fill in the cost with known and obvious minimums
		for (int y = 0; y < height; y++) {
			GrayU16 costXD = aggregatedYXD.getBand(y);
			for (int x = 0; x < width; x++) {
				for (int d = 0; d < rangeD; d++) {
					int c = d == (x+y)%rangeD ? 0 : 200;
					costXD.set(d,x,c);
				}
			}
		}
		return aggregatedYXD;
	}
	private Planar<GrayU16> createCostConst(int selectedD ) {
		Planar<GrayU16> aggregatedYXD = new Planar<>(GrayU16.class,rangeD,width,height);

		GImageMiscOps.fill(aggregatedYXD,SgmDisparityCost.MAX_COST);

		// Fill in the cost with known and obvious minimums
		for (int y = 0; y < height; y++) {
			GrayU16 costXD = aggregatedYXD.getBand(y);
			for (int x = 0; x < width; x++) {
				for (int d = 0; d < Math.min(rangeD,x+1); d++) {
					int c = d == selectedD ? 0 : 200;
					costXD.set(d,x,c);
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
	}
	void rightToLeft(int minDisparity) {
		Planar<GrayU16> aggregatedYXD = createCostConst(5);

		int tx = 7, ty=6, d = 5;
		aggregatedYXD.getBand(ty).set(d, tx , 100);
		aggregatedYXD.getBand(ty).set(3, tx-d+3 , 50);

		SgmDisparitySelector alg = new SgmDisparitySelector();
		alg.aggregatedXD = aggregatedYXD.getBand(ty);
		alg.setMinDisparity(minDisparity);
		alg.setup(aggregatedYXD);

		for (int i = 0; i < 3; i++) {
			alg.setRightToLeftTolerance(i);
			// see the match will be within tolerance
			// minDisparity offsets the estimated disparity so take that in account
			if( i < d-3-minDisparity )
				assertEquals(alg.getInvalidDisparity(), alg.findBestDisparity(tx));
			else
				assertEquals(d, alg.findBestDisparity(tx));
		}
	}

	/**
	 * Check to see if the right border is handled correctly
	 */
	@Test
	void rightToLeft_RightBorder() {
		Planar<GrayU16> aggregatedYXD = createCostConst(5);

		int tx = width-1, ty=6, d = 5;

		SgmDisparitySelector alg = new SgmDisparitySelector();
		alg.aggregatedXD = aggregatedYXD.getBand(ty);
		alg.setMinDisparity(0);
		alg.setup(aggregatedYXD);

		// the only value on the right which can be matched with the left is x
		assertEquals(d, alg.findBestDisparity(tx));

		// this will fail because it's not within the allowed range
		alg.setMinDisparity(1);
		assertEquals(alg.getInvalidDisparity(), alg.findBestDisparity(tx));
	}

	@Test
	void maxError() {
		fail("ensure that max error");
	}
}