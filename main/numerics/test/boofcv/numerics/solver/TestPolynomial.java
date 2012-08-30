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
import static org.junit.Assert.fail;

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
	public void degree() {
		fail("Implement");
	}

	@Test
	public void setTo() {
		fail("Implement");
	}

	@Test
	public void resize() {
		fail("Implement");
	}

	@Test
	public void identical() {
		fail("Implement");
	}
}
