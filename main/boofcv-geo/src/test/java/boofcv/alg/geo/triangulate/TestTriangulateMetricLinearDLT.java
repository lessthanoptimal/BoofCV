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

package boofcv.alg.geo.triangulate;

import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTriangulateMetricLinearDLT extends CommonTriangulationChecks {

	/**
	 * Create 3 perfect observations and solve for the position
	 */
	@Test void triangulate_N() {
		createScene();

		var alg = new TriangulateMetricLinearDLT();
		var found = new Point4D_F64();

		alg.triangulate(obsNorm, motionWorldToCamera, found);

		assertEquals(worldPoint.x, found.x/found.w, UtilEjml.TEST_F64);
		assertEquals(worldPoint.y, found.y/found.w, UtilEjml.TEST_F64);
		assertEquals(worldPoint.z, found.z/found.w, UtilEjml.TEST_F64);
	}

	/**
	 * Create 2 perfect observations and solve for the position
	 */
	@Test void triangulate_two() {
		createScene();

		var alg = new TriangulateMetricLinearDLT();
		var found = new Point4D_F64();

		alg.triangulate(obsNorm.get(0), obsNorm.get(1), motionWorldToCamera.get(1), found);

		assertEquals(worldPoint.x, found.x/found.w, UtilEjml.TEST_F64);
		assertEquals(worldPoint.y, found.y/found.w, UtilEjml.TEST_F64);
		assertEquals(worldPoint.z, found.z/found.w, UtilEjml.TEST_F64);
	}

	/**
	 * Create 3 perfect pointing vector observations and solve for the position
	 */
	@Test void triangulate_N_pointing() {
		createScene();

		var alg = new TriangulateMetricLinearDLT();
		var found = new Point4D_F64();

		var list = new ArrayList<Point3D_F64>();
		for (Point2D_F64 n : obsNorm) {
			list.add(normTo3D(n));
		}

		alg.triangulateP(list, motionWorldToCamera, found);

		assertEquals(worldPoint.x, found.x/found.w, UtilEjml.TEST_F64);
		assertEquals(worldPoint.y, found.y/found.w, UtilEjml.TEST_F64);
		assertEquals(worldPoint.z, found.z/found.w, UtilEjml.TEST_F64);
	}

	/**
	 * Create 2 perfect observations and solve for the position
	 */
	@Test void triangulate_two_pointing() {
		createScene();

		var alg = new TriangulateMetricLinearDLT();
		var found = new Point4D_F64();

		alg.triangulateP(normTo3D(obsNorm.get(0)), normTo3D(obsNorm.get(1)), motionWorldToCamera.get(1), found);

		assertEquals(worldPoint.x, found.x/found.w, UtilEjml.TEST_F64);
		assertEquals(worldPoint.y, found.y/found.w, UtilEjml.TEST_F64);
		assertEquals(worldPoint.z, found.z/found.w, UtilEjml.TEST_F64);
	}
}
