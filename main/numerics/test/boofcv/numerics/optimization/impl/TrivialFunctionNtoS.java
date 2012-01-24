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

import boofcv.numerics.optimization.FunctionNtoS;

/**
 * Test function with a minimum at 0 and minimum value of 0.  One of the variables
 * is redundant.
 *
 * @author Peter Abeles
 */
public class TrivialFunctionNtoS implements FunctionNtoS {
	@Override
	public int getN() {
		return 3;
	}

	@Override
	public double process(double[] input) {
		double x1 = input[0];
		double x2 = input[1];

		return 3*x1*x1 + 6*x2*x2 + 0.000001*Math.pow(x1+x2,4);
	}
}
