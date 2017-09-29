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
import static org.junit.Assert.fail;

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
		assertEquals(output0.size,output1.size);

		for (int i = 0; i < output0.size; i++) {
			assertEquals(output0.get(i),output1.get(i));
		}

		// compare to hand computed solution
		assertEquals(0xA0,output0.data[0]&0xFF);
		assertEquals(0x12,output0.data[1]&0xFF);
		assertEquals(0x54,output0.data[2]&0xFF);
		assertEquals(0xFF^0x45,output0.data[3]&0xFF);
	}

	@Test
	public void polyMult() {
		fail("Implement");
	}

	@Test
	public void polyEval() {
		fail("Implement");
	}

	@Test
	public void polyDivide() {
		fail("Implement");
	}

}