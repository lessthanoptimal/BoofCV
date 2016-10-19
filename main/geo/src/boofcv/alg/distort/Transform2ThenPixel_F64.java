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

package boofcv.alg.distort;

import boofcv.struct.distort.Point2Transform2_F64;
import georegression.struct.point.Point2D_F64;

/**
 * Applies a transform which outputs normalized image coordinates then converts that into
 * pixel coordinates
 *
 * @author Peter Abeles
 */
public class Transform2ThenPixel_F64 implements Point2Transform2_F64 {

	double fx, fy, skew, cx, cy;
	Point2Transform2_F64 first;

	public Transform2ThenPixel_F64(Point2Transform2_F64 first) {
		this.first = first;
	}

	public Point2Transform2_F64 set( /**/double fx, /**/double fy, /**/double skew, /**/double cx, /**/double cy ) {
		this.fx = (double)fx;
		this.fy = (double)fy;
		this.skew = (double)skew;
		this.cx = (double)cx;
		this.cy = (double)cy;

		return this;
	}

	@Override
	public void compute(double x, double y, Point2D_F64 out) {
		first.compute(x,y,out);
		x = out.x; y = out.y;
		out.x = fx*x + skew*y + cx;
		out.y = fy*y + cy;
	}
}
