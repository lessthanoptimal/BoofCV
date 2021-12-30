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

package boofcv.alg.geo.f;

import georegression.struct.point.Point3D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.SingularOps_DDRM;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.ejml.interfaces.decomposition.SingularValueDecomposition_F64;
import org.jetbrains.annotations.Nullable;

/**
 * <p>
 * Extracts the epipoles from an essential or fundamental matrix. The epipoles are extracted
 * from the left and right null space of the provided matrix. Note that the found epipoles are
 * in homogeneous coordinates. If the epipole is at infinity then z=0
 * </p>
 *
 * <p>
 * Left: e<sub>2</sub><sup>T</sup>*F = 0 <br>
 * Right: F*e<sub>1</sub> = 0
 * </p>
 *
 * @author Peter Abeles
 */
public class FundamentalExtractEpipoles {
	SingularValueDecomposition_F64<DMatrixRMaj> svd =
			DecompositionFactory_DDRM.svd(true, true, false);

	DMatrixRMaj U = new DMatrixRMaj(3, 3);
	DMatrixRMaj V = new DMatrixRMaj(3, 3);

	/**
	 * Extracts the left and right epipoles.
	 *
	 * @param F (Input) Fundamental matrix
	 * @param e1 (Output) right null space. Can be null.
	 * @param e2 (Output) left null space. Can be null.
	 */
	public void process( DMatrixRMaj F, @Nullable Point3D_F64 e1, @Nullable Point3D_F64 e2 ) {

		if (!svd.decompose(F))
			throw new RuntimeException("SVD Failed?!");

		svd.getU(U, false);
		svd.getV(V, false);
		double singular[] = svd.getSingularValues();
		SingularOps_DDRM.descendingOrder(U, false, singular, 3, V, false);

		if (e2 != null)
			e2.setTo(U.get(0, 2), U.get(1, 2), U.get(2, 2));
		if (e1 != null)
			e1.setTo(V.get(0, 2), V.get(1, 2), V.get(2, 2));
	}
}
