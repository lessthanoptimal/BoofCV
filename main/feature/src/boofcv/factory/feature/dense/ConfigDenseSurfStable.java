/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.feature.dense;

import boofcv.abst.feature.describe.ConfigSurfDescribe;
import boofcv.struct.Configuration;

/**
 * Configuration for Dense SURF features optimized for stability
 *
 * @author Peter Abeles
 */
public class ConfigDenseSurfStable implements Configuration{
	/**
	 * Configuration for Stable SURF descriptor
	 */
	public ConfigSurfDescribe.Stability surf = new ConfigSurfDescribe.Stability();

	/**
	 * Specifies the frequency it will sample across the image in pixels.  Default ix period X = 8, period Y = 8.
	 */
	public DenseSampling sampling = new DenseSampling(8,8);

	/**
	 * Relative size of descriptor
	 */
	public double descriptorScale = 1;

	public ConfigDenseSurfStable(DenseSampling sampling) {
		this.sampling = sampling;
	}

	public ConfigDenseSurfStable() {
	}

	@Override
	public void checkValidity() {
		if( sampling == null )
			throw new IllegalArgumentException("Most specify sampling");
	}
}
