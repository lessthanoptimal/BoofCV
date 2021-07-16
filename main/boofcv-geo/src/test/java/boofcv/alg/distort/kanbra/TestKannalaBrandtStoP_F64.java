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

import boofcv.alg.distort.pinhole.PinholeNtoP_F64;
import boofcv.struct.calib.CameraKannalaBrandt;
import boofcv.struct.calib.CameraPinhole;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Peter Abeles
 */
class TestKannalaBrandtStoP_F64 extends BoofStandardJUnit {
	/**
	 * Compare to a pinhole camera when there's no distortion terms. Results should be identical.
	 */
	@Test void withoutDistortion() {
		CameraPinhole pinhole = new CameraPinhole().fsetK(500, 550, 0.05, 600, 650);
		CameraKannalaBrandt fish = new CameraKannalaBrandt().fsetK(500, 550, 0.05, 600, 650);

		// Arbitrary 3D point in front of the camera
		Point3D_F64 P3 = new Point3D_F64(0.1, -0.05, 0.8);

		var pinholeStoP = new PinholeNtoP_F64(pinhole);
		var fishStoP = new KannalaBrandtStoP_F64(fish);

		Point2D_F64 expectedPixel = new Point2D_F64();
		Point2D_F64 foundPixel = new Point2D_F64();

		pinholeStoP.compute(P3.x/P3.z, P3.y/P3.z, expectedPixel);
		fishStoP.compute(P3.x, P3.y, P3.z, foundPixel);

		assertEquals(0.0, expectedPixel.distance(foundPixel), 1e-7);
	}

	/**
	 * Qualitative checks for when there's only symmetric distortion
	 */
	@Test void onlySymmetric() {
		fail("Implement");
	}
}
