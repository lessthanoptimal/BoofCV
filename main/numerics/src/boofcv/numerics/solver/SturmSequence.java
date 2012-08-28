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

	Polynomial result;
	// stores results of division operation
	Polynomial next, previous;

	Polynomial []sequence;
	int sequenceDegree[];
	int sequenceLength;

	double f[];

	public SturmSequence( int maxPolySize ) {
		next = new Polynomial(maxPolySize);
		previous = new Polynomial(maxPolySize);
		result = new Polynomial(maxPolySize);

		sequence = new Polynomial[maxPolySize];
		for( int i = 0; i < sequence.length; i++ )
			sequence[i] = new Polynomial(maxPolySize);
		f = new double[ maxPolySize ];
		sequenceDegree = new int[ maxPolySize ];
	}

	public void setPolynomial( Polynomial poly ) {

		sequence[0].setTo(poly);
		PolynomialOps.derivative(poly, previous);

		PolynomialOps.divide(sequence[0], previous, result, next);

		negative(next);

		for( int i = 2; i < poly.size; i++ ) {
			PolynomialOps.divide(previous, next, sequence[i-1], result);

			negative(result);

			if( result.computeDegree() <= 0 ) {
				sequence[i].setTo(next);
				sequence[i+1].c[0] = result.c[0];
				sequence[i+1].size = 1;
				sequenceLength = i+2;
				break;
			} else {
				Polynomial temp = previous;
				previous = next;
				next = result;
				result = temp;
			}
		}
	}

	public int countRealRoots( double low , double upper ) {
		computeFunctions(low);
		int numLow = countSignChanges();
		computeFunctions(upper);
		int numHigh = countSignChanges();

		return numLow-numHigh;
	}

	private void negative( Polynomial p ) {
		for( int j = 0; j < p.size; j++ )
			p.c[j] = -p.c[j];
	}

	public int countSignChanges() {
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

	public void computeFunctions( double value ) {

		f[sequenceLength-1] = sequence[sequenceLength-1].c[0];
		f[sequenceLength-2] = sequence[sequenceLength-2].evaluate(value);

		if( Double.isInfinite(value)) {
			for( int i = sequenceLength-3; i > 0; i-- ) {
				if( sequence[i].evaluate(value) > 0 )
					f[i] = f[i+1];
				else if( f[i+1] > 0 )
					f[i] = Double.NEGATIVE_INFINITY;
				else
					f[i] = Double.POSITIVE_INFINITY;
			}
		} else {
			for( int i = sequenceLength-3; i > 0; i-- ) {
				f[i] = sequence[i].evaluate(value)*f[i+1] - f[i+2];
			}
		}

		f[0] = sequence[0].evaluate(value);
	}

	private static class Helper
	{
		double k;
		double m;
	}
}
