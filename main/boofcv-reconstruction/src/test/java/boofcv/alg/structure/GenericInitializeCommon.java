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

package boofcv.alg.structure;

import boofcv.struct.feature.AssociatedIndex;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray_I32;

public class GenericInitializeCommon extends BoofStandardJUnit {
	final static double reprojectionTol = 1e-4;

	void removeConnectionOutliers( PairwiseImageGraph.View seed, DogArray_I32 seedFeatsIdx ) {
		for (PairwiseImageGraph.Motion m : seed.connections.toList()) {
			// mark all indexes which are inliers
			boolean isSrc = m.src == seed;
			boolean[] inlier = new boolean[seed.totalObservations];
			for (AssociatedIndex a : m.inliers.toList()) {
				inlier[isSrc ? a.src : a.dst] = true;
			}
			// remove the outliers
			for (int i = 0; i < inlier.length; i++) {
				if (!inlier[i]) {
					int idx = seedFeatsIdx.indexOf(i);
					if (idx >= 0)
						seedFeatsIdx.remove(idx);
				}
			}
		}
	}
}
