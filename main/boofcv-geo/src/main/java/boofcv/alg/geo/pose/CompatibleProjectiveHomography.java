/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point4D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.linsol.qr.LinearSolverQrHouseCol_DDRM;
import org.ejml.dense.row.linsol.svd.SolveNullSpaceSvd_DDRM;
import org.ejml.dense.row.linsol.svd.SolvePseudoInverseSvd_DDRM;
import org.ejml.interfaces.SolveNullSpace;
import org.ejml.interfaces.linsol.LinearSolverDense;

import java.util.List;

/**
 * Algorithms for finding a 4x4 homography which can convert two camera matrices of the same view but differ in only
 * the projective ambiguity. This is needed if extending a projective scene by independently computing the solution
 * set sets of 2, 3 or more views. This provides a mechanism "stitching" the solutions together using their common
 * views.
 *
 * <ol>
 * <li>Fitzgibbon, Andrew W., and Andrew Zisserman. "Automatic camera recovery for closed or open image sequences."
 * European conference on computer vision. Springer, Berlin, Heidelberg, 1998.</li>
 * <li> P. Abeles, "BoofCV Technical Report: Automatic Camera Calibration" 2018-1 </li>
 * </ul>
 *
 * @author Peter Abeles
 */
public class CompatibleProjectiveHomography {


	// Linear solver. Change to SVD if a more robust one is needed
	public LinearSolverDense<DMatrixRMaj> solver = new LinearSolverQrHouseCol_DDRM();
	public SolveNullSpace<DMatrixRMaj> nullspace = new SolveNullSpaceSvd_DDRM();
	public SolvePseudoInverseSvd_DDRM solvePInv = new SolvePseudoInverseSvd_DDRM();

	// workspace variables for A*X=B
	private DMatrixRMaj A = new DMatrixRMaj(1,1);
	private DMatrixRMaj B = new DMatrixRMaj(1,1);

	private Point4D_F64 a = new Point4D_F64();
	private Point4D_F64 b = new Point4D_F64();

	// PinvP = pinv(P)*P
	private DMatrixRMaj PinvP = new DMatrixRMaj(4,4);
	// storage for nullspace of P
	private DMatrixRMaj h = new DMatrixRMaj(1,1);

	/**
	 * Finds the homography H by by minimizing algebriac error. Modified version of algorithm described in [1].
	 * See [2] for implementation details. Solution is found by finding the null space. A minimum of 5 points are
	 * required to solve the 15 degrees of freedom in H.
	 *
	 * <p>X1 = H*X2</p>
	 *
	 * @param points1 Set of points from view A but in projective 1. Recommended that they have f-norm of 1
	 * @param points2 Set of points from view A but in projective 2. Recommended that they have f-norm of 1
	 * @param H (Output) Storage for 4x4 homography
	 * @return true if successful or false if it fails
	 */
	public boolean fitPoints(List<Point4D_F64> points1 , List<Point4D_F64> points2 , DMatrixRMaj H )
	{
		if( points1.size() != points2.size() )
			throw new IllegalArgumentException("Must have the same number in each list");
		if( points1.size() < 5 )
			throw new IllegalArgumentException("At least 5 points required");

		final int size = points1.size();

		A.reshape(size*3,16);

		for (int i = 0; i < size; i++) {
			Point4D_F64 a = points1.get(i);
			Point4D_F64 b = points2.get(i);

			double alpha = -(a.x + a.y + a.z + a.w);

			for (int j = 0; j < 3; j++) {
				int idx = 16*(3*i+j);
				double va = a.getIdx(j);
				for (int k = 0; k < 4; k++) {
					A.data[idx++] = va*b.x;
					A.data[idx++] = va*b.y;
					A.data[idx++] = va*b.z;
					A.data[idx++] = va*b.w;
				}
			}
			for (int j = 0; j < 3; j++) {
				int idx = 16*(3*i+j)+4*j;

				A.data[idx  ] += b.x*alpha;
				A.data[idx+1] += b.y*alpha;
				A.data[idx+2] += b.z*alpha;
				A.data[idx+3] += b.w*alpha;
			}
		}

		if( !nullspace.process(A,1,H) )
			return false;

		H.reshape(4,4);

		return true;
	}

	/**
	 * Computes homography which relate common projective camera transforms to each other by solving a linear
	 * system. Even with perfect input data the results have noticable errors. Non-linear refinement is recommended,
	 * even before bundle adjustment.
	 *
	 * <p>P1[i] = P2[i]*inv(H)</p>
	 *
	 * @param cameras1 list of camera matrices
	 * @param cameras2 list of camera matrices
	 * @param H_inv (Output) 4x4 homography inverse
	 * @return true if successful or false if it failed
	 */
	public boolean fitCameras(List<DMatrixRMaj> cameras1 , List<DMatrixRMaj> cameras2 , DMatrixRMaj H_inv ) {
		if( cameras1.size() != cameras2.size() )
			throw new IllegalArgumentException("Must have the same number in each list");
		if( cameras1.size() < 2 )
			throw new IllegalArgumentException("At least two cameras are required");

		final int size = cameras1.size();

		A.reshape(size*3,4);
		B.reshape(size*3,4);

		for (int i = 0; i < size; i++) {
			DMatrixRMaj P1 = cameras1.get(i);
			DMatrixRMaj P2 = cameras2.get(i);

			CommonOps_DDRM.insert(P2,A,i*3,0);
			CommonOps_DDRM.insert(P1,B,i*3,0);
		}

		if( !solver.setA(A) )
			return false;
		H_inv.reshape(4,4);
		solver.solve(B,H_inv);
		return true;
	}

	/**
	 *
	 * <p>H(v) = pinv(P)*P' + hv<sup>T</sup></p>
	 *
	 *
	 * @param camera1
	 * @param camera2
	 * @param points1
	 * @param points2
	 * @param H
	 * @return
	 */
	public boolean fitCameraPoints(DMatrixRMaj camera1 , DMatrixRMaj camera2 ,
								   List<Point4D_F64> points1 , List<Point4D_F64> points2 ,
								   DMatrixRMaj H ) {

		// yes the SVD is computed twice. This can be optimized later, right now it isn't worth it. not a bottle neck
		if( !solvePInv.setA(camera1) )
			return false;
		if( !nullspace.process(camera1,1,h) )
			return false;

		// PinvP = pinv(P)*P'
		solvePInv.solve(camera2,PinvP);

		PinvP.print();
		final int size = points1.size();
		A.reshape(size*3,4);
		B.reshape(size*3,1);

		for (int i = 0,idxA=0,idxB=0; i < size; i++) {
			Point4D_F64 p1 = points1.get(i);
			Point4D_F64 p2 = points2.get(i);

			// a = P+P'X'
			GeometryMath_F64.mult(PinvP,p2,a);

			double a4 = a.w;
			double h4 = h.data[3];
			double x4 = p1.getIdx(0);

			for (int j = 0; j < 3; j++) {

				// b[k] = h[k]*X[4] - h[4]*X[k]
				double b_k = h.data[j]*p1.w - h4*x4;
				// c[k] = X[k]*a[4] - X[4]*a[k]
				double c_k = p1.getIdx(j)*a4 - x4*a.getIdx(j);

				// b*X'^T*v = c
				A.data[idxA++] = b_k*p2.x;
				A.data[idxA++] = b_k*p2.y;
				A.data[idxA++] = b_k*p2.z;
				A.data[idxA++] = b_k*p2.w;

				B.data[idxB++] = c_k;
			}
		}
		A.print();

		// Solve for v
		if( !solver.setA(A) )
			return false;
		H.reshape(4,1);
		solver.solve(B,H);

		// copy v into 'a'
		a.set(H.data[0],H.data[1],H.data[2],H.data[3]);
		// H = P+P' + h*v^T
		H.reshape(4,4);
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				H.data[i*4+j] = PinvP.get(i,j) + h.data[i]*a.getIdx(j);
			}
		}
		return true;
	}

	public void refineReprojection() {

	}
}
