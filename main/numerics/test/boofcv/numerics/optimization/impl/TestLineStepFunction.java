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
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestLineStepFunction {

	@Test
	public void basicTest() {
		double[] p0 = new double[]{1,2};
		double[] v = new double[]{0.5,-2.3};
		
		LineStepFunction alg = new LineStepFunction(new Function());
		
		alg.setLine(p0, v);
		
		double found = alg.process(2);
		double expected = function(new double[]{1+1,2-2*2.3});
		
		assertEquals(found,expected,1e-8);
	}
	
	public static double function( double param[] ) {
		return 2*param[0] + 3*param[1]*param[1];
	}

	public static class Function implements FunctionNtoS
	{

		@Override
		public int getN() {
			return 2;
		}

		@Override
		public double process(double[] input) {
			return function(input);
		}
	}
}
