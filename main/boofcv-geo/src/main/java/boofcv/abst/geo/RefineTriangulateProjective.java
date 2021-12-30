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
 * Refines the location of a triangulated point using non-linear optimization. Projective geometry
 * is assumed.
 *
 * @author Peter Abeles
 */
public interface RefineTriangulateProjective {

	/**
	 * Refines the triangulated point.
	 *
	 * @param observations Observations of feature in N views. Pixel coordinates.
	 * @param cameraMatrices Camera matrices for each view
	 * @param worldPt Initial estimate of point in world coordinates.
	 * @param refinedPt The refined estimated point position.
	 * @return if successful or not
	 */
	boolean process( List<Point2D_F64> observations,
					 List<DMatrixRMaj> cameraMatrices,
					 Point4D_F64 worldPt, Point4D_F64 refinedPt );
}
