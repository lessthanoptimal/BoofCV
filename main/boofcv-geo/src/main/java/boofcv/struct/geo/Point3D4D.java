/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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
import georegression.struct.point.Point4D_F64;
import lombok.Getter;
import lombok.Setter;

/**
 * Observed composed of a pointing direction and homogenous coordinate in 3D space.
 *
 * @author Peter Abeles
 */
public class Point3D4D {
	/**
	 * Observed image feature as a pointing vector
	 */
	public @Getter @Setter Point3D_F64 observation;
	/**
	 * Homogenous 3D location of the feature in world coordinates
	 */
	public @Getter @Setter Point4D_F64 location;

	public Point3D4D() {
		observation = new Point3D_F64();
		location = new Point4D_F64();
	}

	public Point3D4D( Point3D_F64 observation, Point4D_F64 location ) {
		this.observation = observation;
		this.location = location;
	}

	/**
	 * Sets 'this' to be identical to 'src'.
	 */
	public Point3D4D setTo( Point3D4D src ) {
		this.observation.setTo(src.observation);
		this.location.setTo(src.location);
		return this;
	}

	public Point3D4D setTo( Point3D_F64 observation, Point4D_F64 location ) {
		this.observation.setTo(observation);
		this.location.setTo(location);
		return this;
	}


	public Point3D4D setTo( double x2, double y2, double z2, double x3, double y3, double z3, double w4 ) {
		this.observation.setTo(x2, y2, z2);
		this.location.setTo(x3, y3, z3, w4);
		return this;
	}

	public void zero() {
		observation.zero();
		location.zero();
	}

	public Point3D4D copy() {
		return new Point3D4D(observation.copy(), location.copy());
	}
}
