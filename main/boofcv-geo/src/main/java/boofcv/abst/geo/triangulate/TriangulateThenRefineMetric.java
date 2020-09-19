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

import boofcv.abst.geo.RefineTriangulateMetric;
import boofcv.abst.geo.TriangulateNViewsMetric;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import lombok.Getter;

import java.util.List;

/**
 * Estimates the triangulated point then refines it
 *
 * @author Peter Abeles
 */
public class TriangulateThenRefineMetric implements TriangulateNViewsMetric {

	final @Getter TriangulateNViewsMetric estimator;
	final @Getter RefineTriangulateMetric refiner;

	public TriangulateThenRefineMetric( TriangulateNViewsMetric estimator,
										RefineTriangulateMetric refiner ) {
		this.estimator = estimator;
		this.refiner = refiner;
	}

	@Override
	public boolean triangulate( List<Point2D_F64> observations,
								List<Se3_F64> listWorldToView,
								Point3D_F64 location ) {

		if (!estimator.triangulate(observations, listWorldToView, location))
			return false;

		return refiner.process(observations, listWorldToView, location, location);
	}
}
