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
 * Converts the observed distorted normalized image coordinates into undistorted normalized image coordinates.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class RemoveDivisionNtoN_F64 implements Point2Transform2_F64 {
	// lens distortion
	public double radial;

	public RemoveDivisionNtoN_F64() {}

	public RemoveDivisionNtoN_F64 setRadial( double radial ) {
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
		double r2 = x*x + y*y;

		out.x = x/(1.0 + radial*r2);
		out.y = y/(1.0 + radial*r2);
	}

	@Override
	public RemoveDivisionNtoN_F64 copyConcurrent() {
		var ret = new RemoveDivisionNtoN_F64();
		ret.radial = radial;
		return ret;
	}
}
