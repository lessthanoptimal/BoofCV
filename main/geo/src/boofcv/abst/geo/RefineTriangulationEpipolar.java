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
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * Refines the location of a triangulated point using epipolar constraint matrix (Fundamental or Essential).
 * Observations are in pixels if Fundamental matrix is provided, normalized if Essential matrix is provided.
 *
 * @author Peter Abeles
 */
public interface RefineTriangulationEpipolar {

	/**
	 * Refines the triangulated point.
	 *
	 * NOTE: the epipolar matrix is configured such that x2<sup>T</sup>*F*x1 = 0, where x1 is the
	 * observation in the world frame and x2 in the camera frame.
	 *
	 * @param observations Observations of feature in N views. Pixel or Normalized image coordinates.
	 * @param fundamentalWorldToCam Fundamental or essential matrix for each view.  World to Camera.
	 * @param worldPt Initial estimate of point in world coordinates.
	 * @param refinedPt The refined estimated point position.
	 * @return if successful or not
	 */
	public boolean process(List<Point2D_F64> observations,
						   List<DenseMatrix64F> fundamentalWorldToCam ,
						   Point3D_F64 worldPt, Point3D_F64 refinedPt);
}
