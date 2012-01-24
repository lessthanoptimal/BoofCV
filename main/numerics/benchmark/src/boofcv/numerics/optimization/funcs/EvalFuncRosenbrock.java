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

import boofcv.numerics.optimization.FunctionNtoM;

/**
 * @author Peter Abeles
 */
public class EvalFuncRosenbrock implements EvalFuncLeastSquares {
	@Override
	public FunctionNtoM getFunction() {
		return new Func();
	}

	@Override
	public double[] getInitial() {
		return new double[]{-1.2,1};
	}

	@Override
	public double[] getOptimal() {
		return new double[]{1,1};
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
			
			output[0] = 10.0*(x2-x1*x1);
			output[1] = 1.0 - x1;
		}
	}
}
