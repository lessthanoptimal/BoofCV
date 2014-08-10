/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial;

import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Quadrilateral_F64;

/**
 * Contains the ID and pose for a fiducial
 *
 * @author Peter Abeles
 */
public class FoundFiducial {
	/**
	 * ID number of the fiducial
	 */
	public int index;
	/**
	 * Transform from the fiducial to the sensor reference frame
	 */
	public Se3_F64 targetToSensor = new Se3_F64();

	/**
	 * Where the fiducial was found in the undistorted image
	 */
	public Quadrilateral_F64 location = new Quadrilateral_F64();
}

