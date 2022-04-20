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

package boofcv.alg.geo.triangulate;

import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestTriangulate2ViewsGeometricMetric extends CommonTriangulationChecks {

	/**
	 * Create 2 perfect observations and solve for the position
	 */
	@Test void triangulate_two() {
		createScene();

		var alg = new Triangulate2ViewsGeometricMetric();

		var found = new Point3D_F64();
		for (int i = 1; i < N; i++) {
			alg.triangulate(obsNorm.get(0), obsNorm.get(i), motionWorldToCamera.get(i), found);

			assertEquals(worldPoint.x, found.x, 1e-8);
			assertEquals(worldPoint.y, found.y, 1e-8);
			assertEquals(worldPoint.z, found.z, 1e-8);
		}
	}

	@Test void triangulate_two_homogenous() {
		createScene();

		var alg = new Triangulate2ViewsGeometricMetric();

		var found = new Point4D_F64();
		for (int i = 1; i < N; i++) {
			alg.triangulate(obsNorm.get(0), obsNorm.get(i), motionWorldToCamera.get(i), found);

			assertEquals(worldPoint.x, found.x/found.w, 1e-8);
			assertEquals(worldPoint.y, found.y/found.w, 1e-8);
			assertEquals(worldPoint.z, found.z/found.w, 1e-8);
		}
	}

	/**
	 * Create 2 perfect observations and solve for the position
	 */
	@Test void triangulate_two_pointing() {
		createScene();

		var alg = new Triangulate2ViewsGeometricMetric();

		var found = new Point4D_F64();
		for (int i = 1; i < N; i++) {
			Point2D_F64 na = obsNorm.get(0);
			Point2D_F64 nb = obsNorm.get(i);

			alg.triangulate(new Point3D_F64(na.x, na.y, 1), new Point3D_F64(nb.x, nb.y, 1), motionWorldToCamera.get(i), found);

			assertEquals(worldPoint.x, found.x/found.w, 1e-8);
			assertEquals(worldPoint.y, found.y/found.w, 1e-8);
			assertEquals(worldPoint.z, found.z/found.w, 1e-8);
		}
	}
}
