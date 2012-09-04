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
 * Uses a Sturm sequence to bound the location of real roots in the polynomial.  After the roots have been found
 * to the specific precision they are then refined.  If a root has not been found to within tolerance during bisection
 * then the current best estimate will be used during refinement.
 * </p>
 *
 * <P>
 * NOTE: Polynomial division by small coefficients can produce erratic results.  There are systems that the Eigenvalue
 * method will produce accurate solutions for but that this algorithm will fail at.  An example of one such system
 * is provided below.  Maybe someone can come up with a fix/reformulation which works better and is still fast.<br>
 * <pre>
 * Polynomial.wrap(0.06496129844668003,-0.20388125146277708,-0.5346822141623102,3.8451325925247914,
 *				-8.125384551749551,7.281661653591961,-0.1827681555908356,-4.918274060516843,
 *				3.6136415842421954,-0.8418091530846867,5.662137425588298E-15)
 * </pre>
 * </P>
 *
 * @author Peter Abeles
 */
public class FindRealRootsSturm {

	// used to bound the location of real roots
	private SturmSequence sturm;

	// stores the values of found real roots
	private double roots[];
	// number of real roots found
	private int numRoots;

	// if > 0 it will search for roots within [-radius,radius] otherwise it will search for all real roots
	private double searchRadius;
	// how accurately it tries to find a real root
	private double boundTolerance;
	// maximum number of iterations when performing bound operations
	private int maxBoundIterations;
	// maximum number of iterations when refining the root
	private int maxRefineIterations;

	// search regions for roots.  If the search is all numbers then all 3 regions can be searched,
	// otherwise just the first
	Bound region0 = new Bound();
	Bound region1 = new Bound();
	Bound region2 = new Bound();


	/**
	 * Configures search parameters.
	 *
	 * @param maxCoefficients The maximum number of coefficients a polynomial will have.
	 * @param searchRadius If > 0 then roots are searched for inside of -searchRadius <= x <= searchRadius.  If
	 *                     <= 0 then it will find all roots.
	 * @param boundTolerance How accurately roots are found using bisection.  Should be close enough that refinement can
	 *                       accurately converge to the correct root.
	 * @param maxBoundIterations Maximum number of iterations each step can perform when bounding.
	 * @param maxRefineIterations Maximum number of iterations when refining.
	 */
	public FindRealRootsSturm(int maxCoefficients, double searchRadius , double boundTolerance,
							  int maxBoundIterations , int maxRefineIterations ) {
		if( Double.isInfinite(searchRadius) )
			searchRadius = -1;
		sturm = new SturmSequence(maxCoefficients);
		this.searchRadius = searchRadius;
		this.boundTolerance = boundTolerance;
		this.maxBoundIterations = maxBoundIterations;
		this.maxRefineIterations = maxRefineIterations;

		roots = new double[maxCoefficients];
	}

	public double[] getRoots() {
		return roots;
	}
	
	public int getNumberOfRoots() {
		return numRoots;
	}

	public int getMaxRoots() {
		return roots.length;
	}

	/**
	 * Find real roots for the specified polynomial.
	 *
	 * @param poly Polynomial which has less than or equal to the number of coefficients specified in this class's
	 *             constructor.
	 */
	public void process( Polynomial poly ) {
		sturm.initialize(poly);

		if( searchRadius <= 0 )
			numRoots = sturm.countRealRoots(Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY);
		else
			numRoots = sturm.countRealRoots(-searchRadius,searchRadius);

		if( numRoots == 0 )
			return;

		if( searchRadius <= 0 )
			handleAllRoots();
		else {
			boundEachRoot(-searchRadius,searchRadius,0,numRoots);
		}

		// improve the accuracy
		for( int i = 0; i < numRoots; i++ ) {
			roots[i] = PolynomialOps.refineRoot(poly, roots[i], maxRefineIterations);
		}
	}


	/**
	 * Search for all real roots.  First find a region about 0 which contains at least one root.  Then search
	 * above and below.
	 */
	private void handleAllRoots() {
		int totalFound = 0;
		double r = 1;
		int iter = 0;

		// increase the search region centered around 0 until at least one root has been found
		while( iter < maxBoundIterations && (totalFound=sturm.countRealRoots(-r,r)) == 0 ) {
			r = 2*r*r;
		}
		if( Double.isInfinite(r) )
			throw new RuntimeException("r is infinite");
		if( iter >= maxBoundIterations)
			throw new RuntimeException("Too many iterations when searching center region");

		boundEachRoot(-r,r,0,totalFound);

		int target;
		// search for roots below the center bound
		if( totalFound < numRoots && (target = sturm.countRealRoots(Double.NEGATIVE_INFINITY,-r)) > 0 ) {
			double upper = -r;
			double width = r;

			iter = 0;
			while( iter < maxBoundIterations && target > 0 ) {
				int N = sturm.countRealRoots(upper-width,upper);
				if( N != 0 ) {
					boundEachRoot(upper-width,upper,totalFound,N);
					target -= N;
					totalFound += N;
				}
				upper -= width;
				width = 2*width*width;
			}
			if( iter >= maxBoundIterations)
				throw new RuntimeException("Too many iterations when searching lower region");
		}

		if( totalFound < numRoots && (target = sturm.countRealRoots(r,Double.POSITIVE_INFINITY)) > 0 ) {
			double lower = r;
			double width = r;

			iter = 0;
			while( iter < maxBoundIterations && target > 0 ) {
				int N = sturm.countRealRoots(lower,lower+width);
				if( N != 0 ) {
					boundEachRoot(lower,lower+width,totalFound,N);
					target -= N;
					totalFound += N;
				}
				lower += width;
				width = 2*width*width;
			}
			if( iter >= maxBoundIterations)
				throw new RuntimeException("Too many iterations when searching upper region");
		}
	}

	/**
	 * Searches for a single real root inside the range.  Only one root is assumed to be inside
	 *
	 * @param l lower value of search range
	 * @param u upper value of search range
	 * @param index
	 */
	private void bisectionRoot( double l , double u , int index ) {
		// use bisection until there is an estimate within tolerance
		int iter = 0;
		while( u-l > boundTolerance*Math.abs(l) && iter++ < maxBoundIterations) {
			double m = (l+u)/2.0;
			int numRoots = sturm.countRealRoots(m,u);
			if( numRoots == 1 ) {
				l = m;
			} else {
// In systems where some coefficients are close to zero the Sturm sequence starts to yield erratic results.
// In this case, certain basic assumptions are broken and a garbage solution is returned.  The EVD method
// still seems to yield a solution in these cases.  Maybe a different forumation would improve its numerical
// stability?  The problem seems to lie with polynomial division by very small coefficients
//				if( sturm.countRealRoots(l,m) != 1 ) {
//					throw new RuntimeException("Oh Crap");
//				}
				u = m;
			}
		}

		// assign the root to the mid point between the bounds
		roots[index] = (l+u)/2.0;
	}

	/**
	 * Finds a crude upper and lower bound for each root that includes only one root.
	 *
	 * NOTE: Performance could be improved with better book keeping.  For example, if it knows a bound with
	 * for two roots, then one for one root it then knows the bounds for both roots.
	 */
	private void boundEachRoot( double l , double u , int startIndex , int numRoots ) {

		// Find an upper and lower bound which contains one real root only
		int iter = 0;
		double allUpper = u;
		int root = 0;
		while( root < numRoots && iter++ < maxBoundIterations) {
			double m = (l+u)/2.0;
			int found = sturm.countRealRoots(l,m);
			if( found == 0 ) {
				l = m;
			} else if( found == 1 ) {
				bisectionRoot(l,m,startIndex + root++);
				l = m; u = allUpper;
				iter = 0;
			} else {
				u = m;
			}

			if( iter >= maxBoundIterations ) {
				// taking too long to bound these roots, just skip over these
				l = m; u = allUpper;
				root += found;
				iter = 0;
			}
		}

		if( iter >= maxBoundIterations)
			throw new RuntimeException("Too many iterations finding upper and lower bounds");
	}

	private static class Bound
	{
		double l,u;
	}

}
