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

import boofcv.numerics.optimization.FunctionNtoN;
import boofcv.numerics.optimization.FunctionNtoS;
import boofcv.numerics.optimization.FunctionStoS;
import boofcv.numerics.optimization.LineSearch;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestQuasiNewtonBFGS {

	/**
	 * Basic test that is easily solved.
	 */
	@Test
	public void basicTest() {
		QuasiNewtonBFGS alg = createAlg(new TrivialFunctionNtoS());
		
		alg.initialize(new double[]{1,1,1});

		int i = 0;
		for( ; i < 200 && !alg.iterate(); i++ ){}

		assertTrue(alg.isConverged());

		double[] found = alg.getParameters();

		assertEquals(0,found[0],1e-4);
		assertEquals(0,found[1],1e-4);
		assertEquals(1,found[2],1e-4);  // no change expected in last parameter
	}

	public QuasiNewtonBFGS createAlg( FunctionNtoS function ) {
		double gtol = 0.9;
		LineSearch lineSearch = new LineSearchMore94(1e-3,gtol,0.1);
		FunctionNtoN gradient = new NumericalGradientForward(function);
		LineStepFunction lineFunction = new LineStepFunction(function);
		FunctionStoS lineDerivative = new NumericalDerivativeForward(lineFunction);

		LineSearchManager line = new LineSearchManager(lineSearch,lineFunction,lineDerivative,0,gtol);

		return new QuasiNewtonBFGS(function,gradient,line,1e-7,1e-7);
	}
}
