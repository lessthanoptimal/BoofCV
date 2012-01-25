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

import boofcv.numerics.optimization.OptimizationFunction;

/**
 * <p>
 * Well known test function, should be easily solved by any robust solver.
 * </p>
 * 
 * <p>
 * [1] J. More, B. Garbow, K. Hillstrom, "Testing Unconstrained Optimization Software"
 * 1981 ACM Transactions on Mathematical Software, Vol 7, No. 1, Match 1981, pages 17-41
 * </p>
 * 
 * @author Peter Abeles
 */
public class FunctionRosenbrock implements OptimizationFunction<Object> {
	
	double x1,x2;
	
	@Override
	public void setModel(double[] model) {
		x1 = model[0];
		x2 = model[1];
	}

	@Override
	public int getNumberOfFunctions() {
		return 2;
	}

	@Override
	public int getModelSize() {
		return 2;
	}

	@Override
	public boolean estimate(Object o, double[] estimated) {

		estimated[0] = 10.0*(x2-x1*x1);
		estimated[1] = 1-x1;

		return true;
	}


}
