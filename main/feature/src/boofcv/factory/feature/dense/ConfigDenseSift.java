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

import boofcv.abst.feature.describe.ConfigSiftDescribe;
import boofcv.struct.Configuration;

/**
 * Configuration for dense SIFT features
 *
 * @author Peter Abeles
 */
public class ConfigDenseSift implements Configuration {

	/**
	 * Specifies how the SIFT descriptor is computed.   All parameters are used but
	 * {@link ConfigSiftDescribe#sigmaToPixels}
	 */
	public ConfigSiftDescribe sift = new ConfigSiftDescribe();

	/**
	 * Specifies the frequency it will sample across the image in pixels.  Default ix period X = 6, period Y = 6.
	 */
	public DenseSampling sampling = new DenseSampling(6,6);

	public ConfigDenseSift(DenseSampling sampling) {
		this.sampling = sampling;
	}

	public ConfigDenseSift() {
	}

	@Override
	public void checkValidity() {
		if( sampling == null )
			throw new IllegalArgumentException("Most specify sampling");
	}
}
