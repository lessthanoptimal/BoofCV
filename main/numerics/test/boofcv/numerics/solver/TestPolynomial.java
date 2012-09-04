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

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestPolynomial {

	@Test
	public void evaluate() {
		assertEquals(5,Polynomial.wrap(1,2).evaluate(2),1e-8);
		assertEquals(5,Polynomial.wrap(1,2,0).evaluate(2),1e-8);
		assertEquals(10,Polynomial.wrap(0,1,2).evaluate(2),1e-8);
		assertEquals(0,Polynomial.wrap(0).evaluate(2),1e-8);

		assertEquals(0, Polynomial.wrap().evaluate(Double.POSITIVE_INFINITY), 1e-8);
		assertEquals(2, Polynomial.wrap(2).evaluate(Double.POSITIVE_INFINITY), 1e-8);
		assertEquals(Double.POSITIVE_INFINITY,Polynomial.wrap(2,3).evaluate(Double.POSITIVE_INFINITY),1e-8);
		assertEquals(Double.POSITIVE_INFINITY,Polynomial.wrap(2,3,5).evaluate(Double.POSITIVE_INFINITY),1e-8);
		assertEquals(Double.NEGATIVE_INFINITY,Polynomial.wrap(-2,-3).evaluate(Double.POSITIVE_INFINITY),1e-8);
		assertEquals(Double.NEGATIVE_INFINITY,Polynomial.wrap(-2,-3,-5).evaluate(Double.POSITIVE_INFINITY),1e-8);
		assertEquals(Double.NEGATIVE_INFINITY,Polynomial.wrap(2,3).evaluate(Double.NEGATIVE_INFINITY),1e-8);
		assertEquals(Double.POSITIVE_INFINITY,Polynomial.wrap(2,3,5).evaluate(Double.NEGATIVE_INFINITY),1e-8);
		assertEquals(Double.POSITIVE_INFINITY,Polynomial.wrap(-2,-3).evaluate(Double.NEGATIVE_INFINITY),1e-8);
		assertEquals(Double.NEGATIVE_INFINITY,Polynomial.wrap(-2,-3,-5).evaluate(Double.NEGATIVE_INFINITY),1e-8);
		assertEquals(Double.POSITIVE_INFINITY,Polynomial.wrap(2,3,0,0).evaluate(Double.POSITIVE_INFINITY),1e-8);
		assertEquals(Double.NEGATIVE_INFINITY,Polynomial.wrap(2,3,0,0).evaluate(Double.NEGATIVE_INFINITY),1e-8);

		// Make sure it's using size and not length
		Polynomial p = new Polynomial(10);
		p.size = 2;
		p.c[0] = 2;
		p.c[1] = 3;
		p.c[4] = 7;

		assertEquals(8,p.evaluate(2),1e-8);
	}

	@Test
	public void computeDegree() {
		assertEquals(-1,Polynomial.wrap().computeDegree());
		assertEquals(-1,Polynomial.wrap(0).computeDegree());
		assertEquals(-1,Polynomial.wrap(0,0).computeDegree());
		assertEquals(0,Polynomial.wrap(1).computeDegree());
		assertEquals(0,Polynomial.wrap(1,0).computeDegree());
		assertEquals(0,Polynomial.wrap(1,0,0).computeDegree());
		assertEquals(2,Polynomial.wrap(0,1,2).computeDegree());
		assertEquals(3,Polynomial.wrap(0,1,2,1e-15).computeDegree());
		assertEquals(2,Polynomial.wrap(-0,-1,-2,-0).computeDegree());
	}

	@Test
	public void setTo() {
		Polynomial a = new Polynomial(10);
		Polynomial b = Polynomial.wrap(1,2,3,4);

		a.setTo(b);

		assertEquals(b.size(),a.size());
		for( int i = 0; i < a.size(); i++ )
			assertEquals(a.c[i],b.c[i],1e-8);
	}

	@Test
	public void resize() {
		Polynomial a = new Polynomial(10);

		assertEquals(10,a.size());
		a.resize(5);
		assertEquals(5,a.size());
		a.resize(15);
		assertEquals(15,a.size());
		assertEquals(15,a.c.length);
	}

	@Test
	public void identical() {
		Polynomial a = Polynomial.wrap(0,1,2);

		assertTrue(a.isIdentical(Polynomial.wrap(0, 1, 2), 1e-8));
		assertTrue(a.isIdentical(Polynomial.wrap(0,1,2,0),1e-8));
		assertTrue(a.isIdentical(Polynomial.wrap(0,1,2,1e-10),1e-8));
		assertTrue(Polynomial.wrap(0,1,1e-20).isIdentical(Polynomial.wrap(0, 1, 1e-14), 1e-8));
		assertTrue(Polynomial.wrap(0,1,1e-20,0).isIdentical(Polynomial.wrap(0,1,0,1e-14),1e-8));

		assertFalse(a.isIdentical(Polynomial.wrap(0, 1, 3), 1e-8));
		assertFalse(a.isIdentical(Polynomial.wrap(0, 1, 2+1e-5), 1e-8));
		assertFalse(a.isIdentical(Polynomial.wrap(0, 1, 2,3), 1e-8));
	}

	@Test
	public void truncateZeros() {

		Polynomial a = Polynomial.wrap(0,1,2,0);
		Polynomial b = Polynomial.wrap(0,1,2,1e-15);
		Polynomial c = Polynomial.wrap(0,1,2);
		Polynomial d = Polynomial.wrap(0,1,2,-1);

		a.truncateZeros(1e-15);
		b.truncateZeros(1e-15);
		c.truncateZeros(1e-15);
		d.truncateZeros(1e-15);

		assertTrue(a.isIdentical(Polynomial.wrap(0, 1, 2), 1e-8));
		assertTrue(b.isIdentical(Polynomial.wrap(0, 1, 2), 1e-8));
		assertTrue(c.isIdentical(Polynomial.wrap(0, 1, 2), 1e-8));
		assertTrue(d.isIdentical(Polynomial.wrap(0, 1, 2,-1), 1e-8));
	}
}
