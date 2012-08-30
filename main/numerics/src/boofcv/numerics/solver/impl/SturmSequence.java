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

package boofcv.numerics.solver.impl;

import boofcv.numerics.solver.Polynomial;
import boofcv.numerics.solver.PolynomialOps;

/**
 * <p>
 * A Sturm sequence has the property that the number of sign changes can be used to compute the number of real
 * roots in a polynomial.  The Sturm sequence is defined as follows:<br>
 * [f(x) , deriv(f(x)) , f[0](x) % f[1](x) , ... , f[n-1](x) % f[n](x) ]<br>
 * where f(x) is the polynomial evaluated at x, deriv computes the derivative relative to x, and f[i](x) refers
 * to function 'i' in the sequence.
 * </p>
 *
 * <p>
 * An efficient recursive implementation is used, as suggested in [1].  A more detailed description of the algorithm
 * can be found in [2].
 * </p>
 *
 * <p>
 * [1] David Nister "An Efficient Solution to the Five-Point Relative Pose Problem"
 * Pattern Analysis and Machine Intelligence, 2004<br>
 * [2] D. Hook, P. McAree, "Using Sturm Sequences to Bracket Real Roots of Polynomial Equations", Graphic Gems I,
 * Academic Press, 416-423, 1990
 * </p>
 *
 * @author Peter Abeles
 */
public class SturmSequence {

	// Storage for intermediate results while computing the sequence
	protected Polynomial next, previous;
	protected Polynomial result;

	// Description of the Sturm sequence using an iterative formulation
	protected Polynomial []sequence;
	// Number of functions in the sequence
	protected int sequenceLength;

	// function values
	protected double f[];

	/**
	 * Configures the algorithm.
	 *
	 * @param maxPolySize The maximum number of coefficients on the polynomial being processed.
	 */
	public SturmSequence( int maxPolySize ) {
		next = new Polynomial(maxPolySize);
		previous = new Polynomial(maxPolySize);
		result = new Polynomial(maxPolySize);

		sequence = new Polynomial[maxPolySize+1];
		for( int i = 0; i < sequence.length; i++ )
			sequence[i] = new Polynomial(maxPolySize);
		f = new double[ maxPolySize + 1];
	}

	/**
	 * Compute the Sturm sequence using a more efficient iterative implementation as outlined in [1].  For
	 * this formulation to work the polynomial must have 3 or more coefficients.
	 *
	 * @param poly Input polynomial
	 */
	public void initialize(Polynomial poly) {
		sequence[0].setTo(poly);

		// Special Case: Constant
		if( poly.size <= 1 ) {
			sequenceLength = 1;
			return;
		}

		PolynomialOps.derivative(poly, previous);

		PolynomialOps.divide(sequence[0], previous, result, next);

		negative(next);

		// Special Case: Linear Equation
		if( poly.size == 2 ) {
			sequence[1].setTo(previous);
			sequenceLength = 2;
			return;
		}

		// General cases
		for( int i = 2; i < poly.size; i++ ) {
			PolynomialOps.divide(previous, next, sequence[i-1], result);

			negative(result);

			int degree = result.computeDegree();
			if( degree <= 0 ) {
				if( degree < 0 ) {
					sequence[i].setTo(next);
					sequenceLength = i+1;
				} else {
					sequence[i+1].setTo(result);
					PolynomialOps.divide(next, result, sequence[i], previous);
					sequenceLength = i+2;
				}
				break;
			} else {
				Polynomial temp = previous;
				previous = next;
				next = result;
				result = temp;
			}
		}
	}

	/**
	 * Determines the number of real roots there are in the polynomial within the specified bounds.  Must call
	 * {@link #initialize(Polynomial)} first.
	 *
	 * @param lower lower limit on the bound.
	 * @param upper Upper limit on the bound
	 * @return Number of real roots
	 */
	public int countRealRoots( double lower , double upper ) {
		// There are no roots for constant equations
		if( sequenceLength <= 1 )
			return 0;

		computeFunctions(lower);
		int numLow = countSignChanges();
		computeFunctions(upper);
		int numHigh = countSignChanges();

		return numLow-numHigh;
	}

	/**
	 * Multiplies each coefficient by -1
	 */
	private void negative( Polynomial p ) {
		for( int j = 0; j < p.size; j++ )
			p.c[j] = -p.c[j];
	}

	/**
	 * Looks at the value of each function in the sequence and counts the number of sign changes.  Values
	 * of zero are simply ignored.
	 *
	 * @return number of sign changes
	 */
	protected int countSignChanges() {
		int i = 0;
		for( ; i < sequenceLength; i++ ) {
			if( f[i] != 0 )
				break;
		}
		if( i == sequenceLength )
			return 0;

		int signChanges = 0;
		boolean isPlus = f[i] > 0;

		for( i++; i < sequenceLength; i++ ) {
			double v = f[i];
			if( isPlus ) {
				if( v < 0 ) {
					isPlus = false;
					signChanges++;
				}
			} else if( v > 0 ) {
				isPlus = true;
				signChanges++;
			}
		}

		return signChanges;
	}

	/**
	 * Computes the values for each function in the sequence iterative starting at the end and working its way
	 * towards the beginning..
	 */
	protected void computeFunctions( double value ) {
		f[sequenceLength-1] = sequence[sequenceLength-1].c[0];

		if( Double.isInfinite(value)) {
			f[sequenceLength-2] = multiplyInfinity(sequence[sequenceLength-2].evaluate(value),f[sequenceLength-1]);

			for( int i = sequenceLength-3; i > 0; i-- ) {
				// no need to consider the remainder since this will have a higher degree
				f[i] = multiplyInfinity(sequence[i].evaluate(value),f[i+1]);
			}
		} else {
			f[sequenceLength-2] = sequence[sequenceLength-2].evaluate(value)*f[sequenceLength-1];
			for( int i = sequenceLength-3; i > 0; i-- ) {
				f[i] = sequence[i].evaluate(value)*f[i+1] - f[i+2];
			}
		}

		f[0] = sequence[0].evaluate(value);
	}

	private double multiplyInfinity( double a ,double b ) {
		int signA = sign(a);
		int signB = sign(b);

		int s = signA*signB;

		if( s == 0 )
			return 0;
		if( s == -1 )
			return Double.NEGATIVE_INFINITY;
		else
			return Double.POSITIVE_INFINITY;
	}

	private int sign( double a ) {
		if( Double.isInfinite(a) ) {
			if( a == Double.POSITIVE_INFINITY )
				return 1;
			else
				return -1;
		} else if( a > 0 ) {
			return 1;
		} else if( a < 0 ) {
			return -1;
		}
		return 0;
	}
}
