/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.geo.d2;

import boofcv.alg.geo.AssociatedPair;
import boofcv.numerics.fitting.modelset.DistanceFromModel;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se2_F64;
import georegression.transform.se.SePointOps_F64;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class DistanceSe2Sq implements DistanceFromModel<Se2_F64,AssociatedPair> {

	Se2_F64 model;
	Point2D_F64 expected = new Point2D_F64();

	@Override
	public void setModel(Se2_F64 model ) {
		this.model = model;
	}

	@Override
	public double computeDistance(AssociatedPair pt) {
		SePointOps_F64.transform(model,pt.keyLoc, expected);

		return expected.distance2(pt.currLoc);
	}

	@Override
	public void computeDistance(List<AssociatedPair> points, double[] distance) {
		for( int i = 0; i < points.size(); i++ ) {
			AssociatedPair p = points.get(i);
			SePointOps_F64.transform(model,p.keyLoc, expected);

			distance[i] = expected.distance2(p.currLoc);
		}
	}
}
