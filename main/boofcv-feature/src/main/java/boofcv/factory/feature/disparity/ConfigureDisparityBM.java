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

import boofcv.alg.feature.disparity.DisparityBlockMatch;
import boofcv.factory.transform.census.CensusType;
import boofcv.struct.Configuration;

/**
 * Configuration for the basic block matching stereo algorithm that employs a greedy winner takes all strategy.
 *
 * @see DisparityBlockMatch
 *
 * @author Peter Abeles
 */
public class ConfigureDisparityBM implements Configuration {
	/**
	 * Minimum disparity that it will check. Must be &ge; 0 and &lt; maxDisparity
	 */
	public int minDisparity=0;
	/**
	 * Number of disparity values considered. Must be &gt; 0
	 */
	public int rangeDisparity=100;
	/**
	 * Radius of the rectangular region along x-axis.
	 */
	public int regionRadiusX=3;
	/**
	 * Radius of the rectangular region along y-axis.
	 */
	public int regionRadiusY=3;
	/**
	 * Maximum allowed error in a region per pixel.  Only used by "error" based measures, e.g. NCC does not
	 * use this value. Set to &lt; 0 to disable.
	 */
	public double maxPerPixelError=0;
	/**
	 * Tolerance for how difference the left to right associated values can be.  Try 1. Disable with -1
	 */
	public int validateRtoL=1;
	/**
	 * Tolerance for how similar optimal region is to other region.  Closer to zero is more tolerant.
	 * Try 0.1 for SAD or 0.7 for NCC. Disable with a value &le; 0
	 */
	public double texture = 0.1;

	/**
	 * If subpixel should be used to find disparity or not. If on then output disparity image needs to me GrayF32.
	 * If false then GrayU8.
	 */
	public boolean subpixel = true;

	/**
	 * How the error is computed for each block
	 */
	public DisparityError errorType = DisparityError.SAD;

	/**
	 * If Census error is used which variant should it use
	 */
	public CensusType censusVariant = CensusType.BLOCK_5_5;

	/**
	 * Used to avoid a divide by zero error when dividing by the standard deviation. Only used with NCC. Smaller
	 * values are more mathematically accurate but make it more sensitive to floating point error.
	 * This has been tuned to work with pixel values that have been scaled to -1 to 1.
	 */
	public double nccEps = 5e-5;

	@Override
	public void checkValidity() {
		if( minDisparity < 0 )
			throw new IllegalArgumentException("miDisparity < 0");
		if( rangeDisparity < 1 )
			throw new IllegalArgumentException("rangeDisparity < 1");

	}
}
