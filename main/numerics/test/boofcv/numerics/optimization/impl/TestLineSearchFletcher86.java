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

import boofcv.numerics.optimization.EvaluateLineSearchFletcher86;
import boofcv.numerics.optimization.LineSearchEvaluator;
import boofcv.numerics.optimization.functions.FunctionStoS;
import boofcv.numerics.optimization.wrap.WrapCoupledDerivative;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestLineSearchFletcher86 {

	EvaluateLineSearchFletcher86 eval = new EvaluateLineSearchFletcher86(false);
	
	/**
	 * Give it a very simple function and see if it finds the minimum approximately.  More
	 * robustness and correctness tests are found in benchmark directory.
	 */
	@Test
	public void checkBasic() {
		double expected = 10;
		FunctionStoS f = new TrivialQuadraticStoS(expected);
		FunctionStoS d = new TrivialQuadraticDerivStoS(expected);

		// the initial value should pass all the tests with this setting
		LineSearchFletcher86 alg = new LineSearchFletcher86(0.1,0.9,0,9,0.1,0.5);
		alg.setFunction(new WrapCoupledDerivative(f,d));

		double valueZero = f.process(0);
		double derivZero = d.process(0);
		double initValue = f.process(1);

		alg.init(valueZero,derivZero,initValue,1,0,100);
		assertTrue(UtilOptimize.process(alg, 50));
		double foundLoose = alg.getStep();

		// now try it with tighter bounds
		alg = new LineSearchFletcher86(1e-5,0.1,0,9,0.05,0.5);
		alg.setFunction(new WrapCoupledDerivative(f,d));
		alg.init(valueZero,derivZero,initValue,1,0,100);
		assertTrue(UtilOptimize.process(alg, 50));
		double foundTight = alg.getStep();

		// see if the tighter bounds is more accurate
		assertTrue(Math.abs(foundTight - expected) < Math.abs(foundLoose - expected));

		// since it is a quadratic function it should find a perfect solution too
		assertEquals(expected, foundTight, 1e-8);
	}

	@Test
	public void compareFletcher1() {
		List<LineSearchEvaluator.Results> results = eval.fletcher1();
		
		TestLineSearchMore94.testResults(results.get(0), 3, 0.16);
		TestLineSearchMore94.testResults(results.get(1), 3, 0.16);
	}
}
