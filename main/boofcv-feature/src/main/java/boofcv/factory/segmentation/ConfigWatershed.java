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

package boofcv.factory.segmentation;

import boofcv.struct.Configuration;
import boofcv.struct.ConnectRule;

/**
 * Configuration for {@link boofcv.alg.segmentation.watershed.WatershedVincentSoille1991}
 *
 * @author Peter Abeles
 */
public class ConfigWatershed implements Configuration {
	/**
	 * Connectivity rule
	 */
	public ConnectRule connectRule = ConnectRule.FOUR;

	/**
	 * Regions which are smaller than this are merged into a neighbor. Regions which have a similar
	 * color are merged together.
	 */
	public int minimumRegionSize = 45;

	public ConfigWatershed() {}

	public ConfigWatershed( ConnectRule connectRule, int minimumRegionSize ) {
		this.connectRule = connectRule;
		this.minimumRegionSize = minimumRegionSize;
	}

	public ConfigWatershed setTo( ConfigWatershed src ) {
		this.connectRule = src.connectRule;
		this.minimumRegionSize = src.minimumRegionSize;
		return this;
	}

	@Override public void checkValidity() {}
}
