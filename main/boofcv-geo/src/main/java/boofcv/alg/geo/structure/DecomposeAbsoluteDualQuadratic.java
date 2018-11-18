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

package boofcv.alg.geo.structure;

import org.ejml.data.DMatrix3;
import org.ejml.data.DMatrix3x3;
import org.ejml.data.DMatrix4x4;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.fixed.CommonOps_DDF3;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.ejml.interfaces.decomposition.SingularValueDecomposition_F64;

/**
 * Decomposes the absolute quadratic to extract the rectifying homogrpahy H. This is used to go from
 * a projective to metric (calibrated) geometry. See pg 464 in [1].
 *
 * <p>Q = H*I*H<sup>T</sup></p>
 *
 * <ol>
 * <li> R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </ol>
 *
 * @author Peter Abeles
 */
public class DecomposeAbsoluteDualQuadratic {
	SingularValueDecomposition_F64<DMatrixRMaj> svd
			= DecompositionFactory_DDRM.svd(true,false,true);

	// Storage for the found solution
	DMatrixRMaj H = new DMatrixRMaj(4,4);

	// storage for calibration matrix
	DMatrix3x3 k = new DMatrix3x3();
	// work space variables
	DMatrix3x3 w_inv = new DMatrix3x3();
	DMatrix3x3 tmp = new DMatrix3x3();

	DMatrix3 t = new DMatrix3();
	DMatrix3 p = new DMatrix3();

	public boolean decompose(DMatrix4x4 Q ) {

		// TODO consider using eigen decomposition like it was suggested
		// tried SVD, forced diagonal to be [1,1,10], and householder to make column
		// but that didn't work because the householder affected the row/col of symmetric matrix
		// using a decomposition could use more information in the matrix improving stability

		// Directly extract from the definition of Q
		// Q = [w -w*p;-p'*w p'*w*p]
		// w = k*k'
		k.a11 = Q.a11;k.a12 = Q.a12;k.a13 = Q.a13;
		k.a21 = Q.a21;k.a22 = Q.a22;k.a23 = Q.a23;
		k.a31 = Q.a31;k.a32 = Q.a32;k.a33 = Q.a33;

		CommonOps_DDF3.invert(k,w_inv);
		tmp.set(w_inv);
		CommonOps_DDF3.divide(tmp,tmp.a33);
		CommonOps_DDF3.cholU(tmp);
		CommonOps_DDF3.invert(tmp,k);
		CommonOps_DDF3.divide(k,k.a33);

		t.a1 = Q.a14;
		t.a2 = Q.a24;
		t.a3 = Q.a34;

		CommonOps_DDF3.mult(w_inv, t, p);
		CommonOps_DDF3.scale(-1,p);

		// insert the results into H
		// H = [K 0;-p'*K 1 ]
		H.zero();
		for (int i = 0; i < 3; i++) {
			for (int j = i; j < 3; j++) {
				H.set(i,j,k.get(i,j));
			}
		}
		H.set(3,0, -(p.a1*k.a11 + p.a2*k.a21 + p.a3*k.a31));
		H.set(3,1, -(p.a1*k.a12 + p.a2*k.a22 + p.a3*k.a32));
		H.set(3,2, -(p.a1*k.a13 + p.a2*k.a23 + p.a3*k.a33));
		H.set(3,3,1);
		return true;
	}

	public DMatrixRMaj getH() {
		return H;
	}
}
