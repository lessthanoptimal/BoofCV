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
	static final int imageWidth = 160;
	static final int imageHeight = 120;

	@Test void initialize() {
		var alg = new ScoreCalibrationFill();
		alg.scoreInner = -1;
		alg.scoreBorder = -1;
		alg.regionsBorder = 5;
		alg.regionsInner = 7;
		alg.borderExtent.setFixed(5.0);
		alg.initialize(imageWidth, imageHeight);

		assertEquals(0.0, alg.scoreBorder);
		assertEquals(0.0, alg.scoreInner);
		assertEquals(5.0, alg.actualBorderPx);
		assertEquals(imageWidth, alg.imageWidth);
		assertEquals(imageHeight, alg.imageHeight);
		assertEquals(imageWidth - 10, alg.innerWidth);
		assertEquals(imageHeight - 10, alg.innerHeight);
		assertEquals(5*4, alg.occupiedBorder.size);
		assertEquals(49, alg.occupiedInner.size);
	}

	@Test void addInner() {
		var alg = new ScoreCalibrationFill();
		alg.regionsBorder = 3;
		alg.regionsInner = 5;
		alg.borderExtent.setFixed(5.0);
		alg.initialize(imageWidth, imageHeight);

		var obs = new CalibrationObservation();
		obs.width = imageWidth;
		obs.height = imageHeight;

		// This will be in the middle and not on an edge
		obs.add(0, 50, 56);
		alg.add(obs);
		assertEquals(0.0, alg.scoreBorder, UtilEjml.TEST_F64);
		assertEquals(1.0/25.0, alg.scoreInner, UtilEjml.TEST_F64);

		// Add it again, there should be no change
		alg.add(obs);
		assertEquals(0.0, alg.scoreBorder, UtilEjml.TEST_F64);
		assertEquals(1.0/25.0, alg.scoreInner, UtilEjml.TEST_F64);

		// Another inner point
		obs.points.clear();
		obs.add(0, 15, 7);
		alg.add(obs);
		assertEquals(0.0, alg.scoreBorder, UtilEjml.TEST_F64);
		assertEquals(2.0/25.0, alg.scoreInner, UtilEjml.TEST_F64);
	}

	@Test void addBorder() {
		var alg = new ScoreCalibrationFill();
		alg.borderExtent.setFixed(5.0);
		alg.initialize(imageWidth, imageHeight);
		double N = alg.occupiedBorder.size;

		var obs = new CalibrationObservation();
		obs.width = imageWidth;
		obs.height = imageHeight;

		// Inside a border region
		obs.add(0, 3, 3);
		alg.add(obs);
		assertEquals(1.0/N, alg.scoreBorder, UtilEjml.TEST_F64);
		assertEquals(0.0, alg.scoreInner, UtilEjml.TEST_F64);

		// there should be no change this time
		alg.add(obs);
		assertEquals(1.0/N, alg.scoreBorder, UtilEjml.TEST_F64);
		assertEquals(0.0, alg.scoreInner, UtilEjml.TEST_F64);

		// Along the border, at the border to test <=
		obs.points.clear();
		obs.add(0, 155, 5);
		alg.add(obs);
		assertEquals(2.0/N, alg.scoreBorder, UtilEjml.TEST_F64);
		assertEquals(0.0, alg.scoreInner, UtilEjml.TEST_F64);
	}

	@Test void isNearBorder() {
		int w = imageWidth;
		int h = imageHeight;
		double b = 5.0; // border
		var alg = new ScoreCalibrationFill();
		alg.borderExtent.setFixed(b);
		alg.initialize(w, h);

		// test positive cases
		// some are well inside others are right on the threshold
		assertTrue(alg.isNearBorder(0, 0, w, h));
		assertTrue(alg.isNearBorder(b, b, w, h));
		assertTrue(alg.isNearBorder(w, h, w, h));
		assertTrue(alg.isNearBorder(w - b, h - b, w, h));
		assertTrue(alg.isNearBorder(w, 0, w, h));
		assertTrue(alg.isNearBorder(w - b, b, w, h));
		assertTrue(alg.isNearBorder(0, h, w, h));
		assertTrue(alg.isNearBorder(b, h - b, w, h));
		assertTrue(alg.isNearBorder(w/2.0, b, w, h));
		assertTrue(alg.isNearBorder(b, h/2.0, w, h));

		// negative
		double delta = 1e-6; // very small number to throw it over the threshold
		assertFalse(alg.isNearBorder(b + delta, b + delta, w, h));
		assertFalse(alg.isNearBorder(b + delta, h - b - delta, w, h));
		assertFalse(alg.isNearBorder(w/2.0, b + delta, w, h));
		assertFalse(alg.isNearBorder(b + delta, h/2.0, w, h));
	}

	/**
	 * Gets a list of unoccupied, then adds one point in the center of each region. Then makes sure everything is
	 * occupied
	 */
	@Test void updateUnoccupied() {
		var alg = new ScoreCalibrationFill();
		alg.initialize(imageWidth, imageHeight);

		// Create an observation with a point in the center of each unoccupied region
		var obs = new CalibrationObservation();
		obs.width = imageWidth;
		obs.height = imageHeight;
		alg.updateUnoccupied();
		alg.getUnoccupiedRegions().forIdx(( idx, r ) -> {
			int x = (r.region.x0 + r.region.x1)/2;
			int y = (r.region.y0 + r.region.y1)/2;
			obs.add(idx, x, y);
		});

		// Add and verify the score is 100%
		alg.add(obs);
		assertEquals(1.0, alg.scoreBorder);
		assertEquals(1.0, alg.scoreInner);

		// There should no longer be any unoccupied regions
		alg.updateUnoccupied();
		assertEquals(0, alg.unoccupiedRegions.size);
	}
}
