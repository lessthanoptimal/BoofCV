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
 * @author Peter Abeles
 */
public class FindRealRootsSturm {

	SturmSequence sturm;

	double roots[];
	int numRoots;

	double searchRadius;
	double boundTolerance;
	int maxIterations;
	int maxRefineIterations=50;


	// current search bounds
	double l,u;


	public FindRealRootsSturm(int maxCoefficients, double searchRadius , double boundTolerance,
							  int maxIterations ) {
		sturm = new SturmSequence(maxCoefficients);
		this.searchRadius = searchRadius;
		this.boundTolerance = boundTolerance;
		this.maxIterations = maxIterations;

		roots = new double[maxCoefficients];
	}

	public double[] getRoots() {
		return roots;
	}
	
	public int getNumberOfRoots() {
		return numRoots;
	}

	public void process( Polynomial poly ) {
		sturm.initialize(poly);

		countRoots();

		if( numRoots == 0 )
			return;

		boundEachRoot();

		// improve the accuracy
		for( int i = 0; i < numRoots; i++ ) {
			roots[i] = PolynomialOps.refineRoot(poly, roots[i], maxRefineIterations);
		}
	}

	private void countRoots() {
		int iter;
		if( Double.isInfinite(searchRadius) ) {
			numRoots = sturm.countRealRoots(Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY);
			double r = 1;
			iter = 0;

			// increase the search region until all the roots have been found
			while( iter < maxIterations  && sturm.countRealRoots(-r,r) != numRoots ) {
				if( sturm.countRealRoots(-r,r) > numRoots ) {
					sturm.sequence[0].print();
					throw new RuntimeException("BUG!!");
				}                                     //  TODO CHANGE <
				r = 2*r*r;
			}
			if( Double.isInfinite(r) )
				throw new RuntimeException("r is infinite");

			l = -r; u = r;

			if( iter >= maxIterations ) throw new RuntimeException("Too many iterations bounding all roots");
		} else {
			numRoots = sturm.countRealRoots(-searchRadius,searchRadius);
			l = -searchRadius; u = searchRadius;
		}
	}

	private void bisectionRoot( double l , double u , int index ) {
		// use bisection until there is an estimate within tolerance
		int iter = 0;
		while( u-l > boundTolerance*Math.abs(l) && iter++ < maxIterations ) {
			double m = (l+u)/2.0;
			if( sturm.countRealRoots(m,u) == 1 ) {
				l = m;
			} else {
				if( sturm.countRealRoots(l,m) != 1 ) {
					throw new RuntimeException("Oh Crap");
				}
				u = m;
			}
		}

		// assign the root to the mid point between the bounds
		roots[index] = (l+u)/2.0;
	}

	private void boundEachRoot() {

		// Find an upper and lower bound which contains one real root only
		int iter = 0;
		double allUpper = u;
		int root = 0;
		while( root < numRoots && iter++ < maxIterations ) {
			double m = (l+u)/2.0;
			int found = sturm.countRealRoots(l,m);
			if( found == 0 ) {
				l = m;
			} else 	if( found == 1 ) {
				bisectionRoot(l,m,root++);
				l = m; u = allUpper;
				iter = 0;
			} else {
				u = m;
			}
		}

		if( iter >= maxIterations )
			throw new RuntimeException("Too many iterations finding upper and lower bounds");
	}

}
