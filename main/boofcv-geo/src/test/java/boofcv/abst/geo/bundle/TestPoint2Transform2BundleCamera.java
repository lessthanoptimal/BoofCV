/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.geo.bundle;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.bundle.cameras.BundlePinhole;
import boofcv.struct.calib.CameraPinhole;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestPoint2Transform2BundleCamera extends BoofStandardJUnit {
	@Test void compare() {
		var camera = new BundlePinhole();
		camera.setK(100, 100, 0, 100, 100);

		var alg = new Point2Transform2BundleCamera();
		alg.setModel(camera);

		var expected = new Point2D_F64();
		var found = new Point2D_F64();

		PerspectiveOps.renderPixel(new CameraPinhole(100, 100, 0, 100, 100, 0, 0),
				new Point3D_F64(0.1, -0.05, 1.0), expected);
		alg.compute(0.1, -0.05, found);
		assertEquals(0.0, expected.distance(found), UtilEjml.TEST_F64);
	}
}
