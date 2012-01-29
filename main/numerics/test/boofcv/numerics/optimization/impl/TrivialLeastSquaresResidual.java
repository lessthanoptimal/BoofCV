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

package boofcv.numerics.optimization.impl;

import boofcv.numerics.optimization.functions.FunctionNtoM;

/**
 * @author Peter Abeles
 */
public class TrivialLeastSquaresResidual implements FunctionNtoM {
	
	double f[] = new double[3];

	public TrivialLeastSquaresResidual( double a, double b) {
		for( int i = 0; i < 3; i++ ) {
			f[i] = model(a,b,i);
		}
	}

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
		
		double a=input[0];
		double b=input[1];

		for( int i = 0; i < 3; i++ ) {
			output[i] = f[i] - model(a,b,i);
		}
	}
	
	private double model( double a , double b , int func )
	{
		switch( func ) {
			case 0:
				return a;

			case 1:
				return b;

			default:
				return b*b;

		}
	}
}
