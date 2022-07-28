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

package boofcv.struct.calib;

import boofcv.testing.BoofStandardJUnit;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestMultiCameraCalib extends BoofStandardJUnit {
	@Test void getBaseline() {
		var alg = new MultiCameraCalibParams();
		alg.listCameraToSensor.add(SpecialEuclideanOps_F64.eulerXyz(1, 0, 0, 0, 0, 0, null));
		alg.listCameraToSensor.add(SpecialEuclideanOps_F64.eulerXyz(3, 0, 0, 0, 0, 0, null));

		assertEquals(2.0, alg.getBaseline(0, 1), UtilEjml.TEST_F64);
		assertEquals(2.0, alg.getBaseline(1, 0), UtilEjml.TEST_F64);
	}

	@Test void computeExtrinsics() {
		var alg = new MultiCameraCalibParams();
		alg.listCameraToSensor.add(SpecialEuclideanOps_F64.eulerXyz(1, 0, 0, 0, 0, 0, null));
		alg.listCameraToSensor.add(SpecialEuclideanOps_F64.eulerXyz(3, 0, 0, 0, 0, 0, null));

		Se3_F64 found01 = alg.computeExtrinsics(0, 1, null);
		assertEquals(-2, found01.T.x, UtilEjml.TEST_F64);
		Se3_F64 found10 = alg.computeExtrinsics(1, 0, null);
		assertEquals(2, found10.T.x, UtilEjml.TEST_F64);
	}
}
