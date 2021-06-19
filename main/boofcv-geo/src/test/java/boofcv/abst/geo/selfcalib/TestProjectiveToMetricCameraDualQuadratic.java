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

package boofcv.abst.geo.selfcalib;

import boofcv.alg.geo.selfcalib.SelfCalibrationLinearDualQuadratic;
import boofcv.alg.geo.selfcalib.SelfCalibrationLinearDualQuadratic.Intrinsic;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.ElevateViewInfo;
import org.ddogleg.struct.FastArray;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestProjectiveToMetricCameraDualQuadratic extends CommonProjectiveToMetricCamerasChecks {
	@Override
	public ProjectiveToMetricCameras createEstimator( boolean singleCamera ) {
		var alg = new SelfCalibrationLinearDualQuadratic(1.0);
		return new ProjectiveToMetricCameraDualQuadratic(alg);
	}

	/**
	 * Make sure it fails if it converges to a solution with too many points behind the camera
	 */
	@Test void invalidFractionAccept() {
		var selfcalib = new SelfCalibrationLinearDualQuadratic(1.0);
		var alg = new ProjectiveToMetricCameraDualQuadratic(selfcalib);
		alg.invalidFractionAccept = 0.10;

		alg.resolveSign.bestInvalid = 0;
		assertTrue(alg.checkBehindCamera(2, 100));

		alg.resolveSign.bestInvalid = 19;
		assertTrue(alg.checkBehindCamera(2, 100));

		// this ensures that it's <=
		alg.resolveSign.bestInvalid = 20;
		assertTrue(alg.checkBehindCamera(2, 100));

		alg.resolveSign.bestInvalid = 21;
		assertFalse(alg.checkBehindCamera(2, 100));
	}

	/**
	 * Have some cameras be duplicated and others not. See if it averages correctly
	 */
	@Test void averageCommonCameras() {
		List<ElevateViewInfo> views = new ArrayList<>();
		views.add(new ElevateViewInfo(300, 400, 1));
		views.add(new ElevateViewInfo(300, 400, 0));
		views.add(new ElevateViewInfo(300, 400, 0));
		views.add(new ElevateViewInfo(300, 400, 2));

		var solutions = new FastArray<>(Intrinsic.class);
		for (int i = 0; i < 4; i++) {
			Intrinsic cam = new Intrinsic();
			cam.fx = 100 + i;
			cam.fy = 200 + i;
			cam.skew = 400 + i;
			solutions.add(cam);
		}

		var selfcalib = new SelfCalibrationLinearDualQuadratic(1.0);
		var alg = new ProjectiveToMetricCameraDualQuadratic(selfcalib);

		// process
		alg.averageCommonCameras(views, solutions, 3);

		// check results
		assertEquals(3, alg.cameraCounts.size);
		assertEquals(3, alg.workCameras.size);

		// see if it counted the cameras correctly
		assertEquals(2, alg.cameraCounts.get(0));
		assertEquals(1, alg.cameraCounts.get(1));
		assertEquals(1, alg.cameraCounts.get(2));

		// See if the values are as epected
		CameraPinhole cam0 = alg.workCameras.get(0);
		CameraPinhole cam1 = alg.workCameras.get(1);
		CameraPinhole cam2 = alg.workCameras.get(2);

		assertEquals(101.5, cam0.fx, UtilEjml.TEST_F64);
		assertEquals(201.5, cam0.fy, UtilEjml.TEST_F64);
		assertEquals(401.5, cam0.skew, UtilEjml.TEST_F64);

		assertEquals(100, cam1.fx, UtilEjml.TEST_F64);
		assertEquals(200, cam1.fy, UtilEjml.TEST_F64);
		assertEquals(400, cam1.skew, UtilEjml.TEST_F64);

		assertEquals(103, cam2.fx, UtilEjml.TEST_F64);
		assertEquals(203, cam2.fy, UtilEjml.TEST_F64);
		assertEquals(403, cam2.skew, UtilEjml.TEST_F64);
	}
}
