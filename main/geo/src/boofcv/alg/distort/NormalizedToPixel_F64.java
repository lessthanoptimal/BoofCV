/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.distort;

import boofcv.struct.distort.PointTransform_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

/**
 * Converts normalized pixel coordinate into pixel coordinate.
 *
 * @author Peter Abeles
 */
public class NormalizedToPixel_F64 implements PointTransform_F64 {

	// camera calibration matrix
	protected DenseMatrix64F K = new DenseMatrix64F(3,3);

	public void set(double fx, double fy, double skew, double x_c, double y_c ) {

		K.set(0, 0, fx);
		K.set(1, 1, fy);
		K.set(0, 1, skew);
		K.set(0, 2, x_c);
		K.set(1, 2, y_c);
		K.set(2, 2, 1);
	}


	@Override
	public void compute(double x, double y, Point2D_F64 out) {
		out.set(x,y);

		GeometryMath_F64.mult(K, out, out);
	}
}
