/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.geo.RefineTriangulateProjective;
import boofcv.abst.geo.TriangulateNViewsProjective;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import org.ejml.data.DMatrixRMaj;

import java.util.List;

/**
 * Estimates the triangulated point then refines it
 *
 * @author Peter Abeles
 */
public class TriangulateThenRefineProjective implements TriangulateNViewsProjective {

	TriangulateNViewsProjective estimator;
	RefineTriangulateProjective refiner;

	public TriangulateThenRefineProjective(TriangulateNViewsProjective estimator,
										   RefineTriangulateProjective refiner)
	{
		this.estimator = estimator;
		this.refiner = refiner;
	}

	@Override
	public boolean triangulate(List<Point2D_F64> observations,
							   List<DMatrixRMaj> worldToView,
							   Point4D_F64 location) {

		if( !estimator.triangulate(observations,worldToView,location))
			return false;

		return refiner.process(observations,worldToView,location,location);
	}

	public TriangulateNViewsProjective getEstimator() {
		return estimator;
	}

	public RefineTriangulateProjective getRefiner() {
		return refiner;
	}
}
