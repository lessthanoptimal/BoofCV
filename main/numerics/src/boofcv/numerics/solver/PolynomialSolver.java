/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

import boofcv.numerics.complex.ComplexMath;
import org.ejml.alg.dense.decomposition.DecompositionFactory;
import org.ejml.alg.dense.decomposition.EigenDecomposition;
import org.ejml.data.Complex64F;
import org.ejml.data.DenseMatrix64F;

/**
 * Provides functions for finding the roots of polynomials
 *
 * @author Peter Abeles
 */
public class PolynomialSolver {

	/**
	 * Finds real and imaginary roots in a polynomial using the companion matrix and
	 * Eigenvalue decomposition.
	 *
	 * @param coefficients Polynomial coefficients.
	 * @return The found roots.
	 */
	public static Complex64F[] polynomialRootsEVD(double... coefficients) {

		int N = coefficients.length-1;

		// Companion matrix
		DenseMatrix64F c = new DenseMatrix64F(N,N);

		double a = coefficients[N];
		for( int i = 0; i < N; i++ ) {
			c.set(i,N-1,-coefficients[i]/a);
		}
		for( int i = 1; i < N; i++ ) {
			c.set(i,i-1,1);
		}

		// use generalized eigenvalue decomposition to find the roots
		EigenDecomposition<DenseMatrix64F> evd =  DecompositionFactory.eigGeneral(N, false);

		evd.decompose(c);

		Complex64F[] roots = new Complex64F[N];

		for( int i = 0; i < N; i++ ) {
			roots[i] = evd.getEigenvalue(i);
		}

		return roots;
	}

	/**
	 * <p>
	 * A cubic polynomial of the form "f(x) =  a + b*x + c*x<sup>2</sup> + d*x<sup>3</sup>" has
	 * three roots.  These roots will either be all real or one real and two imaginary.  This function
	 * will return a root which is always real.
	 * </p>
	 *
	 * <p>
	 * WARNING: Not as numerically stable as {@link #polynomialRootsEVD(double...)}, but still fairly stable.
	 * </p>
	 *
	 * @param a polynomial coefficient.
	 * @param b polynomial coefficient.
	 * @param c polynomial coefficient.
	 * @param d polynomial coefficient.
	 * @return A real root of the cubic polynomial
	 */
	public static double cubicRootReal(double a, double b, double c, double d)
	{
		// normalize for numerical stability
		double norm = Math.max(Math.abs(a), Math.abs(b));
		norm = Math.max(norm,Math.abs(c));
		norm = Math.max(norm, Math.abs(d));

		a /= norm;
		b /= norm;
		c /= norm;
		d /= norm;

		// proceed with standard algorithm
		double insideLeft = c*(2*c*c - 9*d*b) + 27*d*d*a;
		double temp = c*c-3*d*b;
		double insideOfSqrt = insideLeft*insideLeft - 4*temp*temp*temp;

		if( insideOfSqrt >= 0 ) {
			double insideRight = Math.sqrt(insideOfSqrt );

			double ret = c/d +
					root3(0.5*(insideLeft+insideRight)) +
					root3(0.5*(insideLeft-insideRight));

			return -ret/(3.0*d);
		} else {
			Complex64F inside = new Complex64F(0.5*insideLeft,0.5*Math.sqrt(-insideOfSqrt ));
			Complex64F root = new Complex64F();

			ComplexMath.root(inside,3,2,root);

			// imaginary components cancel out
			double ret = c + 2*root.getReal();

			return -ret/(3.0*d);
		}
	}

	private static double root3( double val ) {
		if( val < 0 )
			return -Math.pow(-val,1.0/3.0);
		else
			return Math.pow(val,1.0/3.0);
	}

	/**
	 * <p>
	 * The cubic discriminant is used to determine the type of roots.
	 * <ul>
	 * <li>if d > 0, then three distinct real roots</li>
	 * <li>if d = 0, then it has a multiple root and all will be real</li>
	 * <li>if d < 0, then one real and two non-real complex conjugate roots</li>
	 * </ul>
	 * </p>
	 *
	 * <p>
	 * From http://en.wikipedia.org/wiki/Cubic_function Novemeber 17, 2011
	 * </p>
	 *
	 * @param a polynomial coefficient.
	 * @param b polynomial coefficient.
	 * @param c polynomial coefficient.
	 * @param d polynomial coefficient.
	 * @return Cubic discriminant
	 */
	public static double cubicDiscriminant(double a, double b, double c, double d) {
		return 18.0*d*c*b*a -4*c*c*c*a + c*c*b*b -4*d*b*b*b - 27*d*d*a*a;
	}
}
