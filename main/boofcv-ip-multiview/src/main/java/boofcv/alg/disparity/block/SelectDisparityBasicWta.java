/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.disparity.block;

import boofcv.struct.image.ImageGray;

/**
 * <p>
 * Selects the optimal disparity given a set of scores using a Winner Take All (WTA) strategy
 * without any validation. In other words, it simply selects the region with the smallest
 * error as the disparity. Tends to be significantly faster than when validation is employed
 * but produces many more poor results.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class SelectDisparityBasicWta<Array , Disparity extends ImageGray>
		implements DisparitySelect<Array,Disparity>
{
	// Output disparity image
	protected Disparity imageDisparity;
	// The minimum and maximum disparity it will search
	protected int disparityMin;
	protected int disparityMax;
	protected int disparityRange;
	// Radius and width of the comparison region
	protected int radiusX;
	protected int regionWidth;

	// How wide the image is
	protected int imageWidth;

	@Override
	public void configure(Disparity imageDisparity, int disparityMin , int disparityMax , int radiusX ) {
		this.imageDisparity = imageDisparity;
		this.disparityMin = disparityMin;
		this.disparityMax = disparityMax;
		this.radiusX = radiusX;

		disparityRange = disparityMax-disparityMin+1;
		regionWidth = radiusX*2+1;
		imageWidth = imageDisparity.width;
	}

	/**
	 * Returns the maximum allowed disparity for a particular column in left to right direction,
	 * as limited by the image border.
	 */
	protected int disparityMaxAtColumnL2R( int col) {
		return Math.min(col,disparityMax);
	}
}
