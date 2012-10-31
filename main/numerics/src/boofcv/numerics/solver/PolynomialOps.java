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

import boofcv.numerics.solver.impl.FindRealRootsSturm;
import boofcv.numerics.solver.impl.RootFinderCompanion;
import boofcv.numerics.solver.impl.SturmSequence;
import boofcv.numerics.solver.impl.WrapRealRootsSturm;

/**
 * @author Peter Abeles
 */
public class PolynomialOps {

	/**
	 * Given the coefficients compute the vertex (minimum/maximum) of the quadratic.
	 *
	 * y = a*x<sup>2</sp> + b*x + c
	 *
	 * @param a quadratic coefficient.
	 * @param b quadratic coefficient.
	 * @return The quadratic's vertex.
	 */
	public static double quadraticVertex( double a, double b ) {
		return -b/(2.0*a);
	}

	public static void derivative( Polynomial poly , Polynomial deriv ) {
		deriv.size = poly.size - 1;

		for( int i = 1; i < poly.size; i++ ) {
			deriv.c[i-1] = poly.c[i]*i;
		}
	}

	// TODO try using a linear search alg here
	public static double refineRoot( Polynomial poly, double root , int maxIterations ) {

//		for( int i = 0; i < maxIterations; i++ ) {
//
//			double v = poly.c[poly.size-1];
//			double d = v*(poly.size-1);
//
//			for( int j = poly.size-1; j > 0; j-- ) {
//				v = poly.c[j] + v*root;
//				d = poly.c[j]*j + d*root;
//			}
//			v = poly.c[0] + v*root;
//
//			if( d == 0 )
//				return root;
//
//			root -= v/d;
//		}
//
//		return root;

		Polynomial deriv = new Polynomial(poly.size());
		derivative(poly,deriv);

		for( int i = 0; i < maxIterations; i++ ) {

			double v = poly.evaluate(root);
			double d = deriv.evaluate(root);

			if( d == 0 )
				return root;

			root -= v/d;
		}

		return root;
	}


	/**
	 * <p>
	 * Polynomial division. Computes both the quotient and the remainder.<br>
	 * <br>
	 * quotient = numerator/denominator<br>
	 * remainder = numerator % denominator
	 * </p>
	 *
	 * @param numerator Numerator in the division. Not modified.
	 * @param denominator Denominator in the division. Not modified.
	 * @param quotient Output quotient, Modified.
	 * @param remainder Output remainder. Modified.
	 */
	public static void divide( Polynomial numerator , Polynomial denominator , Polynomial quotient , Polynomial remainder  ) {
		if( denominator.size <= 0 )
			throw new IllegalArgumentException("Trying to device by a polynomial of size 0");

		int nn = numerator.size-1; int nd = denominator.size-1;

		while( nd >= 0 && denominator.c[nd] == 0 )
			nd -= 1;

		// divide by zero error
		if( nd < 0 ) {
			throw new IllegalArgumentException("Divide by zero error");
		}


		quotient.size = nn-nd+1;
		remainder.setTo(numerator);

		for( int k = nn-nd; k >= 0; k-- ) {
			if( nd < 0 || k < 0 )
				System.out.println("EGADS");
			double c = quotient.c[k] = remainder.c[nd+k]/denominator.c[nd];
			for( int j = k+nd; j >= k; j-- ) {
				remainder.c[j] -= c*denominator.c[j-k];
			}
		}

		// The remainder can't be larger than the denominator
		remainder.size = nd;
	}

	/**
	 * Multiplies the two polynomials together.
	 * @param a Polynomial
	 * @param b Polynomial
	 * @param result Optional storage parameter for the results.  Must be have enough coefficients to store the results.
	 *               If null a new instance is declared.
	 * @return Results of the multiplication
	 */
	public static Polynomial multiply( Polynomial a , Polynomial b , Polynomial result ) {

		int N = Math.max(0,a.size() + b.size() - 1);

		if( result == null ) {
			result = new Polynomial(N);
		} else {
			if( result.size < N )
				throw new IllegalArgumentException("Unexpected length of 'result'");
			result.zero();
		}

		for( int i = 0; i < a.size; i++ ) {
			double coef = a.c[i];

			int index = i;
			for( int j = 0; j < b.size; j++ ) {
				result.c[index++] += coef*b.c[j];
			}
		}

		return result;
	}

	/**
	 * Adds two polynomials together. The lengths of the polynomials do not need to be the zero, but the 'missing'
	 * coefficients are assumed to be zero.
	 *
	 * @param a Polynomial
	 * @param b Polynomial
	 * @param results Optional storage for resulting polynomial.  If null a new instance is declared. If not null
	 *               its length must be the same as the largest polynomial 'a' or 'b'.
	 * @return Polynomial 'a' and 'b' added together.
	 */
	public static Polynomial add( Polynomial a , Polynomial b , Polynomial results ) {
		int N = Math.max(a.size,b.size);

		if( results == null ) {
			results = new Polynomial(N);
		} else if( results.size < N ) {
			throw new IllegalArgumentException("storage for results must be at least as large as the the largest polynomial");
		} else {
			for( int i = N; i < results.size; i++ ) {
				results.c[i] = 0;
			}
		}

		int M = Math.min(a.size,b.size);
		for( int i = 0; i < M; i++ ) {
			results.c[i] = a.c[i] + b.c[i];
		}

		// copy the non-overlapping coefficients for the larger polynomial
		if( a.size > b.size ) {
			for( int i = b.size; i < N; i++ )
				results.c[i] = a.c[i];
		} else {
			for( int i = a.size; i < N; i++ )
				results.c[i] = b.c[i];
		}

		return results;
	}

	public static int countRealRoots( Polynomial poly ) {
		SturmSequence sturm = new SturmSequence(poly.size);

		sturm.initialize(poly);

		return sturm.countRealRoots(Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY);
	}

	/**
	 * Creates different polynomial root finders.
	 *
	 * @param maxCoefficients The maximum number of coefficients that will be processed. This is the order + 1
	 * @param which 0 = Sturm and 1 = companion matrix.
	 * @return PolynomialRoots
	 */
	public static PolynomialRoots createRootFinder( int maxCoefficients , int which ) {
		switch( which ) {
			case 0:
				FindRealRootsSturm sturm = new FindRealRootsSturm(maxCoefficients,-1,1e-10,200,200);
				return new WrapRealRootsSturm(sturm);

			case 1:
				return new RootFinderCompanion();
		}
		throw new RuntimeException("Unknown algorithm");
	}

	/**
	 * Returns a real root to the cubic polynomial: 0 = c0 + c1*x + c2*x^2 + c3*c^3.  There can be other
	 * real roots.
	 *
	 * WARNING: This technique is much less stable than using one of the RootFinder algorithms
	 *
	 * @param c0 Polynomial coefficient for power 0
	 * @param c1 Polynomial coefficient for power 1
	 * @param c2 Polynomial coefficient for power 2
	 * @param c3 Polynomial coefficient for power 3
	 * @return A real root of the polynomial
	 */
	public static double cubicRealRoot( double c0 , double c1 , double c2 , double c3 ) {
		// convert it into this format:  r + q*x + p*x^2 + x^3 = 0
		double p = c2/c3;
		double q = c1/c3;
		double r = c0/c3;

		// reduce by substitution x = y - p/3 which results in y^3 + a*y + b = 0

		double a = (3*q - p*p)/3.0;
		double b = (2*p*p*p - 9*p*q + 27*r)/27.0;

		double left = -b/2.0;
		double right = Math.sqrt(b*b/4.0 + a*a*a/27.0);

		double inner1 = left+right;
		double inner2 = left-right;

		double A,B;

		if( inner1 < 0 )
			A = -Math.pow(-inner1,1.0/3.0);
		else
			A = Math.pow(inner1,1.0/3.0);

		if( inner2 < 0 )
			B = -Math.pow(-inner2,1.0/3.0);
		else
			B = Math.pow(inner2,1.0/3.0);

		return (A + B) - p/3.0;
	}

}
