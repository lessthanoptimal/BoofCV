/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.geo;

import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

import java.util.List;

/**
 * Triangulate the location of a point from N views of a feature given a calibrated
 * camera and known camera motion.
 *
 * @author Peter Abeles
 */
public interface TriangulateNViewsCalibrated {

	/**
	 * Triangulate the points location.
	 *
	 * @param observations Observations of the 3D point in normalized image coordinates from different camera views..
	 * @param worldToView Transform from world to each of the different camera views
	 * @param location Computed location of feature in world coordinates.
	 * @return true if successful, false otherwise.
	 */
	public boolean triangulate( List<Point2D_F64> observations, List<Se3_F64> worldToView ,
								Point3D_F64 location );
}
