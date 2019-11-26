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

package boofcv.factory.feature.disparity;

import boofcv.alg.feature.disparity.sgm.SgmDisparityCost;
import boofcv.alg.feature.disparity.sgm.SgmStereoDisparityHmi;
import boofcv.alg.transform.pyramid.ConfigPyramid2;
import boofcv.struct.Configuration;

import static boofcv.alg.feature.disparity.sgm.SgmDisparityCost.MAX_COST;

/**
 * Configuration for {@link SgmStereoDisparityHmi Semi Global Matching}
 *
 * @author Peter Abeles
 */
public class ConfigureDisparitySGM implements Configuration {
	/**
	 * Minimum disparity that it will check. Must be &ge; 0 and &lt; maxDisparity
	 */
	public int minDisparity=0;
	/**
	 * Number of disparity values considered. Must be &gt; 0. Maximum number is 255 for 8-bit disparity
	 * images.
	 */
	public int rangeDisparity=100;
	/**
	 * Maximum allowed error for a single pixel. Set to a value less than 0 to disable. Has a range from
	 * 0 to {@link SgmDisparityCost#MAX_COST}
	 */
	public int maxError = -1;
	/**
	 * Tolerance for how difference the left to right associated values can be.  Try 1. Disable with -1
	 */
	public int validateRtoL=1;
	/**
	 * Tolerance for how similar optimal region is to other region.  Closer to zero is more tolerant.
	 * Try 0.1 for SAD or 0.7 for NCC. Disable with a value &le; 0
	 */
	public double texture = 0.3;
	/**
	 * If subpixel should be used to find disparity or not. If on then output disparity image needs to me GrayF32.
	 * If false then GrayU8.
	 */
	public boolean subpixel = true;
	/**
	 * The penalty applied to a small change in disparity. 0 &le; x &le; {@link SgmDisparityCost#MAX_COST} and
	 * must be less than {@link #penaltyLargeChange}.
	 */
	public int penaltySmallChange = 100;
	/**
	 * The penalty applied to a large change in disparity. 0 &le; x &le; {@link SgmDisparityCost#MAX_COST}
	 */
	public int penaltyLargeChange = 1500;
	/**
	 * Number of paths it should consider. 2,4,8,16 are valid numbers of paths.
	 */
	public int paths = 8;
	/**
	 * Specifies the smallest layer in pyramid. Used to compute Mutual Information Cost
	 */
	public ConfigPyramid2 pyramidLayers = new ConfigPyramid2(-1,50,-1);
	/**
	 * Radius of Gaussian kernel when applying smoothing during Mutual Information computation.
	 */
	public int smoothingRadius = 3;

	@Override
	public void checkValidity() {
		if( penaltyLargeChange <= penaltySmallChange )
			throw new IllegalArgumentException("large penalty must be larger than small");
		if( penaltySmallChange < 0 || penaltySmallChange > MAX_COST )
			throw new IllegalArgumentException("Invalid value for penaltySmallChange.");
		if( penaltyLargeChange < 0 || penaltyLargeChange > MAX_COST )
			throw new IllegalArgumentException("Invalid value for penaltySmallChange.");
		if( minDisparity < 0 )
			throw new IllegalArgumentException("Minimum disparity must be >= 0");
		if( paths < 2 || paths > 16 )
			throw new IllegalArgumentException("Invalid number of paths. "+paths);
	}
}
