/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

import org.ejml.MapPrintFormat;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TestCameraKannalaBrandt extends CommonCameraChecks {
	@Test void fsetSymmetric() {
		var cam = new CameraKannalaBrandt().fsetSymmetric(0.1, 0.2);
		assertArrayEquals(new double[]{0.1, 0.2}, cam.symmetric, UtilEjml.TEST_F64);
	}

	@Test void fsetDistRadial() {
		var cam = new CameraKannalaBrandt().fsetRadial(0.1, 0.2);
		assertArrayEquals(new double[]{0.1, 0.2}, cam.radial, UtilEjml.TEST_F64);
	}

	@Test void fsetDistTangent() {
		var cam = new CameraKannalaBrandt().fsetTangent(0.1, 0.2);
		assertArrayEquals(new double[]{0.1, 0.2}, cam.tangent, UtilEjml.TEST_F64);
	}

	@Test void formatMap() {
		var a = new CameraKannalaBrandt().fsetTangent(0.1, 0.2).fsetRadial(-0.1, -0.2).fsetSymmetric(3,4);
		a.fsetK(1.1234,2,3,4,5,100,200);
		String found = a.formatMap(new MapPrintFormat().withPrecision(2));
		assertEquals("{fx: 1.12, fy: 2, skew: 3, cx: 4, cy: 5, width: 100, height: 200, " +
				"symmetric: {3, 4}, " +
				"radial: {-0.1, -0.2}, radialTrig: {}, " +
				"tangent: {0.1, 0.2}, tangentTrig: {}}", found);
	}
}
