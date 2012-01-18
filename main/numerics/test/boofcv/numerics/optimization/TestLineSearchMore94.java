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

/**
 * @author Peter Abeles
 */
public class TestLineSearchMore94 {

	/**
	 * Give it a very simple function and see if it finds the minimum approximately.  More
	 * robustness and correctness tests are found in benchmark directory.
	 */
	@Test
	public void checkBasic() {
//		double expected = 10;
//		FunctionStoS f = new TrivialCubicStoS(expected);
//		FunctionStoS d = new TrivialCubicDerivStoS(expected);
//
//		// the initial value should pass all the tests with this setting
//		LineSearch alg = new LineSearchMore94(0.1,0.9,0,1.1,7.0/12.0,0.66,50);
//		alg.setFunction(f,d);
//
//		double valueZero = f.process(0);
//		double derivZero = d.process(0);
//		double initValue = f.process(1);
//
//		double foundLoose = alg.search(valueZero,derivZero,initValue,1);
//
//		// now try it with tighter bounds
//		alg = new LineSearchMore94(1e-4,0.1,0,1.1,7.0/12.0,0.66,50);
//		alg.setFunction(f,d);
//		double foundTight = alg.search(valueZero, derivZero, initValue, 1);
//
//		// see if the tighter bounds is more accurate
//		assertTrue(Math.abs(foundTight - expected) < Math.abs(foundLoose - expected));
//
//		// since it is a quadratic function it should find a perfect solution too
//		assertEquals(expected, foundTight, 1e-8);
	}
}
