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

public class TestPointingToPixelError extends BoofStandardJUnit {
	// intrinsic camera parameters
	double fx = 60;
	double skew = 0.01;
	double fy = 80;
	double cx = 200;
	double cy = 150;

	DMatrixRMaj K = new DMatrixRMaj(3, 3, true, fx, skew, cx, 0, fy, cy, 0, 0, 1);
	Se3_F64 worldToCamera;

	// pointing vector coordinates
	Point3D_F64 pv0, pv1 = new Point3D_F64();
	// pixel coordinates
	Point2D_F64 pix0, pix1;

	public TestPointingToPixelError() {
		worldToCamera = new Se3_F64();
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.05, 0.05, -0.02, worldToCamera.R);
		worldToCamera.getT().setTo(0.3, -0.02, 0.05);

		var X = new Point3D_F64(0.1, -0.02, 3);

		pv0 = PerspectiveOps.renderPointing(worldToCamera, X, null);
		pix0 = PerspectiveOps.renderPixel(worldToCamera, K, X, null);

		pix1 = pix0.copy();
		pix1.x += 0.2;
		pix1.y += -0.3;

		Point2D_F64 norm = PerspectiveOps.convertPixelToNorm(K, pix1, null);
		pv1.setTo(norm.x, norm.y, 1);
		pv1.divideIP(pv1.norm());
	}

	@Test void usingPoints() {
		var alg = new PointingToPixelError(fx, fy, skew);

		double expected = pix0.distance2(pix1);
		double found = alg.errorSq(pv0, pv1);

		assertEquals(expected, found, 1e-8);
	}

	@Test void usingValues() {
		var alg = new PointingToPixelError(fx, fy, skew);

		double expected = pix0.distance2(pix1);
		double found = alg.errorSq(pv0.x, pv0.y, pv0.z, pv1.x, pv1.y, pv1.z);

		assertEquals(expected, found, 1e-8);
	}
}
