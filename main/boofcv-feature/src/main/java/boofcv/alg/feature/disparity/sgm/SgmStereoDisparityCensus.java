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

import boofcv.abst.filter.FilterImageInterface;
import boofcv.alg.InputSanityCheck;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;

/**
 * @author Peter Abeles
 */
public class SgmStereoDisparityCensus<T extends ImageBase<T>,C extends ImageGray<C>>
	extends SgmStereoDisparity<T,C>
{
	FilterImageInterface<T, C> censusTran;
	// Storage for census transform of left and right images
	C cleft;
	C cright;

	public SgmStereoDisparityCensus(FilterImageInterface<T, C> censusTran,
									SgmDisparityCost<C> sgmCost, SgmDisparitySelector selector) {
		super(sgmCost, selector);

		this.censusTran = censusTran;
		cleft = censusTran.getOutputType().createImage(1,1);
		cright = censusTran.getOutputType().createImage(1,1);
	}

	/**
	 * Computes disparity
	 *
	 * @param left (Input) left rectified stereo image
	 * @param right (Input) right rectified stereo image
	 */
	public void process( T left , T right ) {
		InputSanityCheck.checkSameShape(left,right);

		// Apply Census Transform to input images
		censusTran.process(left,cleft);
		censusTran.process(right,cright);

		disparity.reshape(left);
		helper.configure(left.width,disparityMin,disparityRange);

		// Compute the cost using mutual information
		sgmCost.process(cleft,cright, disparityMin, disparityRange,costYXD);
		// Aggregate the cost along all the paths
		aggregation.process(costYXD,disparityMin);

		// Select the best disparity for each pixel given the cost
		selector.setMinDisparity(disparityMin); // TODO move to function below
		selector.select(costYXD,aggregation.getAggregated(),disparity);
	}
}
