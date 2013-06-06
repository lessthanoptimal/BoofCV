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

package boofcv.alg.geo;

import georegression.struct.point.Point2D_F64;

/**
 * Simple function for converting error in normalized image coordinates to pixels using
 * intrinsic camera parameters. Better to use tested code than cut and pasting.
 *
 * @author Peter Abeles
 */
public class NormalizedToPixelError {
	private double fx; // focal length x
	private double fy; // focal length y
	private double skew; // pixel skew

	public NormalizedToPixelError(double fx, double fy, double skew) {
		this.fx = fx;
		this.fy = fy;
		this.skew = skew;
	}

	public NormalizedToPixelError()
	{}

	/**
	 * Specify camera intrinsic parameters
	 *
	 * @param fx focal length x
	 * @param fy focal length y
	 * @param skew camera skew
	 */
	public void set(double fx, double fy, double skew) {
		this.fx = fx;
		this.fy = fy;
		this.skew = skew;
	}

	public double errorSq( Point2D_F64 a , Point2D_F64 b ) {
		double dy = (b.y - a.y);
		double dx = (b.x - a.x)*fx + dy*skew;
		dy *= fy;

		return dx*dx + dy*dy;
	}

	public double errorSq( double a_x , double a_y , double b_x , double b_y ) {
		double dy = (b_y - a_y);
		double dx = (b_x - a_x)*fx + dy*skew;
		dy *= fy;

		return dx*dx + dy*dy;
	}
}
