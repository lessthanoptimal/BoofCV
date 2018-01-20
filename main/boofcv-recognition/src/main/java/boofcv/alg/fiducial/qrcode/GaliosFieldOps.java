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
 * Basic path operators on polynomial Galious Field (GF) with GF(2) coefficients. Polynomials are stored
 * inside of integers with the least significant bits first. This class is primarily used to
 * populate precomputed tables.
 *
 * <p>Code and code comments based on the tutorial at [1].</p>
 *
 *  <p>[1] <a href="https://en.wikiversity.org/wiki/Reed–Solomon_codes_for_coders">Reed-Solomon Codes for Coders</a>
 *  Viewed on September 28, 2017</p>
 *
 * @author Peter Abeles
 */
public class GaliosFieldOps {

	public static int add( int a , int b ) {
		return a ^ b;
	}

	public static int subtract( int a , int b ) {
		return a ^ b;
	}

	/**
	 * <p>Multiply the two polynomials together. The technique used here isn't the fastest but is easy
	 * to understand.</p>
	 *
	 * <p>NOTE: No modulus operation is performed so the result might not be a member of the same field.</p>
	 *
	 * @param a polynomial
	 * @param b polynomial
	 * @return result polynomial
	 */
	public static int multiply( int a , int b ) {
		int z = 0;

		for (int i = 0; (b>>i) > 0; i++) {
			if( (b & (1 << i)) != 0 ) {
				z ^= a << i;
			}
		}
		return z;
	}

	/**
	 * Implementation of multiplication with a primitive polynomial. The result will be a member of the same field
	 * as the inputs, provided primitive is an appropriate irreducible polynomial for that field.
	 *
	 * Uses 'Russian Peasant Multiplication' that should be a faster algorithm.
	 *
	 * @param x polynomial
	 * @param y polynomial
	 * @param primitive Primitive polynomial which is irreducible.
	 * @param domain Value of a the largest possible value plus 1. E.g. GF(2**8) would be 256
	 * @return result polynomial
	 */
	public static int multiply( int x , int y , int primitive , int domain ) {
		int r = 0;
		while( y > 0 ) {
			if( (y&1) != 0 ) {
				r = r ^ x;
			}
			y = y >> 1;
			x = x << 1;

			if( x >= domain) {
				x ^= primitive;
			}
		}
		return r;
	}

	/**
	 * <p>Performs the polynomial GF modulus operation.</p>
	 *
	 * result = dividend mod divisor.
	 */
	public static int modulus( int dividend , int divisor ) {
		// Compute the position of the most significant bit for each integers
		int length_end = length(dividend);
		int length_sor = length(divisor);
        // If the dividend is smaller than the divisor then nothing needs to be done
		if( length_end < length_sor )
			return dividend;

		// Align the most significant 1 of the divisor to the most significant 1
		// of the dividend (by shifting the divisor)
		for( int i = length_end-length_sor; i >= 0; i-- ) {
			// Check that the dividend is divisible (useless for the first iteration but
			// important for the next ones)
			if((dividend & (1 << i + length_sor - 1)) != 0 ) {
				// if divisible, then shift the divisor to align the most significant bits and subtract.
				dividend ^= divisor << i;
			}
		}
		return dividend;
	}

	/**
	 * <p>Divides dividend by the divisor and returns the integer results, e.g. no remainder.</p>
	 *
	 * result = dividend / divisor
	 *
	 * @param dividend number on top
	 * @param divisor number on bottom
	 * @return number of times divisor goes into dividend.
	 */
	public static int divide( int dividend , int divisor ) {
		int length_end = length(dividend);
		int length_sor = length(divisor);

		if( length_end < length_sor )
			return 0;

		int result = 0;

		for( int i = length_end-length_sor; i >= 0; i-- ) {
			if((dividend & (1 << i + length_sor - 1)) != 0 ) {
				dividend ^= divisor << i;
				result |= 1 << i;
			}
		}
		return result;
	}


	/**
	 * The bit in which the most significant non-zero bit is stored
	 */
	public static int length( int value ) {
		int length = 0;
		while( (value>>length) != 0 ) {
			length++;
		}
		return length;
	}
}
