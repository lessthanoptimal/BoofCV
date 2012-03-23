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

import boofcv.numerics.optimization.EvaluateQuasiNewtonBFGS;
import boofcv.numerics.optimization.LineSearch;
import boofcv.numerics.optimization.NonlinearResults;
import boofcv.numerics.optimization.functions.FunctionNtoS;
import boofcv.numerics.optimization.functions.GradientLineFunction;
import boofcv.numerics.optimization.wrap.CachedNumericalGradientLineFunction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestQuasiNewtonBFGS {

	EvaluateQuasiNewtonBFGS evaluator = new EvaluateQuasiNewtonBFGS(false,false);

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
		GradientLineFunction f = new CachedNumericalGradientLineFunction(function);

		QuasiNewtonBFGS alg = new QuasiNewtonBFGS(f,lineSearch,0);
		alg.setConvergence(1e-7,1e-7,gtol);
		return alg;
	}

	@Test
	public void powell() {
		NonlinearResults results = evaluator.powell();

		// no algorithm to compare it against, just do some sanity checks for changes
		assertTrue(results.numFunction<300);
		assertTrue(results.numGradient < 300);

		// The function is degenerate, this test sees if it converges to a solution and improves
		// the parameter values.  It isn't very precise
//		assertEquals(0,results.x[0],1e-4);
//		assertEquals(0,results.x[1],1e-4);
		assertEquals(0,results.f,1e-4);
	}

	@Test
	public void helicalvalley() {
		NonlinearResults results = evaluator.helicalValley();

		// no algorithm to compare it against, just do some sanity checks for changes
		assertTrue(results.numFunction<100);
		assertTrue(results.numGradient<100);
		assertEquals(1,results.x[0],1e-4);
		assertEquals(0,results.x[1],1e-4);
		assertEquals(0,results.x[2],1e-4);
		assertEquals(0,results.f,1e-4);
	}

	@Test
	public void rosenbrock() {
		NonlinearResults results = evaluator.rosenbrock();

		// no algorithm to compare it against, just do some sanity checks for changes
		assertTrue(results.numFunction<100);
		assertTrue(results.numGradient<100);
		assertEquals(1,results.x[0],1e-4);
		assertEquals(1,results.x[1],1e-4);
		assertEquals(0,results.f,1e-4);
	}

	@Test
	public void badlyScaledBrown() {
		NonlinearResults results = evaluator.badlyScaledBrown();

		// no algorithm to compare it against, just do some sanity checks for changes
		assertTrue(results.numFunction<100);
		assertTrue(results.numGradient<100);
		assertEquals(1e6,results.x[0],1e-4);
		assertEquals(2e-6,results.x[1],1e-4);
		assertEquals(0,results.f,1e-4);
	}

	@Test
	public void trigonometric() {
		NonlinearResults results = evaluator.trigonometric();

		// no algorithm to compare it against, just do some sanity checks for changes
		assertTrue(results.numFunction<100);
		assertTrue(results.numGradient < 100);
		assertEquals(0,results.f,1e-4);
	}
}
