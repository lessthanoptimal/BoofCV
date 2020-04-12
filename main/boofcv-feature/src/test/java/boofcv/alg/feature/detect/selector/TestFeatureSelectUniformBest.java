/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.selector;

import boofcv.struct.ConfigGridUniform;
import boofcv.struct.QueueCorner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Peter Abeles
 */
class TestFeatureSelectUniformBest extends ChecksFeatureSelectLimit {

	@Override
	public FeatureSelectUniformBest createAlgorithm() {
		return new FeatureSelectUniformBest();
	}

	/**
	 * Makes sure it found features in all cells and that they were the most intense features
	 */
	@Test
	void checkAllCells() {
		checkAllCells(true);
		checkAllCells(false);
	}

	private void checkAllCells(boolean positive) {
		float largeValue = positive ? 5 : -5;
		// every pixel is a corner
		QueueCorner detected = new QueueCorner();
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				detected.grow().set(x,y);
			}
		}
		int cellSize = 10;
		for (int y = 0; y < height; y += cellSize) {
			for (int x = 0; x < width; x += cellSize) {
				intensity.set(x+2,y+2,largeValue);
			}
		}

		// this one corner will by far have the most intense features
		for (int y = 0; y < cellSize; y++) {
			for (int x = 0; x < cellSize; x++) {
				intensity.set(x,y, intensity.get(x,y)+largeValue*4);
			}
		}

		QueueCorner found = new QueueCorner();
		FeatureSelectUniformBest alg = createAlgorithm();
		// make it easy to know the cell size
		alg.configUniform = new HackedConfig(cellSize);

		// it should spread out the detections evenly throughout the cells
		checkSpread(positive,detected, cellSize,1,found, alg);
		// The ones it selects should be the ones with the large values
		for (int y = 0; y < height; y += cellSize) {
			for (int x = 0; x < width; x += cellSize) {
				checkInside(x+2,y+2,found);
			}
		}
		checkSpread(positive,detected, cellSize,2,found, alg);
		// The ones it selects should be the ones with the large values
		for (int y = 0; y < height; y += cellSize) {
			for (int x = 0; x < width; x += cellSize) {
				checkInside(x+2,y+2,found);
			}
		}
	}

	private void checkInside(int x, int y, QueueCorner found) {
		for (int i = 0; i < found.size; i++) {
			if( found.get(i).isIdentical(x,y) )
				return;
		}
		fail("not found inside "+x+" "+y);
	}

	private void checkSpread(boolean positive,QueueCorner detected, int cellSize,
							 int cellCount, QueueCorner found, FeatureSelectUniformBest alg)
	{
		int limit = cellCount*6;
		alg.select(intensity,positive,null,detected,limit,found);

		assertEquals(limit,found.size);
		int[] cells = new int[6];
		for( var p : found.toList() ) {
 			int index = (p.y/cellSize)*3 + (p.x/cellSize);
 			cells[index]++;
		}
		for (int i = 0; i < cells.length; i++) {
			assertEquals(cellCount,cells[i]);
		}
	}

	/**
	 * Makes sure it doesn't double select features in the prior list
	 */
	@Test
	void checkAcknowledgePrior() {
		checkAcknowledgePrior(true);
		checkAcknowledgePrior(false);
	}

	void checkAcknowledgePrior(boolean positive) {
		int width = 30;
		int height = 20;
		// One detected feature in each cell
		QueueCorner prior = new QueueCorner();
		QueueCorner detected = new QueueCorner();
		int cellSize = 10;
		for (int y = 0; y < height; y += cellSize) {
			for (int x = 0; x < width; x += cellSize) {
				detected.grow().set(x+2,y+2);
			}
		}
		// add two prior features to the top row
		for (int x = 0; x < width; x += cellSize) {
			prior.grow().set(x+2,2);
			prior.grow().set(x+2,2);
		}

		QueueCorner found = new QueueCorner();
		FeatureSelectUniformBest alg = createAlgorithm();
		// make it easy to know the cell size
		alg.configUniform = new HackedConfig(cellSize);

		// Since there is a prior feature in every cell and 6 features were requested nothing should be returned
		// since the prior features already constributed to the spread
		alg.select(intensity,positive,prior,detected,3,found);
		assertEquals(3,found.size);
		// the found features should all be in the bottom row since it gives preference to cells without priors
		for (int x = 0, idx=0; x < width; x += cellSize,idx++) {
			assertEquals(x+2,found.get(idx).x);
			assertEquals(12,found.get(idx).y);
		}
		// We now request two and 6 of the detected features should be returned
		alg.select(intensity,positive,prior,detected,6,found);
		assertEquals(6,found.size);
	}

	/**
	 * Every cell has a prior in it. make sure it doesn't get messed up.
	 */
	@Test
	void everyCellHasPrior() {
		everyCellHasPrior(true);
		everyCellHasPrior(false);
	}

	void everyCellHasPrior(boolean positive) {
		int width = 30;
		int height = 20;
		// One detected feature in each cell and two priors
		QueueCorner prior = new QueueCorner();
		QueueCorner detected = new QueueCorner();
		int cellSize = 10;
		for (int y = 0; y < height; y += cellSize) {
			for (int x = 0; x < width; x += cellSize) {
				detected.grow().set(x+2,y+2);
				prior.grow().set(x+2,y+2);
				prior.grow().set(x+1,y+1);
			}
		}

		QueueCorner found = new QueueCorner();
		FeatureSelectUniformBest alg = createAlgorithm();
		// make it easy to know the cell size
		alg.configUniform = new HackedConfig(cellSize);

		// a bug earlier aborted because the total count didn't change when every cell had a prior in it
		alg.select(intensity,positive,prior,detected,6,found);
		assertEquals(6,found.size);
	}

	private static class HackedConfig extends ConfigGridUniform {
		int cellSize;

		public HackedConfig(int cellSize) {
			this.cellSize = cellSize;
		}

		@Override
		public int selectTargetCellSize(int maxSample, int imageWidth, int imageHeight) {
			return cellSize;
		}
	}
}