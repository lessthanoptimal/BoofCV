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
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestScoreCalibrationFill extends BoofStandardJUnit {
	@Test void initialize() {
		var alg = new ScoreCalibrationFill();
		alg.scoreInner = -1;
		alg.scoreBorder = -1;
		alg.regionsBorder = 5;
		alg.regionsInner = 7;
		alg.borderExtent.setFixed(5.0);
		alg.initialize(160, 120);

		assertEquals(0.0, alg.scoreBorder);
		assertEquals(0.0, alg.scoreInner);
		assertEquals(5.0, alg.actualBorderPx);
		assertEquals(160, alg.imageWidth);
		assertEquals(120, alg.imageHeight);
		assertEquals(7*4, alg.occupiedBorder.size);
		assertEquals(25, alg.occupiedInner.size);
	}

	@Test void addInner() {
		var alg = new ScoreCalibrationFill();
		alg.regionsInner = 3;
		alg.borderExtent.setFixed(5.0);
		alg.initialize(160, 120);

		var obs = new CalibrationObservation();
		obs.width = 160;
		obs.height = 120;

		// This will be in the middle and not on an edge
		obs.add(0, 50, 56);
		alg.add(obs);
		assertEquals(0.0, alg.scoreBorder, UtilEjml.TEST_F64);
		assertEquals(0.0, alg.scoreInner, UtilEjml.TEST_F64);

		// This will be on an edge but not near one of the points
		obs.points.clear();
		obs.add(0, 15, 7);
		alg.add(obs);
		assertEquals(0.0, alg.scoreBorder, UtilEjml.TEST_F64);
		assertEquals(0.0, alg.scoreInner, UtilEjml.TEST_F64);
	}

	@Test void addBorder() {
		var alg = new ScoreCalibrationFill();
		alg.borderExtent.setFixed(5.0);
		alg.initialize(160, 120);
		double N = alg.occupiedBorder.size;

		var obs = new CalibrationObservation();
		obs.width = 160;
		obs.height = 120;

		// Exactly on a point
		obs.add(0, 7, 7);
		alg.add(obs);
		assertEquals(1.0/N, alg.scoreBorder, UtilEjml.TEST_F64);

		// there should be no change this time
		alg.add(obs);
		assertEquals(1.0/N, alg.scoreBorder, UtilEjml.TEST_F64);

		// hit another corner
		obs.points.clear();
		obs.add(0, 155, 5);
		alg.add(obs);
		assertEquals(2.0/N, alg.scoreBorder, UtilEjml.TEST_F64);
	}

	@Test void isNearBorder() {
		int w = 160;
		int h = 120;
		double b = 5.0; // border
		var alg = new ScoreCalibrationFill();
		alg.borderExtent.setFixed(b);
		alg.initialize(w, h);

		// test positive cases
		// some are well inside others are right on the threshold
		assertTrue(alg.isNearBorder(0, 0, w, h));
		assertTrue(alg.isNearBorder(2*b, 2*b, w, h));
		assertTrue(alg.isNearBorder(w, h, w, h));
		assertTrue(alg.isNearBorder(w - 2*b, h - 2*b, w, h));
		assertTrue(alg.isNearBorder(w, 0, w, h));
		assertTrue(alg.isNearBorder(w - 2*b, 2*b, w, h));
		assertTrue(alg.isNearBorder(0, h, w, h));
		assertTrue(alg.isNearBorder(2*b, h - 2*b, w, h));
		assertTrue(alg.isNearBorder(w/2.0, b, w, h));
		assertTrue(alg.isNearBorder(b, h/2.0, w, h));

		// negative
		double delta = 1e-6; // very small number to throw it over the threshold
		assertFalse(alg.isNearBorder(2*b + delta, 2*b + delta, w, h));
		assertFalse(alg.isNearBorder(2*b + delta, h - 2*b - delta, w, h));
		assertFalse(alg.isNearBorder(w/2.0, 2*b + delta, w, h));
		assertFalse(alg.isNearBorder(2*b + delta, h/2.0, w, h));
	}

	@Test void findUnoccupiedTop() {
		fail("Implement");
	}

	@Test void findUnoccupiedRight() {
		fail("Implement");
	}

	@Test void findUnoccupiedBottom() {
		fail("Implement");
	}

	@Test void findUnoccupiedLeft() {
		fail("Implement");
	}

	@Test void findUnoccupiedInner() {
		fail("Implement");
	}
}
