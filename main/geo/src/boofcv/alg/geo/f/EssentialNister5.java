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

package boofcv.alg.geo.f;

import boofcv.alg.geo.AssociatedPair;
import boofcv.numerics.solver.Polynomial;
import boofcv.numerics.solver.PolynomialFindAllRoots;
import boofcv.numerics.solver.RootFinderCompanion;
import boofcv.struct.FastQueue;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.Complex64F;
import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.DecompositionFactory;
import org.ejml.factory.SingularValueDecomposition;
import org.ejml.ops.CommonOps;
import org.ejml.ops.NormOps;
import org.ejml.simple.SimpleMatrix;
import org.ejml.simple.SimpleSVD;

import java.util.List;

/**
 * <p>
 * Finds the essential matrix given exactly 5 corresponding points.  The approach is described
 * in details in [1] and works by linearlizing the problem then solving for the roots in a polynomial.  It
 * is considered one of the fastest and most stable solutions for the 5-point problem.
 * </p>
 *
 * <p>
 * THIS IMPLEMENTATION DOES NOT CONTAIN ALL THE OPTIMIZATIONS OUTLIED IN [1].  A full implementation is
 * quite involved.
 * </p>
 *
 * <p>
 * NOTE: This solution could be generalized for an arbitrary number of points.  However, it would complicate
 * the algorithm even more and isn't considered to be worth the effort.
 * </p>
 *
 * <p>
 * [1] David Nister "An Efficient Solution to the Five-Point Relative Pose Problem"
 * Pattern Analysis and Machine Intelligence, 2004
 * </p>
 *
 * @author Peter Abeles
 */
public class EssentialNister5 {

	// Linear system describing p'*E*q = 0
	DenseMatrix64F Q = new DenseMatrix64F(5,9);
	// contains the span of A
	DenseMatrix64F V = new DenseMatrix64F(9,9);
	// TODO Try using QR-Factorization as in the paper
	SingularValueDecomposition<DenseMatrix64F> svd = DecompositionFactory.svd(5,9,false,true,false);

	// where all the ugly equations go
	HelperNister5 helper = new HelperNister5();

	// the span containing E
	double []X = new double[9];
	double []Y = new double[9];
	double []Z = new double[9];
	double []W = new double[9];

	// unknowns for E = x*X + y*Y + z*Z + W
	double x,y,z;

	DenseMatrix64F A1 = new DenseMatrix64F(10,10);
	DenseMatrix64F A2 = new DenseMatrix64F(10,10);
	DenseMatrix64F C = new DenseMatrix64F(10,10);

	PolynomialFindAllRoots findRoots = new RootFinderCompanion();
	Polynomial poly = new Polynomial(11);

	// found essential matrix
	FastQueue<DenseMatrix64F> solutions = new FastQueue<DenseMatrix64F>(11,DenseMatrix64F.class,false);

	public EssentialNister5() {
		for( int i = 0; i < solutions.data.length; i++ )
			solutions.data[i] = new DenseMatrix64F(3,3);
	}

	/**
	 * Computes the essential matrix from point correspondences.
	 *
	 * @param points List of points correspondences in normalized image coordinates.
	 * @return true for success or false if a fault has been detected
	 */
	public boolean process( List<AssociatedPair> points ) {
		if( points.size() != 5 )
			throw new IllegalArgumentException("Exactly 5 points are required, not "+points.size());
		solutions.reset();

		// Computes the 4-vector span which contains E.  See equations 7-9
		computeSpan(points);

		// Construct a linear system based on the 10 constraint equations. See equations 5,6, and 10 .
		helper.setNullSpace(X,Y,Z,W);
		helper.setupA1(A1);
		helper.setupA2(A2);

		System.out.println("  Condition A1: "+NormOps.conditionP2(A1));

		// instead of Gauss-Jordan elimination LU decomposition is used to solve the system
//		LinearSolver<DenseMatrix64F> solver = LinearSolverFactory.pseudoInverse(true);
//		solver.setA(A1);
//		solver.solve(A2,C);
		CommonOps.solve(A1, A2, C);

		// construct the z-polynomial matrix.  Equations 11-14
		helper.setDeterminantVectors(C);
		helper.extractPolynomial(poly.getCoefficients());

		if( !findRoots.process(poly) )
			return false;

		poly.print();

		for( Complex64F c : findRoots.getRoots() ) {
			if( !c.isReal() )
				continue;

			solveForXandY(c.real);

			DenseMatrix64F E = solutions.pop();
			E.data[0] = x*X[0] + y*Y[0] + z*Z[0] + W[0];
			E.data[1] = x*X[1] + y*Y[1] + z*Z[1] + W[1];
			E.data[2] = x*X[2] + y*Y[2] + z*Z[2] + W[2];
			E.data[3] = x*X[3] + y*Y[3] + z*Z[3] + W[3];
			E.data[4] = x*X[4] + y*Y[4] + z*Z[4] + W[4];
			E.data[5] = x*X[5] + y*Y[5] + z*Z[5] + W[5];
			E.data[6] = x*X[6] + y*Y[6] + z*Z[6] + W[6];
			E.data[7] = x*X[7] + y*Y[7] + z*Z[7] + W[7];
			E.data[8] = x*X[8] + y*Y[8] + z*Z[8] + W[8];

			System.out.println("  Found E: det = "+CommonOps.det(E));
			System.out.println("          rv   = "+c);
			System.out.println("          root = "+poly.evaluate(c.real));
			System.out.print  ("           SV  =");
			SimpleSVD svd = SimpleMatrix.wrap(E).svd();
			for( int i = 0; i < 3; i++ )
				System.out.printf(" %5.2e",svd.getSingleValue(i));
			System.out.println();

//			SimpleMatrix U = svd.getU();
//			SimpleMatrix S = svd.getW();
//			SimpleMatrix V = svd.getV();
//
//			S.set(0,0,1);
//			S.set(1,1,1);
//			S.set(2,2,0);
//
//			E.set(U.mult(S).mult(V.transpose()).getMatrix());

		}

		return true;
	}

	/**
	 * From the epipolar constraint p2^T*E*p1 = 0 construct a linear system
	 * and find its null space.
	 */
	private void computeSpan( List<AssociatedPair> points ) {

		Q.reshape(points.size(), 9);
		int index = 0;

		for( int i = 0; i < points.size(); i++ ) {
			AssociatedPair p = points.get(i);

			Point2D_F64 a = p.currLoc;
			Point2D_F64 b = p.keyLoc;

			// The points are assumed to be in homogeneous coordinates.  This means z = 1
			Q.data[index++] =  a.x*b.x;
			Q.data[index++] =  a.x*b.y;
			Q.data[index++] =  a.x;
			Q.data[index++] =  a.y*b.x;
			Q.data[index++] =  a.y*b.y;
			Q.data[index++] =  a.y;
			Q.data[index++] =      b.x;
			Q.data[index++] =      b.y;
			Q.data[index++] =  1;
		}

		if( !svd.decompose(Q) )
			throw new RuntimeException("SVD should never fail, probably bad input");

		svd.getV(V,true);

		System.out.println(" DET V = "+CommonOps.det(V));

		// extract the span of solutions for E from the null space
		for( int i = 0; i < 9; i++ ) {
			X[i] = V.unsafe_get(5,i);
			Y[i] = V.unsafe_get(6,i);
			Z[i] = V.unsafe_get(7,i);
			W[i] = V.unsafe_get(8,i);
		}
	}

	/**
	 * Once z is known then x and y can be solved for using the B matrix
	 */
	private void solveForXandY( double z ) {
		this.z = z;

		DenseMatrix64F A = new DenseMatrix64F(3,2);
		DenseMatrix64F Y = new DenseMatrix64F(3,1);

		// solve for x and y using the first two rows of B
		A.data[0] = ((helper.K0*z + helper.K1)*z + helper.K2)*z + helper.K3;
		A.data[1] = ((helper.K4*z + helper.K5)*z + helper.K6)*z + helper.K7;
		Y.data[0] = (((helper.K8*z + helper.K9)*z + helper.K10)*z + helper.K11)*z + helper.K12;

		A.data[2] = ((helper.L0*z + helper.L1)*z + helper.L2)*z + helper.L3;
		A.data[3] = ((helper.L4*z + helper.L5)*z + helper.L6)*z + helper.L7;
		Y.data[1] = (((helper.L8*z + helper.L9)*z + helper.L10)*z + helper.L11)*z + helper.L12;

		A.data[4] = ((helper.L0*z + helper.L1)*z + helper.L2)*z + helper.L3;
		A.data[5] = ((helper.L4*z + helper.L5)*z + helper.L6)*z + helper.L7;
		Y.data[2] = (((helper.L8*z + helper.L9)*z + helper.L10)*z + helper.L11)*z + helper.L12;

		CommonOps.scale(-1,Y);

		DenseMatrix64F x = new DenseMatrix64F(2,1);

		CommonOps.solve(A,Y,x);

		this.x = x.get(0,0);
		this.y = x.get(1,0);
	}

	public List<DenseMatrix64F> getSolutions() {
		return solutions.toList();
	}
}
