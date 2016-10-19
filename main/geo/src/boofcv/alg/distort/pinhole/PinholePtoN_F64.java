/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.distort.pinhole;

import boofcv.struct.distort.Point2Transform2_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

/**
 * Converts an image pixel coordinate into a normalized pixel coordinate using the
 * camera's intrinsic parameters.  Lens distortion must have already been removed.
 *
 * @author Peter Abeles
 */
public class PinholePtoN_F64 implements Point2Transform2_F64 {

	// inverse of camera calibration matrix
	protected DenseMatrix64F K_inv = new DenseMatrix64F(3,3);

	public void set(/**/double fx, /**/double fy, /**/double skew, /**/double cx, /**/double cy ) {

		K_inv.set(0,0,fx);
		K_inv.set(1,1,fy);
		K_inv.set(0,1,skew);
		K_inv.set(0,2,cx);
		K_inv.set(1,2,cy);
		K_inv.set(2,2,1);

		CommonOps.invert(K_inv);
	}


	@Override
	public void compute(double x, double y, Point2D_F64 out) {
		out.set(x,y);

		GeometryMath_F64.mult(K_inv, out, out);
	}
}
