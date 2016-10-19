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

import boofcv.struct.distort.Point2Transform2_F32;
import georegression.struct.point.Point2D_F32;

/**
 * Converts normalized pixel coordinate into pixel coordinate.
 *
 * @author Peter Abeles
 */
public class PinholeNtoP_F32 implements Point2Transform2_F32 {

	// camera calibration matrix
	float fx, fy, skew, cx, cy;

	public PinholeNtoP_F32 set( /**/double fx, /**/double fy, /**/double skew, /**/double cx, /**/double cy) {
		this.fx = (float)fx;
		this.fy = (float)fy;
		this.skew = (float)skew;
		this.cx = (float)cx;
		this.cy = (float)cy;
		return this;
	}


	@Override
	public void compute(float x, float y, Point2D_F32 out) {
		out.x = fx * x + skew * y + cx;
		out.y = fy * y + cy;
	}
}
