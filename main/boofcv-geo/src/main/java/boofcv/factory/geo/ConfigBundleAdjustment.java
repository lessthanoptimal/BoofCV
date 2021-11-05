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

package boofcv.factory.geo;

import org.ddogleg.optimization.lm.ConfigLevenbergMarquardt;

/**
 * Configuration for {@link boofcv.abst.geo.bundle.BundleAdjustment}
 *
 * @author Peter Abeles
 */
public class ConfigBundleAdjustment {

	/**
	 * Used to specify which optimization routine to use and how to configure it.
	 *
	 * @see ConfigLevenbergMarquardt
	 * @see org.ddogleg.optimization.trustregion.ConfigTrustRegion
	 */
	public Object configOptimizer = new ConfigLevenbergMarquardt();

	public ConfigBundleAdjustment setTo( ConfigBundleAdjustment src ) {
		// it should copy / overwrite but that isn't possible/easy. So this is the compromise
		this.configOptimizer = src.configOptimizer;
		return this;
	}
}
