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

package boofcv.alg.geo.selfcalib;

import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrix3;
import org.ejml.data.DMatrix3x3;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.fixed.CommonOps_DDF3;
import org.ejml.dense.fixed.NormOps_DDF3;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestEstimatePlaneAtInfinityGivenK extends BoofStandardJUnit {
	/**
	 * Give it perfect noise free inputs
	 */
	@Test void perfect() {
		// Construct metric camera matrices
		DMatrixRMaj K1 = PerspectiveOps.pinholeToMatrix(300, 310, 0.01, 400, 390);
		DMatrixRMaj K2 = PerspectiveOps.pinholeToMatrix(280, 340, -0.03, 410, 420);

		Se3_F64 view0_to_view0 = new Se3_F64();
		Se3_F64 view0_to_view1 = SpecialEuclideanOps_F64.eulerXyz(1, 0.1, -0.6, 0.1, -0.05, -0.2, null);

		DMatrixRMaj P1a = PerspectiveOps.createCameraMatrix(view0_to_view0.R, view0_to_view0.T, K1, null);
		DMatrixRMaj P2a = PerspectiveOps.createCameraMatrix(view0_to_view1.R, view0_to_view1.T, K2, null);

		// Conver it into the canonical projection matrices with P1 = [I|0]
		DMatrixRMaj H = new DMatrixRMaj(4, 4);
		MultiViewOps.projectiveToIdentityH(P1a, H);

		DMatrixRMaj P1 = new DMatrixRMaj(3, 4);
		DMatrixRMaj P2 = new DMatrixRMaj(3, 4);
		CommonOps_DDRM.mult(P1a, H, P1);
		CommonOps_DDRM.mult(P2a, H, P2);

		// Run the algorithm and get the plane at infinity
		EstimatePlaneAtInfinityGivenK alg = new EstimatePlaneAtInfinityGivenK();
		alg.setCamera1(300, 310, 0.01, 400, 390);
		alg.setCamera2(280, 340, -0.03, 410, 420);

		Vector3D_F64 v = new Vector3D_F64();
		assertTrue(alg.estimatePlaneAtInfinity(P2, v));

		// Extract the metric camera matrices using the results. Scale is arbitrarily set to 1
		H = MultiViewOps.createProjectiveToMetric(K1, v.x, v.y, v.z, 1, null);

		DMatrixRMaj P1b = new DMatrixRMaj(3, 4);
		DMatrixRMaj P2b = new DMatrixRMaj(3, 4);

		CommonOps_DDRM.mult(P1, H, P1b);
		CommonOps_DDRM.mult(P2, H, P2b);

		assertTrue(MatrixFeatures_DDRM.isIdentical(P1a, P1b, UtilEjml.TEST_F64));
		assertTrue(MatrixFeatures_DDRM.isIdentical(P2a, P2b, UtilEjml.TEST_F64));
	}

	@Test void computeRotation() {
		DMatrix3 t = new DMatrix3(2, 0.5, 1.2);

		DMatrix3x3 R = new DMatrix3x3();

		EstimatePlaneAtInfinityGivenK.computeRotation(t, R);

		double n = NormOps_DDF3.normF(t);
		assertEquals(n, t.a1, UtilEjml.TEST_F64);
		assertEquals(0, t.a2, UtilEjml.TEST_F64);
		assertEquals(0, t.a3, UtilEjml.TEST_F64);

		DMatrix3 ta = new DMatrix3(2, 0.5, 1.2);
		CommonOps_DDF3.mult(R, ta, t);
		assertEquals(n, t.a1, UtilEjml.TEST_F64);
		assertEquals(0, t.a2, UtilEjml.TEST_F64);
		assertEquals(0, t.a3, UtilEjml.TEST_F64);
	}
}
