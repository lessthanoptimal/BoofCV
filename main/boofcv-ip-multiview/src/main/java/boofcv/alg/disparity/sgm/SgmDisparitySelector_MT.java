/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.disparity.sgm;

import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;

/// Concurrent version of [SgmDisparitySelector]
public class SgmDisparitySelector_MT extends SgmDisparitySelector {
	@Override public void select( Planar<GrayU16> costYXD, Planar<GrayU16> aggregatedYXD, GrayU8 disparity ) {
		setup(aggregatedYXD);
		disparity.reshape(lengthX, lengthY);

		BoofConcurrency.loopFor(0, lengthY, 1, ( y ) -> {
			GrayU16 aggregatedXD = aggregatedYXD.getBand(y);

			// columns before xOffset have nothing stored in the cost tensor that they can compare against
			for (int x = 0; x < xOffset; x++) {
				disparity.unsafe_set(x, y, invalidDisparity);
			}
			for (int x = xOffset; x < lengthX; x++) {
				disparity.unsafe_set(x, y, findBestDisparity(x, aggregatedXD));
			}
		});
	}
}
