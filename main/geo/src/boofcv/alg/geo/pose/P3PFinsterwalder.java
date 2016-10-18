/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import georegression.struct.point.Point2D_F64;
import org.ddogleg.solver.Polynomial;
import org.ddogleg.solver.PolynomialRoots;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.Complex64F;

import static boofcv.alg.geo.pose.P3PGrunert.computeCosine;
import static boofcv.alg.geo.pose.P3PGrunert.pow2;

/**
 * <p>
 * Solves for the 3 unknown distances between camera center and 3 observed points by finding a root of a cubic
 * polynomial and the roots of two quadratic polynomials.  Proposed by Finsterwalder in 1903, this implementation
 * is based off the discussion in [1]. There are up to four solutions.
 * </p>
 *
 * <p> See {@link P3PLineDistance} for a more detailed problem description. </p>
 *
 * <p>
 * [1] Haralick, Robert M. and Lee, Chung-Nan and Ottenberg, Karsten and Nolle, Michael, "Review and analysis of
 * solutions of the three point perspective pose estimation problem"  Int. J. Comput. Vision, 1994 vol 13, no. 13,
 * pages 331-356
 * </p>
 *
 * @author Peter Abeles
 */
public class P3PFinsterwalder implements P3PLineDistance {

	// storage for solutions
	private FastQueue<PointDistance3> solutions = new FastQueue<>(4, PointDistance3.class, true);

	// square of a,b,c
	private double a2,b2,c2;

	// cosine of the angle between lines (1,2) , (1,3) and (2,3)
	private double cos12,cos13,cos23;

	// storage for intermediate results
	double p,q;

	// used to solve the 4th order polynomial
	private PolynomialRoots rootFinder;

	// polynomial which is to be solved
	private Polynomial poly = new Polynomial(4);

	/**
	 * Configure
	 *
	 * @param rootFinder Root finder for a 3rd order polynomial with real roots
	 */
	public P3PFinsterwalder(PolynomialRoots rootFinder) {
		this.rootFinder = rootFinder;
	}

	/**
	 * @inheritDoc
	 */
	public boolean process( Point2D_F64 obs1 , Point2D_F64 obs2, Point2D_F64 obs3,
							double length23 , double length13 , double length12 ) {

		solutions.reset();

		cos12 = computeCosine(obs1,obs2); // cos(gama)
		cos13 = computeCosine(obs1,obs3); // cos(beta)
		cos23 = computeCosine(obs2,obs3); // cos(alpha)

		double a = length23, b = length13, c = length12;

		double a2_d_b2 = (a/b)*(a/b);
		double c2_d_b2 = (c/b)*(c/b);

		a2=a*a;  b2=b*b;  c2 = c*c;

//		poly.c[0] = a2*(a2*pow2(sin13) - b2*pow2(sin23));
//		poly.c[1] = b2*(b2-c2)*pow2(sin23) + a2*(a2 + 2*c2)*pow2(sin13) + 2*a2*b2*(-1 + cos23*cos13*cos12);
//		poly.c[2] = b2*(b2-a2)*pow2(sin12) + c2*(c2 + 2*a2)*pow2(sin13) + 2*b2*c2*(-1 + cos23*cos13*cos12);
//		poly.c[3] = c2*(c2*pow2(sin13) - b2*pow2(sin12) );

		// Auto generated code + hand simplification.  See P3PFinsterwalder.py  I prefer it over the equations found
		// in the paper (commented out above) since it does not require sin(theta).
		poly.c[0] = a2*(a2*(1 - pow2(cos13)) + b2*(pow2(cos23) - 1));
		poly.c[1] = 2*a2*b2*(cos12*cos13*cos23 - 1) + a2*(a2 + 2*c2)*(1 - pow2(cos13)) + b2*(b2 - c2)*( 1 - pow2(cos23));
		poly.c[2] = 2*c2*b2*(cos12*cos13*cos23 - 1) + c2*(c2 + 2*a2)*(1 - pow2(cos13)) + b2*(b2 - a2)*( 1 - pow2(cos12));
		poly.c[3] = c2*(b2*(pow2(cos12) - 1) + c2*( 1 - pow2(cos13)));

		if( poly.computeDegree() < 0 )
			return false;

		if( !rootFinder.process(poly) )
			return false;

		// search for real roots
		Complex64F root = null;
		for( Complex64F r : rootFinder.getRoots() ) {
			if( r.isReal() ) {
				root = r;
				break;
			}
		}

		if( root == null )
			return false;

		double lambda = root.real;

		double A = 1 + lambda;
		double B = -cos23;
		double C = 1 - a2_d_b2 - lambda*c2_d_b2;
		double D = -lambda*cos12;
		double E = (a2_d_b2 + lambda*c2_d_b2)*cos13;
		double F = -a2_d_b2 + lambda*(1-c2_d_b2);

		p = Math.sqrt(B*B - A*C);
		q = Math.signum(B*E - C*D)*Math.sqrt(E*E - C*F);

		computeU((-B+p)/C,(-E+q)/C);
		computeU((-B-p)/C,(-E-q)/C);

		return true;
	}

	private void computeU( double m , double n ) {
		// The paper also has a few type-os in this section
		double A = b2 - m*m*c2;
		double B = c2*(cos13 - n)*m - b2*cos12;
		double C = -c2*n*n + 2*c2*n*cos13 + b2 - c2;

		double insideSqrt = B*B - A*C;
		if( insideSqrt < 0 )
			return;

		double u_large = -Math.signum(B)*(Math.abs(B) + Math.sqrt(insideSqrt))/A;
		double u_small = C/(A*u_large);

		computeSolution(u_large,u_large*m + n);
		computeSolution(u_small,u_small*m + n);
	}

	private void computeSolution( double u , double v ) {

		double bottom = u*u + v*v - 2*u*v*cos23;
		if( bottom == 0 )
			return;

		double inner = a2 / bottom;

		if( inner >= 0 ) {
			PointDistance3 s = solutions.grow();
			s.dist1 = Math.sqrt(inner);
			s.dist2 = s.dist1*u;
			s.dist3 = s.dist1*v;
		}
	}

	/**
	 * @inheritDoc
	 */
	public FastQueue<PointDistance3> getSolutions() {
		return solutions;
	}
}
