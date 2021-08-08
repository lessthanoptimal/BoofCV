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

package boofcv.alg.distort.kanbra;

import boofcv.alg.distort.GeneralLensDistortionWideFOVChecks;
import boofcv.alg.distort.LensDistortionWideFOV;
import boofcv.struct.calib.CameraKannalaBrandt;

/**
 * @author Peter Abeles
 */
class TestLensDistortionKannalaBrandt extends GeneralLensDistortionWideFOVChecks {
	@Override public LensDistortionWideFOV create() {
		CameraKannalaBrandt model = new CameraKannalaBrandt().fsetK(500, 550, 0.0, 600, 650).fsetSymmetric(1.0, 0.1, -0.05, 0.01);
		return new LensDistortionKannalaBrandt(model);
	}
}
