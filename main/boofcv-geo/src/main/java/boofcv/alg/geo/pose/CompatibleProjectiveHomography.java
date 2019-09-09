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

import boofcv.alg.geo.PerspectiveOps;
import boofcv.misc.ConfigConverge;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import org.ddogleg.optimization.FactoryOptimization;
import org.ddogleg.optimization.UnconstrainedLeastSquares;
import org.ddogleg.optimization.UtilOptimize;
import org.ddogleg.optimization.functions.FunctionNtoM;
import org.ddogleg.optimization.lm.ConfigLevenbergMarquardt;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
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
 * <p>projection of world point 'X' to pixel 'x': x = P*X</p>
 * <p>Two projective frames of the same view are related by 4x4 homography H. 3D points are related too by H</p>
 * <p>P1[i]*H = P2[i]</p>
 * <p>X1 = H*X2</p>
 * <p>P is a 3x4 camera matrix and has a projective ambiguity. x = P*inv(H)*H*X, P' = P*inv(H) and X' = H*X</p>
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
	public LinearSolverDense<DMatrixRMaj> solver = LinearSolverFactory_DDRM.leastSquares(200,16);
	public SolveNullSpace<DMatrixRMaj> nullspace = new SolveNullSpaceSvd_DDRM();
	public SolvePseudoInverseSvd_DDRM solvePInv = new SolvePseudoInverseSvd_DDRM();
	// in the paper it doesn't specify so it probably uses a simpler pseudo inverse. A+ = A'inv(A*A')

	// workspace variables for A*X=B
	private DMatrixRMaj A = new DMatrixRMaj(1,1);
	private DMatrixRMaj B = new DMatrixRMaj(1,1);

	private Point4D_F64 a = new Point4D_F64();
	private Point4D_F64 b = new Point4D_F64();

	// PinvP = pinv(P)*P
	private DMatrixRMaj PinvP = new DMatrixRMaj(4,4);
	// storage for nullspace of P
	private DMatrixRMaj h = new DMatrixRMaj(1,1);

	// Distance functions for non-linear optimization
	private DistanceWorldEuclidean distanceWorld = new DistanceWorldEuclidean();
	private DistanceReprojection distanceRepojection = new DistanceReprojection();

	// Non-linear optimization function
	public UnconstrainedLeastSquares<DMatrixRMaj> lm;

	// Convergence criteria
	public final ConfigConverge configConverge = new ConfigConverge(1e-8,1e-8,500);

	public CompatibleProjectiveHomography() {
		ConfigLevenbergMarquardt config = new ConfigLevenbergMarquardt();
		lm = FactoryOptimization.levenbergMarquardt(config,false);
	}

	/**
	 * Finds the homography H by by minimizing algebriac error. Modified version of algorithm described in [1].
	 * See [2] for implementation details. Solution is found by finding the null space. A minimum of 5 points are
	 * required to solve the 15 degrees of freedom in H.
	 *
	 * <p>X1 = H*X2</p>
	 *
	 * <p>NOTE: Fails when all points lie exactly on the same plane</p>
	 *
	 * @param points1 Set of points from view A but in projective 1. Recommended that they have f-norm of 1
	 * @param points2 Set of points from view A but in projective 2. Recommended that they have f-norm of 1
	 * @param H (Output) 4x4 homography relating the views. P1*H = P2, X1 = H*X2
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
	 * system. Non-linear refinement is recommended, before bundle adjustment.
	 *
	 * <p>P1[i] = P2[i]*inv(H)</p>
	 *
	 * <p>NOTE: This will work with planar scenes!</p>
	 * <p>NOTE: It's likely that this approach can be improved. See in code comments</p>
	 *
	 * @param cameras1 list of camera matrices
	 * @param cameras2 list of camera matrices, same views as camera1
	 * @param H (Output) 4x4 homography relating the views. P1*H = P2, X1 = H*X2
	 * @return true if successful or false if it failed
	 */
	public boolean fitCameras(List<DMatrixRMaj> cameras1 , List<DMatrixRMaj> cameras2 , DMatrixRMaj H ) {
		if( cameras1.size() != cameras2.size() )
			throw new IllegalArgumentException("Must have the same number in each list");
		if( cameras1.size() < 2 )
			throw new IllegalArgumentException("At least two cameras are required");

		// NOTE: I don't really understand why this doesn't produce better results. Current suspicion in that
		// data needs to be normalized first to minimize errors

		final int size = cameras1.size();

		A.reshape(size*3,4);
		B.reshape(size*3,4);

		for (int i = 0; i < size; i++) {
			DMatrixRMaj P1 = cameras1.get(i);
			DMatrixRMaj P2 = cameras2.get(i);

			CommonOps_DDRM.insert(P1,A,i*3,0);
			CommonOps_DDRM.insert(P2,B,i*3,0);
		}

		if( !solver.setA(A) )
			return false;
		H.reshape(4,4);
		solver.solve(B,H);
		return true;
	}

	/**
	 * Solves for the transform H using one view and 2 or more points. The two camera matrices associated with the
	 * same view constrain 11 out of the 15 DOF. Each points provides another 3 linear constraints.
	 *
	 * <p>H(v) = pinv(P)*P' + hv<sup>T</sup></p>
	 * <p>where 'h' is null-space of P. 'v' is 4-vector and is unknown, solved with linear equation</p>
	 *
	 * <p>NOTE: While 2 is the minimum number of views during testing it seemed to require 4 points with perfect data.
	 * The math was double checked and no fundamental flaw could be found in the test. More investigation is needed.
	 * There is a large difference in scales that could be contributing to this problem.</p>
	 *
	 * <p>NOTE: Produces poor results when the scene is planar</p>
	 *
	 * @param camera1 Camera matrix
	 * @param camera2 Camera matrix of the same view but another projective space
	 * @param points1 Observations from camera1
	 * @param points2 Observations from camera2
	 * @param H (Output) 4x4 homography relating the views. P1*H = P2, X1 = H*X2
	 * @return true if successful
	 */
	public boolean fitCameraPoints(DMatrixRMaj camera1 , DMatrixRMaj camera2 ,
								   List<Point4D_F64> points1 , List<Point4D_F64> points2 ,
								   DMatrixRMaj H ) {
		if( points1.size() != points2.size() )
			throw new IllegalArgumentException("Lists must be the same size");
		if( points1.size() < 2 )
			throw new IllegalArgumentException("A minimum of two points are required");

		// NOTE: Potential improvement. Normalize input data some how. Get the two camera matrices at the same
		// scale and do the same to the points

		// yes the SVD is computed twice. This can be optimized later, right now it isn't worth it. not a bottle neck
		if( !solvePInv.setA(camera1) )
			return false;
		if( !nullspace.process(camera1,1,h) )
			return false;

		// PinvP = pinv(P)*P'
		solvePInv.solve(camera2,PinvP);

		final int size = points1.size();
		A.reshape(size*3,4);
		B.reshape(size*3,1);

		for (int i = 0,idxA=0,idxB=0; i < size; i++) {
			Point4D_F64 p1 = points1.get(i);
			Point4D_F64 p2 = points2.get(i);

			// a = P+P'X'
			GeometryMath_F64.mult(PinvP,p2,a);

//			double a_sum = a.w;
//			double h_sum = h.data[3];
//			double x_sum = p1.w;

			// Use sum instead of a[4], ... so that points at infinity can be handled
			double a_sum = a.x+a.y+a.z+a.w;
			double h_sum = h.data[0]+h.data[1]+h.data[2]+h.data[3];
			double x_sum = p1.x+p1.y+p1.z+p1.w;

			for (int k = 0; k < 3; k++) {
				// b[k] = h[k]*X[4] - h[4]*X[k]
				double b_k = h.data[k]*x_sum - h_sum*p1.getIdx(k);
				// c[k] = X[k]*a[4] - X[4]*a[k]
				double c_k = p1.getIdx(k)*a_sum - x_sum*a.getIdx(k);

				// b*X'^T*v = c
				A.data[idxA++] = b_k*p2.x;
				A.data[idxA++] = b_k*p2.y;
				A.data[idxA++] = b_k*p2.z;
				A.data[idxA++] = b_k*p2.w;

				B.data[idxB++] = c_k;
			}
		}

//		A.print();

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

	/**
	 * Refines the initial estimate of H by reducing the Euclidean distance between points.
	 *
	 * <p>NOTE: parameterization can be improved. Optimizes 16-DOF when only 15 is needed. scale isn't bounded</p>
	 *
	 * @param scene1 List of points in world coordinates
	 * @param scene2 List of points in world coordinates, but at a different projective frame
	 * @param H (Output) 4x4 homography that related to two sets of camera matrices. P1*H = P2
	 */
	public void refineWorld(List<Point3D_F64> scene1 , List<Point3D_F64> scene2 , DMatrixRMaj H )
	{
		if( H.numCols != 4 || H.numRows != 4 )
			throw new IllegalArgumentException("Expected 4x4 matrix for H");

		distanceWorld.scene1 = scene1;
		distanceWorld.scene2 = scene2;

		lm.setFunction(distanceWorld,null);
		lm.initialize(H.data,1e-8,1e-8);

		UtilOptimize.process(lm,configConverge.maxIterations);

		System.arraycopy(lm.getParameters(),0,H.data,0,16);

		distanceWorld.scene1 = null;
		distanceWorld.scene2 = null;
	}

	public void refineReprojection(List<DMatrixRMaj> cameras1, List<Point4D_F64> scene1 ,
									List<Point4D_F64> scene2 , DMatrixRMaj H )
	{
		if( H.numCols != 4 || H.numRows != 4 )
			throw new IllegalArgumentException("Expected 4x4 matrix for H");
		if( scene1.size() != scene2.size() || scene1.size() <= 0 )
			throw new IllegalArgumentException("Lists must have equal size and be not empty");
		if( cameras1.isEmpty() )
			throw new IllegalArgumentException("Camera must not be empty");


		distanceRepojection.cameras1 = cameras1;
		distanceRepojection.scene1 = scene1;
		distanceRepojection.scene2 = scene2;

		lm.setFunction(distanceRepojection,null);
		lm.initialize(H.data,configConverge.ftol,configConverge.gtol);

		UtilOptimize.process(lm,configConverge.maxIterations);

		System.arraycopy(lm.getParameters(),0,H.data,0,16);

		distanceRepojection.cameras1 = null;
		distanceRepojection.scene1 = null;
		distanceRepojection.scene2 = null;
	}

	/**
	 * Euclidean error for matching up points in the two projective frames
	 *
	 * ||X1-H*X2||
	 */
	private static class DistanceWorldEuclidean implements FunctionNtoM {

		List<Point3D_F64> scene1;
		List<Point3D_F64> scene2;

		// adjusted point
		Point3D_F64 ba = new Point3D_F64();

		// homography
		DMatrixRMaj H = new DMatrixRMaj(4,4);

		@Override
		public void process(double[] input, double[] output) {
			H.data = input;

			for (int i = 0, idx=0; i < scene1.size(); i++) {
				Point3D_F64 a = scene1.get(i);
				Point3D_F64 b = scene2.get(i);

				GeometryMath_F64.mult4(H,b,ba);

				output[idx++] = a.x-ba.x;
				output[idx++] = a.y-ba.y;
				output[idx++] = a.z-ba.z;
			}
		}

		@Override
		public int getNumOfInputsN() {
			return 16; // HMM only 15 DOF, scale invariant. This wil float
		}

		@Override
		public int getNumOfOutputsM() {
			return scene1.size()*3;
		}
	}

	/**
	 * Euclidean reprojection error in projective frame 1 only.
	 */
	private static class DistanceReprojection implements FunctionNtoM {

		List<DMatrixRMaj> cameras1;

		List<Point4D_F64> scene1;
		List<Point4D_F64> scene2;

		// adjusted point
		Point4D_F64 ba = new Point4D_F64();

		// homography
		DMatrixRMaj H = new DMatrixRMaj(4,4);

		Point2D_F64 pixel1 = new Point2D_F64();
		Point2D_F64 pixel2 = new Point2D_F64();

		@Override
		public void process(double[] input, double[] output) {
			H.data = input;

			int outputIdx = 0;
			for (int viewIdx = 0; viewIdx < cameras1.size(); viewIdx++) {
				DMatrixRMaj P = cameras1.get(viewIdx);

				for (int pointIdx = 0; pointIdx < scene1.size(); pointIdx++) {
					Point4D_F64 a = scene1.get(pointIdx);
					Point4D_F64 b = scene2.get(pointIdx);

					GeometryMath_F64.mult(H,b,ba);
					PerspectiveOps.renderPixel(P,a,pixel1); // NOTE: This could be cached
					PerspectiveOps.renderPixel(P,ba,pixel2);

					output[outputIdx++] = pixel1.x - pixel2.x;
					output[outputIdx++] = pixel1.y - pixel2.y;
				}
			}
		}

		@Override
		public int getNumOfInputsN() {
			return 16; // HMM only 15 DOF, scale invariant. This will float
		}

		@Override
		public int getNumOfOutputsM() {
			return cameras1.size()*scene1.size()*2;
		}
	}
}
