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

package boofcv.abst.geo.triangulate;

import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.alg.geo.triangulate.TriangulateGeometric;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

/**
 * Wrapper around {@link TriangulateGeometric} for {@link boofcv.abst.geo.TriangulateTwoViewsCalibrated}.
 * 
 * @author Peter Abeles
 */
public class WrapGeometricTriangulation implements TriangulateTwoViewsCalibrated {

	TriangulateGeometric alg = new TriangulateGeometric();

	@Override
	public boolean triangulate(Point2D_F64 obsA, Point2D_F64 obsB,
							   Se3_F64 fromAtoB, Point3D_F64 foundInA) {

		alg.triangulate(obsA,obsB, fromAtoB, foundInA);
		return true;
	}
}
