/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

/**
 * Computes projective camera matrices from a fundamental matrix. All work space is predeclared and won't thrash
 * the garbage collector.
 *
 * @author Peter Abeles
 */
public class FundamentalToProjective {

	private DMatrixRMaj crossMatrix = new DMatrixRMaj(3,3);
	private DMatrixRMaj outer = new DMatrixRMaj(3,3);
	private DMatrixRMaj KR = new DMatrixRMaj(3,3);

	FundamentalExtractEpipoles alg = new FundamentalExtractEpipoles();
	Point3D_F64 e1 = new Point3D_F64();
	Point3D_F64 e2 = new Point3D_F64();

	/**
	 * <p>
	 * Given a fundamental matrix a pair of camera matrices P0 and P1 can be extracted. Same
	 * {@link #process(DMatrixRMaj, DMatrixRMaj)} but with the suggested values for all variables filled in for you.
	 * </p>
	 * @param F (Input) Fundamental Matrix
	 * @param cameraMatrix (Output) resulting projective camera matrix P'. (3 by 4) Known up to a projective transform.
	 */
	public void process(DMatrixRMaj F , DMatrixRMaj cameraMatrix) {

		alg.process(F,e1,e2);
		process(F, e2, new Vector3D_F64(0, 0, 0), 1,cameraMatrix);
	}


	/**
	 * <p>
	 * Given a fundamental matrix a pair of camera matrices P and P1' are extracted. The camera matrices
	 * are 3 by 4 and used to project a 3D homogenous point onto the image plane. These camera matrices will only
	 * be known up to a projective transform, thus there are multiple solutions, The canonical camera
	 * matrix is defined as: <br>
	 * <pre>
	 * P=[I|0] and P'= [M|-M*t] = [[e']*F + e'*v^t | lambda*e']
	 * </pre>
	 * where e' is the epipole F<sup>T</sup>e' = 0, [e'] is the cross product matrix for the enclosed vector,
	 * v is an arbitrary 3-vector and lambda is a non-zero scalar.
	 * </p>
	 *
	 * <p>
	 *     NOTE: Additional information is needed to upgrade this projective transform into a metric transform.
	 * </p>
	 * <p>
	 * Page 256 in R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003
	 * </p>
	 *
	 * @param F (Input) A fundamental matrix
	 * @param e2 (Input) Left epipole of fundamental matrix, F<sup>T</sup>*e2 = 0.
	 * @param v (Input) Arbitrary 3-vector.  Just pick some value, say (0,0,0).
	 * @param lambda (Input) A non zero scalar.  Try one.
	 * @param cameraMatrix (Output) resulting projective camera matrix P'. (3 by 4) Known up to a projective transform.
	 */
	public void process(DMatrixRMaj F , Point3D_F64 e2, Vector3D_F64 v , double lambda , DMatrixRMaj cameraMatrix ) {

		GeometryMath_F64.crossMatrix(e2, crossMatrix);

		GeometryMath_F64.outerProd(e2,v,outer);

		CommonOps_DDRM.mult(crossMatrix, F, KR);
		CommonOps_DDRM.add(KR, outer, KR);

		CommonOps_DDRM.insert(KR,cameraMatrix,0,0);

		cameraMatrix.set(0,3,lambda*e2.x);
		cameraMatrix.set(1,3,lambda*e2.y);
		cameraMatrix.set(2,3,lambda*e2.z);
	}
}
