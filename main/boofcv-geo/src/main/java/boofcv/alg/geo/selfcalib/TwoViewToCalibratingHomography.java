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

package boofcv.alg.geo.selfcalib;

import boofcv.abst.geo.Triangulate2ViewsMetric;
import boofcv.alg.geo.DecomposeEssential;
import boofcv.alg.geo.DecomposeProjectiveToMetric;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.AssociatedTriple;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import lombok.Getter;
import org.ddogleg.struct.DogArray;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.interfaces.linsol.LinearSolver;

import java.util.List;

/**
 * Estimates the calibrating/rectifying homography when given a trifocal tensor and two calibration matrices for
 * the first two views. Observations are used to select the best hypothesis out of the four possible camera motions.
 *
 * Procedure:
 * <ol>
 *     <li>Get fundamental and camera matrices from trifocal tensor</li>
 *     <li>Use given calibration matrices to compute essential matrix</li>
 *     <li>Decompose essential matrix to get 4 possible motions from view 1 to view 2</li>
 *     <li>Use reprojection error and visibility constraints to select best hypothesis</li>
 * </ol>
 *
 * Reprojection error is computed by triangulating each point in view-1 using views-1 and view-2. This is then
 * switched to view-3's reference frame and the reprojection error found there. A similar process is repeated using
 * triangulation from view-1 and view-3. In each view it's checked if the feature appears behind the camera and
 * increments the invalid counter if it does.
 *
 * When selecting a hypothesis the hypothesis with the most points appearing in front of call cameras is given priority
 * over lower reprojection error.
 *
 * When applied to view 2, the found translation should have a norm(T) = 1.
 *
 * <ol>
 * <li> P. Abeles, "BoofCV Technical Report: Automatic Camera Calibration" 2020-1 </li>
 * </ol>
 *
 * @author Peter Abeles
 */
public class TwoViewToCalibratingHomography {

	/** used to triangulate feature locations when checking a solution */
	public Triangulate2ViewsMetric triangulate = FactoryMultiView.triangulate2ViewMetric(null);

	// Decomposes the essential matrix
	public final DecomposeEssential decomposeEssential = new DecomposeEssential();

	// Input: Camera matric for view-2 with implicit P1=[I|0] for view-1
	public final DMatrixRMaj P2 = new DMatrixRMaj(3, 4);
	// Input: Fundamental matrix
	public final DMatrixRMaj F21 = new DMatrixRMaj(3, 3);
	// Essential matrix for the first two views
	public final DMatrixRMaj E21 = new DMatrixRMaj(3, 3);

	//--------------------------------------------------------------------------------
	// Output Data Structures
	/** List of all hopotheses for calibrating homography */
	public final DogArray<DMatrixRMaj> hypothesesH = new DogArray<>(() -> new DMatrixRMaj(4, 4));
	/** Which hypothesis was selected as the best. Call {@link #getCalibrationHomography()} as an alternative */
	public @Getter int bestSolutionIdx;
	/** The number of invalid observations that appeared behind the camera in the best hypothesis */
	public @Getter int bestInvalid = Integer.MAX_VALUE;
	/** The metric fit error found in view-2 for the best hypothesis. Computed from SVD */
	public @Getter double bestModelError = Double.MAX_VALUE;

	//------------------------------------------------------------------------------
	// work space variables
	private final DMatrixRMaj A = new DMatrixRMaj(3, 3);
	private final Vector3D_F64 a = new Vector3D_F64();
	private final DMatrixRMaj AK1 = new DMatrixRMaj(3, 3);
	private final DMatrixRMaj KiR = new DMatrixRMaj(3, 3);

	// Given and estimated intrinsic calibration
	private final CameraPinhole intrinsic1 = new CameraPinhole();
	private final CameraPinhole intrinsic2 = new CameraPinhole();

	// motion from camera views
	private final Se3_F64 view_1_to_2 = new Se3_F64();

	// H for elevating projective to metric view
	private final DMatrixRMaj calibratingH = new DMatrixRMaj(4, 4);

	// location of 3D feature in view 1
	private final Point3D_F64 pointIn1 = new Point3D_F64();
	// location of 3D feature in the current view being considered
	private final Point3D_F64 Xcam = new Point3D_F64();
	// storage for normalized image coordinates
	private final AssociatedTriple an = new AssociatedTriple();

	// Linear solver
	private final LinearSolver<DMatrixRMaj, DMatrixRMaj> linear = LinearSolverFactory_DDRM.leastSquares(9, 3);
	private final DMatrixRMaj matA = new DMatrixRMaj(9, 3);
	private final DMatrixRMaj matX = new DMatrixRMaj(9, 1);
	private final DMatrixRMaj matB = new DMatrixRMaj(9, 1);

	private final DecomposeProjectiveToMetric projectiveToMetric = new DecomposeProjectiveToMetric();

	// Used to normalize data for better stability when used in a linear solver
	private final DMatrixRMaj K1_inv = new DMatrixRMaj(3, 3);
	private final DMatrixRMaj K2_prime = new DMatrixRMaj(3, 3);
	private final DMatrixRMaj P1_prime = new DMatrixRMaj(3, 4);
	private final DMatrixRMaj P2_prime = new DMatrixRMaj(3, 4);
	private final DMatrixRMaj H_prime = new DMatrixRMaj(4, 4);
	private final DMatrixRMaj P_tmp = new DMatrixRMaj(3, 4);

	/**
	 * Specify known geometric relationship between the two views
	 *
	 * @param F21 (Input) Fundamental matrix between view-1 and view-2
	 * @param P2 (Input) Projective camera matrix for view-1 with inplicit identity matrix view-1
	 */
	public void initialize( DMatrixRMaj F21, DMatrixRMaj P2 ) {
		// TODO add image width,height here and use to normalize
		BoofMiscOps.checkTrue(F21.numRows == 3 && F21.numCols == 3);
		BoofMiscOps.checkTrue(P2.numRows == 3 && P2.numCols == 4);
		this.F21.setTo(F21);
		this.P2.setTo(P2);
	}

	/**
	 * Estimate the calibrating homography with the given assumptions about the intrinsic calibration matrices
	 * for the first two of three views.
	 *
	 * @param K1 (input) known intrinsic camera calibration matrix for view-1
	 * @param K2 (input) known intrinsic camera calibration matrix for view-2
	 * @param observations (input) observations for the two views. Used to select best solution
	 * @return true if it could find a solution. Failure is a rare condition which requires noise free data.
	 */
	public boolean process( DMatrixRMaj K1, DMatrixRMaj K2, List<AssociatedPair> observations ) {
		// TODO try to improve numerics by reducing the scale of K1 and K2 to 0 to 1.0 for diagonal elements
		//      then undo it

		bestSolutionIdx = -1;
		// Using the provided calibration matrices, extract potential camera motions
		MultiViewOps.fundamentalToEssential(F21, K1, K2, E21);
		decomposeEssential.decompose(E21);

		// Use these camera motions to guess different calibrating homographies
		List<Se3_F64> list_view_1_to_2 = decomposeEssential.getSolutions();
		computeHypothesesForH(K1, K2, list_view_1_to_2);
		// DESIGN NOTE: Could swap the role of view-2 and view-3 if view-2 is pathological

		// Select the best hypothesis
		bestInvalid = Integer.MAX_VALUE;
		bestModelError = Double.MAX_VALUE;
		for (int motionIdx = 0; motionIdx < hypothesesH.size; motionIdx++) {

			// computes the reprojection error, valid projections, and fixes sign/scale of H
			int invalid = checkGeometry(list_view_1_to_2.get(motionIdx), hypothesesH.get(motionIdx), K1, K2, observations);
			if (invalid == Double.MAX_VALUE)
				continue;

			// All hypotheses should have the same reprojection error. Only by applying a geometric constraints do you
			// know which is better
			if (invalid < bestInvalid) {
				bestInvalid = invalid;
				bestSolutionIdx = motionIdx;
				bestModelError = projectiveToMetric.singularError; // this is the same for every hypothesis
			}
		}
		return bestSolutionIdx >= 0;
	}

	/**
	 * Returns the found calibration/rectifying homography.
	 */
	public DMatrixRMaj getCalibrationHomography() {
		return hypothesesH.get(bestSolutionIdx);
	}

	/**
	 * Go through all the found camera motions and generate a hypothesis for each one. Care is taken to compute
	 * the hypothesis in a numerically stable way. The left and right hand side of the equation (see in code comments)
	 * are only equal up to a scale factor. So first the scale factor is found by computing it several times and
	 * picking the one with the largest denominator to avoid numerical issues. Once the scale factor is known then
	 * a linear system is created that can be easily solved for.
	 *
	 * Technically the solution is found when finding the scale factor, but only a single equation for each unknown
	 * is used there. Once the scale factor is known then all the variables can be used resulting in a more stable
	 * solution.
	 */
	void computeHypothesesForH( DMatrixRMaj K1, DMatrixRMaj K2, List<Se3_F64> list_view_1_to_2 ) {
		// TODO normalize once using width and height

		// Improve numerics by normalizing based in K1. All values will be close to 0 to 1
		CommonOps_DDRM.invert(K1, K1_inv);
		CommonOps_DDRM.mult(K1_inv, K2, K2_prime);

		// P1' = inv(K1)*P1
		CommonOps_DDRM.insert(K1_inv, P1_prime, 0, 0);
		// P2' = inv(k1)*P2
		CommonOps_DDRM.mult(K1_inv, P2, P2_prime);

		// Make view-1 = [I,0] again
		MultiViewOps.projectiveToIdentityH(P1_prime, H_prime);
		CommonOps_DDRM.mult(P2_prime, H_prime, P_tmp);
		P2_prime.setTo(P_tmp);

		// P2*H ~= [A,a]*H = [A,a]*[K1 0;v',1]
		// AK ~= A*K1
		PerspectiveOps.projectionSplit(P2_prime, A, a);
		AK1.setTo(A);
//		CommonOps_DDRM.mult(A,K1, AK1);

		CommonOps_DDRM.setIdentity(calibratingH);
		// Normally to construct H you would do the following below, but because we normalized it so that K1=eye(3)
		// this is no longer needed
//		CommonOps_DDRM.insert(K1,calibratingH,0,0);
//		calibratingH.set(3,3,1);

		hypothesesH.reset();

		for (int motionIdx = 0; motionIdx < list_view_1_to_2.size(); motionIdx++) {
			view_1_to_2.setTo(list_view_1_to_2.get(motionIdx));

			// K2*[R,T] = [K2*R, K2*T] = P2*H
			// KR = K2*R
			CommonOps_DDRM.mult(K2_prime, view_1_to_2.R, KiR);

			// Find the scale factor between AK1 and AKiR. Brute force through all possible combinations and select
			// the one which is least prone to numerical instability due to a small denominator
			double bestBottom = 0;
			double bestScale = 0.0;
			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 3; j++) {
					for (int k = 0; k < 3; k++) {
						if (i == k)
							continue;
						double top = AK1.get(i, j)*a.getIdx(k) - AK1.get(k, j)*a.getIdx(i);
						double bottom = a.getIdx(k)*KiR.get(i, j) - a.getIdx(i)*KiR.get(k, j);
						double scale = top/bottom;

						if (Math.abs(bottom) > bestBottom) {
							bestBottom = Math.abs(bottom);
							bestScale = scale;
						}
					}
				}
			}

			// Construct a linear system and solve for the 3 unknowns in v. A linear system is used rather than the
			// commented out algebraic solution de to the possibility of it blowing up if sum of "a" is zero
			for (int j = 0, row = 0; j < 3; j++) {
				for (int i = 0; i < 3; i++, row++) {
					matA.set(row, j, a.getIdx(i));
					matB.set(row, 0, KiR.get(i, j)*bestScale - AK1.get(i, j));
				}
			}

			if (!linear.setA(matA)) {
				continue;
			}
			linear.solve(matB, matX);
			for (int i = 0; i < 3; i++) {
				calibratingH.set(3, i, matX.get(i));
			}

//			// algebraic solution, but has at least one known issue
//			for (int j = 0; j < 3; j++) {
//				double sumA = 0;
//				double sumK = 0;
//				for (int i = 0; i < 3; i++) {
//					sumA += AK1.get(i, j);
//					sumK += KiR.get(i, j);
//				}
//				double v_j = (sumK * bestScale - sumA) / (a.x + a.y + a.z); // NOTE: Degenerate geometry here of sum is zero?
//				calibratingH.set(3, j, v_j);
//				System.out.println("v[" + j + "] = " + v_j);
//			}
			// DESIGN NOTE:
			// Could Lagrange multipliers be used here where KiR is known to have zeros?


			// NOTE: At this point the following is not always true K[R|T] ~= P*H
			//       The sign of H(4,4) needs to be set correctly and this will be done later on since doing so now
			//       means extra more complex code
			CommonOps_DDRM.mult(H_prime, calibratingH, hypothesesH.grow());
		}
	}

	/**
	 * Score a hypothesis based on how often the triangulated object appears in front of the camera in each of the
	 * views.
	 *
	 * NOTE: If you ignore this physical constraint all 4 hypotheses are equally valid and produce consistent camera
	 * matrices. Thus reprojection error and the svd fit error cannot be used here since they are identical in all
	 * cases.
	 *
	 * @param view_1_to_2e (Input) camera motion returned by essential matrix
	 * @param H (Input, Output) Calibrating homography. Modifies H(3,3) to set the scale to something reasonable and
	 * for direction
	 * @param K1 (Input) Calibration matrix for view 1
	 * @param K2 (Input) Calibration matrix for view 1
	 * @param observations (Input) observations from both cameras
	 * @return Number of times it failed the geometric test
	 */
	private int checkGeometry( Se3_F64 view_1_to_2e, DMatrixRMaj H, DMatrixRMaj K1, DMatrixRMaj K2,
							   List<AssociatedPair> observations ) {
		PerspectiveOps.matrixToPinhole(K1, 0, 0, intrinsic1);
		PerspectiveOps.matrixToPinhole(K2, 0, 0, intrinsic2);

		// Use of this function forces K2 to be what we said it would be, also provide a mechanism to compute a
		// model fit error
		if (!projectiveToMetric.projectiveToMetricKnownK(P2, H, K2, view_1_to_2))
			return Integer.MAX_VALUE;
		// NOTE: seems like much of the above calculation only really needs to be done once for all views and this could
		//       be speed up, but doesn't seem like it's worth the effort right now.

		// As mentioned earlier, the sign of H(4,4) will be wrong sometimes. This is where we fix it
		double scale = view_1_to_2.T.norm();
		view_1_to_2.T.divide(scale);
		if (view_1_to_2e.T.distance(view_1_to_2.T) > 1.0) {
			scale *= -1;
		}
		view_1_to_2.setTo(view_1_to_2e);

		// Besides fixing the sign, also scale it so that the resulting translation vector has norm(1) for view-2
		H.set(3, 3, 1.0/scale);

		// count the number of times it appears behind a camera
		int foundInvalid = 0;

		for (int i = 0; i < observations.size(); i++) {
			AssociatedPair ap = observations.get(i);

			PerspectiveOps.convertPixelToNorm(intrinsic1, ap.p1.x, ap.p1.y, an.p1);
			PerspectiveOps.convertPixelToNorm(intrinsic2, ap.p2.x, ap.p2.y, an.p2);

			triangulate.triangulate(an.p1, an.p2, view_1_to_2, pointIn1);
			if (pointIn1.z < 0)
				foundInvalid++;
			SePointOps_F64.transform(view_1_to_2, pointIn1, Xcam);
			if (Xcam.z < 0)
				foundInvalid++;
		}

		return foundInvalid;
	}
}
