/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.robust;

import boofcv.struct.geo.AssociatedPair;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.transform.homography.HomographyPointOps_F64;
import org.ddogleg.fitting.modelset.DistanceFromModel;

import java.util.List;


/**
 * <p>
 * Computes the Euclidean error squared between 'p1' and 'p2' after projecting 'p1' into image 2.  Input
 * can be in pixels or normalized image coordinates, but the error for normalized image coordinates doesn't
 * have a physical meaning.
 * </p>
 *
 * <p>
 * error = (p2'.x - p2.x)<sup>2</sup>  + (p2'.y - p2.y)<sup>2</sup>, where p2' is the predicted location and p2 is
 * the observed location.
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
		HomographyPointOps_F64.transform(model, pt.p1, expected);

		return expected.distance2(pt.p2);
	}

	@Override
	public void computeDistance(List<AssociatedPair> points, double[] distance) {
		for( int i = 0; i < points.size(); i++ ) {
			AssociatedPair p = points.get(i);
			HomographyPointOps_F64.transform(model, p.p1, expected);

			distance[i] = expected.distance2(p.p2);
		}
	}
}
