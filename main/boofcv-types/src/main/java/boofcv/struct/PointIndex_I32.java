/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import georegression.struct.point.Point2D_I32;
import lombok.Getter;
import lombok.Setter;

/**
 * Combination of a point and an index in an array
 *
 * @author Peter Abeles
 */
public class PointIndex_I32 extends Point2D_I32 {
	/**
	 * Index of point in an array/list
	 */
	@Getter @Setter public int index;

	public PointIndex_I32() {}

	public PointIndex_I32( int x, int y, int index ) {
		super(x, y);
		this.index = index;
	}

	public void setTo( Point2D_I32 p, int index ) {
		this.setTo(p);
		this.index = index;
	}

	public void setTo( int x, int y, int index ) {
		this.setTo(x, y);
		this.index = index;
	}

	@Override
	public PointIndex_I32 copy() {
		return new PointIndex_I32(x, y, index);
	}
}
