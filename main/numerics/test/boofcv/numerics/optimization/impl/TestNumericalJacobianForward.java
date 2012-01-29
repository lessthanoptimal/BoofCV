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
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestNumericalJacobianForward {

	@Test
	public void simple() {
		// give it a function where one variable does not effect the output
		// to make the test more interesting
		SimpleFunction f = new SimpleFunction();
		NumericalJacobianForward alg = new NumericalJacobianForward(f);

		double output[] = new double[]{1,1,1,1,1,1};
		alg.process(new double[]{2,3,7},output);

		assertEquals(3,output[0],1e-5);
		assertEquals(-36,output[1],1e-5);
		assertEquals(0,output[2],1e-5);

		assertEquals(3,output[3],1e-5);
		assertEquals(2,output[4],1e-5);
		assertEquals(1,output[5],1e-5);
	}

	private static class SimpleFunction implements FunctionNtoM
	{
		@Override
		public int getN() {
			return 3;
		}

		@Override
		public int getM() {
			return 2;
		}

		@Override
		public void process(double[] input, double output[]) {
			double x1 = input[0];
			double x2 = input[1];
			double x3 = input[2];

			output[0] = 3*x1 - 6*x2*x2;
			output[1] = x1*x2+x3;
		}
	}
}
