/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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
import lombok.Getter;
import lombok.Setter;

/**
 * A 2D point with an index associated with it
 *
 * @author Peter Abeles
 */
public class PointIndex2D_F64 {
	public @Getter final Point2D_F64 p = new Point2D_F64();
	public @Getter @Setter int index;

	public PointIndex2D_F64( double x, double y, int index ) { setTo(x, y, index); }

	public PointIndex2D_F64( double x, double y ) { setTo(x, y, 0);}

	public PointIndex2D_F64() {}

	public PointIndex2D_F64( Point2D_F64 p, int index ) { setTo(p, index); }

	public void setTo( Point2D_F64 p, int index ) {
		this.p.setTo(p);
		this.index = index;
	}

	public void setTo( double x, double y, int index ) {
		this.p.setTo(x, y);
		this.index = index;
	}

	public void setTo( PointIndex2D_F64 src ) {
		this.p.setTo(src.p);
		this.index = src.index;
	}

	public PointIndex2D_F64 copy() {
		return new PointIndex2D_F64(p, index);
	}
}
