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

package boofcv.alg.feature.disparity.impl;

import boofcv.alg.feature.disparity.DisparitySparseSelect;

/**
 * Selects the optimal disparity with the smallest error and applies maxError and texture based
 * checks on the found solution.  See {@link ImplSelectRectStandard_S32_U8} for more details on
 * checks.
 *
 * @author Peter Abeles
 */
public class SelectSparseStandardWta_S32 implements DisparitySparseSelect<int[]> {

	// found disparity
	int disparity;

	// maximum allowed error
	int maxError;

	// texture threshold, use an integer value for speed.
	int textureThreshold;
	int discretizer = 10000;

	public SelectSparseStandardWta_S32(int maxError , double texture ) {
		this.maxError = maxError <= 0 ? Integer.MAX_VALUE : maxError;
		this.textureThreshold = (int)(discretizer *texture);
	}

	@Override
	public boolean select(int[] scores, int maxDisparity) {
		disparity = 0;
		int best = scores[0];

		for( int i = 1; i < maxDisparity; i++ ) {
			if( scores[i] < best ) {
				best = scores[i];
				disparity = i;
			}
		}

		if( best > maxError ) {
			return false;
		} else if( textureThreshold > 0 ) {
			// find the second best disparity value and exclude its neighbors
			scores[best] = Integer.MAX_VALUE;
			if( best > 0 )
				scores[best-1] = Integer.MAX_VALUE;
			if( best < maxDisparity - 1)
				scores[best+1] = Integer.MAX_VALUE;

			int secondBest = Integer.MAX_VALUE;
			for( int i = 0; i < maxDisparity; i++ ) {
				if( scores[i] < secondBest ) {
					secondBest = scores[i];
				}
			}

			// similar scores indicate lack of texture
			// C = (C2-C1)/C1
			if( discretizer *(secondBest-best) <= textureThreshold *best )
				return false;
		}

		return true;
	}

	@Override
	public double getDisparity() {
		return disparity;
	}
}
