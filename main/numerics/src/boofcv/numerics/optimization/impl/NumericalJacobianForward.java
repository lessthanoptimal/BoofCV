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

package boofcv.numerics.optimization.impl;

import boofcv.numerics.optimization.functions.FunctionNtoM;
import boofcv.numerics.optimization.functions.FunctionNtoMxN;
import org.ejml.UtilEjml;
import org.ejml.data.DenseMatrix64F;

/**
 * Finite difference numerical gradient calculation using forward equation. Forward
 * difference equation, f'(x) = f(x+h)-f(x)/h.  Scaling is taken in account by h based
 * upon the magnitude of the elements in variable x.
 *
 * @author Peter Abeles
 */
public class NumericalJacobianForward implements FunctionNtoMxN
{
	// number of input variables
	private final int N;
	// number of functions
	private final int M;
	
	// function being differentiated
	private FunctionNtoM function;

	// scaling of the difference parameter
	private double differenceScale;

	private double output0[];
	private double output1[];

	public NumericalJacobianForward(FunctionNtoM function, double differenceScale) {
		this.function = function;
		this.differenceScale = differenceScale;
		this.N = function.getN();
		this.M = function.getM();
		output0 = new double[M];
		output1 = new double[M];
	}

	public NumericalJacobianForward(FunctionNtoM function) {
		this(function,Math.sqrt(UtilEjml.EPS));
	}

	@Override
	public int getN() {
		return N;
	}

	@Override
	public int getM() {
		return M;
	}

	@Override
	public void process(double[] input, double[] jacobian) {
		DenseMatrix64F J = DenseMatrix64F.wrap(M,N,jacobian);
		
		function.process(input,output0);
		
		for( int i = 0; i < N; i++ ) {
			double x = input[i];
			double h = x != 0 ? differenceScale*Math.abs(x) : differenceScale;

			// takes in account round off error
			double temp = x+h;
			h = temp-x;

			input[i] = temp;
			function.process(input,output1);
			for( int j = 0; j < M; j++ ) {
				J.unsafe_set(j,i,(output1[j] - output0[j])/h);
			}
			input[i] = x;
		}
	}
}
