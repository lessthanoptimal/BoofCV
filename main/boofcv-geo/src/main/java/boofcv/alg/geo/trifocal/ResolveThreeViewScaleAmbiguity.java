/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.trifocal;

import boofcv.abst.geo.Triangulate2ViewsMetricH;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.geo.AssociatedTriple;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.sorting.QuickSelect;
import org.ddogleg.struct.DogArray_F64;

import java.util.List;

/**
 * Give a three views with the pose known up to a scale ambiguity for the three views, resolve the scale ambiguity.
 * View-1 is assumed to be the origin. The scale of both views is adjusted so that the one with the largest norm
 * is set to one.
 *
 * @author Peter Abeles
 */
public class ResolveThreeViewScaleAmbiguity {

	// Used to triangular the same point in both views
	protected Triangulate2ViewsMetricH triangulate;

	// Workspace
	protected DogArray_F64 scales = new DogArray_F64();
	protected Point4D_F64 X2 = new Point4D_F64();
	protected Point4D_F64 X3 = new Point4D_F64();

	public ResolveThreeViewScaleAmbiguity( Triangulate2ViewsMetricH triangulate ) {
		this.triangulate = triangulate;
	}

	public ResolveThreeViewScaleAmbiguity() {
		this(FactoryMultiView.triangulate2ViewMetricH(null));
	}

	/**
	 * Solves for scale ambiguity give the transforms to view-2 and view-3
	 *
	 * @param triple (Input) Observations in normalized image coordinates for the 3-views.
	 * @param world_to_view2 (Input/Output) transform from world to view-2
	 * @param world_to_view3 (Input/Output) transform from world to view-3
	 */
	public boolean process( List<AssociatedTriple> triple, Se3_F64 world_to_view2, Se3_F64 world_to_view3 ) {
		scales.resize(triple.size());
		scales.reset();
		for (int i = 0; i < triple.size(); i++) {
			AssociatedTriple a = triple.get(i);
			if (!triangulate.triangulate(a.p1, a.p2, world_to_view2, X2))
				continue;
			// see if it's at infinity and has no depth
			if (X2.w == 0.0)
				continue;

			if (!triangulate.triangulate(a.p1, a.p3, world_to_view3, X3))
				continue;
			// see if it's at infinity and has no depth
			if (X3.w == 0.0)
				continue;

			double scale = (X2.z*X3.w)/(X2.w*X3.z);
			scales.add(scale);
		}

		if (scales.size == 0)
			return false;

		// Avoid noise by selecting the median
		double scale3_to_2 = QuickSelect.select(scales.data, scales.size/2, scales.size);

		if (scale3_to_2 == 0.0)
			return false;

		// Put them into the same scale
		world_to_view3.T.scale(scale3_to_2);

		// Adjust scale so that norm of one is the largest
		double normA = world_to_view2.T.norm();
		double normB = world_to_view3.T.norm();

		double norm = Math.max(normA, normB);
		world_to_view2.T.divide(norm);
		world_to_view3.T.divide(norm);

		return true;
	}
}
