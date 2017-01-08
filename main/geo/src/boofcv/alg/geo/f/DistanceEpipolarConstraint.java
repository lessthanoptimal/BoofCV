/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.geo.AssociatedPair;
import georegression.geometry.GeometryMath_F64;
import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.ejml.data.RowMatrix_F64;
import org.ejml.ops.CommonOps_R64;

import java.util.List;

/**
 * Computes error using the epipolar constraint.  The input matrix is normalized so that different matrices
 * can be compared at the same scale.
 *
 * @author Peter Abeles
 */
public class DistanceEpipolarConstraint implements DistanceFromModel<RowMatrix_F64,AssociatedPair> {

	RowMatrix_F64 M = new RowMatrix_F64(3,3);

	@Override
	public void setModel(RowMatrix_F64 F )
	{
		// assume that each element in the matrix has equal weight
		double v = CommonOps_R64.elementSumAbs(F);
		CommonOps_R64.scale(1.0/v,F,M);
	}

	@Override
	public double computeDistance(AssociatedPair pt) {
		return Math.abs(GeometryMath_F64.innerProd(pt.p2, M, pt.p1));
	}

	@Override
	public void computeDistance(List<AssociatedPair> associatedPairs, double[] distance) {
		for( int i = 0; i < associatedPairs.size(); i++ ) {
			distance[i] = computeDistance(associatedPairs.get(i));
		}
	}

	@Override
	public Class<AssociatedPair> getPointType() {
		return AssociatedPair.class;
	}

	@Override
	public Class<RowMatrix_F64> getModelType() {
		return RowMatrix_F64.class;
	}
}
