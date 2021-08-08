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

package boofcv.struct.calib;

import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestCameraKannalaBrandt extends BoofStandardJUnit {
	@Test void fsetSymmetric() {
		CameraKannalaBrandt cam = new CameraKannalaBrandt().fsetSymmetric(0.1, 0.2);
		assertArrayEquals(new double[]{0.1, 0.2}, cam.symmetric, UtilEjml.TEST_F64);
	}

	@Test void fsetDistRadial() {
		CameraKannalaBrandt cam = new CameraKannalaBrandt().fsetRadial(0.1, 0.2);
		assertArrayEquals(new double[]{0.1, 0.2}, cam.radial, UtilEjml.TEST_F64);
	}

	@Test void fsetDistTangent() {
		CameraKannalaBrandt cam = new CameraKannalaBrandt().fsetTangent(0.1, 0.2);
		assertArrayEquals(new double[]{0.1, 0.2}, cam.tangent, UtilEjml.TEST_F64);
	}

	@Test void setTo() {
		CameraKannalaBrandt orig = new CameraKannalaBrandt().fsetK(500, 550, 0.0, 600, 650);
		orig.fsetSymmetric(1.0, 0.4).fsetRadial(1.1, 0.2, -0.01).fsetTangent(0.5, -0.1, 0.06, 0.12).
				fsetRadialTrig(0.01, 0.03, -0.03, 0.04).fsetTangentTrig(0.01, 0.2, 0.1, 0.4);

		CameraKannalaBrandt copy = new CameraKannalaBrandt();
		copy.setTo(orig);

		assertTrue(orig.isEquals((CameraPinhole)copy, UtilEjml.TEST_F64));
		assertArrayEquals(orig.symmetric, copy.symmetric, UtilEjml.TEST_F64);
		assertArrayEquals(orig.radial, copy.radial, UtilEjml.TEST_F64);
		assertArrayEquals(orig.tangent, copy.tangent, UtilEjml.TEST_F64);
		assertArrayEquals(orig.radialTrig, copy.radialTrig, UtilEjml.TEST_F64);
		assertArrayEquals(orig.tangentTrig, copy.tangentTrig, UtilEjml.TEST_F64);
	}
}
