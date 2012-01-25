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

import boofcv.numerics.optimization.LineSearch;
import boofcv.numerics.optimization.functions.FunctionNtoN;
import boofcv.numerics.optimization.functions.FunctionNtoS;
import boofcv.numerics.optimization.functions.LineSearchFunction;
import boofcv.numerics.optimization.wrap.CachedGradientLineFunction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestLineSearchManager {

	/**
	 * Have it search along a simple function then check to see if the output is as expected
	 */
	@Test
	public void basic() {
		LineSearch search = new LineSearchMore94(0.1,1e-3,1e-4);
		Gradient gradient = new Gradient();
		Function function = new Function();
		LineSearchFunction f = new CachedGradientLineFunction(function,gradient);
		
		LineSearchManager manager = new LineSearchManager(search,f,0,0.5);
		
		double[]start = new double[]{1,1};
		double[]direction = new double[]{-1,0};
		double[]g = new double[2];
		gradient.process(start,g);
		
		manager.initialize(function.process(start),start,g,direction,1,2);
		
		while( !manager.iterate() ){}
		
		double gdot = manager.getLineDerivativeAtZero();
		double step = manager.getStep();
		double fval = manager.getFStep();
		
		double []end = new double[]{1+step*direction[0],1};
		
		assertTrue(step>0);
		assertEquals(function.process(end),fval,1e-8);
		assertEquals(g[0]*direction[0],gdot,1e-8);
	}

	private static class Function implements FunctionNtoS
	{
		@Override
		public int getN() {
			return 2;
		}

		@Override
		public double process(double[] input) {
			double x1 = input[0];
			double x2 = input[1];

			return x1*x1 + x2*x2;
		}
	}

	private static class Gradient implements FunctionNtoN
	{
		@Override
		public int getN() {
			return 2;
		}

		@Override
		public void process(double[] input , double output[]) {
			double x1 = input[0];
			double x2 = input[1];

			output[0] = 2*x1;
			output[1] = 2*x2;
		}
	}
}
