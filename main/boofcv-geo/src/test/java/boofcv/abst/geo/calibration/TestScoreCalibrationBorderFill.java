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

package boofcv.abst.geo.calibration;

import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestScoreCalibrationBorderFill extends BoofStandardJUnit {
	@Test void initialize() {
		var alg = new ScoreCalibrationBorderFill();
		alg.innerSamples = 7;
		alg.borderSpace.setFixed(5.0);
		alg.touchTol.setFixed(7.0);
		alg.initialize(160, 120);

		assertEquals(4 + 4*7, alg.targets.size);
		assertEquals(5.0, alg.actualBorderTol);
		assertEquals(7.0, alg.actualTouchTol);
	}

	@Test void add_NoHits() {
		var alg = new ScoreCalibrationBorderFill();
		alg.innerSamples = 3;
		alg.borderSpace.setFixed(5.0);
		alg.touchTol.setFixed(7.0);
		alg.initialize(160, 120);

		var obs = new CalibrationObservation();
		obs.width = 160;
		obs.height = 120;

		// This will be in the middle and not on an edge
		obs.add(0, 50, 56);
		alg.add(obs);
		assertEquals(0.0, alg.score);

		// This will be on an edge but not near one of the points
		obs.reset();
		obs.add(0, 15, 7);
		alg.add(obs);
		assertEquals(0.0, alg.score, UtilEjml.TEST_F64);
	}

	@Test void add_MultipleHits() {
		var alg = new ScoreCalibrationBorderFill();
		alg.innerSamples = 3;
		alg.borderSpace.setFixed(5.0);
		alg.touchTol.setFixed(7.0);
		alg.initialize(160, 120);
		double N = alg.targets.size;

		var obs = new CalibrationObservation();
		obs.width = 160;
		obs.height = 120;

		// Exactly on a point
		obs.add(0, 7, 7);
		alg.add(obs);
		assertEquals(1.0/N, alg.score, UtilEjml.TEST_F64);

		// there should be no change this time
		alg.add(obs);
		assertEquals(1.0/N, alg.score, UtilEjml.TEST_F64);

		// hit another corner
		obs.points.clear();
		obs.add(0, 155, 5);
		alg.add(obs);
		assertEquals(2.0/N, alg.score, UtilEjml.TEST_F64);
	}

	@Test void isNearBorder() {
		int w = 160;
		int h = 120;
		double b = 5.0; // border
		var alg = new ScoreCalibrationBorderFill();
		alg.borderSpace.setFixed(b);
		alg.initialize(w, h);

		// test positive cases
		// some are well inside others are right on the threshold
		assertTrue(alg.isNearBorder(0, 0, w, h));
		assertTrue(alg.isNearBorder(2*b, 2*b, w, h));
		assertTrue(alg.isNearBorder(w - 1, h - 1, w, h));
		assertTrue(alg.isNearBorder(w - 1 - 2*b, h - 1 - 2*b, w, h));
		assertTrue(alg.isNearBorder(w - 1, 0, w, h));
		assertTrue(alg.isNearBorder(w - 1 - 2*b, 2*b, w, h));
		assertTrue(alg.isNearBorder(0, h - 1, w, h));
		assertTrue(alg.isNearBorder(2*b, h - 1 - 2*b, w, h));

		// negative
		double delta = 1e-6; // very small number to throw it over the threshold
		assertFalse(alg.isNearBorder(2*b + delta, 2*b, w, h));
		assertFalse(alg.isNearBorder(2*b, 2*b + delta, w, h));
		assertFalse(alg.isNearBorder(2*b + delta, h - 1 - 2*b, w, h));
		assertFalse(alg.isNearBorder(2*b, h - 1 - 2*b - delta, w, h));
	}
}
