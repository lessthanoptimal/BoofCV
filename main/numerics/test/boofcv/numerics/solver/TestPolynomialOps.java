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

package boofcv.numerics.solver;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestPolynomialOps {

	@Test
	public void quadraticVertex() {
		double a = -0.5;
		double b = 2.0;

		double x = -b/(2*a);

		double found = PolynomialOps.quadraticVertex(a, b);

		assertEquals(x,found,1e-8);
	}

	@Test
	public void derivative() {
		Polynomial p = Polynomial.wrap(2,3,4,5);
		p.size = 2;

		Polynomial d = new Polynomial(10);

		PolynomialOps.derivative(p,d);
		assertTrue(d.isIdentical(Polynomial.wrap(3), 1e-8));

		p.size = 3;
		PolynomialOps.derivative(p,d);
		assertTrue(d.isIdentical(Polynomial.wrap(3,8),1e-8));
	}


	@Test
	public void refineRoot() {

	}


	@Test
	public void divide() {
		// numerator and denominator, intentionally add trailing zeros to skew things up
		Polynomial n = Polynomial.wrap(1,2,3,0);
		Polynomial d = Polynomial.wrap(-3,1,0);

		// quotient and remainder
		Polynomial q = new Polynomial(10);
		Polynomial r = new Polynomial(10);

		// expected solutions
		Polynomial expectedQ = Polynomial.wrap(11,3,0);
		Polynomial expectedR = Polynomial.wrap(34);

		PolynomialOps.divide(n,d,q,r);

		assertEquals(3,q.size);
		assertEquals(1,r.size);

		assertTrue(expectedQ.isIdentical(q,1e-8));
		assertTrue(expectedR.isIdentical(r,1e-8));
	}
}
