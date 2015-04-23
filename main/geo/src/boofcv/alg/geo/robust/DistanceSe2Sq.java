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
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se2_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.fitting.modelset.DistanceFromModel;

import java.util.List;

/**
 * Computes the Euclidean error squared between the predicted and actual location of 2D features after applying
 * a {@link Se2_F64} transform.
 *
 * @author Peter Abeles
 */
public class DistanceSe2Sq implements DistanceFromModel<Se2_F64,AssociatedPair> {

	// motion from key frame to current frame in plane 2D reference frame
	Se2_F64 keyToCurr;

	// location of point on plane in current ref frame in 2D
	Point2D_F64 curr2D = new Point2D_F64();

	@Override
	public void setModel(Se2_F64 keyToCurr) {
		this.keyToCurr = keyToCurr;
	}

	@Override
	public double computeDistance(AssociatedPair sample ) {

		// apply transform from key frame to current frame
		SePointOps_F64.transform(keyToCurr, sample.p1, curr2D);

		// Euclidean pixel error squared error
		double dx = curr2D.x - sample.p2.x;
		double dy = curr2D.y - sample.p2.y;

		return dx*dx + dy*dy;
	}

	@Override
	public void computeDistance(List<AssociatedPair> samples, double[] distance) {
		for( int i = 0; i < samples.size(); i++ ) {
			distance[i] = computeDistance(samples.get(i));
		}
	}
}
