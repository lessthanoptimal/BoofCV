/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.disparity.sgm;

import boofcv.alg.InputSanityCheck;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;

/**
 * TODO document
 *
 * <p>[1] Hirschmuller, Heiko. "Stereo processing by semiglobal matching and mutual information."
 * IEEE Transactions on pattern analysis and machine intelligence 30.2 (2007): 328-341.</p>
 *
 * @author Peter Abeles
 */
public class SgmMutualInformation implements SgmDisparityCost<GrayU8> {
	StereoMutualInformation mutual;

	public SgmMutualInformation(StereoMutualInformation mutual) {
		this.mutual = mutual;
	}

	/**
	 * Must call {@link #getMutual()} and then invoke {@link StereoMutualInformation#process(GrayU8, GrayU8, int, GrayU8, int)}
	 * first to initialize the data structure, then {@link StereoMutualInformation#precomputeScaledCost(int)} before
	 * this function can be called
	 */
	@Override
	public void process(GrayU8 left, GrayU8 right, int minDisparity, int disparityRange, Planar<GrayU16> costYXD) {
		InputSanityCheck.checkSameShape(left,right);

		// Declare the "tensor" with shape (lengthY,lengthX,lengthD)
		costYXD.reshape(disparityRange,left.width,left.height);

		int maxDisparity = minDisparity+disparityRange-1;

		for (int y = 0; y < left.height; y++) {
			GrayU16 costXD = costYXD.getBand(y);

			int idxLeft  = left.startIndex  + y*left.stride;

			for (int x = 0; x < left.width; x++) {
				int idxOut = costXD.startIndex + x*costYXD.stride;

				// the maximum disparity in which the pixel will be inside the right image
				int m = Math.min(x,maxDisparity);
				int valLeft = left.data[idxLeft++] & 0xFF;

				// start reading the right image at the smallest disparity then increase disparity size
				int idxRight = right.startIndex + y*right.stride + x - minDisparity;

				for (int d = minDisparity; d <= m; d++) {
					int valRight = right.data[idxRight--] & 0xFF;
					costXD.data[idxOut++] = (short)mutual.costScaled(valLeft,valRight);
				}

				// Fill in the disparity values outside the image with max cost
				for (int d =m+1; d <= maxDisparity; d++) {
					costXD.data[idxOut++] = SgmDisparityCost.MAX_COST;
				}
			}
		}
	}

	public StereoMutualInformation getMutual() {
		return mutual;
	}

}
