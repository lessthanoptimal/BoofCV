/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.calibration;

import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestScoreCalibrationInnerFill extends BoofStandardJUnit {
	/**
	 * Test initialization with different cell sizes
	 */
	@Test void initialize() {
		var alg = new ScoreCalibrationInnerFill();
		alg.cellSize.setFixed(20);
		alg.initialize(140, 120);
		assertEquals(120/20, alg.grid.rows);
		assertEquals(140/20, alg.grid.cols);

		// See it it can handel a relative request.
		alg.cellSize.setRelative(0.1, 0);
		// Manually compute cell size
		int target = (int)(0.1*(140 + 120)/2);
		alg.initialize(140, 120);

		// +1 is due to it rounding up
		assertEquals(120/target + 1, alg.grid.rows);
		assertEquals(140/target + 1, alg.grid.cols);
	}

	/**
	 * Adds images and compare against hand computed expected values.
	 */
	@Test void addImage() {
		var alg = new ScoreCalibrationInnerFill();
		alg.cellSize.setFixed(20);
		alg.initialize(140, 120);
		double N = alg.grid.cells.size;

		// Add one point
		var obs = new CalibrationObservation();
		obs.width = 140;
		obs.height = 120;
		obs.add(0, 5, 50);
		alg.add(obs);
		assertEquals(1.0/N, alg.score);

		// Add 3 points. 1 is in the same cell as before. other 2 are in the same cell as each other
		obs.reset();
		obs.add(0, 5, 52);
		obs.add(2, 82, 52);
		obs.add(3, 82, 51);
		alg.add(obs);
		assertEquals(2.0/N, alg.score);
	}
}
