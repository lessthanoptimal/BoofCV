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

package boofcv.geo.simulation;

import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

/**
 * Computes observations from simulated data
 *
 * @author Peter Abeles
 */
public interface CameraModel {

	/**
	 * Computes the observed pixel location of the point in 3D space
	 *
	 * @param world Location of point in world coordinates
	 * @param pixel Location of the point in image pixels
	 * @return true of the point is observable
	 */
	public boolean projectPoint( Point3D_F64 world , Point2D_F64 pixel );

	public void setCameraToWorld(Se3_F64 pose);

	public Se3_F64 getCameraToWorld();
}
