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

package boofcv.alg.geo.selfcalib;

import boofcv.struct.geo.AssociatedTriple;

import java.util.List;

/**
 * Performs self calibration on the set of provided pixel observations to a metric scene with the following assumptions:
 * <ol>
 *     <li>The scene is composed of observations on a single plane</li>
 *     <li>Principle point is zero (0,0)</li>
 *     <li>Image pixels have been normalized for numerical stability</li>
 *     <li>All observations are assumed to be true matches, but with some noise</li>
 * </ol>
 *
 * @author Peter Abeles
 */
class SelfCalibrationPlanarThreeViews {

	public boolean process( List<AssociatedTriple> observations ) {
		// homography view-1 to view-2
		// homography view-1 to view-3
		// Guess intrinsics for each camera
		// decompose homographies
		// resolve scale ambiguity
		// refine with bundle adjustment
		return true;
	}

	public int getMinimumPoints() {
		return 6;
	}
}