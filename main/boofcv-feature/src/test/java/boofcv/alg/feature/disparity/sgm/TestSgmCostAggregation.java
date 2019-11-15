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
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.Planar;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static java.lang.Math.min;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Peter Abeles
 */
class TestSgmCostAggregation {

	Random rand = new Random(234);
	int width=40,height=30, rangeD =5;

	@Test
	void process_basic() {
		fail("Implement");
	}

	@Test
	void process_MultipleCalls() {
		fail("Implement");
	}

	/**
	 * Checks the fill in pattern when a single path is scored
	 */
	@Test
	void score_SinglePath() {
		fail("Implement");
	}

	@Test
	void computeCostInnerD() {
		Planar<GrayU16> costYXD = new Planar<>(GrayU16.class,width,height, rangeD);
		GImageMiscOps.fillUniform(costYXD,rand,0,SgmDisparityCost.MAX_COST);

		SgmCostAggregation alg = new SgmCostAggregation();
		alg.init(costYXD);
		for (int i = 0; i < alg.pathWork.length; i++) {
			alg.pathWork[i] = (short)rand.nextInt(SgmDisparityCost.MAX_COST);
		}

		GrayU16 costXD = costYXD.getBand(2);

		int x = rangeD + 2;  // x-value in image
		int pathI = 3;       // location along the path

		int idxCost = costXD.getIndex(0,x); // x=row, d=col
		int idxWork = alg.lengthD*pathI;
		int minCostPrev = 6;

		// Compute the cost using this algorithm
		alg.computeCostInnerD(costXD,idxCost,idxWork,minCostPrev,minCostPrev+alg.penalty2);

		// Now compare it to a brute force solution
		for (int d = 1; d < rangeD-1; d++) {
			int cost_p_d = costXD.get(d,x);

			int l0 = workArray(alg,pathI,d);
			int l1 = workArray(alg,pathI,d-1) + alg.penalty1;
			int l2 = workArray(alg,pathI,d+1) + alg.penalty1;
			int l3 = minCostPrev+alg.penalty2;

			int v = min(min(min(l0,l1),l2),l3);

			int expected = cost_p_d + v - minCostPrev;
			int found = workArray(alg,pathI+1,d);

			assertEquals(expected,found);
		}
	}

	int workArray( SgmCostAggregation alg , int pathIdx , int d ) {
		return alg.pathWork[alg.lengthD*pathIdx+d]&0xFFFF;
	}

	@Test
	void compare_cost_border_to_inner() {
		fail("Implement");
	}

	@Test
	void saveWorkToAggregated() {
		fail("Implement");
	}

	@Test
	void computePathLength() {
		SgmCostAggregation alg = new SgmCostAggregation();
		alg.lengthX = width;
		alg.lengthY = height;

		checkComputePathLength(alg,0,0,1,0);
		checkComputePathLength(alg,0,0,0,1);
		checkComputePathLength(alg,0,0,1,1);
		checkComputePathLength(alg,0,0,1,2);
		checkComputePathLength(alg,0,0,2,1);

		checkComputePathLength(alg,width-1,height-1,-1, 0);
		checkComputePathLength(alg,width-1,height-1, 0,-1);
		checkComputePathLength(alg,width-1,height-1,-1,-1);
		checkComputePathLength(alg,width-1,height-1,-2,-1);
		checkComputePathLength(alg,width-1,height-1,-1,-2);

		checkComputePathLength(alg,5,0,1,1);
		checkComputePathLength(alg,0,5,1,1);
		checkComputePathLength(alg,width-6,height-1,-1,-1);
		checkComputePathLength(alg,width-1,height-6,-1,-1);
		checkComputePathLength(alg,width-6,height-1,-2,-1);
		checkComputePathLength(alg,width-1,height-6,-1,-2);

		checkComputePathLength(alg,5,0,1,0);
		checkComputePathLength(alg,5,0,1,2);
		checkComputePathLength(alg,5,0,2,1);

		checkComputePathLength(alg,0,5,0,1);
		checkComputePathLength(alg,0,5,2,1);
		checkComputePathLength(alg,0,5,1,2);
	}

	void checkComputePathLength(SgmCostAggregation alg , int x0, int y0, int dx, int dy) {
		int expected = bruteForceLength(x0, y0, dx, dy);
		int found = alg.computePathLength(x0, y0, dx, dy);
		assertEquals(expected,found);
	}

	int bruteForceLength(int x0, int y0, int dx, int dy) {
		int x = x0,y = y0;

		int count = 0;
		while(BoofMiscOps.checkInside(width,height,x,y)) {
			count++;
			x += dx;
			y += dy;
		}

		return count;
	}
}