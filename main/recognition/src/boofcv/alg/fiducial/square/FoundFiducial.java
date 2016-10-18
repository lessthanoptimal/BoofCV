/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.square;

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
	public long id;
	/**
	 * Transform from the fiducial to the sensor reference frame
	 */
	public Se3_F64 targetToSensor = new Se3_F64();

	/**
	 * <p>Where the fiducial was found in the input image.  pixel coordinates.  Lens distortion has not been removed.</p>
	 *
	 * <p>For the meaning of each corner see {@link BaseDetectFiducialSquare}. corner a = 0, b = 1, c = 2, d = 3</p>
	 */
	public Quadrilateral_F64 locationDist = new Quadrilateral_F64();

	/**
	 * Fiducial corners in undistorted pixel coordinates
	 */
	public Quadrilateral_F64 locationUndist = new Quadrilateral_F64();

}

