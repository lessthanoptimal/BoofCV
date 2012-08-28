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

import org.ejml.data.Complex64F;

/**
 * When given a root finding algorithm which searches for all the real roots, it selects the one the smallest
 * error as the returned root.  Error is measured by evaluating the polynomial at the root and seeing how
 * far away from zero it is.
 *
 * @author Peter Abeles
 */
public class FindBestRealRoot implements PolynomialFindRealRoot {

	// Root finder which
	private PolynomialFindAllRoots rootFinder;

	// does a real root exist?
	boolean existRealRoots;
	// the best real root that was found
	double bestRoot;

	public FindBestRealRoot( PolynomialFindAllRoots rootFinder ) {
		this.rootFinder = rootFinder;
	}

	@Override
	public boolean compute( Polynomial poly ) {

		existRealRoots = true;

		if( !rootFinder.process(poly) ) {
			return false;
		}

		// find the root with the smallest error
		bestRoot = 0;
		double bestError = Double.MAX_VALUE;

		for( Complex64F root : rootFinder.getRoots() ) {
			System.out.println(root);
			// consider the real part of all roots.  Some times a real root is flagged as being
			// imaginary due to round off error
			if( root.isReal() ) {
//				double error = Math.abs(poly.evaluate(root.real));
				double error = Math.abs(root.imaginary);
//				double error = root.real;
				if( error < bestError ) {
//				if( bestRoot < root.real ) {
//					System.out.println("best = "+root+"  error "+error);
					bestError = error;
					bestRoot = root.real;
				}
			}
		}

		System.out.println("Best Root = "+bestRoot);

		if( bestError == Double.MAX_VALUE ) {
			existRealRoots = false;
			return false;
		}

		return true;
	}

	@Override
	public double getRoot() {
		return bestRoot;
	}

	@Override
	public boolean hasRealRoots() {
		return existRealRoots;
	}


}
