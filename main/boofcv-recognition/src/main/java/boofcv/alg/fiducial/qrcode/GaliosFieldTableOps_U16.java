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

import org.ddogleg.struct.DogArray_I16;

/**
 * Precomputed look up table for performing operations on GF polynomials of the specified degree.
 *
 * <p>Code and code comments based on the tutorial at [1].</p>
 *
 * <p>[1] <a href="https://en.wikiversity.org/wiki/Reed–Solomon_codes_for_coders">Reed-Solomon Codes for Coders</a>
 * Viewed on September 28, 2017</p>
 *
 * @author Peter Abeles
 */
public class GaliosFieldTableOps_U16 extends GaliosFieldTableOps {
	/**
	 * Specifies the GF polynomial
	 *
	 * @param numBits Number of bits needed to describe the polynomial. GF(2**8) = 8 bits
	 * @param primitive The primitive polynomial
	 */
	public GaliosFieldTableOps_U16( int numBits, int primitive ) {
		super(numBits, primitive);
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
	public void polyScale( DogArray_I16 input, int scale, DogArray_I16 output ) {
		output.resize(input.size);

		for (int i = 0; i < input.size; i++) {
			output.data[i] = (short)multiply(input.data[i] & 0xFFFF, scale);
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
	public void polyAdd( DogArray_I16 polyA, DogArray_I16 polyB, DogArray_I16 output ) {
		output.resize(Math.max(polyA.size, polyB.size));

		// compute offset that would align the smaller polynomial with the larger polynomial
		int offsetA = Math.max(0, polyB.size - polyA.size);
		int offsetB = Math.max(0, polyA.size - polyB.size);
		int N = output.size;

		for (int i = 0; i < offsetB; i++) {
			output.data[i] = polyA.data[i];
		}
		for (int i = 0; i < offsetA; i++) {
			output.data[i] = polyB.data[i];
		}
		for (int i = Math.max(offsetA, offsetB); i < N; i++) {
			output.data[i] = (short)((polyA.data[i - offsetA] & 0xFFFF) ^ (polyB.data[i - offsetB] & 0xFFFF));
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
	public void polyAdd_S( DogArray_I16 polyA, DogArray_I16 polyB, DogArray_I16 output ) {
		output.resize(Math.max(polyA.size, polyB.size));
		int M = Math.min(polyA.size, polyB.size);

		for (int i = M; i < polyA.size; i++) {
			output.data[i] = polyA.data[i];
		}
		for (int i = M; i < polyB.size; i++) {
			output.data[i] = polyB.data[i];
		}

		for (int i = 0; i < M; i++) {
			output.data[i] = (short)((polyA.data[i] & 0xFFFF) ^ (polyB.data[i] & 0xFFFF));
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
	public void polyAddScaleB( DogArray_I16 polyA, DogArray_I16 polyB, int scaleB, DogArray_I16 output ) {
		output.resize(Math.max(polyA.size, polyB.size));

		// compute offset that would align the smaller polynomial with the larger polynomial
		int offsetA = Math.max(0, polyB.size - polyA.size);
		int offsetB = Math.max(0, polyA.size - polyB.size);
		int N = output.size;

		for (int i = 0; i < offsetB; i++) {
			output.data[i] = polyA.data[i];
		}
		for (int i = 0; i < offsetA; i++) {
			output.data[i] = (short)multiply(polyB.data[i] & 0xFFFF, scaleB);
		}
		for (int i = Math.max(offsetA, offsetB); i < N; i++) {
			output.data[i] = (short)((polyA.data[i - offsetA] & 0xFFFF) ^ multiply(polyB.data[i - offsetB] & 0xFFFF, scaleB));
		}
	}

	/**
	 * <p>Coefficients for largest powers are first, e.g. 2*x**3 + 8*x**2+1 = [2,8,0,1]</p>
	 */
	public void polyMult( DogArray_I16 polyA, DogArray_I16 polyB, DogArray_I16 output ) {

		// Lots of room for efficiency improvements in this function
		output.resize(polyA.size + polyB.size - 1);
		output.zero();

		for (int j = 0; j < polyB.size; j++) {
			int vb = polyB.data[j] & 0xFFFF;
			for (int i = 0; i < polyA.size; i++) {
				int va = polyA.data[i] & 0xFFFF;
				output.data[i + j] ^= (short)multiply(va, vb);
			}
		}
	}

	public void polyMult_flipA( DogArray_I16 polyA, DogArray_I16 polyB, DogArray_I16 output ) {

		// Lots of room for efficiency improvements in this function
		output.resize(polyA.size + polyB.size - 1);
		output.zero();

		for (int j = 0; j < polyB.size; j++) {
			int vb = polyB.data[j] & 0xFFFF;
			for (int i = 0; i < polyA.size; i++) {
				int va = polyA.data[polyA.size - i - 1] & 0xFFFF;
				output.data[i + j] ^= (short)multiply(va, vb);
			}
		}
	}

	/**
	 * Identical to {@link #polyMult(DogArray_I16, DogArray_I16, DogArray_I16)}
	 *
	 * <p>Coefficients for smallest powers are first, e.g. 2*x**3 + 8*x**2+1 = [1,0,2,8]</p>
	 */
	public void polyMult_S( DogArray_I16 polyA, DogArray_I16 polyB, DogArray_I16 output ) {

		// Lots of room for efficiency improvements in this function
		output.resize(polyA.size + polyB.size - 1);
		output.zero();

		for (int j = polyB.size - 1; j >= 0; j--) {
			int vb = polyB.data[j] & 0xFFFF;
			for (int i = polyA.size - 1; i >= 0; i--) {
				int va = polyA.data[i] & 0xFFFF;
				output.data[i + j] ^= (short)multiply(va, vb);
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
	public int polyEval( DogArray_I16 input, int x ) {
		int y = input.data[0] & 0xFFFF;

		for (int i = 1; i < input.size; i++) {
			y = multiply(y, x) ^ (input.data[i] & 0xFFFF);
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
	public int polyEval_S( DogArray_I16 input, int x ) {
		int y = input.data[input.size - 1] & 0xFFFF;

		for (int i = input.size - 2; i >= 0; i--) {
			y = multiply(y, x) ^ (input.data[i] & 0xFFFF);
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
	public int polyEvalContinue( int previousOutput, DogArray_I16 part, int x ) {
		int y = previousOutput;
		for (int i = 0; i < part.size; i++) {
			y = multiply(y, x) ^ (part.data[i] & 0xFFFF);
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
	public void polyDivide( DogArray_I16 dividend, DogArray_I16 divisor,
							DogArray_I16 quotient, DogArray_I16 remainder ) {

		// handle special case
		if (divisor.size > dividend.size) {
			remainder.setTo(dividend);
			quotient.resize(0);
			return;
		} else {
			remainder.resize(divisor.size - 1);
			quotient.setTo(dividend);
		}

		int normalizer = divisor.data[0] & 0xFFFF;

		int N = dividend.size - divisor.size + 1;
		for (int i = 0; i < N; i++) {
			quotient.data[i] = (short)divide(quotient.data[i] & 0xFFFF, normalizer);

			int coef = quotient.data[i] & 0xFFFF;
			if (coef != 0) { // division by zero is undefined.
				for (int j = 1; j < divisor.size; j++) { // skip the first coeffient in synthetic division
					int div_j = divisor.data[j] & 0xFFFF;

					if (div_j != 0) {// log(0) is undefined.
						quotient.data[i + j] ^= (short)multiply(div_j, coef);
					}
				}
			}
		}

		// quotient currently contains the quotient and remainder. Copy remainder into it's own polynomial
		System.arraycopy(quotient.data, quotient.size - remainder.size, remainder.data, 0, remainder.size);
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
	public void polyDivide_S( DogArray_I16 dividend, DogArray_I16 divisor,
							  DogArray_I16 quotient, DogArray_I16 remainder ) {

		// handle special case
		if (divisor.size > dividend.size) {
			remainder.setTo(dividend);
			quotient.resize(0);
			return;
		} else {
			quotient.resize(dividend.size - divisor.size + 1);
			remainder.setTo(dividend);
		}

		int normalizer = divisor.data[divisor.size - 1] & 0xFFFF;

		int N = dividend.size - divisor.size + 1;
		for (int i = 0; i < N; i++) {
			int q_i = remainder.size - i - 1;
			remainder.data[q_i] = (short)divide(remainder.data[q_i] & 0xFFFF, normalizer);

			int coef = remainder.data[q_i] & 0xFFFF;
			if (coef != 0) { // division by zero is undefined.
				for (int j = 1; j < divisor.size; j++) { // skip the first coeffient in synthetic division
					int d_j = divisor.size - j - 1;
					int div_j = divisor.data[d_j] & 0xFFFF;
					if (div_j != 0) {// log(0) is undefined.
						remainder.data[remainder.size - i - j - 1] ^= (short)multiply(div_j, coef);
					}
				}
			}
		}

		// quotient currently contains the quotient and remainder. Copy remainder into it's own polynomial
		remainder.size -= quotient.size;
		System.arraycopy(remainder.data, remainder.size, quotient.data, 0, quotient.size);
	}
}
