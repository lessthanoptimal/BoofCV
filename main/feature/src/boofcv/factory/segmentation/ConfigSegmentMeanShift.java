/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.segmentation;

import boofcv.struct.ConnectRule;

/**
 * Configuration for {@link boofcv.alg.segmentation.ms.SegmentMeanShift}
 *
 * @author Peter Abeles
 */
public class ConfigSegmentMeanShift {

 	/**
	 * Radius of mean-shift region in pixels. Try 6
	 */
	public int spacialRadius = 6;
	/**
	 * Radius of mean-shift region for color in Euclidean distance. For 8bit RGB color space try 15
	 */
	public float colorRadius = 15;
	/**
	 * Minimum allowed size of a region in pixels. Try 30
	 */
	public int minimumRegionSize = 30;
	/**
	 * Improve runtime by approximating running mean-shift on each pixel. Try true.
	 */
	public boolean fast = true;
	/**
	 * Connection rule when segmenting disconnected regions. Try FOUR
	 */
	public ConnectRule connectRule = ConnectRule.FOUR;


	public ConfigSegmentMeanShift() {
	}

	public ConfigSegmentMeanShift(int spacialRadius, float colorRadius, int minimumRegionSize, boolean fast) {
		this.spacialRadius = spacialRadius;
		this.colorRadius = colorRadius;
		this.minimumRegionSize = minimumRegionSize;
		this.fast = fast;
	}
}
