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
import org.ejml.UtilEjml;
import org.ejml.data.DenseMatrix64F;

/**
 * <p>
 * Computes the Sampson distance residual for a set of observations given a fundamental matrix.  For use
 * in least-squares non-linear optimization algorithms.
 * </p>
 *
 * <p>
 * Page 287 in: R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </p>
 *
 * @author Peter Abeles
 */
public class FundamentalResidualSampson
		implements ModelObservationResidual<DenseMatrix64F,AssociatedPair> {
	DenseMatrix64F F;
	Point3D_F64 temp = new Point3D_F64();

	@Override
	public void setModel(DenseMatrix64F F) {
		this.F = F;
	}

	@Override
	public double computeResidual(AssociatedPair observation) {
		double bottom = 0;

		GeometryMath_F64.mult(F, observation.p1, temp);
		bottom += temp.x*temp.x + temp.y*temp.y;

		GeometryMath_F64.multTran(F, observation.p2, temp);
		bottom += temp.x*temp.x + temp.y*temp.y;

		bottom = Math.sqrt(bottom);

		if( bottom <= UtilEjml.EPS) {
			return Double.MAX_VALUE;
		} else {
			GeometryMath_F64.multTran(F,observation.p2,temp);

			return (temp.x*observation.p1.x + temp.y*observation.p1.y + temp.z)/bottom;
		}
	}
}
