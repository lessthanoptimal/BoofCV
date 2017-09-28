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

/**
 * Precomputed look up table for performing operations on GF polynomials of the specified degree.
 *
 * @author Peter Abeles
 */
public class GaliosFieldTableOps {
	int max_value; // maximum possible
	int num_values; // number of values in the field
	int numBits;
	int primitive;

	int exp[];
	int log[];

	/**
	 * Specifies the GF polynomial
	 *
	 * @param numBits Number of bits needed to describe the polynomial. GF(2**8) = 8 bits
	 * @param primitive The primitive polynomial
	 */
	public GaliosFieldTableOps( int numBits , int primitive) {
		if( numBits < 1 || numBits > 16 )
			throw new IllegalArgumentException("Degree must be more than 1 and less than or equal to 16");

		this.numBits = numBits;
		this.primitive = primitive;
		max_value = 0;
		for (int i = 0; i < numBits; i++) {
			max_value |= 1<<i;
		}
		num_values = max_value+1;

		log = new int[ num_values ];
		exp = new int[ num_values*2 ]; // make it twice as long to avoid a modulus operation

		// exhaustively compute all values
		int x = 1;
		for (int i = 0; i < max_value; i++) {
			exp[i] = x;
			log[x] = i;
			x = GaliosFieldOps.multiply(x,2,primitive,num_values);
		}

		for (int i = 0; i < num_values; i++) {
			exp[i+max_value] = exp[i];
		}
	}

	/**
	 * Computes the following (x*y) mod primitive. This is done by
	 */
	public int multiply(int x , int y ) {
		if( x==0 || y == 0)
			return 0;
		return exp[ log[x] + log[y]];
	}


	/**
	 * Computes the following the value of output such that:<br>
	 * <p>divide(multiply(x,y),y)==x for any x and any nonzero y.</p>
	 */
	public int divide(int x , int y ) {
		if( y == 0 )
			throw new ArithmeticException("Divide by zero");
		if( x == 0 )
			return 0;

		return exp[ log[x] + max_value - log[y] ];
	}

	/**
	 * Computes the following x**power mod primitive
	 */
	public int power(int x , int power ) {
		return exp[(log[x] * power)%max_value];
	}

	/**
	 * Computes the following 2**(max-x) mod primitive
	 */
	public int inverse(int x ) {
		return exp[max_value - log[x]];
	}

	public int polyScale( int p , int x ) {
		return 0;
	}

	public int polyAdd( int p , int x ) {
		return 0;
	}

	public int polyMult( int p , int x ) {
		return 0;
	}

	public int polyEval( int p , int x ) {
		return 0;
	}

}
