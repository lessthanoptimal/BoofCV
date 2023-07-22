/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.geo.triangulate;

import boofcv.abst.geo.GeneralCheckTriangulateRefineMetric;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DMatrixRMaj;

import java.util.List;

public class TestTriangulateRefineEpipolarLS extends GeneralCheckTriangulateRefineMetric {

	TriangulateRefineEpipolarLS alg = new TriangulateRefineEpipolarLS(1e-8, 200);

	@Override
	public void triangulate( List<Point2D_F64> obsPts, List<Se3_F64> motion,
							 List<DMatrixRMaj> essential,
							 Point3D_F64 initial, Point3D_F64 found ) {
		alg.process(obsPts, essential, initial, found);
	}
}
