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

package boofcv.struct.geo;

import georegression.struct.point.Point4D_F64;

/**
 * A 4D point with an index associated with it
 *
 * @author Peter Abeles
 */
public class PointIndex4D_F64 extends PointIndex<PointIndex4D_F64, Point4D_F64> {

	public PointIndex4D_F64( double x, double y, double z, double w, int index ) {
		this();
		setTo(x, y, z, w, index);
	}

	public PointIndex4D_F64( double x, double y, double z, double w ) {
		this();
		setTo(x, y, z, w, 0);
	}

	public PointIndex4D_F64() {super(new Point4D_F64());}

	public PointIndex4D_F64( Point4D_F64 p, int index ) {
		this();
		setTo(p, index);
	}

	public void setTo( double x, double y, double z, double w, int index ) {
		this.p.setTo(x, y, z, w);
		this.index = index;
	}

	@Override
	public PointIndex4D_F64 copy() {
		return new PointIndex4D_F64(p, index);
	}
}
