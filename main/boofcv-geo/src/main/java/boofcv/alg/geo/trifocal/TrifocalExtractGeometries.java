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

package boofcv.alg.geo.trifocal;

import boofcv.struct.geo.TrifocalTensor;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point3D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.SingularOps_DDRM;
import org.ejml.dense.row.decomposition.svd.SafeSvd_DDRM;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.ejml.interfaces.decomposition.SingularValueDecomposition_F64;

/**
 * <p>
 * Extracts the epipoles, camera matrices, and fundamental matrices for views 2 and 3 with respect
 * to view 1 from the trifocal tensor. Epipoles are found in homogeneous coordinates. Singular value
 * decomposition is used to compute each vector and is robust to noise and epipoles will have a norm of one..
 * </p>
 *
 * <p>Properties:</p>
 * <ul>
 *     <li> e2<sup>T</sup>*F12 = 0
 *     <li> e3<sup>T</sup>*F13 = 0
 * </ul>
 * <p>where F1i is a fundamental matrix from image 1 to i.</p>
 *
 * <p>References:</p>
 * <ul>
 * <li> R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </ul>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class TrifocalExtractGeometries {

	// reference to input tensor
	private TrifocalTensor tensor;

	// used to extract the null space
	private SingularValueDecomposition_F64<DMatrixRMaj> svd;

	// storage for left and right null space of the trifocal matrices
	private DMatrixRMaj u1 = new DMatrixRMaj(3, 1);
	private DMatrixRMaj u2 = new DMatrixRMaj(3, 1);
	private DMatrixRMaj u3 = new DMatrixRMaj(3, 1);
	private DMatrixRMaj v1 = new DMatrixRMaj(3, 1);
	private DMatrixRMaj v2 = new DMatrixRMaj(3, 1);
	private DMatrixRMaj v3 = new DMatrixRMaj(3, 1);

	private DMatrixRMaj U = new DMatrixRMaj(3, 3);
	private DMatrixRMaj V = new DMatrixRMaj(3, 3);

	// temporary storage for computed epipole
	private DMatrixRMaj tempE = new DMatrixRMaj(3, 1);

	// storage for intermediate results
	Point3D_F64 column = new Point3D_F64();
	Point3D_F64 temp0 = new Point3D_F64();
	DMatrixRMaj temp1 = new DMatrixRMaj(3, 3);

	// The epipoles
	Point3D_F64 e2 = new Point3D_F64(), e3 = new Point3D_F64();

	public TrifocalExtractGeometries() {
		svd = DecompositionFactory_DDRM.svd(3, 3, true, true, true);
		svd = new SafeSvd_DDRM(svd);
	}

	/**
	 * Specifies the input tensor. The epipoles are immediately extracted since they
	 * are needed to extract all other data structures
	 *
	 * @param tensor The tensor. Reference is saved but not modified
	 */
	public void setTensor( TrifocalTensor tensor ) {
		this.tensor = tensor;
		if (!svd.decompose(tensor.T1))
			throw new RuntimeException("SVD failed?!");

		SingularOps_DDRM.nullVector(svd, true, v1);
		SingularOps_DDRM.nullVector(svd, false, u1);

//		DMatrixRMaj zero = new DMatrixRMaj(3,1);
//		CommonOps_DDRM.mult(tensor.T1,v1,zero);zero.print();
//		CommonOps_DDRM.multTransA(u1,tensor.T1,zero);zero.print();

		if (!svd.decompose(tensor.T2))
			throw new RuntimeException("SVD failed?!");
		SingularOps_DDRM.nullVector(svd, true, v2);
		SingularOps_DDRM.nullVector(svd, false, u2);
//		CommonOps_DDRM.mult(tensor.T2,v2,zero);zero.print();
//		CommonOps_DDRM.multTransA(u2,tensor.T2,zero);zero.print();

		if (!svd.decompose(tensor.T3))
			throw new RuntimeException("SVD failed?!");
		SingularOps_DDRM.nullVector(svd, true, v3);
		SingularOps_DDRM.nullVector(svd, false, u3);
//		CommonOps_DDRM.mult(tensor.T3,v3,zero);zero.print();
//		CommonOps_DDRM.multTransA(u3,tensor.T3,zero);zero.print();

		for (int i = 0; i < 3; i++) {
			U.set(i, 0, u1.get(i));
			U.set(i, 1, u2.get(i));
			U.set(i, 2, u3.get(i));

			V.set(i, 0, v1.get(i));
			V.set(i, 1, v2.get(i));
			V.set(i, 2, v3.get(i));
		}

		svd.decompose(U);
		SingularOps_DDRM.nullVector(svd, false, tempE);
		e2.setTo(tempE.get(0), tempE.get(1), tempE.get(2));

		svd.decompose(V);
		SingularOps_DDRM.nullVector(svd, false, tempE);
		e3.setTo(tempE.get(0), tempE.get(1), tempE.get(2));
	}

	/**
	 * Extracts the epipoles from the trifocal tensor. Extracted epipoles will have a norm of 1
	 * as an artifact of using SVD.
	 *
	 * @param e2 Output: Epipole in image 2. Homogeneous coordinates. Modified
	 * @param e3 Output: Epipole in image 3. Homogeneous coordinates. Modified
	 */
	public void extractEpipoles( Point3D_F64 e2, Point3D_F64 e3 ) {
		e2.setTo(this.e2);
		e3.setTo(this.e3);
	}

	/**
	 * <p>
	 * Extract the camera matrices up to a common projective transform.
	 * </p>
	 *
	 * <p>P2 = [[T1.T2.T3]e3|e2] and P3=[(e3*e2<sup>T</sup>-I)[T1',T2',T3'|e2|e3]</p>
	 *
	 * <p>
	 * NOTE: The camera matrix for the first view is assumed to be P1 = [I|0].
	 * </p>
	 *
	 * @param P2 Output: 3x4 camera matrix for views 2. Modified.
	 * @param P3 Output: 3x4 camera matrix for views 3. Modified.
	 */
	public void extractCamera( DMatrixRMaj P2, DMatrixRMaj P3 ) {
		// temp1 = [e3*e3^T -I]
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				temp1.set(i, j, e3.getIdx(i)*e3.getIdx(j));
			}
			temp1.set(i, i, temp1.get(i, i) - 1);
		}

		// compute the camera matrices one column at a time
		for (int i = 0; i < 3; i++) {
			DMatrixRMaj T = tensor.getT(i);

			GeometryMath_F64.mult(T, e3, column);
			P2.set(0, i, column.x);
			P2.set(1, i, column.y);
			P2.set(2, i, column.z);
			P2.set(i, 3, e2.getIdx(i));

			GeometryMath_F64.multTran(T, e2, temp0);
			GeometryMath_F64.mult(temp1, temp0, column);

			P3.set(0, i, column.x);
			P3.set(1, i, column.y);
			P3.set(2, i, column.z);
			P3.set(i, 3, e3.getIdx(i));
		}
	}

	/**
	 * <p>
	 * Extract the fundamental matrices between views 1 + 2 and views 1 + 3. The returned Fundamental
	 * matrices will have the following properties: x<sub>i</sub><sup>T</sup>*Fi*x<sub>1</sub> = 0, where i is view 2 or 3.
	 * </p>
	 *
	 * <p>
	 * NOTE: The first camera is assumed to have the camera matrix of P1 = [I|0]. Thus observations in pixels for
	 * the first camera will not meet the epipolar constraint when applied to the returned fundamental matrices.
	 * </p>
	 *
	 * <pre>
	 * F21=[e2]x *[T1,T2,T3]*e3 and F31 = [e3]x*[T1,T2,T3]*e3
	 * </pre>
	 *
	 * @param F21 (Output) Fundamental matrix
	 * @param F31 (Output) Fundamental matrix
	 */
	public void extractFundmental( DMatrixRMaj F21, DMatrixRMaj F31 ) {
		// compute the camera matrices one column at a time
		for (int i = 0; i < 3; i++) {
			DMatrixRMaj T = tensor.getT(i);


			GeometryMath_F64.mult(T, e3, temp0);
			GeometryMath_F64.cross(e2, temp0, column);

			F21.set(0, i, column.x);
			F21.set(1, i, column.y);
			F21.set(2, i, column.z);

			GeometryMath_F64.multTran(T, e2, temp0);
			GeometryMath_F64.cross(e3, temp0, column);

			F31.set(0, i, column.x);
			F31.set(1, i, column.y);
			F31.set(2, i, column.z);
		}
	}
}
