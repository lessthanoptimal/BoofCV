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

package boofcv.alg.distort;

import boofcv.struct.distort.PointTransform_F32;
import georegression.struct.point.Point2D_F32;

/**
 * Given an undistorted normalized pixel coordinate, compute the distorted normalized coordinate.
 *
 * @author Peter Abeles
 */
public class AddRadialNtoN_F32 implements PointTransform_F32 {

	// radial distortion
	private float radial[];

	public AddRadialNtoN_F32() {
	}

	/**
	 * Specify intrinsic camera parameters
	 *
	 * @param radial Radial distortion parameters
	 */
	public void set(float[] radial) {

		this.radial = new float[radial.length];
		for( int i = 0; i < radial.length; i++ ) {
			this.radial[i] = radial[i];
		}
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
		float sum = 0;

		double r2 = x*x + y*y;

		double r = r2;

		for( int i = 0; i < radial.length; i++ ) {
			sum += radial[i]*r;
			r *= r2;
		}

		out.x = x*( 1 + sum);
		out.y = y*( 1 + sum);
	}
}
