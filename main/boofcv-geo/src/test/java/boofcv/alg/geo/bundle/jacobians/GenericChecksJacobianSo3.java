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

package boofcv.alg.geo.bundle.jacobians;

import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import org.ddogleg.optimization.DerivativeChecker;
import org.ddogleg.optimization.functions.FunctionNtoM;
import org.ddogleg.optimization.functions.FunctionNtoMxN;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class GenericChecksJacobianSo3 extends BoofStandardJUnit {
	boolean skipJacobianAtIdentity = false;
	boolean printJacobian = false;

	abstract JacobianSo3 createAlgorithm();

	@Test
	void encode_then_decode() {
		JacobianSo3 alg = createAlgorithm();

		DMatrixRMaj R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.1, 0.2, -0.3, null);
		double[] p = new double[alg.getParameterLength() + 1];
		alg.getParameters(R, p, 1); // offset to ensure it isn't hard coded at 0
		alg.setParameters(p, 1);

		DMatrixRMaj found = alg.getRotationMatrix();
		assertTrue(MatrixFeatures_DDRM.isIdentical(R, found, UtilEjml.TEST_F64));
	}

	@Test
	void compareToNumeric() {
		JacobianSo3 alg = createAlgorithm();
		double[] p = new double[alg.getParameterLength()];

		RodToMatrix f = new RodToMatrix(alg);
		RodToGradient g = new RodToGradient(alg);

		DMatrixRMaj R = CommonOps_DDRM.identity(3);
		for (int i = 0; i < 100; i++) {
//			System.out.println("I = "+i);
			// the first time it will be no rotation. test this edgecase
			if (skipJacobianAtIdentity || i > 0)
				ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,
						(double)rand.nextGaussian(), (double)rand.nextGaussian(), (double)rand.nextGaussian(), R);
			alg.getParameters(R, p, 0);

//			for (int j = 0; j < 3; j++) {
//				System.out.print(param[j]+" ");
//			}
//			System.out.println();

			if (printJacobian)
				DerivativeChecker.jacobianPrint(f, g, p, UtilEjml.TEST_F64_SQ);
			assertTrue(DerivativeChecker.jacobian(f, g, p, UtilEjml.TEST_F64_SQ));
		}
	}

	public static class RodToMatrix implements FunctionNtoM {
		JacobianSo3 alg;

		public RodToMatrix( JacobianSo3 alg ) {
			this.alg = alg;
		}

		@Override public int getNumOfInputsN() { return alg.getParameterLength(); }

		@Override public int getNumOfOutputsM() { return 9; }

		@Override
		public void process( /**/double[] input, /**/double[] output ) {
			alg.setParameters(input, 0);
			DMatrixRMaj M = DMatrixRMaj.wrap(3, 3, output);
			M.setTo(alg.getRotationMatrix());
		}
	}

	public static class RodToGradient implements FunctionNtoMxN<DMatrixRMaj> {
		JacobianSo3 alg;

		public RodToGradient( JacobianSo3 alg ) {
			this.alg = alg;
		}

		@Override public int getNumOfInputsN() { return alg.getParameterLength(); }

		@Override public int getNumOfOutputsM() { return 9; }

		@Override
		public void process( /**/double[] input, DMatrixRMaj J ) {
			alg.setParameters(input, 0);

			double[] output = J.data;
			int index = 0;
			for (int i = 0; i < alg.getParameterLength(); i++) {
				System.arraycopy(alg.getPartial(i).data, 0, output, index, 9);
				index += 9;
			}

			J.numRows = getNumOfInputsN();
			J.numCols = getNumOfOutputsM();
			CommonOps_DDRM.transpose(J);
		}

		@Override
		public /**/DMatrixRMaj declareMatrixMxN() {
			return new DMatrixRMaj(getNumOfOutputsM(), getNumOfInputsN());
		}
	}
}
