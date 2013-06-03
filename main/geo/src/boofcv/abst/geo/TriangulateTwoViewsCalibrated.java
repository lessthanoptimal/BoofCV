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

/**
 * Triangulate the location of a point from two views of a feature given a calibrated
 * camera and known camera motion.
 *
 * @author Peter Abeles
 */
public interface TriangulateTwoViewsCalibrated {

	/**
	 * Triangulate the points location.
	 *
	 * @param obsA View from position A in normalized image coordinates.
	 * @param obsB View from position B in normalized image coordinates.
	 * @param fromAtoB Transform from camera location A to location B
	 * @param foundInA The found triangulated 3D point in A's reference frame.
	 * @return true if successful, false otherwise.
	 */
	public boolean triangulate( Point2D_F64 obsA , Point2D_F64 obsB ,
								Se3_F64 fromAtoB, Point3D_F64 foundInA );
}
