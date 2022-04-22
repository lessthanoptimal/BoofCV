/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.geo.AssociatedPair3D;
import georegression.geometry.GeometryMath_F64;
import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import java.util.List;

/**
 * Computes error using the epipolar constraint when given observations as pointing vectors. The input matrix is
 * normalized so that different matricescan be compared at the same scale.
 *
 * @author Peter Abeles
 */
public class DistanceEpipolarConstraintPointing implements DistanceFromModel<DMatrixRMaj, AssociatedPair3D> {

	DMatrixRMaj M = new DMatrixRMaj(3, 3);

	@Override
	public void setModel( DMatrixRMaj F ) {
		// assume that each element in the matrix has equal weight
		double v = CommonOps_DDRM.elementMaxAbs(F);
		CommonOps_DDRM.scale(1.0/v, F, M);
	}

	@Override
	public double distance( AssociatedPair3D pt ) {
		return Math.abs(GeometryMath_F64.innerProd(pt.p2, M, pt.p1));
	}

	@Override
	public void distances( List<AssociatedPair3D> associatedPairs, double[] distance ) {
		for (int i = 0; i < associatedPairs.size(); i++) {
			distance[i] = distance(associatedPairs.get(i));
		}
	}

	@Override
	public Class<AssociatedPair3D> getPointType() {
		return AssociatedPair3D.class;
	}

	@Override
	public Class<DMatrixRMaj> getModelType() {
		return DMatrixRMaj.class;
	}
}
