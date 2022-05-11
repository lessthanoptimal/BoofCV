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

package boofcv.alg.distort.division;

import boofcv.struct.distort.Point2Transform2_F64;
import georegression.struct.point.Point2D_F64;

/**
 * Converts the undistorted pixel coordinate into distorted pixel coordinates.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class AddDivisionPtoP_F64 implements Point2Transform2_F64 {

	// camera principle point
	double cx, cy;

	// lens distortion
	public double radial;

	/** Convergence tolerance */
	public double tol = (double)1e-8;
	/** Maximum number of iterations */
	public int maxIterations = 500;

	public AddDivisionPtoP_F64() {}

	public AddDivisionPtoP_F64 setIntrinsics( double cx, double cy ) {
		this.cx = cx;
		this.cy = cy;
		return this;
	}

	public AddDivisionPtoP_F64 setRadial( double radial ) {
		this.radial = radial;
		return this;
	}

	/**
	 * Adds radial distortion
	 *
	 * @param x Undistorted x-coordinate pixel image coordinates
	 * @param y Undistorted y-coordinate pixel image coordinates
	 * @param out Distorted normalized image coordinate.
	 */
	@Override
	public void compute( final double x, final double y, Point2D_F64 out ) {
		double xx = x - cx;
		double yy = y - cy;

		double origX = xx;
		double origY = yy;

		double prevR2 = 0;

		for (int iter = 0; iter < maxIterations; iter++) {
			double r2 = xx*xx + yy*yy;

			xx = origX*(1.0 + radial*r2);
			yy = origY*(1.0 + radial*r2);

			if (Math.abs(r2 - prevR2) <= tol) {
				break;
			} else {
				prevR2 = r2;
			}
		}
		out.setTo(xx + cx, yy + cy);
	}

	@Override
	public AddDivisionPtoP_F64 copyConcurrent() {
		var ret = new AddDivisionPtoP_F64();
		ret.radial = radial;
		return ret;
	}
}
