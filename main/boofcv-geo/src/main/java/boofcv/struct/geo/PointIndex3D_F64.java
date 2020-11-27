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

import georegression.struct.point.Point3D_F64;
import lombok.Getter;
import lombok.Setter;

/**
 * A 3D point with an index associated with it
 *
 * @author Peter Abeles
 */
public class PointIndex3D_F64 {
	public @Getter final Point3D_F64 p = new Point3D_F64();
	public @Getter @Setter int index;

	public PointIndex3D_F64( double x, double y, double z, int index ) { setTo(x, y, z, index); }

	public PointIndex3D_F64( double x, double y, double z ) { setTo(x, y, z, 0);}

	public PointIndex3D_F64() {}

	public PointIndex3D_F64( Point3D_F64 p, int index ) { setTo(p, index); }

	public void setTo( Point3D_F64 p, int index ) {
		this.p.setTo(p);
		this.index = index;
	}

	public void setTo( double x, double y, double z, int index ) {
		this.p.setTo(x, y, z);
		this.index = index;
	}

	public void setTo( PointIndex3D_F64 src ) {
		this.p.setTo(src.p);
		this.index = src.index;
	}

	public PointIndex3D_F64 copy() {
		return new PointIndex3D_F64(p, index);
	}
}
