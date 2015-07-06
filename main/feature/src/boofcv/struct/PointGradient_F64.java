/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import georegression.struct.point.Point2D_F64;

/**
 * Data structure for a point coordinate and the gradient at that location
 *
 * @author Peter Abeles
 */
public class PointGradient_F64 extends Point2D_F64{
	/**
	 * Image gradient at this location
	 */
	public double dx,dy;

	public PointGradient_F64(double x, double y, double dx, double dy) {
		super(x, y);
		this.dx = dx;
		this.dy = dy;
	}

	public PointGradient_F64(PointGradient_F64 orig) {
		set(orig);
	}

	public PointGradient_F64() {
	}

	public void set( PointGradient_F64 orig ) {
		this.x = orig.x;
		this.y = orig.y;
		this.dx = orig.dx;
		this.dy = orig.dy;
	}

	public void set( double x , double y , double dx , double dy ) {
		this.x = x;
		this.y = y;
		this.dx = dx;
		this.dy = dy;
	}

	public void setDx(double dx) {
		this.dx = dx;
	}

	public void setDy(double dy) {
		this.dy = dy;
	}

	public double getDx() {
		return dx;
	}

	public double getDy() {
		return dy;
	}

	public PointGradient_F64 copy() {
		return new PointGradient_F64(this);
	}
}
