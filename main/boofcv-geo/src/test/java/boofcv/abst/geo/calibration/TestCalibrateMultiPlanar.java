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

import boofcv.abst.geo.calibration.CalibrateMultiPlanar.FrameCamera;
import boofcv.abst.geo.calibration.CalibrateMultiPlanar.FrameState;
import boofcv.abst.geo.calibration.CalibrateMultiPlanar.TargetExtrinsics;
import georegression.struct.EulerType;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class TestCalibrateMultiPlanar {
	@Test void perfect() {
		fail("Implement");
	}

	@Test void noisy() {
		fail("Implement");
	}

	@Test void monocularCalibration() {
		fail("Implement");
	}

	/**
	 * Define a simple scenario and compute observations from ground truth and see if it reconstructs.
	 */
	@Test void estimateCameraToSensor() {
		int numCameras = 3;
		int numFrames = 5;

		// Define the ground truth for camera to sensor
		var expectedC2S = new ArrayList<Se3_F64>();
		for (int i = 0; i < numCameras; i++) {
			expectedC2S.add(SpecialEuclideanOps_F64.eulerXyz(i, 0, 0, 0, 0, 0, null));
		}

		var frames = new ArrayList<FrameState>();
		for (int frameIdx = 0; frameIdx < numFrames; frameIdx++) {
			Se3_F64 tgt_to_sensor = SpecialEuclideanOps_F64.eulerXyz(20 + frameIdx, 0, 0, 0, 0, 0, null);

			frames.add(new FrameState());
			FrameState f = frames.get(frameIdx);
			for (int camID = 0; camID < numCameras; camID++) {
				var t = new TargetExtrinsics();
				t.targetToCamera = tgt_to_sensor.concat(expectedC2S.get(camID).invert(null), null);
				var c = new FrameCamera();
				c.observations.add(t);
				f.cameras.put(camID, c);
			}
		}

		var alg = new CalibrateMultiPlanar();
		alg.initialize(numCameras, 1);
		alg.estimateCameraToSensor(frames);

		for (int camID = 0; camID < alg.results.listCameraToSensor.size(); camID++) {
			Se3_F64 found = alg.results.listCameraToSensor.get(camID);
			Se3_F64 expected = expectedC2S.get(camID);

			assertTrue(SpecialEuclideanOps_F64.isIdentical(found, expected, 1e-8, 1e-4));
		}
	}

	/**
	 * Give it a scenario with known answers where there are multiple cameras in each frame, but only one
	 * camera has an observation.
	 */
	@Test void estimateSensorToWorldInAllFrames() {
		int numCameras = 3;
		int numFrames = 5;

		var alg = new CalibrateMultiPlanar();
		alg.initialize(numCameras, 1);

		// Define locations and observations using a simple formula
		var frames = new ArrayList<FrameState>();
		for (int frameIdx = 0; frameIdx < numFrames; frameIdx++) {
			frames.add(new FrameState());
			FrameState f = frames.get(frameIdx);
			for (int camID = 0; camID < numCameras; camID++) {
				f.cameras.put(camID, new FrameCamera());
			}

			// Each frame only one camera will have an observation
			int camWithTarget = frameIdx%numCameras;
			FrameCamera c = f.cameras.get(camWithTarget);
			var t = new TargetExtrinsics();
			c.observations.add(t);
			t.targetToCamera.T.setTo(5 + frameIdx, 0, 0);
		}

		for (int camID = 0; camID < numCameras; camID++) {
			alg.results.listCameraToSensor.add(new Se3_F64());
			alg.results.listCameraToSensor.get(camID).T.setTo(100 + camID, 0, 0);
		}

		alg.estimateSensorToWorldInAllFrames(frames);

		// Compute the x-coordinate for each frame
		// Note that target[0] is the world frame
		for (int i = 0; i < frames.size(); i++) {
			int camID = i%numCameras;
			int camToSensor = 100 + camID;
			int targetToCam = 5 + i;

			FrameState f = frames.get(i);
			assertEquals(-camToSensor - targetToCam, f.sensorToWorld.T.x, UtilEjml.TEST_F64);
		}
	}

	/**
	 * Test case where there is no commonly observed target
	 */
	@Test void extrinsicFromKnownCamera_negative() {
		var fcam2 = new FrameCamera();
		var fcam3 = new FrameCamera();

		var frame = new FrameState();
		frame.cameras.put(2, fcam2);
		frame.cameras.put(3, fcam3);

		var alg = new CalibrateMultiPlanar();

		// Test no observations case
		assertNull(alg.extrinsicFromKnownCamera(frame, 2, 3));

		// No matching observations
		fcam2.observations.add(new TargetExtrinsics(2));
		fcam3.observations.add(new TargetExtrinsics(0));
		assertNull(alg.extrinsicFromKnownCamera(frame, 2, 3));
	}

	/**
	 * Case with a common target and da known solution.
	 *
	 * For hand computations:
	 * target.x = 100
	 * cam2.x   = 101
	 * cam3.x   = 102
	 */
	@Test void extrinsicFromKnownCamera() {
		var fcam2 = new FrameCamera();
		var fcam3 = new FrameCamera();

		// Add two distractor targets
		fcam2.observations.add(new TargetExtrinsics(2));
		fcam3.observations.add(new TargetExtrinsics(0));

		// Add a common target
		fcam2.observations.add(new TargetExtrinsics(1));
		fcam3.observations.add(new TargetExtrinsics(1));

		fcam2.observations.get(1).targetToCamera.setTo(-1, 0, 0, EulerType.XYZ, 0, 0, 0);
		fcam3.observations.get(1).targetToCamera.setTo(-2, 0, 0, EulerType.XYZ, 0, 0, 0);

		var frame = new FrameState();
		frame.cameras.put(2, fcam2);
		frame.cameras.put(3, fcam3);

		var alg = new CalibrateMultiPlanar();
		for (int i = 0; i < 5; i++) {
			alg.results.listCameraToSensor.add(new Se3_F64());
		}
		alg.results.listCameraToSensor.get(2).T.setTo(101, 0, 0);

		Se3_F64 cam3_to_world = alg.extrinsicFromKnownCamera(frame, 2, 3);
		Objects.requireNonNull(cam3_to_world);
		assertEquals(0.0, cam3_to_world.T.distance(102, 0, 0), UtilEjml.TEST_F64);
	}
}
