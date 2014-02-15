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
 * Configuration for {@link boofcv.alg.segmentation.fh04.SegmentFelzenszwalbHuttenlocher04}.
 *
 * @author Peter Abeles
 */
public class ConfigFh04 {
	/**
	 * Tuning parameter.  Larger regions are preferred for larger values of K.  Try 100
	 */
	public float K = 100;
	/**
	 * Minimum allowed size of a region.
	 */
	public int minimumRegionSize = 30;
	/**
	 * Connection rule used to connect regions.  ConnectRule.EIGHT was using in the original paper.
	 */
	public ConnectRule connectRule = ConnectRule.EIGHT;

	/**
	 * If set to a value larger than 0 then an approximate sorting routine will be used.  This improves speed
	 * by about 40%.  A value of 2000 is recommended.
	 */
	public int approximateSortBins = 0;

	public ConfigFh04() {
	}

	public ConfigFh04(float k, int minimumRegionSize) {
		K = k;
		this.minimumRegionSize = minimumRegionSize;
	}

	public ConfigFh04(int k, int minimumRegionSize, ConnectRule connectRule) {
		K = k;
		this.minimumRegionSize = minimumRegionSize;
		this.connectRule = connectRule;
	}

	public ConfigFh04(float k, int minimumRegionSize, ConnectRule connectRule, int approximateSortBins) {
		K = k;
		this.minimumRegionSize = minimumRegionSize;
		this.connectRule = connectRule;
		this.approximateSortBins = approximateSortBins;
	}
}
