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

import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import lombok.Getter;
import lombok.Setter;

/**
 * Observed point feature location on the image plane and its 3D position.
 *
 * @author Peter Abeles
 */
public class Point2D3D {
	/**
	 * Observed location of the feature on the image plane
	 */
	public @Getter @Setter Point2D_F64 observation;
	/**
	 * 3D location of the feature in world coordinates
	 */
	public @Getter @Setter Point3D_F64 location;

	public Point2D3D() {
		observation = new Point2D_F64();
		location = new Point3D_F64();
	}

	public Point2D3D( Point2D_F64 observation, Point3D_F64 location ) {
		this.observation = observation;
		this.location = location;
	}

	/**
	 * Sets 'this' to be identical to 'src'.
	 */
	public void setTo( Point2D3D src ) {
		observation.setTo(src.observation);
		location.setTo(src.location);
	}

	public void setTo( double x2, double y2, double x3, double y3, double z3 ) {
		observation.setTo(x2, y2);
		location.setTo(x3, y3, z3);
	}

	public Point2D3D copy() {
		return new Point2D3D(observation.copy(), location.copy());
	}
}
