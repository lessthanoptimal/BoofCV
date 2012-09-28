/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

/**
 * Observed point feature location on the image plane and its 3D position in world coordinates.
 *
 * @author Peter Abeles
 */
public class PointPositionPair {
	/**
	 * Observed location of the feature on the image plane
	 */
	public Point2D_F64 observed;
	/**
	 * 3D location of the feature in world coordinates
	 */
	public Point3D_F64 location;

	public PointPositionPair() {
		observed = new Point2D_F64();
		location = new Point3D_F64();
	}

	public PointPositionPair(Point2D_F64 observed, Point3D_F64 location) {
		this.observed = observed;
		this.location = location;
	}

	public Point2D_F64 getObserved() {
		return observed;
	}

	public void setObserved(Point2D_F64 observed) {
		this.observed = observed;
	}

	public Point3D_F64 getLocation() {
		return location;
	}

	public void setLocation(Point3D_F64 location) {
		this.location = location;
	}
}
