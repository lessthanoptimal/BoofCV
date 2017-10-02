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

	/**
	 * Scales the polynomial.
	 *
	 * <p>Coeffients for largest powers are first, e.g. 2*x**3 + 8*x**2+1 = [2,8,0,1]</p>
	 *
	 * @param input Input polynomial.
	 * @param scale scale
	 * @param output Output polynomial.
	 */
	public void polyScale(GrowQueue_I8 input , int scale , GrowQueue_I8 output) {

		output.resize(input.size);

		for (int i = 0; i < input.size; i++) {
			output.data[i] = (byte)multiply(input.data[i]&0xFF, scale);
		}
	}

	/**
	 *
	 * <p>Coeffients for largest powers are first, e.g. 2*x**3 + 8*x**2+1 = [2,8,0,1]</p>
	 *
	 * @param polyA
	 * @param polyB
	 * @param output
	 */
	public void polyAdd(GrowQueue_I8 polyA , GrowQueue_I8 polyB , GrowQueue_I8 output ) {
		output.resize(Math.max(polyA.size,polyB.size));

		// compute offset that would align the smaller polynomial with the larger polynomial
		int offsetA = Math.max(0,polyB.size-polyA.size);
		int offsetB = Math.max(0,polyA.size-polyB.size);
		int N = output.size;

		for (int i = 0; i < offsetB; i++) {
			output.data[i] = polyA.data[i];
		}
		for (int i = 0; i < offsetA; i++) {
			output.data[i] = polyB.data[i];
		}
		for (int i = Math.max(offsetA,offsetB); i < N; i++) {
			output.data[i] = (byte)((polyA.data[i-offsetA]&0xFF) ^ (polyB.data[i-offsetB]&0xFF));
		}
	}


	/**
	 *
	 * <p>Coeffients for largest powers are first, e.g. 2*x**3 + 8*x**2+1 = [2,8,0,1]</p>
	 *
	 * @param polyA
	 * @param polyB
	 * @param output
	 */
	public void polyMult(GrowQueue_I8 polyA , GrowQueue_I8 polyB , GrowQueue_I8 output ) {

		// Lots of room for efficiency improvements in this function
		output.resize(polyA.size+polyB.size-1);
		output.zero();

		for (int j = 0; j < polyB.size; j++) {
			int vb = polyB.data[j]&0xFF;
			for (int i = 0; i < polyA.size; i++) {
				int va = polyA.data[i]&0xFF;
				output.data[i+j] ^= multiply(va,vb);
			}
		}
	}


	/**
	 *
	 * <p>Coeffients for largest powers are first, e.g. 2*x**3 + 8*x**2+1 = [2,8,0,1]</p>
	 *
	 * @param input
	 * @param x
	 * @return
	 */
	public int polyEval(GrowQueue_I8 input , int x ) {
		int y = input.data[0];

		for (int i = 1; i < input.size; i++) {
			y = multiply(y,x) ^ (input.data[i]&0xFF);
		}

		return y;
	}

	/**
	 * Performs polynomial division using a synthetic division algorithm.
	 *
	 * @param dividend (Input) Polynomial dividend
	 * @param divisor (Input) Polynomial divisor
	 * @param quotent (Output) Division's quotent
	 * @param remainder (Output) Divisions's remainder
	 */
	public void polyDivide(GrowQueue_I8 dividend , GrowQueue_I8 divisor ,
						   GrowQueue_I8 quotent, GrowQueue_I8 remainder ) {

		// handle special case
		if( divisor.size > dividend.size ) {
			remainder.setTo(dividend);
			quotent.resize(0);
			return;
		} else {
			remainder.resize(divisor.size-1);
			quotent.setTo(dividend);
		}

		int normalizer = divisor.data[0]&0xFF;

		int N = dividend.size-divisor.size+1;
		for (int i = 0; i < N; i++) {
			quotent.data[i] = (byte)divide(quotent.data[i]&0xFF,normalizer);

			int coef = quotent.data[i]&0xFF;
			if( coef != 0 ) { // division by zero is undefined.
				for (int j = 1; j < divisor.size; j++) { // skip the first coeffient in synthetic division
					int div_j = divisor.data[j]&0xFF;
					if( div_j != 0 ) {// log(0) is undefined.
						quotent.data[i+j] ^= multiply(div_j,coef);
					}
				}
			}
		}

		// quotent currently contains the quotent and remainder. Copy remainder into it's own polynomial
		System.arraycopy(quotent.data,quotent.size-remainder.size,remainder.data,0,remainder.size);
		quotent.size -= remainder.size;
	}
}
