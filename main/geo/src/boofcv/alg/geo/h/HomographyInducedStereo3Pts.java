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

package boofcv.alg.geo.h;

import boofcv.alg.geo.MultiViewOps;
import boofcv.struct.geo.AssociatedPair;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ejml.alg.dense.linsol.LinearSolverSafe;
import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.LinearSolverFactory;
import org.ejml.interfaces.linsol.LinearSolver;

/**
 * <p>
 * Computes the homography induced by a plane from 3 point correspondences.  Works with both calibrated and
 * uncalibrated cameras.  The Fundamental/Essential matrix must be known.  The found homography will be from view 1
 * to view 2.  The passed in Fundamental matrix must have the following properties for each set of
 * point correspondences: x2*F*x1 = 0, where x1 and x2 are views of the point in image 1 and image 2 respectively.
 * For more information see [1].
 * </p>
 *
 * <p>
 * [1] R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003
 * </p>
 *
 * @author Peter Abeles
 */
public class HomographyInducedStereo3Pts {

	// Epipole in camera 2
	private Point3D_F64 e2 = new Point3D_F64();

	// The found homography from view 1 to view 2
	private DenseMatrix64F H = new DenseMatrix64F(3,3);

	// A = cross(e2)*F
	private DenseMatrix64F A = new DenseMatrix64F(3,3);
	// Rows filled with x from image 1
	private DenseMatrix64F M = new DenseMatrix64F(3,3);

	private DenseMatrix64F temp0 = new DenseMatrix64F(3,1);
	private DenseMatrix64F temp1 = new DenseMatrix64F(3,1);

	private Point3D_F64 A_inv_b = new Point3D_F64();
	private Point3D_F64 Ax = new Point3D_F64();
	private Point3D_F64 b = new Point3D_F64();

	private Point3D_F64 t0 = new Point3D_F64();
	private Point3D_F64 t1 = new Point3D_F64();

	private LinearSolver<DenseMatrix64F> solver;

	// pick a reasonable scale and sign
	private AdjustHomographyMatrix adjust = new AdjustHomographyMatrix();

	public HomographyInducedStereo3Pts()
	{
		// ensure that the inputs are not modified
		solver = new LinearSolverSafe<>(LinearSolverFactory.linear(3));
	}

   /**
	 * Specify the fundamental matrix and the camera 2 epipole.
	 *
	 * @param F Fundamental matrix.
	 * @param e2 Epipole for camera 2.  If null it will be computed internally.
	 */
	public void setFundamental( DenseMatrix64F F , Point3D_F64 e2 ) {
		if( e2 != null )
			this.e2.set(e2);
		else {
			MultiViewOps.extractEpipoles(F,new Point3D_F64(),this.e2);
		}
		GeometryMath_F64.multCrossA(this.e2,F,A);
	}

	/**
	 * Estimates the homography from view 1 to view 2 induced by a plane from 3 point associations.
	 * Each pair must pass the epipolar constraint.  This can fail if the points are colinear.
	 *
	 * @param p1 Associated point observation
	 * @param p2 Associated point observation
	 * @param p3 Associated point observation
	 * @return True if successful or false if it failed
	  */
	public boolean process(AssociatedPair p1, AssociatedPair p2, AssociatedPair p3) {

		// Fill rows of M with observations from image 1
		fillM(p1.p1,p2.p1,p3.p1);

		// Compute 'b' vector
		b.x = computeB(p1.p2);
		b.y = computeB(p2.p2);
		b.z = computeB(p3.p2);

		// A_inv_b = inv(A)*b
		if( !solver.setA(M) )
			return false;

		GeometryMath_F64.toMatrix(b,temp0);
		solver.solve(temp0,temp1);
		GeometryMath_F64.toTuple3D(temp1, A_inv_b);

		GeometryMath_F64.addOuterProd(A, -1, e2, A_inv_b, H);

		// pick a good scale and sign for H
		adjust.adjust(H, p1);

		return true;
	}

	/**
	 * Fill rows of M with observations from image 1
	 */
	private void fillM( Point2D_F64 x1 , Point2D_F64 x2 , Point2D_F64 x3 ) {
		M.data[0] = x1.x; M.data[1] = x1.y; M.data[2] = 1;
		M.data[3] = x2.x; M.data[4] = x2.y; M.data[5] = 1;
		M.data[6] = x3.x; M.data[7] = x3.y; M.data[8] = 1;
	}

	/**
	 * b = [(x cross (A*x))^T ( x cross e2 )] / || x cross e2 ||^2
	 */
	private double computeB( Point2D_F64 x ) {
		GeometryMath_F64.mult(A,x,Ax);

		GeometryMath_F64.cross(x,Ax,t0);
		GeometryMath_F64.cross(x,e2,t1);

		double top = GeometryMath_F64.dot(t0,t1);
		double bottom = t1.normSq();

		return top/bottom;
	}

	/**
	 * The found homography from view 1 to view 2
	 * @return homography
	 */
	public DenseMatrix64F getHomography() {
		return H;
	}
}
