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

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestSearchInterpolate {

	@Test
	public void quadratic() {
		double a = 2;
		double b = -3;
		double c = 1;

		double alpha0 = 0.67;
		double f0 = quad(a,b,c,alpha0);
		double g0 = quadDeriv(a,b,alpha0);
		double alpha1 = 3.35;
		double f1 = quad(a,b,c,alpha1);
		
		double expected = -b/(2*a);
		double found = SearchInterpolate.quadratic(f0,g0,alpha0,f1,alpha1);
	
		assertEquals(expected,found,1e-8);
	}

	@Test
	public void quadratic2() {
		double a = 2;
		double b = -3;
		double c = 1;

		double alpha0 = 1;
		double g0 = quadDeriv(a,b,alpha0);
		double alpha1 = 3.35;
		double g1 = quadDeriv(a,b,alpha1);

		double expected = -b/(2*a);
		double found = SearchInterpolate.quadratic2(g0,alpha0,g1,alpha1);

		assertEquals(expected,found,1e-8);
	}
	
	private double quad( double a , double b , double c , double x ) {
		return a*x*x + b*x + c;
	}

	private double quadDeriv( double a , double b , double x ) {
		return 2*a*x + b;
	}

	@Test
	public void cubic() {
		double a = 2;
		double b = -3;
		double c = 1;
		double d = 3.5;

		double f0 = cubic(a, b, c, d, 0);
		double g0 = cubicDeriv(a, b, c, 0);
		double alpha1 = 3.35;
		double f1 = cubic(a, b, c, d, alpha1);
		double alpha2 = 1.5;
		double f2 = cubic(a,b,c,d,alpha2);

		double expected = (-b + Math.sqrt(b*b-3*a*c))/(3*a);
		double found = SearchInterpolate.cubic(f0, g0, f1, alpha1, f2, alpha2);

		assertEquals(expected,found,1e-8);
	}

	@Test
	public void cubic2() {
		double a = 2;
		double b = -3;
		double c = 1;
		double d = 3.5;

		double alpha0 = 1.5;
		double f0 = cubic(a, b, c, d, alpha0);
		double g0 = cubicDeriv(a, b, c, alpha0);
		double alpha1 = 2;
		double f1 = cubic(a, b, c, d, alpha1);
		double g1 = cubicDeriv(a, b, c, alpha1);

		double expected = (-b + Math.sqrt(b*b-3*a*c))/(3*a);
		double found = SearchInterpolate.cubic2(f0,g0,alpha0,f1,g1,alpha1);

		assertEquals(expected,found,1e-8);
	}

	/**
	 * Cubic safe with a well defined cubic
	 */
	@Test
	public void cubicSafe_nominal() {
		double a = 2;
		double b = -3;
		double c = 1;
		double d = 3.5;

		double alpha0 = 1.5;
		double f0 = cubic(a, b, c, d, alpha0);
		double g0 = cubicDeriv(a, b, c, alpha0);
		double alpha1 = 2;
		double f1 = cubic(a, b, c, d, alpha1);
		double g1 = cubicDeriv(a, b, c, alpha1);

		double expected = (-b + Math.sqrt(b*b-3*a*c))/(3*a);
		double found = SearchInterpolate.cubicSafe(f0, g0, alpha0, f1, g1, alpha1, 10, 100);

		assertEquals(expected,found,1e-8);
	}

	/**
	 * Give it a cubic that
	 */
	@Test
	public void cubicSafe_infinity() {
		double a = 2;
		double b = 0;
		double c = 1;
		double d = 3.5;

		double alpha0 = 1.5;
		double f0 = cubic(a, b, c, d, alpha0);
		double g0 = cubicDeriv(a, b, c, alpha0);
		double alpha1 = 2;
		double f1 = cubic(a, b, c, d, alpha1);
		double g1 = cubicDeriv(a, b, c, alpha1);

		double found = SearchInterpolate.cubicSafe(f0,g0,alpha0,f1,g1,alpha1,0.1,100);
		assertEquals(0.1,found,1e-8);

		found = SearchInterpolate.cubicSafe(f1,g1,alpha1,f0,g0,alpha0,0.1,100);
		assertEquals(100,found,1e-8);
	}

	private double cubic( double a , double b , double c ,double d , double x ) {
		return a*x*x*x + b*x*x + c*x + d;
	}

	private double cubicDeriv( double a , double b , double c , double x ) {
		return 3*a*x*x + 2*b*x + c;
	}
}
