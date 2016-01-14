/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.so.Rodrigues_F64;
import org.ddogleg.optimization.DerivativeChecker;
import org.ddogleg.optimization.functions.FunctionNtoM;
import org.ddogleg.optimization.functions.FunctionNtoMxN;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestRodriguesRotationGradient {

	Random rand = new Random(234);
	
	@Test
	public void checkUsingNumerical() {
		RodToMatrix f = new RodToMatrix();
		RodToGradient g = new RodToGradient();
		
		for( int i = 0; i < 20; i++ ) {
			double param[] = new double[3];
			param[0] = (rand.nextDouble()-0.5);
			param[1] = (rand.nextDouble()-0.5);
			param[2] = (rand.nextDouble()-0.5);

			assertTrue(DerivativeChecker.jacobian(f, g, param, 1e-6));
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
			DenseMatrix64F M = DenseMatrix64F.wrap(3,3,output);

			ConvertRotation3D_F64.rodriguesToMatrix(r,M);
		}
	}

	public static class RodToGradient implements FunctionNtoMxN
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
