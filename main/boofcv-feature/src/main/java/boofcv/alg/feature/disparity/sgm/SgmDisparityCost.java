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

import boofcv.struct.image.GrayU16;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.Planar;

/**
 * <p>Computes a stack of matching costs for all pixels across all possible disparities for use
 * with {@link SgmCostAggregation}. Pay close attention to the element ordering in the output. That was
 * selected to reduce cache misses when aggregating the costs.</p>
 *
 * <p>The output is really a 3D tensor, but to avoid creating another custom data type planar image is used.
 * The other reason to use a planar image is that it was desirable to have multiple arrays define the tensor.</p>
 *
 * @author Peter Abeles
 */
public interface SgmDisparityCost<T extends ImageBase<T>> {

	/**
	 * Maximum allowed cost fo a disparity 11-bits as suggested in the paper
	 */
	int MAX_COST = 2048-1;

	/**
	 * Computes the score for all possible disparity values across all pixels. If a disparity value would
	 * go outside of the image then the cost is set to {@link #MAX_COST}
	 *
	 * @param left left image
	 * @param right right image
	 * @param minDisparity Minimum possible disparity, inclusive
	 * @param maxDisparity Maximum possible disparity, inclusive
	 * @param costYXD Cost of output scaled to have a range of 0 to {@link SgmDisparityCost#MAX_COST}, inclusive.
	 *                Reshaped to match input and disparity range.
	 */
	void process(T left , T right ,
				 int minDisparity , int maxDisparity,
				 Planar<GrayU16> costYXD );
}
