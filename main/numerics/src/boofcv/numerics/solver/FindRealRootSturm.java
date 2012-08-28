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
public class FindRealRootSturm implements PolynomialFindRealRoot {

	SturmSequence sturm;

	double root;

	double searchRadius;
	double boundTolerance;
	int maxIterations;

	public FindRealRootSturm(int maxCoefficients, double searchRadius, double boundTolerance, int maxIterations) {
		sturm = new SturmSequence(maxCoefficients);
		this.searchRadius = searchRadius;
		this.boundTolerance = boundTolerance;
		this.maxIterations = maxIterations;
	}

	@Override
	public boolean compute( Polynomial poly ) {
		sturm.setPolynomial(poly);

		// See if he bound contains
		double l = -searchRadius;
		double u = searchRadius;
		int found = sturm.countRealRoots(l,u);

		if( found == 0 ) {
			return false;
		} else if( found > 1 ) {
			// reduce the bound until only one root is contained
			int iter = 0;
			while( found != 1 && iter++ < maxIterations ) {
				double m = (l+u)/2;
				found = sturm.countRealRoots(m,u);
				if( found == 0 ) {
					found = sturm.countRealRoots(l,m);
					u = m;
				} else {
					l = m;
				}
			}
			if( found == 0 )
				return false;
		}

		// use bisection until there is an estimate within tolerance
		int iter = 0;
		while( u-l > boundTolerance && iter++ < maxIterations ) {
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

		// refine the crude estimate
		root = PolynomialOps.refineRoot(poly,(l+u)/2.0,maxIterations);

		if( root < l || root > u ) {
			System.out.println("Drifted outside of bounds!");
		}

		return true;
	}

	@Override
	public double getRoot() {
		return root;
	}

	@Override
	public boolean hasRealRoots() {
		return sturm.countRealRoots(Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY) != 0;
	}


}
