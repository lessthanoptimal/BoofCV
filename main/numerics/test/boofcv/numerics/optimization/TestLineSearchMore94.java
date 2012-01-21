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

package boofcv.numerics.optimization;

import org.junit.Test;

import java.util.List;

import static boofcv.numerics.optimization.LineSearchEvaluator.Results;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The first test is a very basic test to see if it can minimize a simple problem. 
 * The remaining tests compare it against test problems with known results from
 * More and Thuente's paper. These tests validate that this class produces the 
 * exact same output as the MINPACK-2 Fortran code.  If the algorithm is improved 
 * then these tests are invalid.
 * 
 * @author Peter Abeles
 */
public class TestLineSearchMore94 {

	EvaluateLineSearchMore94 eval = new EvaluateLineSearchMore94(false);
	
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
		LineSearch alg = new LineSearchMore94(0.0001,0.1,0.001,1e-8,30);
		alg.setFunction(f,d);

		double valueZero = f.process(0);
		double derivZero = d.process(0);
		double initValue = f.process(1);

		alg.init(valueZero,derivZero,initValue,1);
		assertTrue(UtilOptimize.process(alg, 50));
		double foundLoose = alg.getStep();

		// now try it with tighter bounds
		alg = new LineSearchMore94(0.00001,0.000001,0.001,1e-8,30);
		alg.setFunction(f,d);
		alg.init(valueZero,derivZero,initValue,1);
		assertTrue(UtilOptimize.process(alg, 50));
		assertTrue(alg.getWarning()==null);
		double foundTight = alg.getStep();

		// see if the tighter bounds is more accurate
		assertTrue(Math.abs(foundTight - expected) < Math.abs(foundLoose - expected));

		// since it is a quadratic function it should find a perfect solution too
		assertEquals(expected, foundTight, 1e-5);
	}

	@Test
	public void compareMore1() {
		List<Results> results = eval.more1();

		testResults(results.get(0),6,1.365);
		testResults(results.get(1),3,1.4413720790892741);
		testResults(results.get(2),1,10.0);
		testResults(results.get(3),4,36.88760696396662);
	}

	@Test
	public void compareMore2() {
		List<Results> results = eval.more2();

		testResults(results.get(0),12,1.596);
		testResults(results.get(1),8,1.596);
		testResults(results.get(2),8,1.596);
		testResults(results.get(3),11,1.596);
	}

	@Test
	public void compareMore3() {
		List<Results> results = eval.more3();

		testResults(results.get(0),12,1);
		testResults(results.get(1),12,1);
		testResults(results.get(2),10,1);
		testResults(results.get(3),13,1);
	}

	@Test
	public void compareMore4() {
		List<Results> results = eval.more4();

		testResults(results.get(0),4,0.085);
		testResults(results.get(1),1,0.1);
		testResults(results.get(2),3,0.34910461641724727);
		testResults(results.get(3),4,0.8294012431694555);
	}

	@Test
	public void compareMore5() {
		List<Results> results = eval.more5();

		testResults(results.get(0),6,0.0750108706000682);
		testResults(results.get(1),3,0.07751042197802416);
		testResults(results.get(2),7,0.07314201106899357);
		testResults(results.get(3),8,0.07615927320140908);
	}

	@Test
	public void compareMore6() {
		List<Results> results = eval.more6();

		testResults(results.get(0),13,0.9279032286385813);
		testResults(results.get(1),11,0.9261500138380063);
		testResults(results.get(2),8,0.9247816734322059);
		testResults(results.get(3),11,0.9243979067550705);
	}
	
	public static void testResults( Results r , int n , double step ) {
		assertEquals(r.numIterations,n);
		assertEquals(r.x,step,1e-2);
	}
}
