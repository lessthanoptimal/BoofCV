/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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
import org.ddogleg.struct.DogArray_I16;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestGaliosFieldTableOps_U16 extends BoofStandardJUnit {
	int primitive12 = 0b1000001101001;

	@Test void polyScale() {
		var alg = new GaliosFieldTableOps_U16(12, primitive12);

		DogArray_I16 input = createArbitraryPolynomial();

		int scale = 0x45;

		var output = new DogArray_I16();

		alg.polyScale(input.copy(), scale, output);

		assertEquals(input.size, output.size);

		for (int i = 0; i < input.size; i++) {
			int expected = alg.multiply(input.data[i] & 0xFFFF, scale);
			assertEquals(expected, output.data[i] & 0xFFFF);
		}
	}

	@Test void polyAdd() {
		var alg = new GaliosFieldTableOps_U16(12, primitive12);

		DogArray_I16 inputA = createArbitraryPolynomial();

		// Create an arbitrary polynomial: 0xA0*x^3 + 0x45
		var inputB = new DogArray_I16(4);
		inputB.resize(4);
		inputB.set(0, 0xA0);
		inputB.set(3, 0x45);

		// make sure the order doesn't matter
		var output0 = new DogArray_I16();
		alg.polyAdd(inputA, inputB, output0);

		var output1 = new DogArray_I16();
		alg.polyAdd(inputB, inputA, output1);

		assertEquals(4, output0.size);
		assertEqualsG(output0, output1);

		// compare to hand computed solution
		assertEquals(0xA0, output0.data[0] & 0xFFFF);
		assertEquals(0x121, output0.data[1] & 0xFFFF);
		assertEquals(0x54, output0.data[2] & 0xFFFF);
		assertEquals(0xFFF ^ 0x45, output0.data[3] & 0xFFFF);
	}

	@Test void polyAdd_S() {
		var alg = new GaliosFieldTableOps_U16(12, primitive12);

		// Create an arbitrary polynomial: 0x12*x^2 + 0x54*x + 0xFFFF
		var inputA = new DogArray_I16(3);
		inputA.resize(3);
		inputA.set(2, 0x12);
		inputA.set(1, 0x54);
		inputA.set(0, 0xFFFF);

		// Create an arbitrary polynomial: 0xA0*x^3 + 0x45
		var inputB = new DogArray_I16(4);
		inputB.resize(4);
		inputB.set(3, 0xA0);
		inputB.set(0, 0x45);

		// make sure the order doesn't matter
		var output0 = new DogArray_I16();
		alg.polyAdd_S(inputA, inputB, output0);

		var output1 = new DogArray_I16();
		alg.polyAdd_S(inputB, inputA, output1);

		assertEquals(4, output0.size);
		assertEqualsG_S(output0, output1);

		// compare to hand computed solution
		assertEquals(0xA0, output0.data[3] & 0xFFFF);
		assertEquals(0x12, output0.data[2] & 0xFFFF);
		assertEquals(0x54, output0.data[1] & 0xFFFF);
		assertEquals(0xFFFF ^ 0x45, output0.data[0] & 0xFFFF);
	}

	@Test void polyAddScaleB() {
		var alg = new GaliosFieldTableOps_U16(12, primitive12);

		DogArray_I16 inputA = createArbitraryPolynomial();

		// Create an arbitrary polynomial: 0xA0*x^3 + 0x45
		var inputB = new DogArray_I16(4);
		inputB.resize(4);
		inputB.set(0, 0xA0);
		inputB.set(3, 0x45);

		int scale = 0x62;
		DogArray_I16 scaleB = new DogArray_I16();
		alg.polyScale(inputB, scale, scaleB);
		DogArray_I16 expected = new DogArray_I16();
		alg.polyAdd(inputA, scaleB, expected);

		var found = new DogArray_I16();
		alg.polyAddScaleB(inputA, inputB, scale, found);

		assertEqualsG(expected, found);
	}

	@Test void polyMult() {
		var alg = new GaliosFieldTableOps_U16(12, primitive12);

		DogArray_I16 inputA = createArbitraryPolynomial();

		var inputB = new DogArray_I16();
		inputB.resize(2);
		inputB.set(1, 0x03);

		// make sure the order doesn't matter
		var output0 = new DogArray_I16();
		alg.polyMult(inputA, inputB, output0);

		var output1 = new DogArray_I16();
		alg.polyMult(inputB, inputA, output1);

		assertEquals(4, output0.size);
		assertEqualsG(output0, output1);

		// check the value against a manual solution
		DogArray_I16 expected = new DogArray_I16();
		expected.resize(4);
		expected.set(1, alg.multiply(0x121, 0x03));
		expected.set(2, alg.multiply(0x54, 0x03));
		expected.set(3, alg.multiply(0xFFF, 0x03));
	}

	@Test void polyMult_flipA() {
		var alg = new GaliosFieldTableOps_U16(12, primitive12);

		// Create an arbitrary polynomial: 0x121*x^2 + 0x54*x + 0xFFF
		var inputA = new DogArray_I16();
		inputA.resize(3);
		inputA.set(2, 0x121);
		inputA.set(1, 0x54);
		inputA.set(0, 0xFFF);

		var inputB = new DogArray_I16();
		inputB.resize(2);
		inputB.set(1, 0x03);

		var output0 = new DogArray_I16();
		alg.polyMult_flipA(inputA, inputB, output0);

		assertEquals(4, output0.size);

		// check the value against a manual solution
		DogArray_I16 expected = new DogArray_I16();
		expected.resize(4);
		expected.set(1, alg.multiply(0x121, 0x03));
		expected.set(2, alg.multiply(0x54, 0x03));
		expected.set(3, alg.multiply(0xFFF, 0x03));
	}

	@Test void polyMult_S() {
		var alg = new GaliosFieldTableOps_U16(12, primitive12);

		// Create an arbitrary polynomial: 0x121*x^2 + 0x54*x + 0xFFF
		var inputA = new DogArray_I16();
		inputA.resize(3);
		inputA.set(2, 0x121);
		inputA.set(1, 0x54);
		inputA.set(0, 0xFFF);

		var inputB = new DogArray_I16();
		inputB.resize(2);
		inputB.set(0, 0x03);

		// make sure the order doesn't matter
		var output0 = new DogArray_I16();
		alg.polyMult_S(inputA, inputB, output0);

		var output1 = new DogArray_I16();
		alg.polyMult_S(inputB, inputA, output1);

		assertEquals(4, output0.size);
		assertEqualsG_S(output0, output1);

		// check the value against a manual solution
		DogArray_I16 expected = new DogArray_I16();
		expected.resize(4);
		expected.set(2, alg.multiply(0x121, 0x03));
		expected.set(1, alg.multiply(0x54, 0x03));
		expected.set(0, alg.multiply(0xFFF, 0x03));
	}

	private void randomPoly( DogArray_I16 inputA, int length ) {
		inputA.reset();
		for (int j = 0; j < length; j++) {
			inputA.add(rand.nextInt(256));
		}
	}

	@Test void polyEval() {
		var alg = new GaliosFieldTableOps_U16(12, primitive12);

		// Create an arbitrary polynomial: 0x121*x^2 + 0x54*x + 0xFFF
		var inputA = new DogArray_I16();
		inputA.resize(3);
		inputA.set(0, 0x121);
		inputA.set(1, 0x54);
		inputA.set(2, 0xFFF);

		int input = 0x09;
		int found = alg.polyEval(inputA, input);

		int expected = 0xFFF ^ alg.multiply(0x54, input);
		expected ^= alg.multiply(0x121, alg.multiply(input, input));

		assertEquals(expected, found);
	}

	@Test void polyEval_random() {
		var alg = new GaliosFieldTableOps_U16(12, primitive12);
		var inputA = new DogArray_I16();

		for (int i = 0; i < 1000; i++) {
			randomPoly(inputA, 30);

			int value = rand.nextInt(256);

			int found = alg.polyEval(inputA, value);
			assertTrue(found >= 0 && found < 4096);
		}
	}

	@Test void polyEval_S() {
		var alg = new GaliosFieldTableOps_U16(12, primitive12);

		// Create an arbitrary polynomial: 0x12*x^2 + 0x54*x + 0xFFF
		var inputA = new DogArray_I16();
		inputA.resize(3);
		inputA.set(2, 0x12);
		inputA.set(1, 0x54);
		inputA.set(0, 0xFFFF);

		int input = 0x09;
		int found = alg.polyEval_S(inputA, input);

		int expected = 0xFFFF ^ alg.multiply(0x54, input);
		expected ^= alg.multiply(0x12, alg.multiply(input, input));

		assertEquals(expected, found);
	}

	@Test void polyEvalContinue() {
		var alg = new GaliosFieldTableOps_U16(12, primitive12);

		DogArray_I16 polyA = new DogArray_I16();
		randomPoly(polyA, 30);

		int x = 0x09;
		int expected = alg.polyEval(polyA, x);

		var polyB = new DogArray_I16(10);
		polyB.resize(10);
		System.arraycopy(polyA.data, 20, polyB.data, 0, 10);
		polyA.size = 20;

		int found = alg.polyEval(polyA, x);
		found = alg.polyEvalContinue(found, polyB, x);

		assertEquals(expected, found);
	}

	@Test void polyDivide() {
		var alg = new GaliosFieldTableOps_U16(12, primitive12);

		// Create an arbitrary polynomial: 0BB*x^4 + 0x12*x^3 + 0x54*x^2 + 0*x + 0xFFFF
		var inputA = new DogArray_I16();
		inputA.resize(5);
		inputA.set(0, 0xBB);
		inputA.set(1, 0x12);
		inputA.set(2, 0x54);
		inputA.set(4, 0xFFFF);

		var inputB = new DogArray_I16();
		inputB.resize(2);
		inputB.set(0, 0xF0);
		inputB.set(1, 0x0A);

		var quotient = new DogArray_I16();
		var remainder = new DogArray_I16();
		alg.polyDivide(inputA, inputB, quotient, remainder);
		assertEquals(4, quotient.size);
		assertEquals(1, remainder.size);

		// see if division was done correct and reconstruct the original equation
		checkDivision(alg, inputA, inputB, quotient, remainder);

		// have the divisor be larger than the dividend
		alg.polyDivide(inputB, inputA, quotient, remainder);
		assertEquals(0, quotient.size);
		assertEquals(2, remainder.size);

		checkDivision(alg, inputB, inputA, quotient, remainder);
	}

	private void checkDivision( GaliosFieldTableOps_U16 alg, DogArray_I16 inputA, DogArray_I16 inputB, 
								DogArray_I16 quotient, DogArray_I16 remainder ) {
		var tmp = new DogArray_I16();
		var found = new DogArray_I16();
		alg.polyMult(inputB, quotient, tmp);
		alg.polyAdd(tmp, remainder, found);
		assertEqualsG(inputA, found);
	}

	@Test void polyDivide_S() {
		var alg = new GaliosFieldTableOps_U16(12, primitive12);

		// Create an arbitrary polynomial: 0BB*x^4 + 0x12*x^3 + 0x54*x^2 + 0*x + 0xFFFF
		var inputA = new DogArray_I16();
		inputA.resize(5);
		inputA.set(4, 0xBB);
		inputA.set(3, 0x12);
		inputA.set(2, 0x54);
		inputA.set(0, 0xFFFF);

		var inputB = new DogArray_I16();
		inputB.resize(2);
		inputB.set(1, 0xF0);
		inputB.set(0, 0x0A);

		var quotient = new DogArray_I16();
		var remainder = new DogArray_I16();
		alg.polyDivide_S(inputA, inputB, quotient, remainder);
		assertEquals(4, quotient.size);
		assertEquals(1, remainder.size);

		// see if division was done correct and reconstruct the original equation
		checkDivision_S(alg, inputA, inputB, quotient, remainder);

		// have the divisor be larger than the dividend
		alg.polyDivide_S(inputB, inputA, quotient, remainder);
		assertEquals(0, quotient.size);
		assertEquals(2, remainder.size);

		checkDivision_S(alg, inputB, inputA, quotient, remainder);
	}

	private void checkDivision_S( GaliosFieldTableOps_U16 alg, DogArray_I16 inputA, DogArray_I16 inputB, 
								  DogArray_I16 quotient, DogArray_I16 remainder ) {
		var tmp = new DogArray_I16();
		var found = new DogArray_I16();
		alg.polyMult_S(inputB, quotient, tmp);
		alg.polyAdd_S(tmp, remainder, found);
		assertEqualsG_S(inputA, found);
	}

	private static void assertEqualsG( DogArray_I16 inputA, DogArray_I16 inputB ) {
		int offsetA = 0, offsetB = 0;
		if (inputA.size > inputB.size) {
			offsetA = inputA.size - inputB.size;
		} else {
			offsetB = inputB.size - inputA.size;
		}
		for (int i = 0; i < offsetA; i++) {
			assertEquals(0, inputA.data[i]);
		}
		for (int i = 0; i < offsetB; i++) {
			assertEquals(0, inputB.data[i]);
		}

		int N = Math.min(inputA.size, inputB.size);
		for (int i = 0; i < N; i++) {
			assertEquals(inputA.get(i + offsetA), inputB.get(i + offsetB));
		}
	}

	private static void assertEqualsG_S( DogArray_I16 inputA, DogArray_I16 inputB ) {
		int M = Math.min(inputA.size, inputB.size);

		for (int i = M; i < inputA.size; i++) {
			assertEquals(0, inputA.data[i]);
		}
		for (int i = M; i < inputB.size; i++) {
			assertEquals(0, inputB.data[i]);
		}
		for (int i = 0; i < M; i++) {
			assertEquals(inputA.get(i), inputB.get(i));
		}
	}

	private DogArray_I16 createArbitraryPolynomial() {
		// Create an arbitrary polynomial: 0x121*x^2 + 0x54*x + 0xFFF
		var input = new DogArray_I16(3);
		input.resize(3);
		input.set(0, 0x121);
		input.set(1, 0x54);
		input.set(2, 0xFFF);
		return input;
	}
}
