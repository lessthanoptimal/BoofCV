/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import georegression.struct.point.Point3D_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestTriangulateLinearDLT extends CommonTriangulationChecks {

	/**
	 * Create 3 perfect observations and solve for the position
	 */
	@Test
	public void triangulate_N() {
		createScene();

		TriangulateLinearDLT alg = new TriangulateLinearDLT();

		Point3D_F64 found = new Point3D_F64();

		alg.triangulate(obsPts, motionWorldToCamera,found);

		assertEquals(worldPoint.x,found.x,1e-8);
		assertEquals(worldPoint.y,found.y,1e-8);
		assertEquals(worldPoint.z,found.z,1e-8);
	}

	/**
	 * Create 2 perfect observations and solve for the position
	 */
	@Test
	public void triangulate_two() {
		createScene();

		TriangulateLinearDLT alg = new TriangulateLinearDLT();

		Point3D_F64 found = new Point3D_F64();
		alg.triangulate(obsPts.get(0),obsPts.get(1), motionWorldToCamera.get(1),found);

		assertEquals(worldPoint.x,found.x,1e-8);
		assertEquals(worldPoint.y,found.y,1e-8);
		assertEquals(worldPoint.z,found.z,1e-8);
	}
}
