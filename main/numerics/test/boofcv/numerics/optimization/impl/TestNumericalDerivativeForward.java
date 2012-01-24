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

import boofcv.numerics.optimization.FunctionStoS;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestNumericalDerivativeForward {

	@Test
	public void simple() {
		// give it a function where one variable does not effect the output
		// to make the test more interesting
		SimpleFunction f = new SimpleFunction();
		NumericalDerivativeForward alg = new NumericalDerivativeForward(f);

		double output = alg.process(3);

		assertEquals(36,output,1e-5);
	}

	private static class SimpleFunction implements FunctionStoS
	{

		@Override
		public double process(double x) {


			return 6*x*x;
		}
	}
}
