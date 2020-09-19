/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.geo.Triangulate2ViewsProjective;
import boofcv.alg.geo.GeometricResult;
import boofcv.alg.geo.triangulate.TriangulateMetricLinearDLT;
import boofcv.alg.geo.triangulate.TriangulateProjectiveLinearDLT;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import org.ejml.data.DMatrixRMaj;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link TriangulateMetricLinearDLT} for {@link Triangulate2ViewsProjective}.
 *
 * @author Peter Abeles
 */
public class Wrap2ViewsTriangulateProjectiveDLT implements Triangulate2ViewsProjective {

	final TriangulateProjectiveLinearDLT alg = new TriangulateProjectiveLinearDLT();

	// pixel observations
	final List<Point2D_F64> pixels = new ArrayList<>();
	// camera matrices
	final List<DMatrixRMaj> cameras = new ArrayList<>();

	@Override
	public boolean triangulate( Point2D_F64 obsA, Point2D_F64 obsB, DMatrixRMaj projectionA, DMatrixRMaj projectionB, Point4D_F64 foundInA ) {

		pixels.clear();
		cameras.clear();

		pixels.add(obsA);
		pixels.add(obsB);
		cameras.add(projectionA);
		cameras.add(projectionB);

		return GeometricResult.SUCCESS == alg.triangulate(pixels, cameras, foundInA);
	}
}
