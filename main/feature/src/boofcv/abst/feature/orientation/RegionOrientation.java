/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.orientation;


/**
 * Estimates the orientation of a region which is approximately circular. This is typically
 * used to rotationally invariant scale/size dependent features.
 *
 * @author Peter Abeles
 */
public interface RegionOrientation {
	/**
	 * Specifies the circle's radius that the orientation should be
	 *
	 * @param radius Object's radius.
	 */
	void setObjectRadius( double radius );

	/**
	 * Computes the orientation of a region about its center.
	 *
	 * @param c_x Center of the region in image pixels.
	 * @param c_y Center of the region in image pixels.
	 *
	 * @return Orientation in radians.  Angle zero points along x-axis and pi/2 along y-axis.
	 */
	double compute( double c_x , double c_y );
}
