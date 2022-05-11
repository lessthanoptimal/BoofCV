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

package boofcv.alg.geo.h;

import boofcv.misc.BoofMiscOps;
import boofcv.struct.geo.AssociatedPair;
import lombok.Getter;
import org.ddogleg.struct.VerbosePrint;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.SingularOps_DDRM;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.interfaces.decomposition.SingularValueDecomposition_F64;
import org.ejml.interfaces.linsol.LinearSolver;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;
import java.util.Set;

/**
 * <p>Estimates homography between two views and independent radial distortion from each camera. Radial
 * distortion is modeled using the one parameter division model [1]. Implementation based
 * on the 6-point algorithm in [2] that solves 2 quadratic equations and 6 linear equations. See
 * Tech Report [3] for the final form of equations used in code below.</p>
 *
 * <p>f(x,y) = [x, y, 1 + &lambda;(x**2 + y**2)]<sup>T</sup></p>
 * where &lambda; is the radial distortion parameter from one of the cameras.
 *
 * <p>NOTE: This assumes that the center of distortion is the center of the image. I.e. image center = (0,0)</p>
 * <p>NOTE: This will work on homographies induced from planes or pure rotations</p>
 *
 * <ol>
 *     <li>Fitzgibbon, Andrew W. "Simultaneous linear estimation of multiple view geometry and lens distortion."
 *     CVPR 2001</li>
 *     <li>Kukelova, Z., Heller, J., Bujnak, M., & Pajdla, T. "Radial distortion homography" (2015)</li>
 *     <li>Peter Abeles, "Notes on Scene Reconstruction: BoofCV Technical Report", 2022</li>
 * </ol>
 *
 * @author Peter Abeles
 */
public class HomographyRadial6Pts implements VerbosePrint {

	/** Higher the value the more well-formed observations are for this model. */
	@Getter double crossSingularRatio;

	// Storage for cross product matrix and linear constraints matrix
	DMatrixRMaj A = new DMatrixRMaj(6, 8);
	// Storage for solution of linear system
	DMatrixRMaj Y = new DMatrixRMaj(6, 1);
	DMatrixRMaj X = new DMatrixRMaj(4, 1);

	// Storage for the two null spaces in the cross constraint
	DMatrixRMaj null1 = new DMatrixRMaj(8, 1);
	DMatrixRMaj null2 = new DMatrixRMaj(8, 1);

	// Linear Solvers
	SingularValueDecomposition_F64<DMatrixRMaj> svd =
			DecompositionFactory_DDRM.svd(6, 8, false, true, true);

	LinearSolver<DMatrixRMaj, DMatrixRMaj> solver = LinearSolverFactory_DDRM.linear(6);

	// Storage for two hypotheses from quadratic formula
	Hypothesis hypothesis1 = new Hypothesis();
	Hypothesis hypothesis2 = new Hypothesis();

	@Nullable PrintStream verbose;

	/**
	 * <p>
	 * Computes the homography matrix given a set of observed points in two images. A set of {@link AssociatedPair}
	 * is passed in. The computed homography 'H' is found such that the attributes 'p1' and 'p2' in {@link AssociatedPair}
	 * refers to x1 and x2, respectively, in the equation  below:<br>
	 * x<sub>2</sub> = H*x<sub>1</sub>
	 * </p>
	 *
	 * @param points A set of observed image points that are generated from a planar object. Minimum of 4 pairs required.
	 * @param foundA Output: Where first solution is written to
	 * @param foundB Output: Where second solution is written to
	 * @return True if successful. False if it failed.
	 */
	public boolean process( List<AssociatedPair> points, Results foundA, Results foundB ) {
		if (points.size() < 6)
			throw new IllegalArgumentException("A minimum of 6 points is required");

		if (!linearCrossConstraint(points)) {
			if (verbose != null) verbose.println("Failed at linear cross constraint");
			return false;
		}

		if (!solveQuadraticRelationship(hypothesis1, hypothesis2)) {
			if (verbose != null) verbose.println("Failed at quadratic relationship");
			return false;
		}

		if (!solveForRemaining(points, hypothesis1, foundA)) {
			if (verbose != null) verbose.println("Failed at linear solver for 4 remaining parameters. A");
			return false;
		}

		if (!solveForRemaining(points, hypothesis2, foundB)) {
			if (verbose != null) verbose.println("Failed at linear solver for 4 remaining parameters. B");
			return false;
		}

		return true;
	}

	/**
	 * Construct a matrix that encapsulates the constraint of applying cross product as a skew symmetric matrix to
	 * input observations:
	 *
	 * alpha*x' = H*x
	 * skew(x')*H*x = 0
	 *
	 * @return true if no errors detected
	 */
	boolean linearCrossConstraint( List<AssociatedPair> points ) {
		A.reshape(points.size(), 8);

		// NOTE: p2 = x' and p1 = x in paper
		for (int row = 0; row < points.size(); row++) {
			AssociatedPair p = points.get(row);
			int index = row*A.numCols;

			// x**2 + y**2
			double r = p.p1.normSq();

			// Matrix is stored in a row-major format. This is filling in a row
			A.data[index++] = -p.p2.y*p.p1.x;
			A.data[index++] = -p.p2.y*p.p1.y;
			A.data[index++] = -p.p2.y;
			A.data[index++] = p.p2.x*p.p1.x;
			A.data[index++] = p.p2.x*p.p1.y;
			A.data[index++] = p.p2.x;
			A.data[index++] = -p.p2.y*r;
			A.data[index] = p.p2.x*r;
		}

		// Find the null space
		if (!svd.decompose(A))
			return false;

		DMatrixRMaj V_t = svd.getV(null, true);
		DMatrixRMaj W = svd.getW(null);

		// Singular values are in an arbitrary order initially
		SingularOps_DDRM.descendingOrder(null, false, W, V_t, true);

		// If there is a well-defined null space then sv[4] >>> sv[5]
		// EPS in denominator to avoid divide by zero
		crossSingularRatio = W.unsafe_get(4, 4)/(UtilEjml.EPS + W.unsafe_get(5, 5));

		// Space the null space
		CommonOps_DDRM.extract(V_t, 5, 6, 0, 8, null1);
		CommonOps_DDRM.extract(V_t, 6, 7, 0, 8, null2);

		return true;
	}

	/**
	 * The solution vector v1 = gamma*n1 + n2, where (n1, n2) are the previously found null space.
	 * There is a known relaionship between elements in n1 and n2 which is then exploited to create a
	 * quadratic equation to solve for the unknown radial distortion.
	 *
	 * @return true if no errors detected
	 */
	boolean solveQuadraticRelationship( Hypothesis hypo1, Hypothesis hypo2 ) {
		// Note the conversion from 0 indexed to 1 indexed, so that variables match what's in the paper
		double n13 = null1.data[2];
		double n16 = null1.data[5];
		double n23 = null2.data[2];
		double n26 = null2.data[5];
		double n17 = null1.data[6];
		double n18 = null1.data[7];
		double n27 = null2.data[6];
		double n28 = null2.data[7];

		// Coeffients in quadratic equation: a*x**2 + b*x + c = 0
		double a = n16*n17 - n13*n18;
		double b = n16*n27 + n17*n26 - n13*n28 - n18*n23;
		double c = n26*n27 - n23*n28;

		if (a == 0.0)
			return false;

		// Solve for gamma now
		double inner = Math.sqrt(b*b - 4.0*a*c);
		hypo1.gamma = (-b + inner)/(2.0*a);
		hypo2.gamma = (-b - inner)/(2.0*a);

		// You now have two solutions for lambda and gamma
		hypo1.lambda = solveForLambdaGivenGamma(n13, n23, n17, n27, n16, n26, n18, n28, hypo1.gamma);
		hypo2.lambda = solveForLambdaGivenGamma(n13, n23, n17, n27, n16, n26, n18, n28, hypo2.gamma);

		return true;
	}

	/**
	 * Create a linear system for the 4 remaining unknowns by feeding the knowns into the second line of
	 * matrix equation (4), see paper.
	 *
	 * @param points Observed points
	 * @param hypo Hypothesis being considered
	 * @param solution Resulting solution
	 */
	boolean solveForRemaining( List<AssociatedPair> points, Hypothesis hypo, Results solution ) {
		A.reshape(points.size(), 4);
		Y.reshape(points.size(),1);

		// Compute values of H which are now known from the previously computed null space
		double h11 = hypo.gamma*null1.data[0] + null2.data[0];
		double h12 = hypo.gamma*null1.data[1] + null2.data[1];
		double h13 = hypo.gamma*null1.data[2] + null2.data[2];

		// NOTE: p2 = x' and p1 = x in paper
		for (int row = 0; row < points.size(); row++) {
			AssociatedPair p = points.get(row);
			int index = row*A.numCols;

			double r1 = p.p1.normSq();
			double r2 = p.p2.normSq();
			double w1 = 1.0 + hypo.lambda*r1;

			A.data[index++] = p.p2.x*p.p1.x; // H[31]
			A.data[index++] = p.p2.x*p.p1.y; // H[32]
			A.data[index++] = p.p2.x*w1;     // H[33]
			A.data[index] = -r2*(h11*p.p1.x + h12*p.p1.y + h13*w1); // lambda'

			Y.data[row] = h11*p.p1.x + h12*p.p1.y + h13*w1;
		}

		// Solve for the unknowns
		if (!solver.setA(A))
			return false;

		solver.solve(Y, X);

		// Save radial distortion parameter
		solution.l1 = hypo.lambda;
		solution.l2 = X.data[3];

		// Save H directory to the array.
		solution.H.data[0] = h11;
		solution.H.data[1] = h12;
		solution.H.data[2] = h13;
		solution.H.data[3] = hypo.gamma*null1.data[3] + null2.data[3]; // h21
		solution.H.data[4] = hypo.gamma*null1.data[4] + null2.data[4]; // h22
		solution.H.data[5] = hypo.gamma*null1.data[5] + null2.data[5]; // h23
		solution.H.data[6] = X.data[0]; // h31
		solution.H.data[7] = X.data[0]; // h32
		solution.H.data[8] = X.data[0]; // h33

		return true;
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}

	/** Storage for internal parameters that define a hypothesis. See paper. */
	static class Hypothesis {
		public double gamma;
		public double lambda;
	}

	private double solveForLambdaGivenGamma( double n13, double n23, double n17, double n27,
											 double n16, double n26, double n18, double n28, double gamma ) {
		// Attempt to reduce numerical error by averaging the two equivalent formulas
		// might want to also consider just using the one with the larger denominator
		double solA = (gamma*n17 + n27)/(gamma*n13 + n23);
		double solB = (gamma*n18 + n28)/(gamma*n16 + n26);

		return (solA + solB)/2.0;
	}

	public static class Results {
		/** Homography between the two views */
		public final DMatrixRMaj H = new DMatrixRMaj(3, 3);
		/** Radial distortion in view-1 and view-2, respectively */
		public double l1, l2;
	}
}
