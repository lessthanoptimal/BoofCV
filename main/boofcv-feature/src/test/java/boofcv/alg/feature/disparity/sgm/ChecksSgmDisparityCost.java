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

import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import boofcv.testing.BoofTesting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Common unit tests for implementations of {@link SgmDisparityCost}
 *
 * @author Peter Abeles
 */
abstract class ChecksSgmDisparityCost {

	// Image shape
	int width=25;
	int height=20;

	int maxPixelValue = 255;

	GrayU8 left = new GrayU8(width,height);
	GrayU8 right = new GrayU8(width,height);

	abstract SgmDisparityCost<GrayU8> createAlg();

	/**
	 * Give it a simple problem that's composed of a horizontal gradient. See if cost matches expectation
	 */
	@Test
	void simple_gradient() {
		// see if min and max disparity are both respected
		simple_gradient(0, 20);
		simple_gradient(4, 16);
		simple_gradient(5, 1);
	}

	/**
	 * It should output the same answer when called multiple times
	 */
	@Test
	void multipleCalls() {
		int disparity = 5;
		fillWithGradient(disparity);

		SgmDisparityCost<GrayU8> alg = createAlg();
		Planar<GrayU16> cost1 = new Planar<>(GrayU16.class,1,1,1);
		Planar<GrayU16> cost2 = new Planar<>(GrayU16.class,1,1,1);
		alg.process(left,right,1,14,cost1);
		alg.process(left,right,1,14,cost2);

		BoofTesting.assertEquals(cost1,cost2,0);
	}

	private void simple_gradient(int minDisparity, int disparityRange) {
		// the actual disparity of each pixel
		int disparity = 5;

		// Set each image to a gradient that has a simple known solution
		fillWithGradient(disparity);

		// This has a known solution. See if it worked
		Planar<GrayU16> cost = new Planar<>(GrayU16.class,1,1,1);
		SgmDisparityCost<GrayU8> alg = createAlg();
		alg.process(left,right,minDisparity,disparityRange,cost);

		// Check outside
		checkOutsideDispIsMax(minDisparity,disparityRange, cost);

		// For each pixel, find the best disparity and see if it matches up with the input image
		for (int y = 0; y < height; y++) {
			for (int x = disparity; x < width - disparity; x++) {
				int d = selectDisparity(x,y,cost)+minDisparity;
				assertEquals(left.get(x,y),right.get(x-d,y),1,y+" "+x+" "+d);
			}
		}
	}

	private void fillWithGradient(int disparity) {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				left.set(x,y, y + x+20);
				right.set(x,y, y + x+20+disparity);
			}
		}
	}

	private int selectDisparity(int x , int y , Planar<GrayU16> cost ) {
		int bestValue = Integer.MAX_VALUE;
		int best = -1;

		for (int d = 0; d < cost.width; d++) {
			int v = lookup(cost,x,y,d);
			assertTrue(v <= SgmMutualInformation.MAX_COST);
			if( v < bestValue ) {
				bestValue = v;
				best = d;
			}
		}

		return best;
	}

	/**
	 * Make sure that the of the cost which go outside the image should be filled with max value
	 */
	private void checkOutsideDispIsMax(int minD, int rangeD, Planar<GrayU16> cost) {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < rangeD; x++) {
				// skip over disparity of zero since it will be inside the right image
				for (int d = x+Math.max(minD,1); d < minD+rangeD; d++) {
					assertEquals(SgmDisparityCost.MAX_COST,lookup(cost,x,y,d-minD),y+" "+x+" "+d);
				}
			}
		}
	}

	private static int lookup( Planar<GrayU16> cost , int x , int y , int d ) {
		return cost.getBand(y).get(d,x);
	}

	/**
	 * Does it reshape the output?
	 */
	@Test
	void reshape() {
		SgmDisparityCost<GrayU8> alg = createAlg();

		int rangeD = 6;
		Planar<GrayU16> cost = new Planar<>(GrayU16.class,1,1,1);
		alg.process(left,right,5,rangeD,cost);

		// cost is Y,X,D order
		assertEquals(height,cost.getNumBands());
		assertEquals(width,cost.getHeight());
		assertEquals(rangeD,cost.getWidth());
	}
}