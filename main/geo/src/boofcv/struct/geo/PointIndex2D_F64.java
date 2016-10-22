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

package boofcv.struct.geo;

import georegression.struct.point.Point2D_F64;

/**
 * A 2D point with an index associated with it
 *
 * @author Peter Abeles
 */
public class PointIndex2D_F64 extends Point2D_F64 {
	public int index;

	public PointIndex2D_F64(double x, double y, int index) {
		super(x, y);
		this.index = index;
	}

	public PointIndex2D_F64(double x, double y) {
		super(x, y);
	}

	public PointIndex2D_F64() {
	}

	public PointIndex2D_F64( Point2D_F64 p , int index ) {
		this.x = p.x;
		this.y = p.y;
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public PointIndex2D_F64 copy() {
		return new PointIndex2D_F64(x,y,index);
	}
}
