/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.qrcode;

import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestGaliosFieldOps extends BoofStandardJUnit {

	/**
	 * Test addition and subtraction via their properties
	 */
	@Test void add_subtract() {
		for (int i = 0; i < 100; i++) {
			int a = 1 + rand.nextInt(255);
			int b = 1 + rand.nextInt(255);

			int c = GaliosFieldOps.add(a, b);
			int d = GaliosFieldOps.subtract(c, b);

			assertEquals(a, d);
			assertNotEquals(a, c);
			assertNotEquals(b, c);

			// adding and subtracting are the same on GF(2)
			assertEquals(d, GaliosFieldOps.add(c, b));
		}
	}

	@Test void multiply() {
		for (int i = 0; i < 100; i++) {
			int a = 1 + rand.nextInt(255);
			int b = 1 + rand.nextInt(255);

			int c = GaliosFieldOps.multiply(a, b);

			if (b != 1)
				assertNotEquals(a, c);
			else
				assertEquals(a, c);

			if (a != 1)
				assertNotEquals(b, c);
			else
				assertEquals(b, c);

			assertTrue(c >= a);
			assertTrue(c >= b);

			// communative
			assertEquals(c, GaliosFieldOps.multiply(b, a));

			// can't test via addition since this multiplication operation is actually not a field due to the lack
			// of modulus operation
		}
	}

	@Test void multiply_field() {
		for (int i = 0; i < 100; i++) {
			int a = 1 + rand.nextInt(255);
			int b = 1 + rand.nextInt(255);

			int expected = GaliosFieldOps.multiply(a, b);
			expected = GaliosFieldOps.modulus(expected, 0b100011101);
			int result = GaliosFieldOps.multiply(a, b, 0b100011101, 256);
			assertEquals(expected, result);
		}
	}

	@Test void divide_modulus() {
		for (int i = 0; i < 100; i++) {
			int a = 1 + rand.nextInt(255);
			int b = 1 + rand.nextInt(255);

			int n = GaliosFieldOps.divide(a, b);
			int r = GaliosFieldOps.modulus(a, b);

			if (n == 0) {
				assertTrue(GaliosFieldOps.length(a) < GaliosFieldOps.length(b));
				assertEquals(a, r);
			} else {
//				System.out.printf("%8s / %8s = %8s*b + %8s\n",
//						Integer.toBinaryString(a),Integer.toBinaryString(b),n,Integer.toBinaryString(r));
				assertTrue(GaliosFieldOps.length(a) >= GaliosFieldOps.length(b));
				int found = GaliosFieldOps.multiply(n, b) ^ r;
				assertEquals(a, found);
			}
		}
	}

	@Test void length() {
		assertEquals(0, GaliosFieldOps.length(0b00000));
		assertEquals(1, GaliosFieldOps.length(0b00001));
		assertEquals(3, GaliosFieldOps.length(0b00100));
		assertEquals(3, GaliosFieldOps.length(0b00111));
	}
}
