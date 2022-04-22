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

package boofcv.alg.geo;

import georegression.struct.point.Point3D_F64;

/**
 * Simple function for converting error in pointing vector coordinates to pixels using
 * intrinsic camera parameters. Better to use tested code than cut and pasting.
 *
 * @author Peter Abeles
 */
public class PointingToPixelError {
	private double fx; // focal length x
	private double fy; // focal length y
	private double skew; // pixel skew

	public PointingToPixelError( double fx, double fy, double skew ) {
		setTo(fx, fy, skew);
	}

	public PointingToPixelError() {}

	/**
	 * Specify camera intrinsic parameters
	 *
	 * @param fx focal length x
	 * @param fy focal length y
	 * @param skew camera skew
	 */
	public void setTo( double fx, double fy, double skew ) {
		this.fx = fx;
		this.fy = fy;
		this.skew = skew;
	}

	public double errorSq( Point3D_F64 a, Point3D_F64 b ) {
		return errorSq(a.x, a.y, a.z, b.x, b.y, b.z);
	}

	public double errorSq( double a_x, double a_y, double a_z, double b_x, double b_y, double b_z ) {
		// There's a singularity if z == 0. No good way to handle that so we treat that as if it's 1
		if (a_z != 0.0) {
			a_x /= a_z;
			a_y /= a_z;
		}
		if (b_z != 0.0) {
			b_x /= b_z;
			b_y /= b_z;
		}
		double dy = (b_y - a_y);
		double dx = (b_x - a_x)*fx + dy*skew;
		dy *= fy;

		return dx*dx + dy*dy;
	}
}
