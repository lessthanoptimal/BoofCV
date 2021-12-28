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

package boofcv.alg.distort.brown;

import boofcv.struct.distort.Point2Transform2_F64;
import georegression.struct.point.Point2D_F64;
import org.jetbrains.annotations.Nullable;

/**
 * Given an undistorted normalized pixel coordinate, compute the distorted normalized coordinate.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class AddBrownNtoN_F64 implements Point2Transform2_F64 {

	private RadialTangential_F64 params;

	public AddBrownNtoN_F64() {}

	/**
	 * Specify intrinsic camera parameters
	 *
	 * @param radial Radial distortion parameters
	 */
	public AddBrownNtoN_F64 setDistortion( @Nullable /**/double[] radial, /**/double t1, /**/double t2 ) {
		params = new RadialTangential_F64(radial, t1, t2);
		return this;
	}

	/**
	 * Adds radial distortion
	 *
	 * @param x Undistorted x-coordinate normalized image coordinates
	 * @param y Undistorted y-coordinate normalized image coordinates
	 * @param out Distorted normalized image coordinate.
	 */
	@Override
	public void compute( double x, double y, Point2D_F64 out ) {

		double[] radial = params.radial;
		double t1 = params.t1;
		double t2 = params.t2;

		double r2 = x*x + y*y;
		double ri2 = r2;

		double sum = 0;
		for (int i = 0; i < radial.length; i++) {
			sum += radial[i]*ri2;
			ri2 *= r2;
		}

		out.x = x*(1 + sum) + 2*t1*x*y + t2*(r2 + 2*x*x);
		out.y = y*(1 + sum) + t1*(r2 + 2*y*y) + 2*t2*x*y;
	}

	@Override
	public AddBrownNtoN_F64 copyConcurrent() {
		AddBrownNtoN_F64 ret = new AddBrownNtoN_F64();
		ret.params = new RadialTangential_F64(this.params);
		return ret;
	}
}
