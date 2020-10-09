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

package boofcv.abst.geo.selfcalib;

import boofcv.alg.geo.selfcalib.SelfCalibrationPraticalGuessAndCheckFocus;

/**
 * @author Peter Abeles
 */
class TestProjectiveToMetricCameraPracticalGuessAndCheck extends CommonProjectiveToMetricCamerasChecks {
	public TestProjectiveToMetricCameraPracticalGuessAndCheck() {
		// This approach is unstable and the tests needed to be made less strict and less difficult
		skewTol = 0.4;
		noiseSigma = 0.1;
	}

	@Override
	public ProjectiveToMetricCameras createEstimator() {
		var alg = new SelfCalibrationPraticalGuessAndCheckFocus();
		alg.setSampling(0.3,2,100);
		alg.setSingleCamera(false);
		return new ProjectiveToMetricCameraPracticalGuessAndCheck(alg);
	}
}