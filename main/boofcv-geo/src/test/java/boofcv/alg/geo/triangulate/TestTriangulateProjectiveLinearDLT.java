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

import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestTriangulateProjectiveLinearDLT extends CommonTriangulationChecks {

	/**
	 * Create 3 perfect observations and solve for the position. Everything is in metric instead of an arbtirary
	 * projective frame for ease of testing.
	 */
	@Test void triangulate_metric_N() {
		createScene();

		TriangulateProjectiveLinearDLT alg = new TriangulateProjectiveLinearDLT();

		Point4D_F64 found = new Point4D_F64();

		alg.triangulate(obsPixels, cameraMatrices, found);

		found.x /= found.w;
		found.y /= found.w;
		found.z /= found.w;

		assertEquals(worldPoint.x, found.x, 1e-8);
		assertEquals(worldPoint.y, found.y, 1e-8);
		assertEquals(worldPoint.z, found.z, 1e-8);
	}

	/**
	 * Test case with a true projective situation
	 */
	@Test void triangulate_projective() {
		createScene();

		TriangulateProjectiveLinearDLT alg = new TriangulateProjectiveLinearDLT();

		Point4D_F64 foundX = new Point4D_F64();

		alg.triangulate(obsPixels, cameraMatrices, foundX);

		// project the found coordinate back on to each image
		Point2D_F64 foundPixel = new Point2D_F64();
		for (int i = 0; i < cameraMatrices.size(); i++) {
			DMatrixRMaj P = cameraMatrices.get(i);
			Point2D_F64 expected = obsPixels.get(i);

			GeometryMath_F64.mult(P, foundX, foundPixel);

			assertEquals(0, expected.distance(foundPixel), UtilEjml.TEST_F64);
		}
	}

	/**
	 * Add a tinny bit of noise and see if it blows up
	 */
	@Test void triangulate_projective_noise() {
		createScene();

		TriangulateProjectiveLinearDLT alg = new TriangulateProjectiveLinearDLT();

		Point4D_F64 foundX = new Point4D_F64();

		obsPixels.get(0).x += 0.01;
		obsPixels.get(0).y -= 0.01;

		alg.triangulate(obsPixels, cameraMatrices, foundX);

		// project the found coordinate back on to each image
		Point2D_F64 foundPixel = new Point2D_F64();
		for (int i = 0; i < cameraMatrices.size(); i++) {
			DMatrixRMaj P = cameraMatrices.get(i);
			Point2D_F64 expected = obsPixels.get(i);

			GeometryMath_F64.mult(P, foundX, foundPixel);

			assertEquals(0, expected.distance(foundPixel), 0.03);
		}
	}
}
