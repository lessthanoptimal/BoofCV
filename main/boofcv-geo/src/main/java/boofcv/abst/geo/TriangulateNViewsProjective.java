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

import java.util.List;

/**
 * Triangulate the location of a point from N projective views of a feature from an uncalibrated camera. Observations
 * are in pixels.
 *
 * @author Peter Abeles
 */
public interface TriangulateNViewsProjective {

	/**
	 * Triangulate the points location.
	 *
	 * @param observations (Input) Observations of the 3D point in pixel coordinates from different camera views
	 * @param cameraMatrices (Input) Camera projection matrices. x = A*X
	 * @param location (Output) Homogenous coordinate of 3D feature in world coordinates.
	 * @return true if successful, false otherwise.
	 */
	boolean triangulate( List<Point2D_F64> observations, List<DMatrixRMaj> cameraMatrices, Point4D_F64 location );
}
