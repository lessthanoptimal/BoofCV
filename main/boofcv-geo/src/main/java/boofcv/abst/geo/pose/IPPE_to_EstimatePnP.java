/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.geo.pose;

import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.abst.geo.Estimate1ofPnP;
import boofcv.alg.geo.pose.PnPInfinitesimalPlanePoseEstimation;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.Point2D3D;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.DogArray;

import java.util.List;

/**
 * Wrapper around {@link PnPInfinitesimalPlanePoseEstimation} for {@link Estimate1ofPnP}.
 *
 * @author Peter Abeles
 */
public class IPPE_to_EstimatePnP implements Estimate1ofPnP {

	PnPInfinitesimalPlanePoseEstimation alg;

	DogArray<AssociatedPair> pairs = new DogArray<>(AssociatedPair::new);

	public IPPE_to_EstimatePnP( Estimate1ofEpipolar homography ) {
		alg = new PnPInfinitesimalPlanePoseEstimation(homography);
	}

	@Override
	public boolean process( List<Point2D3D> points, Se3_F64 estimatedModel ) {

		pairs.resize(points.size());

		for (int i = 0; i < pairs.size; i++) {
			AssociatedPair pair = pairs.get(i);
			Point2D3D p = points.get(i);

			if (p.location.z != 0) {
				throw new IllegalArgumentException("All points must lie on the x-y plane. If data is planar rotate it first");
			}

			pair.p1.setTo(p.location.x, p.location.y);
			pair.p2.setTo(p.observation);
		}

		if (!alg.process(pairs.toList()))
			return false;

		estimatedModel.setTo(alg.getWorldToCamera0());

		return true;
	}

	@Override
	public int getMinimumPoints() {
		return alg.getMinimumPoints();
	}
}
