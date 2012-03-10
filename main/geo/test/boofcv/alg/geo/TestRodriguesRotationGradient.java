/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

import boofcv.numerics.optimization.JacobianChecker;
import boofcv.numerics.optimization.functions.FunctionNtoM;
import boofcv.numerics.optimization.functions.FunctionNtoMxN;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.so.Rodrigues;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestRodriguesRotationGradient {

	@Test
	public void checkUsingNumerical() {
		RodToMatrix f = new RodToMatrix();
		RodToGradient g = new RodToGradient();
		
		double param[] = new double[]{1,2,3};
//		double param[] = new double[]{1e-6,2e-6,3e-6};

//		JacobianChecker.jacobianPrint(f, g, param, 1e-6);
		assertTrue(JacobianChecker.jacobian(f, g, param, 1e-6));
	}
	
	private static class RodToMatrix implements FunctionNtoM
	{
		@Override
		public int getN() {
			return 3;
		}

		@Override
		public int getM() {
			return 9;
		}

		@Override
		public void process(double[] input, double[] output) {
			Rodrigues r = new Rodrigues();

			r.setParamVector(input[0],input[1],input[2]);
			DenseMatrix64F M = DenseMatrix64F.wrap(3,3,output);

			RotationMatrixGenerator.rodriguesToMatrix(r,M);
		}
	}

	private static class RodToGradient implements FunctionNtoMxN
	{
		@Override
		public int getN() {
			return 3;
		}

		@Override
		public int getM() {
			return 9;
		}

		@Override
		public void process(double[] input, double[] output) {
			RodriguesRotationJacobian g = new RodriguesRotationJacobian();
			
			g.process(input[0],input[1],input[2]);

			DenseMatrix64F J = DenseMatrix64F.wrap(3,9,output);
			
			System.arraycopy(g.Rx.data,0,output,0,9);
			System.arraycopy(g.Ry.data,0,output,9,9);
			System.arraycopy(g.Rz.data,0,output,18,9);

			CommonOps.transpose(J);
		}
	}
}
