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

package boofcv.alg.distort.radtan;

import boofcv.struct.distort.Point2Transform2_F32;
import georegression.struct.point.Point2D_F32;

/**
 * Given an undistorted normalized pixel coordinate, compute the distorted normalized coordinate.
 *
 * @author Peter Abeles
 */
public class AddRadialNtoN_F32 implements Point2Transform2_F32 {

	private RadialTangential_F32 params;

	public AddRadialNtoN_F32() {
	}

	/**
	 * Specify intrinsic camera parameters
	 *
	 * @param radial Radial distortion parameters
	 */
	public AddRadialNtoN_F32 setDistortion( /**/double[] radial, /**/double t1, /**/double t2) {
		params = new RadialTangential_F32(radial,t1,t2);
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
	public void compute(float x, float y, Point2D_F32 out) {

		float[] radial = params.radial;
		float t1 = params.t1;
		float t2 = params.t2;

		float r2 = x*x + y*y;
		float ri2 = r2;

		float sum = 0;
		for( int i = 0; i < radial.length; i++ ) {
			sum += radial[i]*ri2;
			ri2 *= r2;
		}

		out.x = x*( 1 + sum);
		out.y = y*( 1 + sum);

		out.x += 2*t1*x*y + t2*(r2 + 2*x*x);
		out.y += t1*(r2 + 2*y*y) + 2*t2*x*y;
	}
}
