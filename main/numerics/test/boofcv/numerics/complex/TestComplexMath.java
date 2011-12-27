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

package boofcv.numerics.complex;

import org.ejml.data.Complex64F;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestComplexMath {

	@Test
	public void plus() {
		Complex64F a = new Complex64F(2,3);
		Complex64F b = new Complex64F(-3,6);
		Complex64F c = new Complex64F();

		ComplexMath.plus(a,b,c);

		assertEquals(-1,c.real,1e-8);
		assertEquals(9,c.imaginary,1e-8);
	}

	@Test
	public void minus() {
		Complex64F a = new Complex64F(2,3);
		Complex64F b = new Complex64F(-3,6);
		Complex64F c = new Complex64F();

		ComplexMath.minus(a, b, c);

		assertEquals(5,c.real,1e-8);
		assertEquals(-3, c.imaginary, 1e-8);
	}

	@Test
	public void mult() {
		Complex64F a = new Complex64F(2,3);
		Complex64F b = new Complex64F(-3,6);
		Complex64F c = new Complex64F();

		ComplexMath.mult(a, b, c);

		assertEquals(-24,c.real,1e-8);
		assertEquals(3, c.imaginary, 1e-8);
	}

	@Test
	public void div() {
		Complex64F a = new Complex64F(2,3);
		Complex64F b = new Complex64F(-3,6);
		Complex64F c = new Complex64F();

		ComplexMath.div(a, b, c);

		assertEquals(0.26666666666,c.real,1e-8);
		assertEquals(-0.466666666666, c.imaginary, 1e-8);
	}

	/**
	 * Test conversion to and from polar form by doing just that and see if it gets the original answer again
	 */
	@Test
	public void convert() {
		Complex64F a = new Complex64F(2,3);
		ComplexPolar64F b = new ComplexPolar64F();
		Complex64F c = new Complex64F();

		ComplexMath.convert(a,b);
		ComplexMath.convert(b,c);

		assertEquals(a.real,c.real,1e-8);
		assertEquals(a.imaginary,c.imaginary,1e-8);
	}

	@Test
	public void mult_polar() {
		Complex64F a = new Complex64F(2,3);
		Complex64F b = new Complex64F(-3,6);
		Complex64F expected = new Complex64F();

		ComplexMath.mult(a, b, expected);

		ComplexPolar64F pa = new ComplexPolar64F(a);
		ComplexPolar64F pb = new ComplexPolar64F(b);
		ComplexPolar64F pc = new ComplexPolar64F();

		ComplexMath.mult(pa, pb, pc);

		Complex64F found = pc.toStandard();

		assertEquals(expected.real,found.real,1e-8);
		assertEquals(expected.imaginary, found.imaginary,1e-8);
	}

	@Test
	public void div_polar() {
		Complex64F a = new Complex64F(2,3);
		Complex64F b = new Complex64F(-3,6);
		Complex64F expected = new Complex64F();

		ComplexMath.div(a, b, expected);

		ComplexPolar64F pa = new ComplexPolar64F(a);
		ComplexPolar64F pb = new ComplexPolar64F(b);
		ComplexPolar64F pc = new ComplexPolar64F();

		ComplexMath.div(pa, pb, pc);

		Complex64F found = pc.toStandard();

		assertEquals(expected.real,found.real,1e-8);
		assertEquals(expected.imaginary, found.imaginary,1e-8);
	}

	@Test
	public void pow() {
		ComplexPolar64F a = new ComplexPolar64F(2,0.2);
		ComplexPolar64F expected = new ComplexPolar64F();
		ComplexPolar64F found = new ComplexPolar64F();

		ComplexMath.mult(a,a,expected);
		ComplexMath.mult(a,expected,expected);

		ComplexMath.pow(a,3,found);

		assertEquals(expected.r,found.r,1e-8);
		assertEquals(expected.theta, found.theta,1e-8);
	}

	@Test
	public void root_polar() {
		ComplexPolar64F expected = new ComplexPolar64F(2,0.2);
		ComplexPolar64F root = new ComplexPolar64F();
		ComplexPolar64F found = new ComplexPolar64F();

		// compute the square root of a complex number then see if the
		// roots equal the output
		for( int i = 0; i < 2; i++ ) {
			ComplexMath.root(expected ,2 , 0 , root);

			ComplexMath.mult(root,root,found);

			Complex64F e = expected.toStandard();
			Complex64F f = found.toStandard();

			assertEquals(e.real,f.real,1e-8);
			assertEquals(e.imaginary, f.imaginary,1e-8);
		}
	}

	@Test
	public void root_standard() {
		Complex64F expected = new Complex64F(2,0.2);
		Complex64F root = new Complex64F();
		Complex64F found = new Complex64F();

		// compute the square root of a complex number then see if the
		// roots equal the output
		for( int i = 0; i < 2; i++ ) {
			ComplexMath.root(expected ,2 , 0 , root);

			ComplexMath.mult(root,root,found);

			assertEquals(expected.real,found.real,1e-8);
			assertEquals(expected.imaginary, found.imaginary,1e-8);
		}
	}

}
