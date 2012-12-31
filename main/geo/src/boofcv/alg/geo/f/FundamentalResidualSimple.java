/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.geo.ModelObservationResidual;
import boofcv.struct.geo.AssociatedPair;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point3D_F64;
import org.ejml.data.DenseMatrix64F;

/**
 * <p>
 * Computes the residual just using the fundamental matrix constraint
 * </p>
 *
 * @author Peter Abeles
 */
public class FundamentalResidualSimple
		implements ModelObservationResidual<DenseMatrix64F,AssociatedPair> {
	DenseMatrix64F F;
	Point3D_F64 temp = new Point3D_F64();

	@Override
	public void setModel(DenseMatrix64F F) {
		this.F = F;
	}

	@Override
	public double computeResidual(AssociatedPair observation) {
		GeometryMath_F64.multTran(F,observation.p2,temp);

		return temp.x*observation.p1.x + temp.y*observation.p1.y + temp.z;
	}
}
