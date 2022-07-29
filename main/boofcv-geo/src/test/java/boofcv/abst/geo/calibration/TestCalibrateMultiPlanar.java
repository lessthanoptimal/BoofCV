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

import boofcv.abst.fiducial.calib.CalibrationDetectorSquareGrid;
import boofcv.abst.geo.calibration.CalibrateMultiPlanar.FrameCamera;
import boofcv.abst.geo.calibration.CalibrateMultiPlanar.FrameState;
import boofcv.abst.geo.calibration.CalibrateMultiPlanar.TargetExtrinsics;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.alg.geo.calibration.CalibrationObservationSet;
import boofcv.alg.geo.calibration.SynchronizedCalObs;
import boofcv.factory.distort.LensDistortionFactory;
import boofcv.struct.calib.CameraModel;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.calib.MultiCameraCalibParams;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static georegression.struct.se.SpecialEuclideanOps_F64.eulerXyz;
import static org.junit.jupiter.api.Assertions.*;

public class TestCalibrateMultiPlanar extends BoofStandardJUnit {
	CameraPinholeBrown intrinsicA = new CameraPinholeBrown(200,210,0,320,240,640,480).
			fsetRadial(0.01, -0.02);
	CameraPinholeBrown intrinsicB = new CameraPinholeBrown(400,405,0,320,240,800,600);

	List<Point2D_F64> layout = CalibrationDetectorSquareGrid.createLayout(6, 5, .02, .02);

	/**
	 * Test everything together given perfect input
	 */
	@Test void perfect() {
		var expected = new MultiCameraCalibParams();
		var alg = new CalibrateMultiPlanar();

		createScenarioAndConfigure(alg, expected);

		assertTrue(alg.process());

		assertEquals(expected.intrinsics.size(), alg.results.intrinsics.size());
		for (int i = 0; i < expected.intrinsics.size(); i++) {
			CameraPinholeBrown e = expected.getIntrinsics(i);
			CameraPinholeBrown f = alg.results.getIntrinsics(i);

			// Check just some parameters. When it fails it tends to be very wrong
			assertEquals(e.fx, f.fx, 1e-2);
			assertEquals(e.fy, f.fy, 1e-2);
			assertEquals(e.width, f.width);
			assertEquals(e.height, f.height);
		}

		for (int i = 0; i < expected.camerasToSensor.size(); i++) {
			Se3_F64 e = expected.getCameraToSensor(i);
			Se3_F64 f = alg.results.getCameraToSensor(i);
			assertTrue(SpecialEuclideanOps_F64.isIdentical(e, f, 0.01, 0.02));
		}
	}

	/**
	 * Test monocular calibration only
	 */
	@Test void monocularCalibration() {
		var expected = new MultiCameraCalibParams();
		var alg = new CalibrateMultiPlanar();

		createScenarioAndConfigure(alg, expected);

		var frames = new ArrayList<FrameState>();
		for (int i = 0; i < alg.frameObs.size(); i++) {
			frames.add(new FrameState());
		}
		alg.monocularCalibration(0, frames);

		assertEquals(expected.intrinsics.size(), alg.results.intrinsics.size());
		for (int i = 0; i < expected.intrinsics.size(); i++) {
			CameraPinholeBrown e = expected.getIntrinsics(i);
			CameraPinholeBrown f = alg.results.getIntrinsics(i);

			// Check just some parameters. When it fails it tends to be very wrong
			assertEquals(e.fx, f.fx, 1e-2);
			assertEquals(e.fy, f.fy, 1e-2);
			assertEquals(e.width, f.width);
			assertEquals(e.height, f.height);
		}
	}

	void createScenarioAndConfigure( CalibrateMultiPlanar alg, MultiCameraCalibParams expected) {
		expected.intrinsics.add(intrinsicA);
		expected.intrinsics.add(intrinsicB);
		expected.intrinsics.add(intrinsicB);
		expected.camerasToSensor.add(eulerXyz(0, 0, 0, 0, 0, 0, null));
		expected.camerasToSensor.add(eulerXyz(0, 0.15, 0, 0.02, 0, 0, null));
		expected.camerasToSensor.add(eulerXyz(0.1, 0.0, 0, 0, -0.05, 0, null));

		List<Se3_F64> listSensorToWorld = new ArrayList<>();
		listSensorToWorld.add(eulerXyz(0, 0, -2, 0, 0, 0, null));
		listSensorToWorld.add(eulerXyz(-0.3, 0, -2, 0.05, 0, 0, null));
		listSensorToWorld.add(eulerXyz(0, 0.1, -2, 0, 0, 0.04, null));
		listSensorToWorld.add(eulerXyz(0, 0.5, -2, 0, 0.2, 0.04, null));
		listSensorToWorld.add(eulerXyz(0, 0.55, -1.5, 0, -0.2, 0.04, null));
		listSensorToWorld.add(eulerXyz(0.2, 0, -1.8, 0, 0.04, 0, null));
		listSensorToWorld.add(eulerXyz(-0.6, 0, -2, -0.05, -0.01, 0, null));
		listSensorToWorld.add(eulerXyz(0.6, 0, -2, 0.15, -0.1, 0, null));

		alg.getCalibratorMono().configurePinhole(true, 2, true);
		alg.initialize(expected.intrinsics.size(), 1);
		for (int i = 0; i < expected.intrinsics.size(); i++) {
			CameraModel cam = expected.intrinsics.get(i);
			alg.setCameraProperties(i, cam.width, cam.height);
		}
		alg.setTargetLayout(0, layout);

		for (Se3_F64 sensorToWorld : listSensorToWorld) {
			alg.addObservation(createObs(sensorToWorld, expected));
		}
	}

	SynchronizedCalObs createObs(Se3_F64 sensorToWorld, MultiCameraCalibParams params) {
		var syncObs = new SynchronizedCalObs();

		var tgtX = new Point3D_F64();
		var camX = new Point3D_F64();

		for (int i = 0; i < params.intrinsics.size(); i++) {
			Se3_F64 cameraToSensor = params.getCameraToSensor(i);
			CameraPinholeBrown intrinsic = params.getIntrinsics(i);
			Point2Transform2_F64 normToPixel = LensDistortionFactory.narrow(intrinsic).distort_F64(false, true);

			Se3_F64 worldToCamera = cameraToSensor.concat(sensorToWorld, null).invert(null);

			CalibrationObservationSet set = syncObs.cameras.grow();
			set.cameraID = i;
			CalibrationObservation tgtObs = set.targets.grow();

			int behind = 0;
			for (int landmarkID = 0; landmarkID < layout.size(); landmarkID++) {
				tgtX.x = layout.get(landmarkID).x;
				tgtX.y = layout.get(landmarkID).y;

				worldToCamera.transform(tgtX, camX);

				// Skip behind camera
				if (camX.z < 0) {
					behind++;
					continue;
				}

				var landmarkObs = new PointIndex2D_F64();
				normToPixel.compute(camX.x/camX.z, camX.y/camX.z, landmarkObs.p);
				landmarkObs.index = landmarkID;
				tgtObs.points.add(landmarkObs);

				if (!intrinsic.isInside(landmarkObs.p.x, landmarkObs.p.y))
					throw new RuntimeException("Not inside image");
			}

			if (behind >= layout.size()/2)
				throw new RuntimeException("Too many behind camera");
		}
		return syncObs;
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
			expectedC2S.add(eulerXyz(i, 0, 0, 0, 0, 0, null));
		}

		var frames = new ArrayList<FrameState>();
		for (int frameIdx = 0; frameIdx < numFrames; frameIdx++) {
			Se3_F64 tgt_to_sensor = eulerXyz(20 + frameIdx, 0, 0, 0, 0, 0, null);

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

		for (int camID = 0; camID < alg.results.camerasToSensor.size(); camID++) {
			Se3_F64 found = alg.results.camerasToSensor.get(camID);
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
			alg.results.camerasToSensor.add(new Se3_F64());
			alg.results.camerasToSensor.get(camID).T.setTo(100 + camID, 0, 0);
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
			alg.results.camerasToSensor.add(new Se3_F64());
		}
		alg.results.camerasToSensor.get(2).T.setTo(101, 0, 0);

		Se3_F64 cam3_to_world = alg.extrinsicFromKnownCamera(frame, 2, 3);
		Objects.requireNonNull(cam3_to_world);
		assertEquals(0.0, cam3_to_world.T.distance(102, 0, 0), UtilEjml.TEST_F64);
	}
}
