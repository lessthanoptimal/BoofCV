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

package boofcv.alg.feature.detect.selector;

import boofcv.struct.ConfigGridUniform;
import boofcv.struct.QueueCorner;
import georegression.struct.point.Point2D_I16;
import org.ddogleg.struct.FastArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestFeatureSelectUniform extends ChecksFeatureSelectLimit.I16 {

	@Override
	public FeatureSelectUniform<Point2D_I16> createAlgorithm() {
		return new FeatureSelectUniform.I16();
	}

	/**
	 * Makes sure it found features in all cells
	 */
	@Test
	void checkAllCells() {
		// every pixel is a corner
		QueueCorner detected = new QueueCorner();
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				detected.grow().setTo(x, y);
			}
		}
		int cellSize = 10;
		var found = new FastArray<>(Point2D_I16.class);
		FeatureSelectUniform<Point2D_I16> alg = createAlgorithm();
		// make it easy to know the cell size
		alg.configUniform = new TestFeatureSelectUniform.HackedConfig(cellSize);

		// it should spread out the detections evenly throughout the cells
		checkSpread(detected, cellSize, 1, found, alg);
		checkSpread(detected, cellSize, 2, found, alg);
	}

	private void checkSpread( QueueCorner detected, int cellSize,
							  int cellCount, FastArray<Point2D_I16> found, FeatureSelectUniform<Point2D_I16> alg ) {
		int limit = cellCount*6;
		alg.select(width, height, null, detected, limit, found);

		assertEquals(limit, found.size);
		int[] cells = new int[6];
		for (var p : found.toList()) {
			int index = (p.y/cellSize)*3 + (p.x/cellSize);
			cells[index]++;
		}
		for (int i = 0; i < cells.length; i++) {
			assertEquals(cellCount, cells[i]);
		}
	}

	/**
	 * Makes sure it doesn't double select features in the prior list
	 */
	@Test
	void checkAcknowledgePrior() {
		int width = 30;
		int height = 20;
		// One detected feature in each cell
		var prior = new QueueCorner();
		var detected = new QueueCorner();
		int cellSize = 10;
		for (int y = 0; y < height; y += cellSize) {
			for (int x = 0; x < width; x += cellSize) {
				detected.grow().setTo(x + 2, y + 2);
			}
		}
		// add two prior features to the top row
		for (int x = 0; x < width; x += cellSize) {
			prior.grow().setTo(x + 2, 2);
			prior.grow().setTo(x + 2, 2);
		}

		var found = new FastArray<>(Point2D_I16.class);
		FeatureSelectUniform<Point2D_I16> alg = createAlgorithm();
		// make it easy to know the cell size
		alg.configUniform = new HackedConfig(cellSize);

		// Since there is a prior feature in every cell and 6 features were requested nothing should be returned
		// since the prior features already constributed to the spread
		alg.select(width, height, prior, detected, 3, found);
		assertEquals(3, found.size);
		// the found features should all be in the bottom row since it gives preference to cells without priors
		for (int x = 0, idx = 0; x < width; x += cellSize, idx++) {
			assertEquals(x + 2, found.get(idx).x);
			assertEquals(12, found.get(idx).y);
		}
		// We now request two and 6 of the detected features should be returned
		alg.select(width, height, prior, detected, 6, found);
		assertEquals(6, found.size);
	}

	/**
	 * Every cell has a prior in it. make sure it doesn't get messed up.
	 */
	@Test
	void everyCellHasPrior() {
		int width = 30;
		int height = 20;
		// One detected feature in each cell and two priors
		QueueCorner prior = new QueueCorner();
		QueueCorner detected = new QueueCorner();
		int cellSize = 10;
		for (int y = 0; y < height; y += cellSize) {
			for (int x = 0; x < width; x += cellSize) {
				detected.grow().setTo(x + 2, y + 2);
				prior.grow().setTo(x + 2, y + 2);
				prior.grow().setTo(x + 1, y + 1);
			}
		}

		var found = new FastArray<>(Point2D_I16.class);
		FeatureSelectUniform<Point2D_I16> alg = createAlgorithm();
		// make it easy to know the cell size
		alg.configUniform = new HackedConfig(cellSize);

		// a bug earlier aborted because the total count didn't change when every cell had a prior in it
		alg.select(width, height, prior, detected, 6, found);
		assertEquals(6, found.size);
	}

	private static class HackedConfig extends ConfigGridUniform {
		int cellSize;

		public HackedConfig( int cellSize ) {
			this.cellSize = cellSize;
		}

		@Override
		public int selectTargetCellSize( int maxSample, int imageWidth, int imageHeight ) {
			return cellSize;
		}
	}
}
