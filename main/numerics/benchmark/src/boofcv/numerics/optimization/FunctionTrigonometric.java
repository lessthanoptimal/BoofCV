/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
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

package boofcv.numerics.optimization;

/**
 * <p>
 * Test function for non-linear functions. n variables and m=n functions.
 * x0=[1/n,...,1/n)
 * f=0
 * </p>
 *
 * <p>
 * [1] J. More, B. Garbow, K. Hillstrom, "Testing Unconstrained Optimization Software"
 * 1981 ACM Transactions on Mathematical Software, Vol 7, No. 1, Match 1981, pages 17-41
 * </p>
 * 
 * @author Peter Abeles
 */
public class FunctionTrigonometric implements OptimizationFunction<Object>{
	
	double x[];

	public FunctionTrigonometric(int N) {
		this.x = new double[N];
	}

	@Override
	public void setModel(double[] model) {
		System.arraycopy(model,0,x,0,x.length);
	}

	@Override
	public int getNumberOfFunctions() {
		return x.length;
	}

	@Override
	public int getModelSize() {
		return x.length;
	}

	@Override
	public boolean estimate(Object o, double[] estimated) {
		for( int i = 0; i < x.length; i++ ) {
			estimated[i] = F(i);
		}

		return true;
	}
	
	public double F( int degree ) {
		double total = x.length;
		for( int i = 0; i < x.length; i++ ) {
			total -= Math.cos(x[i]);
		}

		total += (degree+1)*(1-Math.cos(x[degree])) - Math.sin(x[degree]);

		return total;
	}
}
