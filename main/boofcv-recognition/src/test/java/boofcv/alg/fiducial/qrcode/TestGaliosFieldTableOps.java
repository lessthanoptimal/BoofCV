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

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

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
}