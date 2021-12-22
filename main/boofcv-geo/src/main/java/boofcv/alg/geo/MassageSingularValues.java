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

package boofcv.alg.geo;

import boofcv.misc.BoofLambdas;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.SingularOps_DDRM;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.ejml.interfaces.decomposition.SingularValueDecomposition_F64;

/**
 * Forces the smallest singular value in the matrix to be zero
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class MassageSingularValues {
	protected SingularValueDecomposition_F64<DMatrixRMaj> svd;

	// SVD decomposition of F = U*S*V^T
	protected DMatrixRMaj U;
	protected DMatrixRMaj W;
	protected DMatrixRMaj V;
	protected DMatrixRMaj temp0 = new DMatrixRMaj(1, 1);

	// Used to massage the singular values
	BoofLambdas.ProcessObject<DMatrixRMaj> massageSingular;

	public MassageSingularValues( SingularValueDecomposition_F64<DMatrixRMaj> svd,
								  BoofLambdas.ProcessObject<DMatrixRMaj> massageSingular ) {
		this.svd = svd;
		this.massageSingular = massageSingular;
	}

	public MassageSingularValues( BoofLambdas.ProcessObject<DMatrixRMaj> massageSingular ) {
		svd = DecompositionFactory_DDRM.svd(true, true, true);
		this.massageSingular = massageSingular;
	}

	public MassageSingularValues() {
		this(F -> {});
	}

	/**
	 * Massages the input matrix with the user provided function.
	 *
	 * @return true if svd returned true.
	 */
	public boolean process( DMatrixRMaj E ) {
		return process(E, massageSingular);
	}

	/**
	 * Massages the input matrix with the user provided function
	 *
	 * @param E Input matrix that has its singular values modified
	 * @param massageSingular Function which massages the singular values
	 * @return true if svd returned true.
	 */
	public boolean process( DMatrixRMaj E, BoofLambdas.ProcessObject<DMatrixRMaj> massageSingular ) {
		if (!svd.decompose(E)) {
			return false;
		}
		V = svd.getV(V, false);
		U = svd.getU(U, false);
		W = svd.getW(W);

		SingularOps_DDRM.descendingOrder(U, false, W, V, false);

		massageSingular.process(W);

		// recompute the input matrix
		CommonOps_DDRM.mult(U, W, temp0);
		CommonOps_DDRM.multTransB(temp0, V, E);

		return true;
	}
}
