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

package boofcv.alg.geo.h;

import boofcv.alg.geo.ModelObservationResidualN;
import boofcv.struct.geo.AssociatedPair;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DenseMatrix64F;

/**
 * <p>
 * Computes the difference between the point projected by the homography and its observed location.
 * Fast to compute but less theoretically correct than others.
 * </p>
 *
 * @author Peter Abeles
 */
public class HomographyResidualTransfer
		implements ModelObservationResidualN<DenseMatrix64F,AssociatedPair> {

	DenseMatrix64F H;

	Point2D_F64 temp = new Point2D_F64();

	@Override
	public void setModel(DenseMatrix64F F) {
		this.H = F;
	}

	@Override
	public int computeResiduals(AssociatedPair p, double[] residuals, int index) {

		GeometryMath_F64.mult(H, p.p1, temp);

		residuals[index++] = temp.x-p.p2.x;
		residuals[index++] = temp.y-p.p2.y;

		return index;
	}

	@Override
	public int getN() {
		return 2;
	}
}
