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

package boofcv.alg.geo.pose;

import boofcv.numerics.solver.Polynomial;
import boofcv.numerics.solver.PolynomialRoots;
import boofcv.struct.FastQueue;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.Complex64F;

import java.util.List;

/**
 * <p>
 * Solves for the 3 unknown distances between camera center and 3 observed points by finding the roots of a 4th order
 * polynomial,  This is probably the first solution to the P3P problem and first proposed in 1841 by Grunert.  This
 * implementation is based off the discussion in [1]. There are up to four solutions.
 * </p>
 *
 * <p>
 * Problem Description: Three points (P1,P2,P3) in 3D space are observed in the image plane in normalized image
 * coordinates (q1,q2,q3).  Solve for the distance between the camera's origin and each of the three points.
 * </p>
 *
 * <p>
 * [1] Haralick, Robert M. and Lee, Chung-Nan and Ottenberg, Karsten and Nolle, Michael, "Review and analysis of
 * solutions of the three point perspective pose estimation problem"  Int. J. Comput. Vision, 1994 vol 13, no. 13,
 * pages 331-356
 * </p>
 *
 * @author Peter Abeles
 */
public class P3PGrunert {

	// used to solve the 4th order polynomial
	private PolynomialRoots rootFinder;

	// polynomial which is to be solved
	private Polynomial poly = new Polynomial(5);

	// storage for solutions
	private FastQueue<PointDistance3> solutions = new FastQueue<PointDistance3>(4,PointDistance3.class,true);

	public P3PGrunert(PolynomialRoots rootFinder) {
		this.rootFinder = rootFinder;
	}

	/**
	 * Solve for the distance between the camera's origin and each of the 3 points.
	 *
	 * @param obs1 Observation of P1 in normalized image coordinates
	 * @param obs2 Observation of P1 in normalized image coordinates
	 * @param obs3 Observation of P1 in normalized image coordinates
	 * @param length23 Distance between points P2 and P3
	 * @param length13 Distance between points P1 and P3
	 * @param length12 Distance between points P1 and P2
	 * @return true if successful or false if it failed to generate any solutions
	 */
	public boolean process( Point2D_F64 obs1 , Point2D_F64 obs2, Point2D_F64 obs3,
							double length23 , double length13 , double length12 ) {

		double cos12 = computeCosine(obs1,obs2);
		double cos13 = computeCosine(obs1,obs3);
		double cos23 = computeCosine(obs2,obs3);

		double a = length23, b = length13, c = length12;

		// divide out numbers before multiplying them.  less overflow/underflow that way
		double a2_div_b2 = (a/b)*(a/b);
		double c2_div_b2 = (c/b)*(c/b);
		double a2_m_c2_div_b2 = a2_div_b2 - c2_div_b2;
		double a2_p_c2_div_b2 = a2_div_b2 + c2_div_b2;

		poly.c[0] = -4*a2_div_b2*pow2(cos12) + pow2(a2_m_c2_div_b2 + 1);
		poly.c[1] = 4*(-a2_m_c2_div_b2*(1 + a2_m_c2_div_b2)*cos13 + 2*a2_div_b2*pow2(cos12)*cos13 - (1-a2_p_c2_div_b2)*cos23*cos12);
		poly.c[2] = 2*(pow2(a2_m_c2_div_b2) - 1 + 2*pow2(a2_m_c2_div_b2)*pow2(cos13) + 2*(1-c2_div_b2)*pow2(cos23) - 4*a2_p_c2_div_b2*cos12*cos13*cos23 + 2*(1-a2_div_b2)*pow2(cos12));
		poly.c[3] = 4*(a2_m_c2_div_b2*(1-a2_m_c2_div_b2)*cos13 - (1 - a2_p_c2_div_b2)*cos23*cos12 + 2*c2_div_b2*pow2(cos23)*cos13);
		poly.c[4] = -4*c2_div_b2*cos23*cos23 + pow2(a2_m_c2_div_b2 - 1);

		// solve for real roots
		solutions.reset();
		if( !rootFinder.process(poly) )
			return false;

		List<Complex64F> roots = rootFinder.getRoots();

		for( Complex64F r : roots ) {
			if( !r.isReal() ) {
				continue;
			}

			double v = r.real;
//			double u = ((-1 + (a*a - c*c)/(b*b))*v*v - 2*((a*a - c*c)/(b*b))*cos13*v + 1 + (a*a - c*c)/(b*b))/
//					(2*(cos12 - v*cos23));
			double u = ((-1 + a2_div_b2 - c2_div_b2)*v*v - 2*(a2_div_b2 - c2_div_b2)*cos13*v + 1 + a2_div_b2 - c2_div_b2)/
					(2*(cos12 - v*cos23));

			// compute the distance of each point
			PointDistance3 s = solutions.pop();

			s.dist1 = Math.sqrt(a*a/(u*u + v*v - 2*u*v*cos23));
			s.dist2 = s.dist1*u;
			s.dist3 = s.dist1*v;
		}

		return solutions.size() != 0;
	}

	public static double pow2( double a ) {
		return a*a;
	}

	public static double computeCosine( Point2D_F64 a , Point2D_F64 b ) {
		double top = a.x*b.x + a.y*b.y + 1;
		double bottom = Math.sqrt(a.x*a.x + a.y*a.y + 1) * Math.sqrt(b.x*b.x + b.y*b.y + 1);

		return top/bottom;
	}

	/**
	 * Returns possible solutions
	 */
	public FastQueue<PointDistance3> getSolutions() {
		return solutions;
	}
}
