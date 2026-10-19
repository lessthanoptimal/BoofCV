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
import georegression.struct.so.Quaternion_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Every inherited check is run with parameters that are deliberately not unit length, since for this
/// class that is the contract rather than a violation
class TestQuaternionNumericsHamNorm extends CommonRotationNumerics {

	@Override protected RotationNumerics createAlg() {
		return new QuaternionNumericsHamNorm();
	}

	/// A spread of lengths, including one just off unit, which is what a solver actually produces
	@Override protected List<double[]> createParams() {
		return List.of(
				new double[]{0.1, 0.2, 0.3, 0.4},
				new double[]{2.0, -3.0, 1.5, 0.5},
				new double[]{-1.2, 0.4, -0.8, 1.1},
				new double[]{0.7074828, 0.7074828, 0.0, 0.0});
	}

	@Override protected DMatrixRMaj paramToMatrix( double[] params ) {
		var q = new Quaternion_F64(params[0], params[1], params[2], params[3]);
		q.normalize();
		return ConvertRotation3D_F64.quaternionToMatrix(q, null);
	}

	@Test void length() {
		assertEquals(4, new QuaternionNumericsHamNorm().length());
	}

	/// Scaling the parameters must not change anything the class computes
	@Test void scaleInvariant() {
		var alg = new QuaternionNumericsHamNorm();
		var paramsA = new double[]{0.4, -0.2, 0.8, 0.3};
		var paramsB = new double[]{0.1, 0.5, -0.3, 0.7};

		var expectedR = new DMatrixRMaj(3, 3);
		alg.set(paramsA, 0);
		alg.toMatrix(expectedR);
		var expectedResidual = new double[3];
		alg.residual(paramsB, 0, expectedResidual, 0);
		double expectedAngle = alg.angleError(paramsB);

		for (double scale : new double[]{0.25, 1.0, 7.5}) {
			var scaledA = scale(paramsA, scale);
			var scaledB = scale(paramsB, 1.0/scale);

			var found = new DMatrixRMaj(3, 3);
			alg.set(scaledA, 0);
			alg.toMatrix(found);
			assertTrue(MatrixFeatures_DDRM.isEquals(expectedR, found, UtilEjml.TEST_F64));

			var residual = new double[3];
			alg.residual(scaledB, 0, residual, 0);
			for (int i = 0; i < residual.length; i++)
				assertEquals(expectedResidual[i], residual[i], UtilEjml.TEST_F64);

			assertEquals(expectedAngle, alg.angleError(scaledB), UtilEjml.TEST_F64);
		}
	}

	/// Any non-zero vector encodes a rotation. Only all zeros is rejected
	@Test void isValid_direction() {
		var alg = new QuaternionNumericsHamNorm();

		assertTrue(alg.isValid(new double[]{1, 0, 0, 0}, 0));
		assertTrue(alg.isValid(new double[]{0, 0.1, 0.2, 0.3, 0.4}, 1));
		assertFalse(alg.isValid(new double[]{0, 0, 0, 0}, 0));

		assertThrows(IllegalArgumentException.class, () -> alg.set(new double[]{0, 0, 0, 0}, 0));
	}

	/// The parent uses its internal quaternion as scratch here, which would leave the cached
	/// parameters stale. Verify the state survives
	@Test void normalizeParams_leavesStateAlone() {
		var alg = new QuaternionNumericsHamNorm();
		var params = new double[]{0.4, -0.2, 0.8, 0.3};
		alg.set(params, 0);

		var expected = new DMatrixRMaj(3, 3);
		alg.toMatrix(expected);

		// normalize an unrelated set of parameters
		var other = new double[]{0, 2.0, -3.0, 1.5, 0.5};
		alg.normalizeParams(other, 1);
		assertEquals(1.0, Math.sqrt(other[1]*other[1] + other[2]*other[2]
				+ other[3]*other[3] + other[4]*other[4]), UtilEjml.TEST_F64);

		var found = new DMatrixRMaj(3, 3);
		alg.toMatrix(found);
		assertTrue(MatrixFeatures_DDRM.isEquals(expected, found, UtilEjml.TEST_F64));

		// the raw parameters are still what was passed in
		var readBack = new double[4];
		alg.get(readBack, 0);
		for (int i = 0; i < 4; i++)
			assertEquals(params[i], readBack[i], UtilEjml.TEST_F64);
	}

	/// Apply the definition, `J*(I - qq')/n`, with the projector built explicitly
	@Test void chainRule() {
		var unit = new double[]{0.5, -0.5, 0.5, 0.5};   // already unit length
		double norm = 2.3;

		// I - qq'
		var projector = CommonOps_DDRM.identity(4);
		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 4; col++)
				projector.data[row*4 + col] -= unit[row]*unit[col];
		}

		// every shape the class asks for: angleError, residual, and concat
		for (int rows : new int[]{1, 3, 4}) {
			var jacobian = new DMatrixRMaj(rows, 4);
			for (int i = 0; i < jacobian.data.length; i++)
				jacobian.data[i] = 1 + 0.25*i;

			var expected = new DMatrixRMaj(rows, 4);
			CommonOps_DDRM.mult(1.0/norm, jacobian, projector, expected);

			var found = jacobian.copy();
			QuaternionNumericsHamNorm.chainRule(found, unit, norm);
			assertTrue(MatrixFeatures_DDRM.isEquals(expected, found, UtilEjml.TEST_F64));

			// the direction being projected out is a null direction afterwards
			for (int row = 0; row < rows; row++) {
				double dot = 0;
				for (int col = 0; col < 4; col++)
					dot += found.data[row*4 + col]*unit[col];
				assertEquals(0.0, dot, UtilEjml.TEST_F64);
			}
		}
	}

	/// One column per parameter, so any other width is a mistake worth catching
	@Test void chainRule_wrongShape() {
		var unit = new double[]{1, 0, 0, 0};
		assertThrows(IllegalArgumentException.class,
				() -> QuaternionNumericsHamNorm.chainRule(new DMatrixRMaj(3, 3), unit, 1.0));
	}

	@Test void normalize() {
		var src = new double[]{9, 1, 2, 3, 4, 9};    // the quaternion sits at offset 1
		double expectedNorm = Math.sqrt(1 + 4 + 9 + 16);

		// writes from the start of the destination and returns the length before scaling
		var dst = new double[4];
		assertEquals(expectedNorm, QuaternionNumericsHamNorm.normalize(src, 1, dst), UtilEjml.TEST_F64);
		for (int i = 0; i < 4; i++)
			assertEquals((i + 1)/expectedNorm, dst[i], UtilEjml.TEST_F64);

		// independent source and destination offsets
		var offsetDst = new double[]{7, 7, 7, 7, 7, 7};
		assertEquals(expectedNorm, QuaternionNumericsHamNorm.normalize(src, 1, offsetDst, 2), UtilEjml.TEST_F64);
		assertEquals(7.0, offsetDst[1]);
		assertEquals(7.0, offsetDst[0]);
		for (int i = 0; i < 4; i++)
			assertEquals((i + 1)/expectedNorm, offsetDst[2 + i], UtilEjml.TEST_F64);

		// in place, which is what normalizeParams relies on. Reading and writing must not interfere
		var inPlace = src.clone();
		assertEquals(expectedNorm, QuaternionNumericsHamNorm.normalize(inPlace, 1, inPlace, 1), UtilEjml.TEST_F64);
		assertEquals(9.0, inPlace[0]);
		assertEquals(9.0, inPlace[5]);
		for (int i = 0; i < 4; i++)
			assertEquals((i + 1)/expectedNorm, inPlace[1 + i], UtilEjml.TEST_F64);

		assertThrows(IllegalArgumentException.class,
				() -> QuaternionNumericsHamNorm.normalize(new double[4], 0, new double[4]));
	}

	/// Deliberately not unit length. The length is kept inside a band so that the 1/n the Jacobian
	/// carries never grows large enough to matter against the numerical differentiation tolerance
	@Override protected void randomParams( double[] params, int offset ) {
		double norm = 0;
		for (int i = 0; i < 4; i++) {
			double v = uniform();
			params[offset + i] = v;
			norm += v*v;
		}
		norm = Math.sqrt(norm);

		double target = 0.5 + 1.5*rand.nextDouble();
		for (int i = 0; i < 4; i++)
			params[offset + i] *= target/norm;
	}

	private static double[] scale( double[] params, double scale ) {
		var out = new double[params.length];
		for (int i = 0; i < params.length; i++)
			out[i] = params[i]*scale;
		return out;
	}

	/// Random value from -1 to 1
	double uniform() {
		return 2.0*rand.nextDouble() - 1.0;
	}
}
