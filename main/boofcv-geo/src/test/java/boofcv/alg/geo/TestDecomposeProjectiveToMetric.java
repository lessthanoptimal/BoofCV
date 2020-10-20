/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.equation.Equation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestDecomposeProjectiveToMetric extends BoofStandardJUnit {

	@Test
	void decomposeMetricCamera() {
		var alg = new DecomposeProjectiveToMetric();

		// compute an arbitrary projection matrix from known values
		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(200, 250, 0.0, 100, 110);

		// try a bunch of different matrices to try to exercise all possible options
		for (int i = 0; i < 50; i++) {
			Se3_F64 worldToView = SpecialEuclideanOps_F64.eulerXyz(
					rand.nextGaussian(),rand.nextGaussian(),rand.nextGaussian(),
					rand.nextGaussian(),rand.nextGaussian(),rand.nextGaussian(),null);

			DMatrixRMaj P = PerspectiveOps.createCameraMatrix(worldToView.R,worldToView.T,K,null);

			// The camera matrix is often only recovered up to a scale factor
			CommonOps_DDRM.scale(rand.nextGaussian(),P);

			// decompose the projection matrix
			DMatrixRMaj foundK = new DMatrixRMaj(3,3);
			Se3_F64 foundWorldToView = new Se3_F64();
			assertTrue(alg.decomposeMetricCamera(P, foundK, foundWorldToView));

			// When you recombine everything it should produce the same camera matrix
			var foundP = PerspectiveOps.createCameraMatrix(foundWorldToView.R,foundWorldToView.T,foundK,null);
			double scale = MultiViewOps.findScale(foundP,P);
			CommonOps_DDRM.scale(scale,foundP);
			assertTrue(MatrixFeatures_DDRM.isIdentical(foundP,P, UtilEjml.TEST_F64));

			// see if it extract the input
			assertEquals(1,CommonOps_DDRM.det(foundWorldToView.R), UtilEjml.TEST_F64);
			assertTrue(MatrixFeatures_DDRM.isIdentical(K,foundK, UtilEjml.TEST_F64));
			assertTrue(MatrixFeatures_DDRM.isIdentical(worldToView.R,foundWorldToView.R, UtilEjml.TEST_F64));

			// make sure it didn't change the scale of the decomposed T
			// this is very important when decomposing cameras which had a common projective frame
			assertEquals(0.0,worldToView.T.distance(foundWorldToView.T),UtilEjml.TEST_F64);
		}
	}

	@Test
	void projectiveToMetric() {
		var alg = new DecomposeProjectiveToMetric();

		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(200, 250, 0.0, 100, 110);
		DMatrixRMaj foundK = new DMatrixRMaj(3,3);
		for (int i = 0; i < 50; i++) {
			double Tx = rand.nextGaussian();
			double Ty = rand.nextGaussian();
			double Tz = rand.nextGaussian();
			double rotX = rand.nextGaussian();
			double rotY = rand.nextGaussian();
			double rotZ = rand.nextGaussian();

			Se3_F64 m = SpecialEuclideanOps_F64.eulerXyz(Tx,Ty,Tz,rotX,rotY,rotZ,null);

			DMatrixRMaj P = PerspectiveOps.createCameraMatrix(m.R,m.T,K,null);
			CommonOps_DDRM.scale(0.9,P,P); // mess up the scale of P

			Equation eq = new Equation(P,"P",K,"K");
			eq.process("p=[-0.9,0.1,0.7]'").
					process("H=[K zeros(3,1);-p'*K 1]").
					process("P=P*H").process("H_inv=inv(H)");

			DMatrixRMaj H_inv = eq.lookupDDRM("H_inv");

			Se3_F64 found = new Se3_F64();

			assertTrue(alg.projectiveToMetric(P,H_inv,found,foundK));

			assertTrue(MatrixFeatures_DDRM.isEquals(K,foundK, UtilEjml.TEST_F64));
			assertEquals(0,m.T.distance(found.T), UtilEjml.TEST_F64);
		}
	}

	@Test
	void projectiveToMetricKnownK() {
		var alg = new DecomposeProjectiveToMetric();

		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(200, 250, 0.0, 100, 110);
		for (int i = 0; i < 50; i++) {
			double Tx = rand.nextGaussian();
			double Ty = rand.nextGaussian();
			double Tz = rand.nextGaussian();
			double rotX = rand.nextGaussian();
			double rotY = rand.nextGaussian();
			double rotZ = rand.nextGaussian();

			Se3_F64 m = SpecialEuclideanOps_F64.eulerXyz(Tx,Ty,Tz,rotX,rotY,rotZ,null);

			DMatrixRMaj P = PerspectiveOps.createCameraMatrix(m.R,m.T,K,null);
			CommonOps_DDRM.scale(0.9,P,P); // mess up the scale of P

			Equation eq = new Equation(P,"P",K,"K");
			eq.process("p=[-0.9,0.1,0.7]'").
					process("H=[K zeros(3,1);-p'*K 1]").
					process("P=P*H").process("H_inv=inv(H)");

			DMatrixRMaj H_inv = eq.lookupDDRM("H_inv");

			Se3_F64 found = new Se3_F64();

			assertTrue(alg.projectiveToMetricKnownK(P,H_inv,K,found));

			assertEquals(0,m.T.distance(found.T), UtilEjml.TEST_F64);
		}
	}

}
