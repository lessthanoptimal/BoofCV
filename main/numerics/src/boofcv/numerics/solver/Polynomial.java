/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.numerics.solver;

/**
 * @author Peter Abeles
 */
public class Polynomial {
	public double c[];
	public int size;

	public static Polynomial wrap( double ...coefficients ) {
		Polynomial p = new Polynomial(coefficients.length);
		p.setTo(coefficients,coefficients.length);

		return p;
	}

	public Polynomial( int maxDegree ) {
		c = new double[ maxDegree ];
		this.size = maxDegree;
	}

	public Polynomial( Polynomial original ) {
		this.size = original.size;
		c = new double[size];
		System.arraycopy(original.c,0,c,0,size);
	}

	/**
	 * Computes the polynomials output given the variable value  Can handle infinite numbers
	 *
	 * @return Output
	 */
	public double evaluate( double variable ) {

		if( size == 0 ) {
			return 0;
		} else if( size == 1 ) {
			return c[0];
		} else if( Double.isInfinite(variable)) {

			// Only the largest power with a non-zero coefficient needs to be evaluated
			int degree = computeDegree();
			if( degree%2 == 0 )
				variable = Double.POSITIVE_INFINITY;

			if( c[degree] < 0 )
				variable = variable == Double.POSITIVE_INFINITY ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;

			return variable;
		}

		// Evaluate using Horner's Method.
		// This formulation produces more accurate results than the straight forward one and has fewer
		// multiplications
		double total = c[size-1];

		for( int i = size-2; i >= 0; i-- ) {
			total = c[i] + total*variable;
		}

		return total;
	}

	public void setTo( Polynomial source ) {
		this.size = source.size;
		System.arraycopy(source.c,0,c,0,size);
	}

	public void setTo( double[] coefficients, int size ) {
		this.size = size;
		System.arraycopy(coefficients,0,c,0,size);
	}

	public void resize( int size ) {
		if( c.length < size )
			c = new double[size];
		this.size = size;
	}

	public void zero() {
		for( int i = 0; i < size; i++ )
			c[i] = 0;
	}

	/**
	 * Finds the power of the largest non-zero coefficient in the polynomial.  If all the coefficients
	 * are zero or if there is only the constant term, zero is returned.
	 *
	 * @return Degree of the polynomial
	 */
	public int computeDegree() {
		for( int i = size-1; i >= 0; i-- ) {
			if( c[i] != 0.0 )
				return i;
		}
		return -1;
	}

	/**
	 * Checks to see if the coefficients of two polynomials are identical to within tolerance.  If the lengths
	 * of the polynomials are not the same then the extra coefficients in the longer polynomial must be within tolerance
	 * of zero.
	 *
	 * @param p Polynomial that this polynomial is being compared against.
	 * @param tol Similarity tolerance.  Try 1e-15 for high confidence
	 * @return true if the two polynomials are identical to within tolerance.
	 */
	public boolean isIdentical( Polynomial p , double tol ) {
		int m = Math.max(p.size(), size());

		// make sure trailing coefficients are close to zero
		for( int i = p.size; i < m; i++ ) {
			if( Math.abs(c[i]) > tol ) {
				return false;
			}
		}

		for( int i = size; i < m; i++ ) {
			if( Math.abs(p.c[i]) > tol ) {
				return false;
			}
		}

		// ensure that the rest of the coefficients are close
		int n = Math.min(p.size(),size());
		for( int i = 0; i < n; i++ ) {
			if( Math.abs(c[i]-p.c[i]) > tol ) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Prunes zero coefficients from the end of a sequence.  A coefficient is zero if its absolute value
	 * is less than or equal to tolerance.
	 *
	 * @param tol Tolerance for zero.  Try 1e-15
	 */
	public void truncateZeros( double tol ) {

//		double max = 0;
//		for( int i = 0; i < size; i++ ) {
//			double d = Math.abs(c[i]);
//			if( d > max )
//				max = d;
//		}
//
//		tol *= max;

		int i = size-1;

		for( ; i >= 0; i-- ) {
			if( Math.abs(c[i]) > tol )
				break;
		}
		size = i+1;
	}

	@Override
	public String toString() {
		String ret = "Poly("+size+")[ ";
		for( int i = 0; i < size; i++ ) {
			ret += c[i]+" ";
		}
		return ret +" ]";
	}

	public void print() {
		System.out.println(this);
	}

	public double[] getCoefficients() {
		return c;
	}

	public int size() {
		return size;
	}

	public double get(int i) {
		return c[i];
	}
}
