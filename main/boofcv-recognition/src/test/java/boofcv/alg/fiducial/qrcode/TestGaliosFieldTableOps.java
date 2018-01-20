/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import org.ddogleg.struct.GrowQueue_I8;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestGaliosFieldTableOps {
	Random rand = new Random(234);
	int primitive2 = 0b111;
	int primitive8 = 0b100011101;

	@Test
	public void constructor() {
		GaliosFieldTableOps alg = new GaliosFieldTableOps(2,primitive2);
		assertEquals(4,alg.num_values);
		assertEquals(3,alg.max_value);
		assertEquals(0b111,alg.primitive);
		checkTableSum(alg);

		alg = new GaliosFieldTableOps(8,primitive8);
		assertEquals(256,alg.num_values);
		assertEquals(255,alg.max_value);
		assertEquals(0b100011101,alg.primitive);
		checkTableSum(alg);
	}

	/**
	 * Test to see if each element in the table is unique
	 */
	private void checkTableSum( GaliosFieldTableOps alg ) {
		int expected = 0;
		for (int i = 0; i < alg.max_value; i++) {
			expected += i;
		}

		// Starts counting at 1. hence - max_value
		int sum = 0;
		for (int i = 0; i < alg.max_value; i++) {
			sum += alg.exp[i];
		}
		assertEquals(expected,sum-alg.max_value);

		// zero will be in the first two elements, hence up to num_values
		sum = 0;
		for (int i = 0; i < alg.num_values; i++) {
			sum += alg.log[i];
		}
		assertEquals(expected,sum);
	}

	@Test
	public void multiply() {
		multiply(2, primitive2);
		multiply(8, primitive8);
	}

	public void multiply( int numBits , int primitive ) {
		GaliosFieldTableOps alg =  new GaliosFieldTableOps(numBits,primitive);

		int num_values = alg.num_values;

		assertEquals(0,alg.multiply(0, 5));
		assertEquals(0,alg.multiply(5, 0));

		for (int i = 0; i < num_values; i++) {
			for (int j = 0; j < num_values; j++) {

				int expected = pow(i + j, primitive, num_values);

				int valA = pow(i, primitive, num_values);
				int valB = pow(j, primitive, num_values);

				int found = alg.multiply(valA, valB);

				assertEquals(expected, found);
			}
		}
	}

	private int pow( int n , int primitive ,int num_values) {
		int val = 1;
		for (int i = 0; i < n; i++) {
			val <<= 1;
			if( val >= num_values ) {
				val ^= primitive;
			}
		}
		return val;
	}

	@Test
	public void divide() {
		GaliosFieldTableOps alg =  new GaliosFieldTableOps(8, primitive8);

		for (int i = 0; i < 256; i++) {
			for (int j = 1; j < 256; j++) {
				int multAB = alg.multiply(i,j);
				int found = alg.divide(multAB,j);

				assertEquals(i,found);
			}
		}
	}

	@Test
	public void power() {
		GaliosFieldTableOps alg =  new GaliosFieldTableOps(8, primitive8);

		for (int i = 0; i < 100; i++) {
			int a = rand.nextInt(20);
			int b = rand.nextInt(20);

			int expected = pow(a*b, primitive8,256);

			int valA = pow(a, primitive8,256);
			int found = alg.power(valA,b);

			assertEquals(expected,found);
		}
	}

	@Test
	public void inverse() {
		GaliosFieldTableOps alg =  new GaliosFieldTableOps(8, primitive8);

		for (int i = 0; i < 100; i++) {
			int a = rand.nextInt(255)+1;

			int expected = alg.divide(1,a);
			int found = alg.inverse(a);

			assertEquals(expected,found);
		}
	}

	@Test
	public void polyScale() {

		GaliosFieldTableOps alg =  new GaliosFieldTableOps(8, primitive8);

		// Create an arbitrary polynomial: 0x12*x^2 + 0x54*x + 0xFF
		GrowQueue_I8 input = new GrowQueue_I8(3);
		input.set(0,0x12);
		input.set(1,0x54);
		input.set(2,0xFF);

		int scale = 0x45;

		GrowQueue_I8 output = new GrowQueue_I8();

		alg.polyScale(input.copy(),scale,output);

		assertEquals(input.size,output.size);

		for (int i = 0; i < input.size; i++) {
			int expected = alg.multiply(input.data[i]&0xFF, scale);
			assertEquals(expected,output.data[i]&0xFF);
		}
	}

	@Test
	public void polyAdd() {
		GaliosFieldTableOps alg =  new GaliosFieldTableOps(8, primitive8);

		// Create an arbitrary polynomial: 0x12*x^2 + 0x54*x + 0xFF
		GrowQueue_I8 inputA = new GrowQueue_I8(3);
		inputA.resize(3);
		inputA.set(0,0x12);
		inputA.set(1,0x54);
		inputA.set(2,0xFF);

		// Create an arbitrary polynomial: 0xA0*x^3 + 0x45
		GrowQueue_I8 inputB = new GrowQueue_I8(4);
		inputB.resize(4);
		inputB.set(0,0xA0);
		inputB.set(3,0x45);

		// make sure the order doesn't matter
		GrowQueue_I8 output0 = new GrowQueue_I8();
		alg.polyAdd(inputA,inputB,output0);

		GrowQueue_I8 output1 = new GrowQueue_I8();
		alg.polyAdd(inputB,inputA,output1);

		assertEquals(4,output0.size);
		assertEqualsG(output0, output1);

		// compare to hand computed solution
		assertEquals(0xA0,output0.data[0]&0xFF);
		assertEquals(0x12,output0.data[1]&0xFF);
		assertEquals(0x54,output0.data[2]&0xFF);
		assertEquals(0xFF^0x45,output0.data[3]&0xFF);
	}

	@Test
	public void polyAdd_S() {
		GaliosFieldTableOps alg =  new GaliosFieldTableOps(8, primitive8);

		// Create an arbitrary polynomial: 0x12*x^2 + 0x54*x + 0xFF
		GrowQueue_I8 inputA = new GrowQueue_I8(3);
		inputA.resize(3);
		inputA.set(2,0x12);
		inputA.set(1,0x54);
		inputA.set(0,0xFF);

		// Create an arbitrary polynomial: 0xA0*x^3 + 0x45
		GrowQueue_I8 inputB = new GrowQueue_I8(4);
		inputB.resize(4);
		inputB.set(3,0xA0);
		inputB.set(0,0x45);

		// make sure the order doesn't matter
		GrowQueue_I8 output0 = new GrowQueue_I8();
		alg.polyAdd_S(inputA,inputB,output0);

		GrowQueue_I8 output1 = new GrowQueue_I8();
		alg.polyAdd_S(inputB,inputA,output1);

		assertEquals(4,output0.size);
		assertEqualsG_S(output0, output1);

		// compare to hand computed solution
		assertEquals(0xA0,output0.data[3]&0xFF);
		assertEquals(0x12,output0.data[2]&0xFF);
		assertEquals(0x54,output0.data[1]&0xFF);
		assertEquals(0xFF^0x45,output0.data[0]&0xFF);
	}

	@Test
	public void polyAddScaleB() {
		GaliosFieldTableOps alg =  new GaliosFieldTableOps(8, primitive8);

		// Create an arbitrary polynomial: 0x12*x^2 + 0x54*x + 0xFF
		GrowQueue_I8 inputA = new GrowQueue_I8(3);
		inputA.resize(3);
		inputA.set(0,0x12);
		inputA.set(1,0x54);
		inputA.set(2,0xFF);

		// Create an arbitrary polynomial: 0xA0*x^3 + 0x45
		GrowQueue_I8 inputB = new GrowQueue_I8(4);
		inputB.resize(4);
		inputB.set(0,0xA0);
		inputB.set(3,0x45);

		int scale = 0x62;
		GrowQueue_I8 scaleB = new GrowQueue_I8();
		alg.polyScale(inputB,scale,scaleB);
		GrowQueue_I8 expected = new GrowQueue_I8();
		alg.polyAdd(inputA,scaleB,expected);

		GrowQueue_I8 found = new GrowQueue_I8();
		alg.polyAddScaleB(inputA,inputB,scale,found);

		assertEqualsG(expected, found);
	}

	@Test
	public void polyMult() {
		GaliosFieldTableOps alg =  new GaliosFieldTableOps(8, primitive8);

		// Create an arbitrary polynomial: 0x12*x^2 + 0x54*x + 0xFF
		GrowQueue_I8 inputA = new GrowQueue_I8();
		inputA.resize(3);
		inputA.set(0,0x12);
		inputA.set(1,0x54);
		inputA.set(2,0xFF);

		GrowQueue_I8 inputB = new GrowQueue_I8();
		inputB.resize(2);
		inputB.set(1,0x03);

		// make sure the order doesn't matter
		GrowQueue_I8 output0 = new GrowQueue_I8();
		alg.polyMult(inputA,inputB,output0);

		GrowQueue_I8 output1 = new GrowQueue_I8();
		alg.polyMult(inputB,inputA,output1);

		assertEquals(4,output0.size);
		assertEqualsG(output0, output1);

		// check the value against a manual solution
		GrowQueue_I8 expected = new GrowQueue_I8();
		expected.resize(4);
		expected.set(1,alg.multiply(0x12,0x03));
		expected.set(2,alg.multiply(0x54,0x03));
		expected.set(3,alg.multiply(0xFF,0x03));
	}

	@Test
	public void polyMult_flipA() {
		GaliosFieldTableOps alg =  new GaliosFieldTableOps(8, primitive8);

		// Create an arbitrary polynomial: 0x12*x^2 + 0x54*x + 0xFF
		GrowQueue_I8 inputA = new GrowQueue_I8();
		inputA.resize(3);
		inputA.set(2,0x12);
		inputA.set(1,0x54);
		inputA.set(0,0xFF);

		GrowQueue_I8 inputB = new GrowQueue_I8();
		inputB.resize(2);
		inputB.set(1,0x03);

		GrowQueue_I8 output0 = new GrowQueue_I8();
		alg.polyMult_flipA(inputA,inputB,output0);

		assertEquals(4,output0.size);

		// check the value against a manual solution
		GrowQueue_I8 expected = new GrowQueue_I8();
		expected.resize(4);
		expected.set(1,alg.multiply(0x12,0x03));
		expected.set(2,alg.multiply(0x54,0x03));
		expected.set(3,alg.multiply(0xFF,0x03));
	}

	@Test
	public void polyMult_S() {
		GaliosFieldTableOps alg =  new GaliosFieldTableOps(8, primitive8);

		// Create an arbitrary polynomial: 0x12*x^2 + 0x54*x + 0xFF
		GrowQueue_I8 inputA = new GrowQueue_I8();
		inputA.resize(3);
		inputA.set(2,0x12);
		inputA.set(1,0x54);
		inputA.set(0,0xFF);

		GrowQueue_I8 inputB = new GrowQueue_I8();
		inputB.resize(2);
		inputB.set(0,0x03);

		// make sure the order doesn't matter
		GrowQueue_I8 output0 = new GrowQueue_I8();
		alg.polyMult_S(inputA,inputB,output0);

		GrowQueue_I8 output1 = new GrowQueue_I8();
		alg.polyMult_S(inputB,inputA,output1);

		assertEquals(4,output0.size);
		assertEqualsG_S(output0, output1);

		// check the value against a manual solution
		GrowQueue_I8 expected = new GrowQueue_I8();
		expected.resize(4);
		expected.set(2,alg.multiply(0x12,0x03));
		expected.set(1,alg.multiply(0x54,0x03));
		expected.set(0,alg.multiply(0xFF,0x03));
	}

	private void randomPoly(GrowQueue_I8 inputA, int length) {
		inputA.reset();
		for (int j = 0; j < length; j++) {
			inputA.add( rand.nextInt(256));
		}
	}

	@Test
	public void polyEval() {
		GaliosFieldTableOps alg =  new GaliosFieldTableOps(8, primitive8);

		// Create an arbitrary polynomial: 0x12*x^2 + 0x54*x + 0xFF
		GrowQueue_I8 inputA = new GrowQueue_I8();
		inputA.resize(3);
		inputA.set(0,0x12);
		inputA.set(1,0x54);
		inputA.set(2,0xFF);

		int input = 0x09;
		int found = alg.polyEval(inputA,input);

		int expected = 0xFF ^ alg.multiply(0x54,input);
		expected ^= alg.multiply(0x12,alg.multiply(input,input));

		assertEquals(expected,found);
	}

	@Test
	public void polyEval_random() {
		GaliosFieldTableOps alg =  new GaliosFieldTableOps(8, primitive8);
		GrowQueue_I8 inputA = new GrowQueue_I8();

		for (int i = 0; i < 1000; i++) {
			randomPoly(inputA,30);

			int value = rand.nextInt(256);

			int found = alg.polyEval(inputA,value);
			assertTrue(found >= 0 && found < 256);
		}
	}

	@Test
	public void polyEval_S() {
		GaliosFieldTableOps alg =  new GaliosFieldTableOps(8, primitive8);

		// Create an arbitrary polynomial: 0x12*x^2 + 0x54*x + 0xFF
		GrowQueue_I8 inputA = new GrowQueue_I8();
		inputA.resize(3);
		inputA.set(2,0x12);
		inputA.set(1,0x54);
		inputA.set(0,0xFF);

		int input = 0x09;
		int found = alg.polyEval_S(inputA,input);

		int expected = 0xFF ^ alg.multiply(0x54,input);
		expected ^= alg.multiply(0x12,alg.multiply(input,input));

		assertEquals(expected,found);
	}

	@Test
	public void polyEvalContinue() {
		GaliosFieldTableOps alg =  new GaliosFieldTableOps(8, primitive8);

		GrowQueue_I8 polyA = new GrowQueue_I8();
		randomPoly(polyA,30);

		int x = 0x09;
		int expected = alg.polyEval(polyA,x);

		GrowQueue_I8 polyB = new GrowQueue_I8(10);
		polyB.resize(10);
		System.arraycopy(polyA.data,20,polyB.data,0,10);
		polyA.size = 20;

		int found = alg.polyEval(polyA,x);
		found = alg.polyEvalContinue(found,polyB,x);

		assertEquals(expected,found);
	}

	@Test
	public void polyDivide() {
		GaliosFieldTableOps alg =  new GaliosFieldTableOps(8, primitive8);

		// Create an arbitrary polynomial: 0BB*x^4 + 0x12*x^3 + 0x54*x^2 + 0*x + 0xFF
		GrowQueue_I8 inputA = new GrowQueue_I8();
		inputA.resize(5);
		inputA.set(0,0xBB);
		inputA.set(1,0x12);
		inputA.set(2,0x54);
		inputA.set(4,0xFF);

		GrowQueue_I8 inputB = new GrowQueue_I8();
		inputB.resize(2);
		inputB.set(0,0xF0);
		inputB.set(1,0x0A);

		GrowQueue_I8 quotient = new GrowQueue_I8();
		GrowQueue_I8 remainder = new GrowQueue_I8();
		alg.polyDivide(inputA,inputB,quotient,remainder);
		assertEquals(4,quotient.size);
		assertEquals(1,remainder.size);

		// see if division was done correct and reconstruct the original equation
		checkDivision(alg, inputA, inputB, quotient, remainder);

		// have the divisor be larger than the dividend
		alg.polyDivide(inputB,inputA,quotient,remainder);
		assertEquals(0,quotient.size);
		assertEquals(2,remainder.size);

		checkDivision(alg, inputB, inputA, quotient, remainder);
	}

	private void checkDivision(GaliosFieldTableOps alg, GrowQueue_I8 inputA, GrowQueue_I8 inputB, GrowQueue_I8 quotent, GrowQueue_I8 remainder) {
		GrowQueue_I8 tmp = new GrowQueue_I8();
		GrowQueue_I8 found = new GrowQueue_I8();
		alg.polyMult(inputB,quotent,tmp);
		alg.polyAdd(tmp,remainder,found);
		assertEqualsG(inputA, found);
	}

	@Test
	public void polyDivide_S() {
		GaliosFieldTableOps alg =  new GaliosFieldTableOps(8, primitive8);

		// Create an arbitrary polynomial: 0BB*x^4 + 0x12*x^3 + 0x54*x^2 + 0*x + 0xFF
		GrowQueue_I8 inputA = new GrowQueue_I8();
		inputA.resize(5);
		inputA.set(4,0xBB);
		inputA.set(3,0x12);
		inputA.set(2,0x54);
		inputA.set(0,0xFF);

		GrowQueue_I8 inputB = new GrowQueue_I8();
		inputB.resize(2);
		inputB.set(1,0xF0);
		inputB.set(0,0x0A);

		GrowQueue_I8 quotient = new GrowQueue_I8();
		GrowQueue_I8 remainder = new GrowQueue_I8();
		alg.polyDivide_S(inputA,inputB,quotient,remainder);
		assertEquals(4,quotient.size);
		assertEquals(1,remainder.size);

		// see if division was done correct and reconstruct the original equation
		checkDivision_S(alg, inputA, inputB, quotient, remainder);

		// have the divisor be larger than the dividend
		alg.polyDivide_S(inputB,inputA,quotient,remainder);
		assertEquals(0,quotient.size);
		assertEquals(2,remainder.size);

		checkDivision_S(alg, inputB, inputA, quotient, remainder);
	}

	private void checkDivision_S(GaliosFieldTableOps alg, GrowQueue_I8 inputA, GrowQueue_I8 inputB, GrowQueue_I8 quotent, GrowQueue_I8 remainder) {
		GrowQueue_I8 tmp = new GrowQueue_I8();
		GrowQueue_I8 found = new GrowQueue_I8();
		alg.polyMult_S(inputB,quotent,tmp);
		alg.polyAdd_S(tmp,remainder,found);
		assertEqualsG_S(inputA, found);
	}

	private static void assertEqualsG(GrowQueue_I8 inputA, GrowQueue_I8 inputB) {
		int offsetA=0,offsetB=0;
		if( inputA.size > inputB.size ) {
			offsetA = inputA.size-inputB.size;
		} else {
			offsetB = inputB.size-inputA.size;
		}
		for (int i = 0; i < offsetA; i++) {
			assertEquals(0,inputA.data[i]);
		}
		for (int i = 0; i < offsetB; i++) {
			assertEquals(0,inputB.data[i]);
		}

		int N = Math.min(inputA.size,inputB.size);
		for (int i = 0; i < N; i++) {
			assertEquals(inputA.get(i+offsetA),inputB.get(i+offsetB));
		}
	}

	private static void assertEqualsG_S(GrowQueue_I8 inputA, GrowQueue_I8 inputB) {
		int M = Math.min(inputA.size,inputB.size);

		for (int i = M; i < inputA.size; i++) {
			assertEquals(0,inputA.data[i]);
		}
		for (int i = M; i < inputB.size; i++) {
			assertEquals(0,inputB.data[i]);
		}
		for (int i = 0; i < M; i++) {
			assertEquals(inputA.get(i),inputB.get(i));
		}
	}

}