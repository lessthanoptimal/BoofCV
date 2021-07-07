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

package boofcv.alg.geo.triangulate;

import georegression.struct.point.Point4D_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestTriangulateMetricLinearDLT extends CommonTriangulationChecks {

	/**
	 * Create 3 perfect observations and solve for the position
	 */
	@Test void triangulate_N() {
		createScene();

		TriangulateMetricLinearDLT alg = new TriangulateMetricLinearDLT();

		Point4D_F64 found = new Point4D_F64();

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

		TriangulateMetricLinearDLT alg = new TriangulateMetricLinearDLT();

		Point4D_F64 found = new Point4D_F64();

		alg.triangulate(obsNorm.get(0), obsNorm.get(1), motionWorldToCamera.get(1), found);

		assertEquals(worldPoint.x, found.x/found.w, UtilEjml.TEST_F64);
		assertEquals(worldPoint.y, found.y/found.w, UtilEjml.TEST_F64);
		assertEquals(worldPoint.z, found.z/found.w, UtilEjml.TEST_F64);
	}
}
