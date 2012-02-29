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
 * @author Peter Abeles
 */
public class EvalFuncCurveFit implements EvalFuncLeastSquares {
	
	double t[];
	double f[];

	public EvalFuncCurveFit(double x1, double x2, double x3, double x4, double deltaT, int N) {
		t = new double[N];
		f = new double[N];
		
		for( int i = 0; i < N; i++ ) {
			t[i] = deltaT*i;
			f[i] = model(x1,x2,x3,x4,t[i]);
		}
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
	
	public static double model( double x1 ,double x2, double x3 , double x4 , double t ) {
		return x3*Math.exp(x1*t) + x4*Math.exp(x2*t);
	}
	
	public class Func implements FunctionNtoM
	{
		@Override
		public int getN() {
			return 4;
		}

		@Override
		public int getM() {
			return t.length;
		}

		@Override
		public void process(double[] input, double[] output) {
			double x1 = input[0];
			double x2 = input[1];
			double x3 = input[2];
			double x4 = input[3];
			
			for( int i = 0; i < t.length; i++ ) {
				output[i] = f[i] - model(x1,x2,x3,x4,t[i]);
			}
		}
	}

	public class Deriv implements FunctionNtoMxN
	{
		@Override
		public int getN() {
			return 2;
		}

		@Override
		public int getM() {
			return t.length;
		}

		@Override
		public void process(double[] input, double[] output) {
			DenseMatrix64F J = DenseMatrix64F.wrap(t.length,2,output);
			double x1 = input[0];
			double x2 = input[1];
			double x3 = input[2];
			double x4 = input[3];

			for( int i = 0; i < t.length; i++ ) {
				double g1 = -x3*t[i]*Math.exp(x1*t[i]);
				double g2 = -x4*t[i]*Math.exp(x2*t[i]);
				double g3 = -Math.exp(x1*t[i]);
				double g4 = -Math.exp(x2*t[i]);

				J.set(i,0,g1);
				J.set(i,1,g2);
				J.set(i,2,g3);
				J.set(i,3,g4);
			}

			
			J.set(0,0,1);
			J.set(0,1,0);
			J.set(1,0,1.0/Math.pow(x1+0.1,2));
			J.set(1,1,4*x2);
		}
	}

}
