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
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestPositiveDepthConstraintCheckHPointing extends BoofStandardJUnit {
	/**
	 * Point a point in front of both cameras and see if it returns true
	 */
	@Test void positive() {
		// create transform from A to B
		DMatrixRMaj R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0, -0.05, 0, null);
		var T = new Vector3D_F64(1, 0, 0);
		var fromAtoB = new Se3_F64(R, T);

		// point in front of both cameras
		var pt = new Point3D_F64(0, 0, 2);

		// create observations of the point in calibrated coordinates
		Point3D_F64 obsA = new Point3D_F64(0, 0, 1);
		Point3D_F64 obsB = SePointOps_F64.transform(fromAtoB, pt, null);

		var alg = new PositiveDepthConstraintCheckHPointing();

		assertTrue(alg.checkConstraint(obsA, obsB, fromAtoB));
	}

	@Test void positive_NearlyPureRotation() {
		// create transform from A to B
		DMatrixRMaj R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0, -0.05, 0, null);
		Vector3D_F64 T = new Vector3D_F64(1e-4, 0, 0);
		Se3_F64 fromAtoB = new Se3_F64(R, T);

		// point in front of both cameras
		Point4D_F64 pt = new Point4D_F64(0, 0, 1e3, 1e-5);

		// create observations of the point in calibrated coordinates
		Point3D_F64 obsA = new Point3D_F64(0, 0, 1);
		Point4D_F64 pt_inB = SePointOps_F64.transform(fromAtoB, pt, (Point4D_F64)null);
		Point3D_F64 obsB = new Point3D_F64(pt_inB.x, pt_inB.y, pt_inB.z);

		var alg = new PositiveDepthConstraintCheckHPointing();

		assertTrue(alg.checkConstraint(obsA, obsB, fromAtoB));
	}

	/**
	 * Point a point in behind the cameras
	 */
	@Test void negative() {
		// create transform from A to B
		DMatrixRMaj R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0, -0.05, 0, null);
		Vector3D_F64 T = new Vector3D_F64(1, 0, 0);
		Se3_F64 fromAtoB = new Se3_F64(R, T);

		// point in front of both cameras
		Point3D_F64 pt = new Point3D_F64(0, 0, -1);

		// create observations of the point in calibrated coordinates
		Point3D_F64 obsA = new Point3D_F64(0, 0, 1);
		Point3D_F64 obsB = SePointOps_F64.transform(fromAtoB, pt, null);

		var alg = new PositiveDepthConstraintCheckHPointing();

		assertFalse(alg.checkConstraint(obsA, obsB, fromAtoB));
	}

	@Test void negative_NearlyPureRotation() {
		// create transform from A to B
		DMatrixRMaj R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0, -0.05, 0, null);
		Vector3D_F64 T = new Vector3D_F64(1e-4, 0, 0);
		Se3_F64 fromAtoB = new Se3_F64(R, T);

		// point in front of both cameras
		Point4D_F64 pt = new Point4D_F64(0, 0, -1e3, 1e-5);

		// create observations of the point in calibrated coordinates
		Point3D_F64 obsA = new Point3D_F64(0, 0, 1);
		Point4D_F64 pt_inB = SePointOps_F64.transform(fromAtoB, pt, (Point4D_F64)null);
		Point3D_F64 obsB = new Point3D_F64(pt_inB.x, pt_inB.y, pt_inB.z);

		var alg = new PositiveDepthConstraintCheckHPointing();

		assertFalse(alg.checkConstraint(obsA, obsB, fromAtoB));
	}
}
