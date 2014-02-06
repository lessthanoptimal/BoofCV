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
 * Configuration used when creating {@link boofcv.alg.segmentation.slic.SegmentSlic} via
 * {@link FactoryImageSegmentation}.
 *
 * @author Peter Abeles
 */
public class ConfigSlic {
	/**
	 * Number of regions which will be initially seeded.  The actually number of regions at the end will
	 * vary a bit due to merging of small regions and the image border.
	 */
	public int numberOfRegions;
	/**
	 * Larger values place more weight on the spacial component.  For 8-bit RGB a value of 200 works well.
	 */
	public float spacialWeight = 200;
	/**
	 * Number of mean-shift iterations.  Typically has converged by 10 iterations.
	 */
	public int totalIterations = 10;
	/**
	 * Connection rule that is used when merging small regions.
	 */
	public ConnectRule connectRule = ConnectRule.EIGHT;

	public ConfigSlic(int numberOfRegions) {
		this.numberOfRegions = numberOfRegions;
	}

	public ConfigSlic(int numberOfRegions, float spacialWeight) {
		this.numberOfRegions = numberOfRegions;
		this.spacialWeight = spacialWeight;
	}
}
