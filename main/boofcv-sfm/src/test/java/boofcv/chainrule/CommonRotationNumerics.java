/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.chainrule;

import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import org.ddogleg.struct.DogArray;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public abstract class CommonRotationNumerics extends BoofStandardJUnit {
	// equality tolerance
	protected double tolerance = 1e-6;

	// delta for numerical derivatives
	protected double deltaT = 1e-6;

	protected abstract RotationNumerics createAlg();

	protected abstract List<double[]> createParams();

	protected abstract DMatrixRMaj paramToMatrix( double[] params );

	/// Fills in the array with a valid random parameterized rotation. Whether the parameters are
	/// normalized is up to the implementation, since some define the rotation by direction alone
	protected abstract void randomParams( double[] params, int offset );

	@Test void set_get() {
		var input = new double[]{1, 2, 3, 4, 5, 6};
		var output = new double[10];

		int offset = 3;
		RotationNumerics alg = createAlg();
		alg.set(input, 1);
		alg.get(output, offset);

		for (int i = 0; i < alg.length(); i++) {
			assertEquals(input[i + 1], output[i + offset], tolerance);
		}
	}

	@Test void identity() {
		var input = new double[]{1, 2, 3, 4, 5, 6, 7, 8};
		int offset = 3;
		RotationNumerics alg = createAlg();
		alg.identity(input, offset);
		alg.set(input, offset);

		var R = new DMatrixRMaj(3, 3);
		alg.toMatrix(R);
		assertTrue(MatrixFeatures_DDRM.isIdentity(R, UtilEjml.TEST_F64));
	}

	@Test void toMatrix() {
		RotationNumerics alg = createAlg();
		for (var param : createParams()) {
			alg.set(param, 0);

			var found = new DMatrixRMaj(3, 3);

			// Test by comparing against the code in GeoRegression
			alg.toMatrix(found);

			assertTrue(MatrixFeatures_DDRM.isEquals(paramToMatrix(param), found, tolerance));
		}
	}

	@Test void matrixJacobians() {
		RotationNumerics alg = createAlg();
		for (var param : createParams()) {
			alg.set(param, 0);
			checkMatrixNumerical(param, alg);
		}

		// Check a pathological case
		var identity = new double[alg.length()];
		alg.identity(identity, 0);
		alg.set(identity, 0);
		checkMatrixNumerical(identity, alg);
	}

	/// Test the correctness of the residual calculation by applying its definition
	@Test void residual_definition() {
		RotationNumerics alg = createAlg();

		var A = new DMatrixRMaj(3, 3);
		var B = new DMatrixRMaj(3, 3);
		var foundB = new DMatrixRMaj(3, 3);
		var paramA = new double[alg.length()];
		var paramB = new double[alg.length()];
		var residual = new double[alg.residualLength()];

		for (int trial = 0; trial < 20; trial++) {
			randomParams(paramA, 0);
			randomParams(paramB, 0);

			alg.set(paramA, 0);
			alg.residual(paramB, 0, residual, 0);

			// NOTE: We assume residual is axis angle, which it isn't always true
			alg.toMatrix(A);
			alg.set(paramB, 0);
			alg.toMatrix(B);

			DMatrixRMaj AtB = ConvertRotation3D_F64.rodriguesToMatrix(residual[0], residual[1], residual[2], null);
			CommonOps_DDRM.mult(A, AtB, foundB);
			assertTrue(MatrixFeatures_DDRM.isEquals(B, foundB, deltaT));
		}
	}

	@Test void concatAB_concatBA() {
		RotationNumerics alg = createAlg();

		var paramA = new double[alg.length()];
		var paramB = new double[alg.length()];

		for (int trial = 0; trial < 4; trial++) {
			// Create two valid random parameterized rotations
			randomParams(paramA, 0);
			randomParams(paramB, 0);

			// Create the rotation matrices that will be used to validate the test
			var rotA = new DMatrixRMaj(3, 3);
			var rotB = new DMatrixRMaj(3, 3);
			alg.set(paramB, 0);
			alg.toMatrix(rotB);
			alg.set(paramA, 0);
			alg.toMatrix(rotA);

			// Perform the function being tested
			var paramC = new double[alg.length()];
			alg.concatAB(paramB, 0, paramC, 0);

			// compute what it should be according to the documentation
			var rotAB = new DMatrixRMaj(3, 3);
			CommonOps_DDRM.mult(rotA, rotB, rotAB);

			var found = new DMatrixRMaj(3, 3);
			alg.set(paramC, 0);
			alg.toMatrix(found);
			assertTrue(MatrixFeatures_DDRM.isEquals(rotAB, found, 1e-6));

			// Test concatBA now
			var rotBA = new DMatrixRMaj(3, 3);
			CommonOps_DDRM.mult(rotB, rotA, rotBA);
			alg.set(paramA, 0);
			alg.concatBA(paramB, 0, paramC, 0);
			alg.set(paramC, 0);
			alg.toMatrix(found);
			assertTrue(MatrixFeatures_DDRM.isEquals(rotBA, found, 1e-6));

			// Sanity check
			assertFalse(MatrixFeatures_DDRM.isEquals(rotAB, rotBA, 1e-6));
		}
	}

	@Test void concatABJacobian() {
		RotationNumerics alg = createAlg();
		int N = alg.length();
		var paramsA = new double[N];
		var paramsB = new double[N];

		for (int trial = 0; trial < 10; trial++) {
			randomParams(paramsA, 0);
			randomParams(paramsB, 0);

			alg.set(paramsA, 0);
			var foundA = new DMatrixRMaj(N, N);
			var foundB = new DMatrixRMaj(N, N);
			alg.concatABJacobian(paramsB, 0, foundA, foundB);

			checkConcatABJacobianA(paramsA, paramsB, alg, foundA);
			checkConcatABJacobianB(paramsA, paramsB, alg, foundB);
		}
	}

	@Test void concatBAJacobian() {
		RotationNumerics alg = createAlg();
		int N = alg.length();
		var paramsA = new double[N];
		var paramsB = new double[N];

		for (int trial = 0; trial < 10; trial++) {
			randomParams(paramsA, 0);
			randomParams(paramsB, 0);

			alg.set(paramsA, 0);
			var foundA = new DMatrixRMaj(N, N);
			var foundB = new DMatrixRMaj(N, N);
			alg.concatBAJacobian(paramsB, 0, foundA, foundB);

			checkConcatBAJacobianA(paramsA, paramsB, alg, foundA);
			checkConcatBAJacobianB(paramsA, paramsB, alg, foundB);
		}
	}

	/// 'B' is read at an offset so that handling it incorrectly is caught
	@Test void residualJacobian() {
		RotationNumerics alg = createAlg();
		int N = alg.length();
		var paramsA = new double[N];
		var paramsB = new double[N + 1];

		for (int trial = 0; trial < 10; trial++) {
			randomParams(paramsA, 0);
			randomParams(paramsB, 1);

			var foundA = new DMatrixRMaj(alg.residualLength(), N);
			var foundB = new DMatrixRMaj(alg.residualLength(), N);
			alg.set(paramsA, 0);
			alg.residualJacobian(paramsB, 1, foundA, foundB);

			residualJacobian_JacA(paramsA, 0, paramsB, 1, alg, foundA);
			residualJacobian_JacB(paramsA, 0, paramsB, 1, alg, foundB);
		}
	}

	@Test void angleErrorJacobian() {
		RotationNumerics alg = createAlg();
		var paramsA = new double[alg.length()];
		var paramsB = new double[alg.length()];

		for (int trial = 0; trial < 10; trial++) {
			randomParams(paramsA, 0);
			randomParams(paramsB, 0);

			checkAngleErrorJacobian(paramsA, paramsB, alg);
		}
	}

	@Test void invert() {
		RotationNumerics alg = createAlg();

		var R = new DMatrixRMaj(3, 3);
		var param = new double[alg.length()];

		// Test against an identity rotation, which should do nothing
		alg.identity(param, 0);
		alg.invert(param, 0, param, 0);
		alg.set(param, 0);
		alg.toMatrix(R);
		assertTrue(MatrixFeatures_DDRM.isIdentity(R, UtilEjml.TEST_F64));

		// Now a few random matrices
		var T = new DMatrixRMaj(3, 3);
		var found = new DMatrixRMaj(3, 3);
		for (int i = 0; i < 50; i++) {
			randomParams(param, 0);

			// Get the rotation matrix
			alg.set(param, 0);
			alg.toMatrix(R);

			// Invert and get matrix
			alg.invert(param, 0, param, 0);
			alg.set(param, 0);
			alg.toMatrix(T);

			// R*R' = I
			CommonOps_DDRM.mult(R, T, found);
			assertTrue(MatrixFeatures_DDRM.isIdentity(found, UtilEjml.TEST_F64));
		}
	}

	@Test void invertJacobian() {
		RotationNumerics alg = createAlg();
		var jac = new DMatrixRMaj(3, 3);
		var param = new double[alg.length()];

		// Test identity first
		alg.identity(param, 0);
		alg.invertJacobian(param, 0, jac);
		invertJacobian(alg, param, jac);

		// Random matrices
		for (int i = 0; i < 50; i++) {
			randomParams(param, 0);

			alg.invertJacobian(param, 0, jac);
			invertJacobian(alg, param, jac);
		}
	}

	/// Verifies that normalize uses the passed in parameters and not the internal state
	@Test void normalizeUsesParam() {
		RotationNumerics alg = createAlg();
		var paramA = new double[alg.length()];
		var paramB = new double[alg.length()];

		randomParams(paramA, 0);
		randomParams(paramB, 0);

		// What the matrix should be from paramA
		var expectedA = new DMatrixRMaj(3, 3);
		alg.set(paramA, 0);
		alg.toMatrix(expectedA);

		// What the matrix should be from paramB
		var expectedB = new DMatrixRMaj(3, 3);
		alg.set(paramB, 0);
		alg.toMatrix(expectedB);

		// Change the internal state
		alg.set(paramA, 0);
		alg.normalizeParams(paramB, 0);

		// Now see if it modified paramB or if it still has the same rotation
		var found = new DMatrixRMaj(3, 3);
		alg.set(paramB, 0);
		alg.toMatrix(found);
		assertTrue(MatrixFeatures_DDRM.isEquals(expectedB, found, 1e-7));
	}

	void invertJacobian( RotationNumerics alg, double[] param, DMatrixRMaj found ) {
		int N = alg.length();
		var resultM = new double[N];
		var resultP = new double[N];

		var expected = new DMatrixRMaj(N, N);
		for (int idxIn = 0; idxIn < param.length; idxIn++) {
			double value = param[idxIn];
			param[idxIn] = value - deltaT;
			alg.invert(param, 0, resultM, 0);

			param[idxIn] = value + deltaT;
			alg.invert(param, 0, resultP, 0);
			param[idxIn] = value;

			for (int idxN = 0; idxN < N; idxN++) {
				double diff = resultP[idxN] - resultM[idxN];
				expected.set(idxN, idxIn, diff/(2*deltaT));
			}
		}

		assertTrue(MatrixFeatures_DDRM.isEquals(expected, found, tolerance));
	}

	/** Use numerical differentiation to check the Jacobian */
	protected void checkMatrixNumerical( double[] param, RotationNumerics alg ) {
		var outputP = new DMatrixRMaj(3, 3);
		var outputM = new DMatrixRMaj(3, 3);

		var expected = new DogArray<>(() -> new DMatrixRMaj(3, 3));
		for (int idxIn = 0; idxIn < param.length; idxIn++) {
			double value = param[idxIn];
			param[idxIn] = value - deltaT;
			alg.set(param, 0);
			alg.toMatrix(outputM);
			param[idxIn] = value + deltaT;
			alg.set(param, 0);
			alg.toMatrix(outputP);
			param[idxIn] = value;

			DMatrixRMaj m = expected.grow();
			for (int row = 0; row < 3; row++) {
				for (int col = 0; col < 3; col++) {
					double diff = outputP.get(row, col) - outputM.get(row, col);
					m.set(row, col, diff/(2*deltaT));
				}
			}
		}

		var found = new DogArray<>(() -> new DMatrixRMaj(3, 3));
		alg.set(param, 0);
		alg.matrixJacobians(found);

		assertEquals(expected.size, found.size);
		for (int i = 0; i < expected.size; i++) {
			assertTrue(MatrixFeatures_DDRM.isEquals(expected.get(i), found.get(i), 5*tolerance));
		}
	}

	/** Use numerical differentiation to check the Jacobian */
	protected void checkAngleErrorJacobian( double[] paramsA, double[] paramsB, RotationNumerics alg ) {
		int N = alg.length();
		var expectedA = new DMatrixRMaj(1, N);
		var expectedB = new DMatrixRMaj(1, N);

		for (int i = 0; i < N; i++) {
			double value = paramsA[i];
			paramsA[i] = value - deltaT;
			alg.set(paramsA, 0);
			double errorM = alg.angleError(paramsB);
			paramsA[i] = value + deltaT;
			alg.set(paramsA, 0);
			double errorP = alg.angleError(paramsB);
			paramsA[i] = value;

			expectedA.set(0, i, (errorP - errorM)/(2*deltaT));
		}

		alg.set(paramsA, 0);
		for (int i = 0; i < N; i++) {
			double value = paramsB[i];
			paramsB[i] = value - deltaT;
			double errorM = alg.angleError(paramsB);
			paramsB[i] = value + deltaT;
			double errorP = alg.angleError(paramsB);
			paramsB[i] = value;

			expectedB.set(0, i, (errorP - errorM)/(2*deltaT));
		}

		var foundA = new DMatrixRMaj(1, N);
		var foundB = new DMatrixRMaj(1, N);
		alg.angleErrorJacobian(paramsB, foundA, foundB);

		assertTrue(MatrixFeatures_DDRM.isEquals(expectedA, foundA, tolerance));
		assertTrue(MatrixFeatures_DDRM.isEquals(expectedB, foundB, tolerance));
	}

	/** Use numerical differentiation to check the Jacobian */
	protected void checkConcatABJacobianA( double[] paramA, double[] paramB, RotationNumerics alg, DMatrixRMaj foundA ) {
		int N = alg.length();
		var resultM = new double[N];
		var resultP = new double[N];

		var expected = new DMatrixRMaj(N, N);
		for (int idxIn = 0; idxIn < paramA.length; idxIn++) {
			double value = paramA[idxIn];
			paramA[idxIn] = value - deltaT;
			alg.set(paramA, 0);
			alg.concatAB(paramB, 0, resultM, 0);
			paramA[idxIn] = value + deltaT;
			alg.set(paramA, 0);
			alg.concatAB(paramB, 0, resultP, 0);
			paramA[idxIn] = value;

			for (int idxN = 0; idxN < N; idxN++) {
				double diff = resultP[idxN] - resultM[idxN];
				expected.set(idxN, idxIn, diff/(2*deltaT));
			}
		}

		assertTrue(MatrixFeatures_DDRM.isEquals(expected, foundA, tolerance));
	}

	/** Use numerical differentiation to check the Jacobian */
	protected void checkConcatABJacobianB( double[] paramA, double[] paramB, RotationNumerics alg, DMatrixRMaj foundB ) {
		int N = alg.length();
		var resultM = new double[N];
		var resultP = new double[N];

		alg.set(paramA, 0);
		var expected = new DMatrixRMaj(N, N);
		for (int idxIn = 0; idxIn < paramB.length; idxIn++) {
			double value = paramB[idxIn];
			paramB[idxIn] = value - deltaT;
			alg.concatAB(paramB, 0, resultM, 0);
			paramB[idxIn] = value + deltaT;
			alg.concatAB(paramB, 0, resultP, 0);
			paramB[idxIn] = value;

			for (int idxN = 0; idxN < N; idxN++) {
				double diff = resultP[idxN] - resultM[idxN];
				expected.set(idxN, idxIn, diff/(2*deltaT));
			}
		}

		assertTrue(MatrixFeatures_DDRM.isEquals(expected, foundB, tolerance));
	}

	/** Use numerical differentiation to check the Jacobian */
	protected void checkConcatBAJacobianA( double[] paramA, double[] paramB, RotationNumerics alg, DMatrixRMaj foundA ) {
		int N = alg.length();
		var resultM = new double[N];
		var resultP = new double[N];

		var expected = new DMatrixRMaj(N, N);
		for (int idxIn = 0; idxIn < paramA.length; idxIn++) {
			double value = paramA[idxIn];
			paramA[idxIn] = value - deltaT;
			alg.set(paramA, 0);
			alg.concatBA(paramB, 0, resultM, 0);
			paramA[idxIn] = value + deltaT;
			alg.set(paramA, 0);
			alg.concatBA(paramB, 0, resultP, 0);
			paramA[idxIn] = value;

			for (int idxN = 0; idxN < N; idxN++) {
				double diff = resultP[idxN] - resultM[idxN];
				expected.set(idxN, idxIn, diff/(2*deltaT));
			}
		}

		assertTrue(MatrixFeatures_DDRM.isEquals(expected, foundA, tolerance));
	}

	/** Use numerical differentiation to check the Jacobian */
	protected void checkConcatBAJacobianB( double[] paramA, double[] paramB, RotationNumerics alg, DMatrixRMaj foundB ) {
		int N = alg.length();
		var resultM = new double[N];
		var resultP = new double[N];

		alg.set(paramA, 0);
		var expected = new DMatrixRMaj(N, N);
		for (int idxIn = 0; idxIn < paramB.length; idxIn++) {
			double value = paramB[idxIn];
			paramB[idxIn] = value - deltaT;
			alg.concatBA(paramB, 0, resultM, 0);
			paramB[idxIn] = value + deltaT;
			alg.concatBA(paramB, 0, resultP, 0);
			paramB[idxIn] = value;

			for (int idxN = 0; idxN < N; idxN++) {
				double diff = resultP[idxN] - resultM[idxN];
				expected.set(idxN, idxIn, diff/(2*deltaT));
			}
		}

		assertTrue(MatrixFeatures_DDRM.isEquals(expected, foundB, tolerance));
	}

	/** Use numerical differentiation to check the Jacobian */
	protected void residualJacobian_JacA( double[] paramA, int offsetA,
										  double[] paramB, int offsetB, RotationNumerics alg, DMatrixRMaj foundA ) {
		int N = alg.residualLength();
		var resultM = new double[N];
		var resultP = new double[N];

		var expected = new DMatrixRMaj(N, alg.length());
		int endA = offsetA + alg.length();
		for (int idxIn = offsetA; idxIn < endA; idxIn++) {
			double value = paramA[idxIn];
			paramA[idxIn] = value - deltaT;
			alg.set(paramA, offsetA);
			alg.residual(paramB, offsetB, resultM, 0);
			paramA[idxIn] = value + deltaT;
			alg.set(paramA, offsetA);
			alg.residual(paramB, offsetB, resultP, 0);
			paramA[idxIn] = value;

			for (int idxN = 0; idxN < N; idxN++) {
				double diff = resultP[idxN] - resultM[idxN];
				expected.set(idxN, idxIn - offsetA, diff/(2*deltaT));
			}
		}

		assertTrue(MatrixFeatures_DDRM.isEquals(expected, foundA, tolerance));
	}

	protected void residualJacobian_JacB( double[] paramA, int offsetA,
										  double[] paramB, int offsetB, RotationNumerics alg, DMatrixRMaj foundB ) {
		int N = alg.residualLength();
		var resultM = new double[N];
		var resultP = new double[N];

		alg.set(paramA, offsetA);

		var expected = new DMatrixRMaj(N, alg.length());
		int endB = offsetB + alg.length();
		for (int idxIn = offsetB; idxIn < endB; idxIn++) {
			double value = paramB[idxIn];
			paramB[idxIn] = value - deltaT;
			alg.residual(paramB, offsetB, resultM, 0);

			paramB[idxIn] = value + deltaT;
			alg.residual(paramB, offsetB, resultP, 0);

			paramB[idxIn] = value;

			for (int idxN = 0; idxN < N; idxN++) {
				double diff = resultP[idxN] - resultM[idxN];
				expected.set(idxN, idxIn - offsetB, diff/(2*deltaT));
			}
		}

		assertTrue(MatrixFeatures_DDRM.isEquals(expected, foundB, tolerance));
	}
}
