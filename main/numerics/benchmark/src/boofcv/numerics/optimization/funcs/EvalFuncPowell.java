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
 *
 * <p>
 * Powel 1970
 * </p>
 *
 * @author Peter Abeles
 */
public class EvalFuncPowell implements EvalFuncLeastSquares {
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
		return new double[]{3,1};
	}

	@Override
	public double[] getOptimal() {
		return new double[]{0,0};
	}
	
	public static class Func implements FunctionNtoM
	{
		@Override
		public int getN() {
			return 2;
		}

		@Override
		public int getM() {
			return 2;
		}

		@Override
		public void process(double[] input, double[] output) {
			double x1 = input[0];
			double x2 = input[1];
			
			output[0] = x1;
			output[1] = 10*x1/(x1+0.1) + 2*x2*x2;
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
			return 2;
		}

		@Override
		public void process(double[] input, double[] output) {
			DenseMatrix64F J = DenseMatrix64F.wrap(2,2,output);
			double x1 = input[0];
			double x2 = input[1];
			
			J.set(0,0,1);
			J.set(0,1,0);
			J.set(1,0,1.0/Math.pow(x1 + 0.1, 2));
			J.set(1,1,4*x2);
		}
	}
}
