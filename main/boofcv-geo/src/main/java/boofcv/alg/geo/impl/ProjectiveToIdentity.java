/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.impl;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.SingularOps_DDRM;
import org.ejml.dense.row.SpecializedOps_DDRM;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.ejml.interfaces.decomposition.SingularValueDecomposition_F64;

/**
 * Uses SVD to compute the projective transform which will turn a matrix matrix into identity, e.g.
 * P*H = [I|0], where P is a 3x4 camera matrix and H is a 4x4 homography transform. H has the structure
 * [P'|u] where P' (4x3) is the pseudo inverse of P and u is the nullspace of P (4x1)
 *
 * @author Peter Abeles
 */
public class ProjectiveToIdentity {
	SingularValueDecomposition_F64<DMatrixRMaj> svd = DecompositionFactory_DDRM.svd(true, true, false);
	DMatrixRMaj Ut = new DMatrixRMaj(3, 3);
	DMatrixRMaj Wt = new DMatrixRMaj(4, 3);
	DMatrixRMaj V = new DMatrixRMaj(4, 4);

	DMatrixRMaj tmp = new DMatrixRMaj(4, 4);
	DMatrixRMaj ns = new DMatrixRMaj(4, 1);

	// storage for pseudo inverse of P
	DMatrixRMaj PA = new DMatrixRMaj(4, 3);

	/**
	 * Compute projective transform that converts P into identity
	 *
	 * @param P (Input) 3x4 camera matrix
	 * @return true if no errors
	 */
	public boolean process( DMatrixRMaj P ) {
		if (!svd.decompose(P))
			return false;

		svd.getU(Ut, true);
		svd.getV(V, false);
		double sv[] = svd.getSingularValues();

		SingularOps_DDRM.descendingOrder(Ut, true, sv, 3, V, false);

		// compute W+, which is transposed and non-negative inverted
		for (int i = 0; i < 3; i++) {
			Wt.unsafe_set(i, i, 1.0/sv[i]);
		}

		// get the pseudo inverse
		// A+ = V*(W+)*U'
		CommonOps_DDRM.mult(V, Wt, tmp);
		CommonOps_DDRM.mult(tmp, Ut, PA);

		// Vector U, which is P*U = 0
		SpecializedOps_DDRM.subvector(V, 0, 3, V.numRows, false, 0, ns);

		return true;
	}

	/**
	 * Retrieve projective transform H
	 */
	public void computeH( DMatrixRMaj H ) {
		H.reshape(4, 4);

		CommonOps_DDRM.insert(PA, H, 0, 0);

		for (int i = 0; i < 4; i++) {
			H.unsafe_set(i, 3, ns.data[i]);
		}
	}

	public DMatrixRMaj getPseudoInvP() {
		return PA;
	}

	public DMatrixRMaj getU() {
		return ns;
	}
}
