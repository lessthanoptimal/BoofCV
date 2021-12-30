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

package boofcv.alg.geo.robust;

import boofcv.abst.geo.Triangulate2ViewsMetricH;
import boofcv.alg.geo.PositiveDepthConstraintCheckH;
import boofcv.factory.geo.ConfigTriangulation;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.geo.AssociatedPair;
import georegression.struct.se.Se3_F64;

import java.util.List;

/**
 * Given a set of observations in normalized image coordinates and a set of possible
 * stereo transforms select the best view. Best in this case means meeting the positive depth constraint in
 * both cameras
 *
 * @author Peter Abeles
 */
public class SelectBestStereoTransformH {

	// used to select best hypothesis
	PositiveDepthConstraintCheckH depthCheck;

	/**
	 * Specifies how the essential matrix is computed
	 */
	public SelectBestStereoTransformH( Triangulate2ViewsMetricH triangulate ) {
		this.depthCheck = new PositiveDepthConstraintCheckH(triangulate);
	}

	public SelectBestStereoTransformH() {
		this(FactoryMultiView.triangulate2ViewMetricH(new ConfigTriangulation(ConfigTriangulation.Type.GEOMETRIC)));
	}

	/**
	 * Selects the transform which describes a view where observations appear in front of the camera the most
	 *
	 * @param candidatesAtoB List of possible transforms
	 * @param observations observations in both stereo cameras in normalized image coordinates
	 * @param model (Output) the selected transform from a to b
	 */
	public void select( List<Se3_F64> candidatesAtoB,
						List<AssociatedPair> observations,
						Se3_F64 model ) {

		// use positive depth constraint to select the best one
		Se3_F64 bestModel = null;
		int bestCount = -1;
		for (int i = 0; i < candidatesAtoB.size(); i++) {
			Se3_F64 s = candidatesAtoB.get(i);
			int count = 0;
			for (int pairIdx = 0; pairIdx < observations.size(); pairIdx++) {
				AssociatedPair p = observations.get(pairIdx);
				if (depthCheck.checkConstraint(p.p1, p.p2, s)) {
					count++;
				}
			}

			if (count > bestCount) {
				bestCount = count;
				bestModel = s;
			}
		}

		if (bestModel == null)
			throw new RuntimeException("BUG");

		model.setTo(bestModel);
	}
}
