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

package boofcv.struct;

/**
 * <p>
 * A rectangle which can be rotated.  Angle of rotation is relative to +x axis, which is along the rectangle's
 * width. +y is along its height. Point of rotation is the rectangle's center.
 * </p>
 * <p>
 * Conversion from rectangle to parent frame:<br>
 * x' = x*cos(theta) - y*sin(theta) + cx;<br>
 * y' = x*sin(theta) + y*cos(theta) + cy;<br>
 * where (x,y) are points in the rectangle's frame.
 * </p>
 *
 * @author Peter Abeles
 */
public class RectangleRotate_F64 {
	/** Center point of rectangle */
	public double cx,cy;
	/** Width of rectangle */
	public double width;
	/** height of rectangle */
	public double height;
	/** Angle of rotation about rectangle's center */
	public double theta;

	public RectangleRotate_F64(double cx, double cy, double width, double height, double theta) {
		this.cx = cx;
		this.cy = cy;
		this.width = width;
		this.height = height;
		this.theta = theta;
	}

	public RectangleRotate_F64() {
	}

	public void set( RectangleRotate_F64 r ) {
		this.cx = r.cx;
		this.cy = r.cy;
		this.width = r.width;
		this.height = r.height;
		this.theta = r.theta;
	}

	public String toString() {
		return getClass().getSimpleName()+"( cx = "+cx+" cy = "+cy+" width = "+width+" height = "+height+" theta = "+theta+" )";
	}
}
