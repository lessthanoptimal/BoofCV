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
 * <p>Code and code comments based on the tutorial at [1].</p>
 *
 *  <p>[1] <a href="https://en.wikiversity.org/wiki/Reed–Solomon_codes_for_coders">Reed-Solomon Codes for Coders</a>
 *  Viewed on September 28, 2017</p>
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

	public int power_n(int x , int power ) {
		int a = (log[x] * power)%max_value;
		if( a < 0 )
			a = max_value*2 + a;
		return exp[a];
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
	 * <p>Coefficients for largest powers are first, e.g. 2*x**3 + 8*x**2+1 = [2,8,0,1]</p>
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
	 * Adds two polynomials together. output = polyA + polyB
	 *
	 * <p>Coefficients for largest powers are first, e.g. 2*x**3 + 8*x**2+1 = [2,8,0,1]</p>
	 *
	 * @param polyA (Input) First polynomial
	 * @param polyB (Input) Second polynomial
	 * @param output (Output) Results of addition
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
	 * Adds two polynomials together.
	 *
	 * <p>Coefficients for smallest powers are first, e.g. 2*x**3 + 8*x**2+1 = [1,0,2,8]</p>
	 *
	 * @param polyA (Input) First polynomial
	 * @param polyB (Input) Second polynomial
	 * @param output (Output) Results of addition
	 */
	public void polyAdd_S(GrowQueue_I8 polyA , GrowQueue_I8 polyB , GrowQueue_I8 output ) {
		output.resize(Math.max(polyA.size,polyB.size));
		int M = Math.min(polyA.size, polyB.size);

		for (int i = M; i < polyA.size; i++) {
			output.data[i] = polyA.data[i];
		}
		for (int i = M; i < polyB.size; i++) {
			output.data[i] = polyB.data[i];
		}

		for (int i = 0; i < M; i++) {
			output.data[i] = (byte)((polyA.data[i]&0xFF) ^ (polyB.data[i]&0xFF));
		}
	}

	/**
	 * Adds two polynomials together while scaling the second.
	 *
	 * <p>Coefficients for largest powers are first, e.g. 2*x**3 + 8*x**2+1 = [2,8,0,1]</p>
	 *
	 * @param polyA (Input) First polynomial
	 * @param polyB (Input) Second polynomial
	 * @param scaleB (Input) Scale factor applied to polyB
	 * @param output (Output) Results of addition
	 */
	public void polyAddScaleB(GrowQueue_I8 polyA , GrowQueue_I8 polyB , int scaleB , GrowQueue_I8 output ) {
		output.resize(Math.max(polyA.size,polyB.size));

		// compute offset that would align the smaller polynomial with the larger polynomial
		int offsetA = Math.max(0,polyB.size-polyA.size);
		int offsetB = Math.max(0,polyA.size-polyB.size);
		int N = output.size;

		for (int i = 0; i < offsetB; i++) {
			output.data[i] = polyA.data[i];
		}
		for (int i = 0; i < offsetA; i++) {
			output.data[i] = (byte)multiply(polyB.data[i]&0xFF,scaleB);
		}
		for (int i = Math.max(offsetA,offsetB); i < N; i++) {
			output.data[i] = (byte)((polyA.data[i-offsetA]&0xFF) ^ multiply(polyB.data[i-offsetB]&0xFF,scaleB));
		}
	}

	/**
	 *
	 * <p>Coefficients for largest powers are first, e.g. 2*x**3 + 8*x**2+1 = [2,8,0,1]</p>
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

	public void polyMult_flipA(GrowQueue_I8 polyA , GrowQueue_I8 polyB , GrowQueue_I8 output ) {

		// Lots of room for efficiency improvements in this function
		output.resize(polyA.size+polyB.size-1);
		output.zero();

		for (int j = 0; j < polyB.size; j++) {
			int vb = polyB.data[j]&0xFF;
			for (int i = 0; i < polyA.size; i++) {
				int va = polyA.data[polyA.size-i-1]&0xFF;
				output.data[i+j] ^= multiply(va,vb);
			}
		}
	}

	/**
	 *
	 * Identical to {@link #polyMult(GrowQueue_I8, GrowQueue_I8, GrowQueue_I8)}
	 *
	 * <p>Coefficients for smallest powers are first, e.g. 2*x**3 + 8*x**2+1 = [1,0,2,8]</p>
	 *
	 * @param polyA
	 * @param polyB
	 * @param output
	 */
	public void polyMult_S(GrowQueue_I8 polyA , GrowQueue_I8 polyB , GrowQueue_I8 output ) {

		// Lots of room for efficiency improvements in this function
		output.resize(polyA.size+polyB.size-1);
		output.zero();

		for (int j = polyB.size-1; j >= 0; j--) {
			int vb = polyB.data[j]&0xFF;
			for (int i = polyA.size-1; i >= 0; i--) {
				int va = polyA.data[i]&0xFF;
				output.data[i+j] ^= multiply(va,vb);
			}
		}
	}

	/**
	 * Evaluate the polynomial using Horner's method. Avoids explicit calculating the powers of x.
	 *
	 * <p>01x**4 + 0fx**3 + 36x**2 + 78x + 40 = (((01 x + 0f) x + 36) x + 78) x + 40</p>
	 *
	 *
	 * <p>Coefficients for largest powers are first, e.g. 2*x**3 + 8*x**2+1 = [2,8,0,1]</p>
	 *
	 * @param input Polynomial being evaluated
	 * @param x Value of x
	 * @return Output of function
	 */
	public int polyEval(GrowQueue_I8 input , int x ) {
		int y = input.data[0]&0xFF;

		for (int i = 1; i < input.size; i++) {
			y = multiply(y,x) ^ (input.data[i]&0xFF);
		}

		return y;
	}

	/**
	 * Evaluate the polynomial using Horner's method. Avoids explicit calculating the powers of x.
	 *
	 * <p>01x**4 + 0fx**3 + 36x**2 + 78x + 40 = (((01 x + 0f) x + 36) x + 78) x + 40</p>
	 *
	 *
	 * <p>Coefficients for smallest powers are first, e.g. 2*x**3 + 8*x**2+1 = [1,0,2,8]</p>
	 *
	 * @param input Polynomial being evaluated
	 * @param x Value of x
	 * @return Output of function
	 */
	public int polyEval_S(GrowQueue_I8 input , int x ) {
		int y = input.data[input.size-1]&0xFF;

		for (int i = input.size-2; i >= 0; i--) {
			y = multiply(y,x) ^ (input.data[i]&0xFF);
		}

		return y;
	}

	/**
	 * Continue evaluating a polynomial which has been broken up into multiple arrays.
	 *
	 * @param previousOutput Output from the evaluation of the prior part of the polynomial
	 * @param part Additional segment of the polynomial
	 * @param x Point it's being evaluated at
	 * @return results
	 */
	public int polyEvalContinue( int previousOutput, GrowQueue_I8 part , int x ) {
		int y = previousOutput;
		for (int i = 0; i < part.size; i++) {
			y = multiply(y,x) ^ (part.data[i]&0xFF);
		}

		return y;
	}

	/**
	 * Performs polynomial division using a synthetic division algorithm.
	 *
	 * <p>Coefficients for largest powers are first, e.g. 2*x**3 + 8*x**2+1 = [2,8,0,1]</p>
	 *
	 * @param dividend (Input) Polynomial dividend
	 * @param divisor (Input) Polynomial divisor
	 * @param quotient (Output) Division's quotient
	 * @param remainder (Output) Divisions's remainder
	 */
	public void polyDivide(GrowQueue_I8 dividend , GrowQueue_I8 divisor ,
						   GrowQueue_I8 quotient, GrowQueue_I8 remainder ) {

		// handle special case
		if( divisor.size > dividend.size ) {
			remainder.setTo(dividend);
			quotient.resize(0);
			return;
		} else {
			remainder.resize(divisor.size-1);
			quotient.setTo(dividend);
		}

		int normalizer = divisor.data[0]&0xFF;

		int N = dividend.size-divisor.size+1;
		for (int i = 0; i < N; i++) {
			quotient.data[i] = (byte)divide(quotient.data[i]&0xFF,normalizer);

			int coef = quotient.data[i]&0xFF;
			if( coef != 0 ) { // division by zero is undefined.
				for (int j = 1; j < divisor.size; j++) { // skip the first coeffient in synthetic division
					int div_j = divisor.data[j]&0xFF;

					if( div_j != 0 ) {// log(0) is undefined.
						quotient.data[i+j] ^= multiply(div_j,coef);
					}
				}
			}
		}

		// quotient currently contains the quotient and remainder. Copy remainder into it's own polynomial
		System.arraycopy(quotient.data,quotient.size-remainder.size,remainder.data,0,remainder.size);
		quotient.size -= remainder.size;
	}

	/**
	 * Performs polynomial division using a synthetic division algorithm.
	 *
	 * <p>Coefficients for smallest powers are first, e.g. 2*x**3 + 8*x**2+1 = [1,0,2,8]</p>
	 *
	 * @param dividend (Input) Polynomial dividend
	 * @param divisor (Input) Polynomial divisor
	 * @param quotient (Output) Division's quotient
	 * @param remainder (Output) Divisions's remainder
	 */
	public void polyDivide_S(GrowQueue_I8 dividend , GrowQueue_I8 divisor ,
							 GrowQueue_I8 quotient, GrowQueue_I8 remainder ) {

		// handle special case
		if( divisor.size > dividend.size ) {
			remainder.setTo(dividend);
			quotient.resize(0);
			return;
		} else {
			quotient.resize(dividend.size-divisor.size+1);
			remainder.setTo(dividend);
		}

		int normalizer = divisor.data[divisor.size-1]&0xFF;

		int N = dividend.size-divisor.size+1;
		for (int i = 0; i < N; i++) {
			int q_i = remainder.size-i-1;
			remainder.data[q_i] = (byte)divide(remainder.data[q_i]&0xFF,normalizer);

			int coef = remainder.data[q_i]&0xFF;
			if( coef != 0 ) { // division by zero is undefined.
				for (int j = 1; j < divisor.size; j++) { // skip the first coeffient in synthetic division
					int d_j = divisor.size-j-1;
					int div_j = divisor.data[d_j]&0xFF;
					if( div_j != 0 ) {// log(0) is undefined.
						remainder.data[remainder.size-i-j-1] ^= multiply(div_j,coef);
					}
				}
			}
		}

		// quotient currently contains the quotient and remainder. Copy remainder into it's own polynomial
		remainder.size -= quotient.size;
		System.arraycopy(remainder.data,remainder.size,quotient.data,0,quotient.size);
	}
}
