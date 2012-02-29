/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.numerics.optimization.funcs;

import boofcv.numerics.optimization.functions.FunctionNtoM;
import boofcv.numerics.optimization.functions.FunctionNtoMxN;
import org.ejml.data.DenseMatrix64F;

/**
 * Frandsen et al 1999
 *
 * @author Peter Abeles
 */
public class EvalFuncRosenbrockMod implements EvalFuncLeastSquares {

	double lambda;

	public EvalFuncRosenbrockMod(double lambda) {
		this.lambda = lambda;
	}

	@Override
	public FunctionNtoM getFunction() {
		return new Func();
	}

	@Override
	public FunctionNtoMxN getJacobian() {
		return new Deriv();
	}

	@Override
	public double[] getInitial() {
		return new double[]{-1.2,1};
	}

	@Override
	public double[] getOptimal() {
		return new double[]{1,1};
	}
	
	public class Func implements FunctionNtoM
	{
		@Override
		public int getN() {
			return 2;
		}

		@Override
		public int getM() {
			return 3;
		}

		@Override
		public void process(double[] input, double[] output) {
			double x1 = input[0];
			double x2 = input[1];
			
			output[0] = 10.0*(x2-x1*x1);
			output[1] = 1.0 - x1;
			output[2] = lambda;
		}
	}

	public static class Deriv implements FunctionNtoMxN
	{
		@Override
		public int getN() {
			return 2;
		}

		@Override
		public int getM() {
			return 3;
		}

		@Override
		public void process(double[] input, double[] output) {
			DenseMatrix64F J = DenseMatrix64F.wrap(3,2,output);
			double x1 = input[0];
			
			J.set(0,0,-20*x1);
			J.set(0,1,10);
			J.set(1,0,-1);
			J.set(1,1,0);
			J.set(2,0,0);
			J.set(2,1,0);
		}
	}
}
