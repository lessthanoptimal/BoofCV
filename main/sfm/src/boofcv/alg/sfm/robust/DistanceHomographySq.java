/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.robust;

import boofcv.alg.geo.AssociatedPair;
import boofcv.numerics.fitting.modelset.DistanceFromModel;
import georegression.struct.homo.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.transform.homo.HomographyPointOps;

import java.util.List;


/**
 * <p>
 * Applies an homography transformation to the associated pair and computes the euclidean distance
 * squared between their locations.  This reduces computations by avoiding the square root
 * functions, which is computationally expensive. While both this error metric and euclidean
 * distance have the same minimum, this exaggerates the magnitude of outliers.
 * The transform is applied to the "keyLoc".
 * </p>
 * 
 * @author Peter Abeles
 */
public class DistanceHomographySq implements DistanceFromModel<Homography2D_F64,AssociatedPair> {

	Homography2D_F64 model;
	Point2D_F64 expected = new Point2D_F64();

	@Override
	public void setModel(Homography2D_F64 model ) {
		this.model = model;
	}

	@Override
	public double computeDistance(AssociatedPair pt) {
		HomographyPointOps.transform(model, pt.keyLoc, expected);

		return expected.distance2(pt.currLoc);
	}

	@Override
	public void computeDistance(List<AssociatedPair> points, double[] distance) {
		for( int i = 0; i < points.size(); i++ ) {
			AssociatedPair p = points.get(i);
			HomographyPointOps.transform(model,p.keyLoc,expected);

			distance[i] = expected.distance2(p.currLoc);
		}
	}
}
