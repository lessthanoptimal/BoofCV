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

package boofcv.alg.disparity.sgm;

import boofcv.alg.InputSanityCheck;
import boofcv.struct.image.ImageBase;

/**
 * Base class for SGM score functions that compute the score directly from the input images.
 *
 * @author Peter Abeles
 */
public class SgmStereoDisparityError<T extends ImageBase<T>>
		extends SgmStereoDisparity<T, T> {
	public SgmStereoDisparityError( SgmDisparityCost<T> sgmCost, SgmDisparitySelector selector ) {
		super(sgmCost, selector);
	}

	/**
	 * Computes disparity
	 *
	 * @param left (Input) left rectified stereo image
	 * @param right (Input) right rectified stereo image
	 */
	@Override
	public void process( T left, T right ) {
		InputSanityCheck.checkSameShape(left, right);
		disparity.reshape(left);
		helper.configure(left.width, disparityMin, disparityRange);
		sgmCost.configure(disparityMin, disparityRange);
		aggregation.configure(disparityMin);

		// Compute the cost using mutual information
		sgmCost.process(left, right, costYXD);
		// Aggregate the cost along all the paths
		aggregation.process(costYXD);

		// Select the best disparity for each pixel given the cost
		selector.setDisparityMin(disparityMin);
		selector.select(costYXD, aggregation.getAggregated(), disparity);
	}
}
