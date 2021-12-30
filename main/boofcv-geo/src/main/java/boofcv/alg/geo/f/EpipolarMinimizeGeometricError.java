/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.f;

import boofcv.alg.geo.PerspectiveOps;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import org.ddogleg.solver.Polynomial;
import org.ddogleg.solver.PolynomialOps;
import org.ddogleg.solver.PolynomialRoots;
import org.ddogleg.solver.RootFinderType;
import org.ejml.data.Complex_F64;
import org.ejml.data.DMatrixRMaj;

import java.util.List;

import static boofcv.misc.BoofMiscOps.pow2;

/**
 * Given point correspondences x[1] and x[2] and a fundamental matrix F, compute the
 * correspondences x'[1] and x'[2] which minimize the geometric error
 * subject to x'[2] F' x'[1] = 0
 *
 * <p>
 * Page 318 in: R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </p>
 *
 * @author Peter Abeles
 */
public class EpipolarMinimizeGeometricError {

	// Ft = inv(T2')*F*inv(T1)
	DMatrixRMaj Ft = new DMatrixRMaj(3, 3);

	// [1 0 -x1;0 1 -y1; 0 0 1];
	DMatrixRMaj T1 = new DMatrixRMaj(3, 3);
	// [1 0 -x2;0 1 -y2; 0 0 1];
	DMatrixRMaj T2 = new DMatrixRMaj(3, 3);

	FundamentalExtractEpipoles extract = new FundamentalExtractEpipoles();
	Point3D_F64 e1 = new Point3D_F64();
	Point3D_F64 e2 = new Point3D_F64();

	// rotation matrices
	DMatrixRMaj R1 = new DMatrixRMaj(3, 3);
	DMatrixRMaj R2 = new DMatrixRMaj(3, 3);

	// lines
	Vector3D_F64 l1 = new Vector3D_F64();
	Vector3D_F64 l2 = new Vector3D_F64();

	PolynomialRoots rootFinder = PolynomialOps.createRootFinder(6, RootFinderType.EVD);
	Polynomial poly = new Polynomial(6);

	double solutionT;

	/**
	 * Minimizes the geometric error
	 *
	 * @param F21 (Input) Fundamental matrix x2 * F21 * x1 == 0
	 * @param x1 (Input) Point 1 x-coordinate. Pixels
	 * @param y1 (Input) Point 1 y-coordinate. Pixels
	 * @param x2 (Input) Point 2 x-coordinate. Pixels
	 * @param y2 (Input) Point 2 y-coordinate. Pixels
	 * @param p1 (Output) Point 1. Pixels
	 * @param p2 (Output) Point 2. Pixels
	 * @return true if a solution was found or false if it failed
	 */
	public boolean process( DMatrixRMaj F21,
							double x1, double y1, double x2, double y2,
							Point2D_F64 p1, Point2D_F64 p2 ) {
		// translations used to move points to the origin
		assignTinv(T1, x1, y1);
		assignTinv(T2, x2, y2);

		// take F to the new coordinate system
		// F1 = T2'*F*T1
		PerspectiveOps.multTranA(T2, F21, T1, Ft);

		extract.process(Ft, e1, e2);

		// normalize so that e[x]*e[x] + e[y]*e[y] == 1
		normalizeEpipole(e1);
		normalizeEpipole(e2);

		assignR(R1, e1);
		assignR(R2, e2);

		// Ft = R2*Ft*R1'
		PerspectiveOps.multTranC(R2, Ft, R1, Ft);

		double f1 = e1.z;
		double f2 = e2.z;
		double a = Ft.get(1, 1);
		double b = Ft.get(1, 2);
		double c = Ft.get(2, 1);
		double d = Ft.get(2, 2);

		if (!solvePolynomial(f1, f2, a, b, c, d))
			return false;

		if (!selectBestSolution(rootFinder.getRoots(), f1, f2, a, b, c, d))
			return false;

		// find the closeset point on the two lines below to the origin
		double t = solutionT;
		l1.setTo(t*f1, 1, -t);
		l2.setTo(-f2*(c*t + d), a*t + b, c*t + d);
		closestPointToOrigin(l1, e1); // recycle epipole storage
		closestPointToOrigin(l2, e2);

		// original coordinates
		originalCoordinates(T1, R1, e1);
		originalCoordinates(T2, R2, e2);

		// back to 2D coordinates
		p1.setTo(e1.x/e1.z, e1.y/e1.z);
		p2.setTo(e2.x/e2.z, e2.y/e2.z);

		return true;
	}

	void closestPointToOrigin( Vector3D_F64 l, Point3D_F64 p ) {
		p.x = -l.x*l.z;
		p.y = -l.y*l.z;
		p.z = l.x*l.x + l.y*l.y;
	}

	void originalCoordinates( DMatrixRMaj T, DMatrixRMaj R, Point3D_F64 p ) {
		GeometryMath_F64.multTran(R, p, p);
		GeometryMath_F64.mult(T, p, p);
	}

	/**
	 * Solves for the roots of an ugly polynomial defined in 12.7 in book [1]
	 *
	 * Coefficients found using Sage Math
	 *
	 * {@code
	 * a,b,c,d,f1,f2,t = var('a,b,c,d,f1,f2,t')
	 * g = t*((a*t+b)^2 + f2^2*(c*t+d)^2)^2 - (a*d-b*c)*(1+f1^2*t^2)^2*(a*t+b)*(c*t+d)
	 * g.expand().collect(t)
	 * }
	 */
	public boolean solvePolynomial( double f1, double f2, double a, Double b, double c, double d ) {
		double f1_2 = f1*f1;
		double f1_4 = f1_2*f1_2;
		double f2_2 = f2*f2;
		double f2_4 = f2_2*f2_2;
		double a_2 = a*a;
		double b_2 = b*b;
		double c_2 = c*c;
		double d_2 = d*d;
		double b_3 = b_2*b;
		double d_3 = d_2*d;
		double a_4 = a_2*a_2;
		double b_4 = b_2*b_2;
		double c_4 = c_2*c_2;
		double d_4 = d_2*d_2;

		poly.size = 6;
		poly.c[5] = a*b*c_2*f1_4 - a_2*c*d*f1_4;
		poly.c[4] = b_2*c_2*f1_4 - a_2*d_2*f1_4 + c_4*f2_4 + 2*a_2*c_2*f2_2 + a_4;
		poly.c[3] = 2*(3*c_2*d_2*f2_4 + b_2*c_2*f1_2 - a_2*d_2*f1_2 + b_2*c_2*f2_2 + 4*a*b*c*d*f2_2 + a_2*d_2*f2_2 + 3*a_2*b_2);
		poly.c[2] = (4*c*d_3*f2_4 + 2*b_2*c*d*f1_2 - 2*a*b*d_2*f1_2 + 4*b_2*c*d*f2_2 + 4*a*b*d_2*f2_2 + 4*a*b_3 + a*b*c_2 - a_2*c*d);
		poly.c[1] = d_4*f2_4 + 2*b_2*d_2*f2_2 + b_4 + b_2*c_2 - a_2*d_2;
		poly.c[0] = b_2*c*d - a*b*d_2;

		return rootFinder.process(poly);
	}

	boolean selectBestSolution( List<Complex_F64> roots,
								double f1, double f2, double a, Double b, double c, double d ) {


		// cost at t=infinty, must do better than this
		double best = 1.0/(f1*f1) + c*c/(a*a + f2*f2*c*c);
		int bestIndex = -1;

		for (int i = 0; i < roots.size(); i++) {
			Complex_F64 cr = roots.get(i);

			if (!cr.isReal())
				continue;

			double t = cr.real;

			double left = t*t/(1 + f1*f1*t*t);
			double right = pow2(c*t + d)/(pow2(a*t + b) + f2*f2*pow2(c*t + d));

			double squaredDistance = left + right;
			if (squaredDistance < best) {
				best = squaredDistance;
				bestIndex = i;
				solutionT = t;
			}
		}

		return bestIndex != -1;
	}

	// @formatter:off
	static void assignTinv( DMatrixRMaj T, double x, double y ) {
		T.data[0] = T.data[4] = T.data[8] = 1;
		T.data[2] = x; T.data[5]= y;
	}

	static void assignR( DMatrixRMaj R , Point3D_F64 e ) {
		R.data[0] =  e.x; R.data[1] = e.y;
		R.data[3] = -e.y; R.data[4] = e.x;
		R.data[8] = 1;
	}
	// @formatter:on

	static void normalizeEpipole( Point3D_F64 e ) {
		double n = e.x*e.x + e.y*e.y;
		e.divideIP(Math.sqrt(n));
	}
}
