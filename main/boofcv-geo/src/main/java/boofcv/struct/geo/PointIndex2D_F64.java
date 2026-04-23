/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.geo;

import georegression.struct.point.Point2D_F64;

/**
 * A 2D point with an index associated with it
 *
 * @author Peter Abeles
 */
public class PointIndex2D_F64 extends PointIndex<PointIndex2D_F64, Point2D_F64> {

	public PointIndex2D_F64( double x, double y, int index ) {
		this();
		setTo(x, y, index);
	}

	public PointIndex2D_F64( double x, double y ) {
		this();
		setTo(x, y, 0);
	}

	public PointIndex2D_F64() {super(new Point2D_F64());}

	public PointIndex2D_F64( Point2D_F64 p, int index ) {
		this();
		setTo(p, index);
	}

	public PointIndex2D_F64 setTo( double x, double y, int index ) {
		this.p.setTo(x, y);
		this.index = index;
		return this;
	}

	public boolean isIdentical( double x, double y, int index ) {
		if (this.index != index)
			return false;
		if (p.x != x)
			return false;
		if (p.y != y)
			return false;
		return true;
	}

	public boolean isIdentical( PointIndex2D_F64 o ) {
		if (index != o.index)
			return false;
		if (p.x != o.p.x)
			return false;
		if (p.y != o.p.y)
			return false;
		return true;
	}

	public boolean isIdentical( PointIndex2D_F64 o, double tol ) {
		if (index != o.index)
			return false;
		if (Math.abs(p.x - o.p.x) > tol || Math.abs(p.y - o.p.y) > tol)
			return false;
		return true;
	}

	@Override
	public PointIndex2D_F64 copy() {
		return new PointIndex2D_F64(p, index);
	}
}
