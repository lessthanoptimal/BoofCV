/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.disparity.sgm.SgmDisparityCost;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;

/**
 * @author Peter Abeles
 */
class TestSgmMutualInformation_U8 extends ChecksSgmDisparityCost<GrayU8> {

	final static int maxPixelValue = 255;

	public TestSgmMutualInformation_U8() {
		super(0, maxPixelValue, ImageType.single(GrayU8.class));
	}

	@Override
	SgmDisparityCost<GrayU8> createAlg() {
		StereoMutualInformation smi = new StereoMutualInformation();
		smi.configureHistogram(maxPixelValue + 1);

		// A bit of a hack below.
		// Everything hss high cost when the pixel values are identical
		ImageMiscOps.fill(smi.scaledCost, 1000);
		for (int i = 0; i <= maxPixelValue; i++) {
			smi.scaledCost.set(i, i, 10);
		}

		return new SgmMutualInformation_U8(smi);
	}
}
