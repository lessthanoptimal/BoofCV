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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestProjectiveToMetricCameraDualQuadratic extends CommonProjectiveToMetricCamerasChecks {
	@Override
	public ProjectiveToMetricCameras createEstimator() {
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
		assertTrue(alg.checkBehindCamera(2,100));

		alg.resolveSign.bestInvalid = 19;
		assertTrue(alg.checkBehindCamera(2,100));

		// this ensures that it's <=
		alg.resolveSign.bestInvalid = 20;
		assertTrue(alg.checkBehindCamera(2,100));

		alg.resolveSign.bestInvalid = 21;
		assertFalse(alg.checkBehindCamera(2,100));
	}
}
