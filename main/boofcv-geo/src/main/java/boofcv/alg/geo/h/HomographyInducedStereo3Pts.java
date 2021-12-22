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

package boofcv.alg.geo.h;

import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.f.EpipolarMinimizeGeometricError;
import boofcv.struct.geo.AssociatedPair;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.DogArray;
import org.ejml.LinearSolverSafe;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.interfaces.linsol.LinearSolverDense;
import org.jetbrains.annotations.Nullable;

/**
 * <p>
 * Computes the homography induced by a plane from 3 point correspondences. Works with both calibrated and
 * uncalibrated cameras. The Fundamental/Essential matrix must be known. The found homography will be from view 1
 * to view 2. The passed in Fundamental matrix must have the following properties for each set of
 * point correspondences: x2*F*x1 = 0, where x1 and x2 are views of the point in image 1 and image 2 respectively.
 * For more information see [1].
 * </p>
 *
 * <p>
 * [1] Page 332, R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003
 * </p>
 *
 * @author Peter Abeles
 */
public class HomographyInducedStereo3Pts {

	// Input fundamental matrix
	private final DMatrixRMaj F21 = new DMatrixRMaj(3, 3);

	// Epipole in camera 2
	private final Point3D_F64 e2 = new Point3D_F64();

	// The found homography from view 1 to view 2
	private final DMatrixRMaj H = new DMatrixRMaj(3, 3);

	// A = cross(e2)*F
	private final DMatrixRMaj A = new DMatrixRMaj(3, 3);
	// Rows filled with x from image 1
	private final DMatrixRMaj M = new DMatrixRMaj(3, 3);

	private final DMatrixRMaj temp0 = new DMatrixRMaj(3, 1);
	private final DMatrixRMaj temp1 = new DMatrixRMaj(3, 1);

	private final Point3D_F64 A_inv_b = new Point3D_F64();
	private final Point3D_F64 Ax = new Point3D_F64();
	private final Point3D_F64 b = new Point3D_F64();

	private final Point3D_F64 t0 = new Point3D_F64();
	private final Point3D_F64 t1 = new Point3D_F64();

	private final LinearSolverDense<DMatrixRMaj> solver;

	// pick a reasonable scale and sign
	private final AdjustHomographyMatrix adjust = new AdjustHomographyMatrix();

	// Adjusts points to minimize geometric error
	EpipolarMinimizeGeometricError adjusterEpipolar = new EpipolarMinimizeGeometricError();
	private final DogArray<AssociatedPair> adjustedPairs = new DogArray<>(AssociatedPair::new);

	public HomographyInducedStereo3Pts() {
		// ensure that the inputs are not modified
		solver = new LinearSolverSafe<>(LinearSolverFactory_DDRM.linear(3));
	}

	/**
	 * Specify the fundamental matrix and the camera 2 epipole.
	 *
	 * @param F Fundamental matrix.
	 * @param e2 Epipole for camera 2. If null it will be computed internally.
	 */
	public void setFundamental( DMatrixRMaj F, @Nullable Point3D_F64 e2 ) {
		if (e2 != null)
			this.e2.setTo(e2);
		else {
			MultiViewOps.extractEpipoles(F, new Point3D_F64(), this.e2);
		}
		GeometryMath_F64.multCrossA(this.e2, F, A);
		this.F21.setTo(F);
	}

	/**
	 * Estimates the homography from view 1 to view 2 induced by a plane from 3 point associations.
	 * Each pair must pass the epipolar constraint. This can fail if the points are colinear.
	 *
	 * @param p1 Associated point observation
	 * @param p2 Associated point observation
	 * @param p3 Associated point observation
	 * @return True if successful or false if it failed
	 */
	public boolean process( AssociatedPair p1, AssociatedPair p2, AssociatedPair p3 ) {
		// Computed corrected points that minimize epipolar error
		adjustedPairs.resize(3);
		adjustEpipolar(p1, adjustedPairs.get(0));
		adjustEpipolar(p2, adjustedPairs.get(1));
		adjustEpipolar(p3, adjustedPairs.get(2));

		// The algorithm in the book doesn't appear to be terribly stable.
		// One possible way to improve it is to normalize the inputs so that they have a magnitude around one
		// This is a bit of a pain and nothing is using the code right now. I'm being lazy
		// but at least I'm documenting my laziness
//		LowLevelMultiViewOps.computeNormalization(adjustedPairs.toList(), N1, N2);

		// Fill rows of M with observations from image 1
		fillM(adjustedPairs.get(0).p1, adjustedPairs.get(1).p1, adjustedPairs.get(2).p1);

		// Compute 'b' vector
		b.x = computeB(adjustedPairs.get(0).p2);
		b.y = computeB(adjustedPairs.get(1).p2);
		b.z = computeB(adjustedPairs.get(2).p2);

		// A_inv_b = inv(A)*b
		if (!solver.setA(M))
			return false;

		GeometryMath_F64.toMatrix(b, temp0);
		solver.solve(temp0, temp1);
		GeometryMath_F64.toTuple3D(temp1, A_inv_b);

		GeometryMath_F64.addOuterProd(A, -1, e2, A_inv_b, H);

		// pick a good scale and sign for H
		adjust.adjust(H, p1);

		return true;
	}

	private void adjustEpipolar( AssociatedPair a, AssociatedPair adjusted ) {
		adjusterEpipolar.process(F21, a.p1.x, a.p1.y, a.p2.x, a.p2.y, adjusted.p1, adjusted.p2);
	}

	/**
	 * Fill rows of M with observations from image 1
	 */
	private void fillM( Point2D_F64 x1, Point2D_F64 x2, Point2D_F64 x3 ) {
		M.data[0] = x1.x;
		M.data[1] = x1.y;
		M.data[2] = 1;
		M.data[3] = x2.x;
		M.data[4] = x2.y;
		M.data[5] = 1;
		M.data[6] = x3.x;
		M.data[7] = x3.y;
		M.data[8] = 1;
	}

	/**
	 * b = [(x cross (A*x))^T ( x cross e2 )] / || x cross e2 ||^2
	 */
	private double computeB( Point2D_F64 x ) {
		GeometryMath_F64.mult(A, x, Ax);

		GeometryMath_F64.cross(x, Ax, t0);
		GeometryMath_F64.cross(x, e2, t1);

		double top = GeometryMath_F64.dot(t0, t1);
		double bottom = t1.normSq();

		return top/bottom;
	}

	/**
	 * The found homography from view 1 to view 2
	 *
	 * @return homography
	 */
	public DMatrixRMaj getHomography() {
		return H;
	}
}
