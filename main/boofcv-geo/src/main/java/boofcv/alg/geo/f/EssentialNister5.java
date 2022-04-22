/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.AssociatedPair3D;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.solver.Polynomial;
import org.ddogleg.solver.PolynomialRoots;
import org.ddogleg.solver.impl.FindRealRootsSturm;
import org.ddogleg.solver.impl.WrapRealRootsSturm;
import org.ddogleg.struct.DogArray;
import org.ejml.data.Complex_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.dense.row.linsol.qr.SolveNullSpaceQRP_DDRM;
import org.ejml.interfaces.SolveNullSpace;
import org.ejml.interfaces.linsol.LinearSolverDense;

import java.util.List;

/**
 * <p>
 * Finds the essential matrix given exactly 5 corresponding points. The approach is described
 * in details in [1] and works by linearlizing the problem then solving for the roots in a polynomial. It
 * is considered one of the fastest and most stable solutions for the 5-point problem.
 * </p>
 *
 * <p>
 * THIS IMPLEMENTATION DOES NOT CONTAIN ALL THE OPTIMIZATIONS OUTLIED IN [1]. A full implementation is
 * quite involved. Example: SVD instead of the proposed QR factorization.
 * </p>
 *
 * <p>
 * NOTE: This solution could be generalized for an arbitrary number of points. However, it would complicate
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
	private final DMatrixRMaj Q = new DMatrixRMaj(5, 9);
	// contains the span of A
	private final DMatrixRMaj nullspace = new DMatrixRMaj(9, 9);
	private final SolveNullSpace<DMatrixRMaj> solverNull = new SolveNullSpaceQRP_DDRM();

	// where all the ugly equations go
	private final HelperNister5 helper = new HelperNister5();

	// the span containing E
	private final double[] X = new double[9];
	private final double[] Y = new double[9];
	private final double[] Z = new double[9];
	private final double[] W = new double[9];

	// unknowns for E = x*X + y*Y + z*Z + W
	private double x, y, z;

	// Solver for the linear system below
	LinearSolverDense<DMatrixRMaj> solver = LinearSolverFactory_DDRM.linear(10);

	// Storage for linear systems
	private final DMatrixRMaj A1 = new DMatrixRMaj(10, 10);
	private final DMatrixRMaj A2 = new DMatrixRMaj(10, 10);
	private final DMatrixRMaj C = new DMatrixRMaj(10, 10);

	// Used for finding polynomial roots
	private final FindRealRootsSturm sturm = new FindRealRootsSturm(11, -1, 1e-10, 20, 20);
	private final PolynomialRoots rootFinder = new WrapRealRootsSturm(sturm);

	// private PolynomialRoots findRoots = new RootFinderCompanion();
	private final Polynomial poly = new Polynomial(11);

	/**
	 * Computes the essential matrix from point correspondences in normalized image coordinates.
	 *
	 * @param points Input: List of points correspondences in normalized image coordinates
	 * @param solutions Output: Storage for the found solutions. .
	 * @return true for success or false if a fault has been detected
	 */
	public boolean processNormalized( List<AssociatedPair> points, DogArray<DMatrixRMaj> solutions ) {
		if (points.size() != 5)
			throw new IllegalArgumentException("Exactly 5 points are required, not " + points.size());
		solutions.reset();

		// Computes the 4-vector span which contains E. See equations 7-9
		computeSpan(points);

		return solveForSolutions(solutions);
	}

	/**
	 * Computes the essential matrix from point correspondences represented by pointing vectors
	 *
	 * @param points Input: List of points correspondences as 3D pointing vectors
	 * @param solutions Output: Storage for the found solutions. .
	 * @return true for success or false if a fault has been detected
	 */
	public boolean processPointing( List<AssociatedPair3D> points, DogArray<DMatrixRMaj> solutions ) {
		if (points.size() != 5)
			throw new IllegalArgumentException("Exactly 5 points are required, not " + points.size());
		solutions.reset();

		// Computes the 4-vector span which contains E. See equations 7-9
		computeSpan3(points);

		return solveForSolutions(solutions);
	}

	private boolean solveForSolutions( DogArray<DMatrixRMaj> solutions ) {
		// Construct a linear system based on the 10 constraint equations. See equations 5,6, and 10 .
		helper.setNullSpace(X, Y, Z, W);
		helper.setupA1(A1);
		helper.setupA2(A2);

		// instead of Gauss-Jordan elimination LU decomposition is used to solve the system
		if (!solver.setA(A1))
			return false;
		solver.solve(A2, C);

		// construct the z-polynomial matrix. Equations 11-14
		helper.setDeterminantVectors(C);
		helper.extractPolynomial(poly.getCoefficients());

		if (!rootFinder.process(poly))
			return false;

		List<Complex_F64> zeros = rootFinder.getRoots();

		for (int rootIdx = 0; rootIdx < zeros.size(); rootIdx++) {
			Complex_F64 c = zeros.get(rootIdx);

			if (!c.isReal())
				continue;

			solveForXandY(c.real);

			DMatrixRMaj E = solutions.grow();

			for (int i = 0; i < 9; i++) {
				E.data[i] = x*X[i] + y*Y[i] + z*Z[i] + W[i];
			}
		}

		return true;
	}

	/**
	 * From the epipolar constraint p2^T*E*p1 = 0 construct a linear system and find its null space.
	 */
	private void computeSpan( List<AssociatedPair> points ) {

		Q.reshape(points.size(), 9);
		int index = 0;

		for (int i = 0; i < points.size(); i++) {
			AssociatedPair p = points.get(i);

			Point2D_F64 a = p.p2;
			Point2D_F64 b = p.p1;

			// The points are assumed to be in homogeneous coordinates. This means z = 1

			// @formatter:off
			Q.data[index++] =  a.x*b.x;
			Q.data[index++] =  a.x*b.y;
			Q.data[index++] =  a.x;
			Q.data[index++] =  a.y*b.x;
			Q.data[index++] =  a.y*b.y;
			Q.data[index++] =  a.y;
			Q.data[index++] =      b.x;
			Q.data[index++] =      b.y;
			Q.data[index++] =  1;
			// @formatter:on
		}

		if (!solverNull.process(Q, 4, nullspace))
			throw new RuntimeException("Nullspace solver should never fail, probably bad input");

		// extract the span of solutions for E from the null space
		for (int i = 0; i < 9; i++) {
			X[i] = nullspace.unsafe_get(i, 0);
			Y[i] = nullspace.unsafe_get(i, 1);
			Z[i] = nullspace.unsafe_get(i, 2);
			W[i] = nullspace.unsafe_get(i, 3);
		}
	}

	/**
	 * From the epipolar constraint p2^T*E*p1 = 0 construct a linear system and find its null space.
	 * Observations as pointing vectors.
	 */
	private void computeSpan3( List<AssociatedPair3D> points ) {

		Q.reshape(points.size(), 9);
		int index = 0;

		for (int i = 0; i < points.size(); i++) {
			AssociatedPair3D p = points.get(i);

			Point3D_F64 a = p.p2;
			Point3D_F64 b = p.p1;

			// The points are assumed to be in homogeneous coordinates. This means z = 1

			// @formatter:off
			Q.data[index++] =  a.x*b.x;
			Q.data[index++] =  a.x*b.y;
			Q.data[index++] =  a.x*b.z;
			Q.data[index++] =  a.y*b.x;
			Q.data[index++] =  a.y*b.y;
			Q.data[index++] =  a.y*b.z;
			Q.data[index++] =  a.z*b.x;
			Q.data[index++] =  a.z*b.y;
			Q.data[index++] =  a.z*b.z;
			// @formatter:on
		}

		if (!solverNull.process(Q, 4, nullspace))
			throw new RuntimeException("Nullspace solver should never fail, probably bad input");

		// extract the span of solutions for E from the null space
		for (int i = 0; i < 9; i++) {
			X[i] = nullspace.unsafe_get(i, 0);
			Y[i] = nullspace.unsafe_get(i, 1);
			Z[i] = nullspace.unsafe_get(i, 2);
			W[i] = nullspace.unsafe_get(i, 3);
		}
	}

	DMatrixRMaj tmpA = new DMatrixRMaj(3, 2);
	DMatrixRMaj tmpY = new DMatrixRMaj(3, 1);
	DMatrixRMaj tmpX = new DMatrixRMaj(2, 1);

	/**
	 * Once z is known then x and y can be solved for using the B matrix
	 */
	private void solveForXandY( double z ) {
		this.z = z;

		// solve for x and y using the first two rows of B
		tmpA.data[0] = ((helper.K00*z + helper.K01)*z + helper.K02)*z + helper.K03;
		tmpA.data[1] = ((helper.K04*z + helper.K05)*z + helper.K06)*z + helper.K07;
		tmpY.data[0] = (((helper.K08*z + helper.K09)*z + helper.K10)*z + helper.K11)*z + helper.K12;

		tmpA.data[2] = ((helper.L00*z + helper.L01)*z + helper.L02)*z + helper.L03;
		tmpA.data[3] = ((helper.L04*z + helper.L05)*z + helper.L06)*z + helper.L07;
		tmpY.data[1] = (((helper.L08*z + helper.L09)*z + helper.L10)*z + helper.L11)*z + helper.L12;

		tmpA.data[4] = ((helper.M00*z + helper.M01)*z + helper.M02)*z + helper.M03;
		tmpA.data[5] = ((helper.M04*z + helper.M05)*z + helper.M06)*z + helper.M07;
		tmpY.data[2] = (((helper.M08*z + helper.M09)*z + helper.M10)*z + helper.M11)*z + helper.M12;

		CommonOps_DDRM.scale(-1, tmpY);
		CommonOps_DDRM.solve(tmpA, tmpY, tmpX);

		this.x = tmpX.get(0, 0);
		this.y = tmpX.get(1, 0);
	}
}
