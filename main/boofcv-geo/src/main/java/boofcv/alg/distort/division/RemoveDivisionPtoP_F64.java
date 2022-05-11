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
 * Converts the observed distorted pixel image coordinates into undistorted pixel image coordinates.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class RemoveDivisionPtoP_F64 implements Point2Transform2_F64 {

	// camera principle point
	double cx, cy;

	// lens distortion
	public double radial;

	public RemoveDivisionPtoP_F64() {}

	public RemoveDivisionPtoP_F64 setIntrinsics( double cx, double cy ) {
		this.cx = cx;
		this.cy = cy;
		return this;
	}

	public RemoveDivisionPtoP_F64 setRadial( double radial ) {
		this.radial = radial;
		return this;
	}

	/**
	 * Adds radial distortion
	 *
	 * @param x Distorted x-coordinate pixel image coordinates
	 * @param y Distorted y-coordinate pixel image coordinates
	 * @param out Undistorted normalized image coordinate.
	 */
	@Override
	public void compute( double x, double y, Point2D_F64 out ) {
		// must shift points so that they are in the image center first
		double xx = x - cx;
		double yy = y - cy;

		double r2 = xx*xx + yy*yy;

		out.x = cx + xx/(1.0 + radial*r2);
		out.y = cy + yy/(1.0 + radial*r2);
	}

	@Override
	public RemoveDivisionPtoP_F64 copyConcurrent() {
		var ret = new RemoveDivisionPtoP_F64();
		ret.radial = radial;
		return ret;
	}
}
