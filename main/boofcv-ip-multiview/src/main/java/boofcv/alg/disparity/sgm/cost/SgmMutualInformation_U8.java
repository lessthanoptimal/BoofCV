/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.disparity.sgm.cost;

import boofcv.struct.image.GrayU8;

/**
 * Computes the cost using Mutual Information as described in [1].
 *
 * <p>[1] Hirschmuller, Heiko. "Stereo processing by semiglobal matching and mutual information."
 * IEEE Transactions on pattern analysis and machine intelligence 30.2 (2007): 328-341.</p>
 *
 * @author Peter Abeles
 * @see StereoMutualInformation
 */
public class SgmMutualInformation_U8 extends SgmCostBase<GrayU8> {
	StereoMutualInformation mutual;

	public SgmMutualInformation_U8( StereoMutualInformation mutual ) {
		this.mutual = mutual;
	}

	@Override
	protected void computeDisparityErrors( int idxLeft, int idxRight, int idxOut, int disparityRange ) {
		int valLeft = left.data[idxLeft] & 0xFF;
		for (int d = 0; d < disparityRange; d++) {
			int valRight = right.data[idxRight--] & 0xFF;
			costXD.data[idxOut++] = (short)mutual.costScaled(valLeft, valRight);
		}
	}

	public StereoMutualInformation getMutual() {
		return mutual;
	}
}
