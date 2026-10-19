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

import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.so.Quaternion_F64;
import georegression.struct.so.Rodrigues_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.NormOps_DDRM;
import org.junit.jupiter.api.Test;

import java.util.List;

import static georegression.geometry.ConvertRotation3D_F64.eulerToQuaternion;
import static georegression.geometry.ConvertRotation3D_F64.rodriguesToQuaternion;
import static org.junit.jupiter.api.Assertions.*;

class TestQuaternionNumericsHam extends CommonRotationNumerics {

	@Override protected RotationNumerics createAlg() {
		return new QuaternionNumericsHam();
	}

	@Override protected List<double[]> createParams() {
		var quat = new Quaternion_F64(0.1, 0.2, 0.3, 0.4);
		quat.normalize();

		return List.of(new double[]{quat.w, quat.x, quat.y, quat.z});
	}

	@Override protected DMatrixRMaj paramToMatrix( double[] params ) {
		var q = new Quaternion_F64(params[0], params[1], params[2], params[3]);
		return ConvertRotation3D_F64.quaternionToMatrix(q, null);
	}

	/// Parameters must be unit length for this implementation
	@Override protected void randomParams( double[] params, int offset ) {
		for (int i = 0; i < 4; i++) {
			params[offset + i] = uniform();
		}
		createAlg().normalizeParams(params, offset);
	}

	@Test void length() {
		assertEquals(4, new QuaternionNumericsHam().length());
	}

	@Test void eulerXyzToParam() {
		// Euler angles
		double x = -1.2, y = -0.6, z = 2.3;
		var expected = new Quaternion_F64();

		eulerToQuaternion(EulerType.XYZ, x, y, z, expected);

		var found = new double[4];
		new QuaternionNumericsHam().eulerXyzToParam(x, y, z, found);

		assertEquals(expected.w, found[0], 1e-6);
		assertEquals(expected.x, found[1], 1e-6);
		assertEquals(expected.y, found[2], 1e-6);
		assertEquals(expected.z, found[3], 1e-6);
	}

	@Test void rodriguesToParam() {
		// Euler angles
		double x = -1.2, y = -0.6, z = 2.3;
		var rod = new Rodrigues_F64(x, y, z);
		var expected = new Quaternion_F64();

		rodriguesToQuaternion(rod, expected);

		var found = new double[4];
		new QuaternionNumericsHam().rodriguesToParam(x, y, z, found);

		assertEquals(expected.w, found[0], 1e-6);
		assertEquals(expected.x, found[1], 1e-6);
		assertEquals(expected.y, found[2], 1e-6);
		assertEquals(expected.z, found[3], 1e-6);
	}

	/** See if it can handle the case where the vector is all zeros */
	@Test void rodriguesToParam_zero() {
		var found = new double[4];
		new QuaternionNumericsHam().rodriguesToParam(0, 0, 0, found);

		var expected = new Quaternion_F64();
		ConvertRotation3D_F64.eulerToQuaternion(EulerType.XYZ, 0, 0, 0, expected);

		assertEquals(expected.w, found[0], 1e-6);
		assertEquals(expected.x, found[1], 1e-6);
		assertEquals(expected.y, found[2], 1e-6);
		assertEquals(expected.z, found[3], 1e-6);
	}

	@Test void rodriguesToJacobian() {
		var alg = new QuaternionNumericsHam();
		for (int i = 0; i < 10; i++) {
			double rotX = rand.nextGaussian();
			double rotY = rand.nextGaussian();
			double rotZ = rand.nextGaussian();

			checkRodriguesNumerical(rotX, rotY, rotZ, alg);
		}
	}

	/** Test it when converting small angles */
	@Test void rodriguesToJacobian_SmallAngle() {
		var alg = new QuaternionNumericsHam();
		alg.setSmallAngleTol(1.0);
		// make the size very large to make it easier to excite that code

		for (int i = 0; i < 10; i++) {
			double rotX = rand.nextGaussian()*0.05;
			double rotY = rand.nextGaussian()*0.05;
			double rotZ = rand.nextGaussian()*0.05;

			checkRodriguesNumerical(rotX, rotY, rotZ, alg);
		}
	}

	@Test void rodriguesToJacobian_zeros() {
		var alg = new QuaternionNumericsHam();
		checkRodriguesNumerical(0, 0, 0, alg);
	}

	@Test void eulerXyzToJacobian() {
		var alg = new QuaternionNumericsHam();
		for (int i = 0; i < 10; i++) {
			double rotX = rand.nextGaussian();
			double rotY = rand.nextGaussian();
			double rotZ = rand.nextGaussian();

			checkEulerNumerical(rotX, rotY, rotZ, alg);
		}
	}

	@Test void angleError() {
		Quaternion_F64 quatA = eulerToQuaternion(EulerType.XYZ, 0.5, 0.1, -0.2, null);
		Quaternion_F64 quatB = eulerToQuaternion(EulerType.XYZ, 0.5, 0.3, -0.2, null);

		var alg = new QuaternionNumericsHam();
		alg.quat.setTo(quatA);
		assertEquals(0.2, alg.angleError(quatToArray(quatB)), 1e-6);

		// try the other direction
		eulerToQuaternion(EulerType.XYZ, 0.5, -0.1, -0.2, quatB);
		assertEquals(0.2, alg.angleError(quatToArray(quatB)), 1e-6);

		eulerToQuaternion(EulerType.XYZ, 0.5, 0.5, -0.2, quatB);
		assertEquals(0.4, alg.angleError(quatToArray(quatB)), 1e-6);

		eulerToQuaternion(EulerType.XYZ, 0.5, 0.1 + 1.9*Math.PI, -0.2, quatB);
		assertEquals(1.9*Math.PI, alg.angleError(quatToArray(quatB)), 1e-6);

		eulerToQuaternion(EulerType.XYZ, 0.5, 0.1 - 1.9*Math.PI, -0.2, quatB);
		assertEquals(1.9*Math.PI, alg.angleError(quatToArray(quatB)), 1e-6);
	}

	/// The two pathological cases, where the dot product saturates and the derivative of acos is
	/// singular. Random parameters will not hit either
	@Test void angleError_saturated() {
		var alg = new QuaternionNumericsHam();
		var identity = new double[]{1, 0, 0, 0};
		var opposite = new double[]{-1, 0, 0, 0};
		alg.set(identity, 0);

		assertEquals(0.0, alg.angleError(identity), UtilEjml.TEST_F64);
		assertEquals(2.0*Math.PI, alg.angleError(opposite), UtilEjml.TEST_F64);

		// the implementation reports zero rather than a singular derivative
		var foundA = new DMatrixRMaj(1, 4);
		var foundB = new DMatrixRMaj(1, 4);
		for (double[] paramsB : new double[][]{identity, opposite}) {
			alg.angleErrorJacobian(paramsB, foundA, foundB);
			for (int i = 0; i < 4; i++) {
				assertEquals(0.0, foundA.get(0, i));
				assertEquals(0.0, foundB.get(0, i));
			}
		}
	}

	/** Use numerical differentiation to check the Jacobian */
	private void checkEulerNumerical( double rotX, double rotY, double rotZ, RotationNumerics alg ) {
		var param = new double[]{rotX, rotY, rotZ};
		var outputP = new double[alg.length()];
		var outputM = new double[alg.length()];

		var expected = new DMatrixRMaj(alg.length(), param.length);
		for (int idxIn = 0; idxIn < param.length; idxIn++) {
			double value = param[idxIn];
			param[idxIn] = value - deltaT;
			alg.eulerXyzToParam(param[0], param[1], param[2], outputM);
			param[idxIn] = value + deltaT;
			alg.eulerXyzToParam(param[0], param[1], param[2], outputP);
			param[idxIn] = value;

			for (int idxOut = 0; idxOut < outputP.length; idxOut++) {
				expected.set(idxOut, idxIn, (outputP[idxOut] - outputM[idxOut])/(2*deltaT));
			}
		}

		var found = new DMatrixRMaj(alg.length(), param.length);
		alg.eulerXyzToJacobian(param[0], param[1], param[2], found);

		assertTrue(MatrixFeatures_DDRM.isEquals(expected, found, tolerance));
	}

	/** Use numerical differentiation to check the Jacobian */
	private void checkRodriguesNumerical( double rotX, double rotY, double rotZ, RotationNumerics alg ) {
		var param = new double[]{rotX, rotY, rotZ};
		var outputP = new double[alg.length()];
		var outputM = new double[alg.length()];

		var expected = new DMatrixRMaj(alg.length(), param.length);
		for (int idxIn = 0; idxIn < param.length; idxIn++) {
			double value = param[idxIn];
			param[idxIn] = value - deltaT;
			alg.rodriguesToParam(param[0], param[1], param[2], outputM);
			param[idxIn] = value + deltaT;
			alg.rodriguesToParam(param[0], param[1], param[2], outputP);
			param[idxIn] = value;

			for (int idxOut = 0; idxOut < outputP.length; idxOut++) {
				expected.set(idxOut, idxIn, (outputP[idxOut] - outputM[idxOut])/(2*deltaT));
			}
		}

		var found = new DMatrixRMaj(alg.length(), param.length);
		alg.rodriguesToJacobian(param[0], param[1], param[2], found);

		assertTrue(MatrixFeatures_DDRM.isEquals(expected, found, tolerance));
	}

	@Test public void normalizeParams() {
		var q = new Quaternion_F64();

		// create valid parameters
		q.setTo(1, 2, 3, 4);
		q.normalize();

		// scale them
		var array = new double[]{0, q.w*2, q.x*2, q.y*2, q.z*2};

		// normalize
		new QuaternionNumericsHam().normalizeParams(array, 1);

		// Should have undone the scaling
		assertEquals(q.w, array[1], UtilEjml.TEST_F64);
		assertEquals(q.x, array[2], UtilEjml.TEST_F64);
		assertEquals(q.y, array[3], UtilEjml.TEST_F64);
		assertEquals(q.z, array[4], UtilEjml.TEST_F64);
	}

	@Test public void matrixToParam() {
		// declare what is being tested. Offset the parameters to make sure the offset is handled correctly
		var alg = new QuaternionNumericsHam();
		var params = new double[alg.length() + 1];

		// Create an arbitrary rotation matrix
		var expected = new DMatrixRMaj(3, 3);
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.1, -0.5, 1.3, expected);

		// Parameterize the matrix
		alg.matrixToParam(expected, params, 1);

		// Assign these parameters to 'alg'
		alg.set(params, 1);

		// Request the matrix that they represent
		var found = new DMatrixRMaj(3, 3);
		alg.toMatrix(found);

		// R'*R = I, if R is orthogonal
		var error = new DMatrixRMaj(3, 3);
		CommonOps_DDRM.multTransA(expected, found, error);

		// See if they are the same / error is an identity matrix
		assertTrue(MatrixFeatures_DDRM.isIdentity(error, UtilEjml.TEST_F64));
	}

	/// Pass in two identical matrices that should be pathological, see if it's handled correctly
	@Test void residual_identical() {
		var alg = new QuaternionNumericsHam();
		int N = alg.length();
		var params = new double[N];
		var residuals = new double[alg.residualLength()];

		for (int trial = 0; trial < 100; trial++) {
			for (int i = 0; i < params.length; i++) {
				params[i] = 2.0*rand.nextDouble() - 1.0;
			}
			normalize(params);

			alg.set(params, 0);
			alg.residual(params, 0, residuals, 0);

			// make sure the residual is zero
			for (int i = 0; i < residuals.length; i++) {
				assertEquals(0.0, residuals[i], 5e-7);
			}
		}
	}

	/// Test edge cases
	@Test void residualJacobian_edge() {
		var alg = new QuaternionNumericsHam();

		residualJacobian(new double[]{1, 0, 0, 0}, new double[]{1, 0, 0, 0}, alg);
		residualJacobian(new double[]{1, 0, 0, 0}, new double[]{1, 0, 0, 0.1}, alg);
		residualJacobian(new double[]{1, 0, 0, 0.1}, new double[]{1, 0, 0, 0}, alg);
	}

	private void residualJacobian( double[] paramsA, double[] paramsB, QuaternionNumericsHam alg ) {
		int N = alg.length();

		normalize(paramsA);
		normalize(paramsB);

		var foundA = new DMatrixRMaj(N, N);
		var foundB = new DMatrixRMaj(N, N);
		alg.set(paramsA, 0);
		alg.residualJacobian(paramsB, 0, foundA, foundB);

		residualJacobian_JacA(paramsA, 0, paramsB, 0, alg, foundA);
		residualJacobian_JacB(paramsA, 0, paramsB, 0, alg, foundB);
	}

	/**
	 * The two quaternions will be almost identical. See if it produces a good Jacobian. This is a known challening
	 * scenario that is very common.
	 */
	@Test void residualJacobian_smallAngle() {
		// because the numbers are so small we need to adjust things
		deltaT = 1e-11;
		tolerance = 1e-5;

		var alg = new QuaternionNumericsHam();
		int N = alg.length();
		var paramsA = new double[N];
		var paramsB = new double[N];

		for (int trial = 0; trial < 10; trial++) {
			for (int i = 0; i < paramsA.length; i++) {
				paramsA[i] = 2.0*rand.nextDouble() - 1.0;
				paramsB[i] = paramsA[i] + 1e-10*(2.0*rand.nextDouble() - 1.0);
			}
			normalize(paramsA);
			normalize(paramsB);

			var foundA = new DMatrixRMaj(N, N);
			var foundB = new DMatrixRMaj(N, N);
			alg.set(paramsA, 0);
			alg.residualJacobian(paramsB, 0, foundA, foundB);

			residualJacobian_JacA(paramsA, 0, paramsB, 0, alg, foundA);
			residualJacobian_JacB(paramsA, 0, paramsB, 0, alg, foundB);
		}
	}

	// [ass in two identical matrices that should be pathological, see if it's handled correctly
	@Test void residualJacobian_identical() {
		var alg = new QuaternionNumericsHam();
		var params = new double[alg.length()];

		for (int trial = 0; trial < 10; trial++) {
			for (int i = 0; i < params.length; i++) {
				params[i] = 2.0*rand.nextDouble() - 1.0;
			}
			normalize(params);

			var foundA = new DMatrixRMaj(alg.residualLength(), alg.length());
			var foundB = new DMatrixRMaj(alg.residualLength(), alg.length());
			alg.set(params, 0);
			alg.residualJacobian(params, 0, foundA, foundB);

			// Make sure there are no NaN
			assertFalse(MatrixFeatures_DDRM.hasUncountable(foundA));
			assertFalse(MatrixFeatures_DDRM.hasUncountable(foundB));

			// see if it has very large numbers
			assertTrue(NormOps_DDRM.normF(foundA) < 10);
			assertTrue(NormOps_DDRM.normF(foundB) < 10);
		}
	}

	private static void normalize( double[] array ) {
		double sum = 0.0;
		for (int i = 0; i < array.length; i++) {
			sum += array[i]*array[i];
		}
		double norm = Math.sqrt(sum);
		for (int i = 0; i < array.length; i++) {
			array[i] /= norm;
		}
	}

	private static double[] quatToArray( Quaternion_F64 q ) {
		return new double[]{q.w, q.x, q.y, q.z};
	}

	/// Random value from -1 to 1
	double uniform() {
		return 2.0*rand.nextDouble() - 1.0;
	}
}
