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

package boofcv.abst.geo;

import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import org.ejml.data.DMatrixRMaj;

/**
 * Triangulates the 3D location of a point given two uncalibrated observations of the point. Uncalibrated
 * observations are in pixel coordinates.
 *
 * @author Peter Abeles
 */
public interface Triangulate2ViewsProjective {

	/**
	 * Triangulate the points 3D location from pixel observations given two views.
	 *
	 * @param obsA View from position A in pixels.
	 * @param obsB View from position B in pixels
	 * @param projectionA Camera matrix for view A. x = P*X
	 * @param projectionB Camera matrix for view B. x = P*X
	 * @param foundInA The found triangulated 3D point in A's reference frame. Homogenous coordinates.
	 * @return true if successful, false otherwise.
	 */
	boolean triangulate( Point2D_F64 obsA, Point2D_F64 obsB,
						 DMatrixRMaj projectionA, DMatrixRMaj projectionB,
						 Point4D_F64 foundInA );
}
