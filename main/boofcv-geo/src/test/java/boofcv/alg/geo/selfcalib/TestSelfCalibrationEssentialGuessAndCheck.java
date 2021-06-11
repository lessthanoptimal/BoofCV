/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.selfcalib;

import boofcv.struct.calib.CameraPinhole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestSelfCalibrationEssentialGuessAndCheck extends CommonThreeViewSelfCalibration {
	/**
	 * fixed focus for all the cameras
	 */
	@Test void perfect_fixedFocus() {
		standardScene();
		var camera = new CameraPinhole(700, 700, 0.0, 0, 0, 800, 600);
		setCameras(camera, camera, camera);
		simulateScene(0);

		var alg = new SelfCalibrationEssentialGuessAndCheck();
		alg.imageLengthPixels = 800;
		alg.fixedFocus = true;
		alg.process(F21, P2, observations2);

		assertFalse(alg.isLimit);
		assertEquals(camera.fx, alg.focalLengthA, 25);
		assertEquals(camera.fx, alg.focalLengthB, 25);
	}

	/**
	 * See if it can estimate two different camera models
	 */
	@Test void perfect_two_cameras() {
		standardScene();
		var camera1 = new CameraPinhole(700, 700, 0.0, 0, 0, 800, 600);
		var camera2 = new CameraPinhole(450, 450, 0.0, 0, 0, 800, 600);

		setCameras(camera1, camera2, camera2);
		simulateScene(0);

		var alg = new SelfCalibrationEssentialGuessAndCheck();
		alg.imageLengthPixels = 800;
		alg.fixedFocus = false;
		alg.process(F21, P2, observations2);

		assertFalse(alg.isLimit);
		assertEquals(camera1.fx, alg.focalLengthA, 25);
		assertEquals(camera2.fx, alg.focalLengthB, 25);
	}

	/**
	 * See if it blows up if noise is added
	 */
	@Test void noisy_two_cameras() {
		standardScene();
		var camera1 = new CameraPinhole(700, 700, 0.0, 0, 0, 800, 600);
		var camera2 = new CameraPinhole(450, 450, 0.0, 0, 0, 800, 600);

		setCameras(camera1, camera2, camera2);
		simulateScene(0.25);

		var alg = new SelfCalibrationEssentialGuessAndCheck();
		alg.imageLengthPixels = 800;
		alg.fixedFocus = false;
		alg.process(F21, P2, observations2);

		assertFalse(alg.isLimit);
		assertEquals(camera1.fx, alg.focalLengthA, 25);
		assertEquals(camera2.fx, alg.focalLengthB, 25);
	}

	/**
	 * See if the hit limit flag actually works
	 */
	@Test void hit_limit() {
		standardScene();
		var camera = new CameraPinhole(1500, 1500, 0.0, 0, 0, 800, 600);
		setCameras(camera, camera, camera);
		simulateScene(0);

		var alg = new SelfCalibrationEssentialGuessAndCheck();
		alg.imageLengthPixels = 800;
		alg.fixedFocus = true;
		alg.configure(0.3, 1.0);
		alg.process(F21, P2, observations2);

		// true value of focal length is greater than the range it will test. It should git the limit
		assertTrue(alg.isLimit);
		assertEquals(800, alg.focalLengthA, 25);
		assertEquals(800, alg.focalLengthB, 25);
	}
}
