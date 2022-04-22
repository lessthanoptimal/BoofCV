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

package boofcv.alg.geo;

import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestNormalizedToPinholePixelError extends BoofStandardJUnit {

	// intrinsic camera parameters
	double fx = 60;
	double skew = 0.01;
	double fy = 80;

	DMatrixRMaj K = new DMatrixRMaj(3, 3, true, fx, skew, 200, 0, fy, 150, 0, 0, 1);
	Se3_F64 worldToCamera;

	// normalized image coordinates
	Point2D_F64 n0, n1;
	// pixel coordinates
	Point2D_F64 p0, p1;

	public TestNormalizedToPinholePixelError() {
		worldToCamera = new Se3_F64();
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.05, 0.05, -0.02, worldToCamera.R);
		worldToCamera.getT().setTo(0.3, -0.02, 0.05);

		var X = new Point3D_F64(0.1, -0.02, 3);

		n0 = PerspectiveOps.renderPixel(worldToCamera, X, null);
		p0 = PerspectiveOps.renderPixel(worldToCamera, K, X, null);

		p1 = p0.copy();
		p1.x += 0.2;
		p1.y += -0.3;

		n1 = PerspectiveOps.convertPixelToNorm(K, p1, null);
	}

	@Test void usingPixels() {
		var alg = new NormalizedToPinholePixelError(fx, fy, skew);

		double expected = p0.distance2(p1);
		double found = alg.errorSq(n0, n1);

		assertEquals(expected, found, 1e-8);
	}

	@Test void usingDoubles() {
		var alg = new NormalizedToPinholePixelError(fx, fy, skew);

		double expected = p0.distance2(p1);
		double found = alg.errorSq(n0.x, n0.y, n1.x, n1.y);

		assertEquals(expected, found, 1e-8);
	}
}
