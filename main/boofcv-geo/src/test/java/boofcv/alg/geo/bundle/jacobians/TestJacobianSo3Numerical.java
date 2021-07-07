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

package boofcv.alg.geo.bundle.jacobians;

import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestJacobianSo3Numerical extends BoofStandardJUnit {
	@Test void getRotationMatrix() {
		JacobianSo3Numerical alg = new SimpleSo3();

		// offset of 1 to make sure it doesn't start at zero
		alg.setParameters(new double[]{0, 1, 2}, 1);
		DMatrixRMaj R = alg.getRotationMatrix();

		assertEquals(1, R.data[0], 10*UtilEjml.TEST_F64);
		assertEquals(2, R.data[1], 10*UtilEjml.TEST_F64);
		assertEquals(3, R.data[2], 10*UtilEjml.TEST_F64);
		assertEquals(1, R.data[3], 10*UtilEjml.TEST_F64);
		assertEquals(2, R.data[4], 10*UtilEjml.TEST_F64);
		for (int i = 5; i < 9; i++) {
			assertEquals(0, R.data[i], UtilEjml.TEST_F64);
		}
	}

	@Test void getPartial() {
		JacobianSo3Numerical alg = new SimpleSo3();

		assertEquals(2, alg.getParameterLength());

		// offset of 1 to make sure it doesn't start at zero
		alg.setParameters(new double[]{0, 1, 2}, 1);
		DMatrixRMaj R0 = alg.getPartial(0);
		DMatrixRMaj R1 = alg.getPartial(1);

		double[] expected0 = new double[]{1, 0, 1, 2, 2, 0, 0, 0, 0};
		double[] expected1 = new double[]{0, 1, 1, 0, 1, 0, 0, 0, 0};

		for (int i = 0; i < 9; i++) {
			assertEquals(expected0[i], R0.data[i], UtilEjml.TEST_F64_SQ);
			assertEquals(expected1[i], R1.data[i], UtilEjml.TEST_F64_SQ);
		}
	}

	public static class SimpleSo3 extends JacobianSo3Numerical {

		@Override
		public void computeRotationMatrix( double[] parameters, int offset, DMatrixRMaj R ) {
			double a = parameters[offset];
			double b = parameters[offset + 1];

			R.zero();

			R.data[0] = a;
			R.data[1] = b;
			R.data[2] = a + b;
			R.data[3] = a*a;
			R.data[4] = a*b;
		}

		@Override
		public void getParameters( DMatrixRMaj R, double[] parameters, int offset ) {
			parameters[offset] = R.data[0];
			parameters[offset + 1] = R.data[1];
		}

		@Override
		public int getParameterLength() {
			return 2;
		}
	}
}
