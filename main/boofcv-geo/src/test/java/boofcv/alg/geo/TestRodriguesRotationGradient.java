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

package boofcv.alg.geo;

import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.so.Rodrigues_F64;
import org.ddogleg.optimization.DerivativeChecker;
import org.ddogleg.optimization.functions.FunctionNtoM;
import org.ddogleg.optimization.functions.FunctionNtoMxN;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestRodriguesRotationGradient extends BoofStandardJUnit {

	@Test void checkUsingNumerical() {
		RodToMatrix f = new RodToMatrix();
		RodToGradient g = new RodToGradient();
		
		for( int i = 0; i < 100; i++ ) {
			double param[] = new double[3];
			param[0] = 5*(rand.nextDouble()-0.5);
			param[1] = 5*(rand.nextDouble()-0.5);
			param[2] = 5*(rand.nextDouble()-0.5);

//			for (int j = 0; j < 3; j++) {
//				System.out.print(param[j]+" ");
//			}
//			System.out.println();

//			DerivativeChecker.jacobianPrintR(f, g, param, 1e-4);
			assertTrue(DerivativeChecker.jacobian(f, g, param, UtilEjml.TEST_F64_SQ));
		}
	}

// Commented out since I'm not sure if numerical or analytical Jacobian is correct
// Potential room for improvement here
//	@Test
//	public void checkHardCases() {
//
//		RodToMatrix f = new RodToMatrix();
//		RodToGradient g = new RodToGradient();
//
//		DerivativeChecker.jacobianPrintR(f, g, new double[]{1e-6,1e-6,1e-6}, 1e-6);
//		assertTrue(DerivativeChecker.jacobianR(f, g, new double[]{1e-6,1e-6,1e-6}, 1e-6));
//	}
	
	public static class RodToMatrix implements FunctionNtoM
	{
		@Override
		public int getNumOfInputsN() {
			return 3;
		}

		@Override
		public int getNumOfOutputsM() {
			return 9;
		}

		@Override
		public void process(double[] input, double[] output) {
			Rodrigues_F64 r = new Rodrigues_F64();

			r.setParamVector(input[0],input[1],input[2]);
			DMatrixRMaj M = DMatrixRMaj.wrap(3,3,output);

			ConvertRotation3D_F64.rodriguesToMatrix(r,M);
		}
	}

	public static class RodToGradient implements FunctionNtoMxN<DMatrixRMaj>
	{
		@Override
		public int getNumOfInputsN() {
			return 3;
		}

		@Override
		public int getNumOfOutputsM() {
			return 9;
		}

		@Override
		public void process(double[] input, DMatrixRMaj J) {
			RodriguesRotationJacobian_F64 g = new RodriguesRotationJacobian_F64();
			
			g.process(input[0],input[1],input[2]);

			double output[] = J.data;
			
			System.arraycopy(g.Rx.data,0,output,0,9);
			System.arraycopy(g.Ry.data,0,output,9,9);
			System.arraycopy(g.Rz.data,0,output,18,9);

			J.numRows = getNumOfInputsN();
			J.numCols = getNumOfOutputsM();
			CommonOps_DDRM.transpose(J);
		}

		@Override
		public DMatrixRMaj declareMatrixMxN() {
			return new DMatrixRMaj(getNumOfOutputsM(),getNumOfInputsN());
		}
	}
}
