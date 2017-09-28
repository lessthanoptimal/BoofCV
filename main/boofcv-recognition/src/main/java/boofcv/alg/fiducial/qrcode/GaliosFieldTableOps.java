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
	int max_value; // maximum value in the field plus 1
	int degree;
	int primitive;

	int table_exp[];
	int table_log[];

	/**
	 * Specifies the GF polynomial
	 *
	 * @param degree maximum degree of the polynomial
	 * @param primitive The primitive polynomial
	 */
	public GaliosFieldTableOps( int degree , int primitive) {
		if( degree < 1 || degree > 16 )
			throw new IllegalArgumentException("Degree must be more than 1 and less than or equal to 16");

		this.degree = degree;
		this.primitive = primitive;
		max_value = 1;
		for (int i = 0; i < degree; i++) {
			max_value *= 2;
		}

		table_log = new int[ max_value ];
		table_exp = new int[ max_value*2 ]; // make it twice as long to avoid a modulus operation

		// exhaustively compute all values
		int x = 1;
		for (int i = 0; i < max_value; i++) {
			table_exp[i] = x;
			table_log[x] = i;
			x = GaliosFieldOps.multiply(x,2,primitive,max_value);
		}

		for (int i = 0; i < max_value; i++) {
			table_exp[i+max_value] = table_exp[i];
		}
	}

	public int power( int x , int power ) {
		return 0;
	}

	public int inverse( int x ) {
		return 0;
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
