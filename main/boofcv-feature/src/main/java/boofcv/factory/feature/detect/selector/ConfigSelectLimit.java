/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.feature.detect.selector;

import boofcv.alg.feature.detect.selector.FeatureSelectLimit;
import boofcv.struct.ConfigGridUniform;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link FeatureSelectLimit}
 *
 * @author Peter Abeles
 */
public class ConfigSelectLimit implements Configuration {
	/** Specified which selector to use */
	public SelectLimitTypes type = SelectLimitTypes.BEST_N;

	/**
	 *  Random seed used by RANDOM selector
	 */
	public long randomSeed = 0xDEADBEEF;

	/** Configuration used by Uniform selector */
	public ConfigGridUniform uniform = new ConfigGridUniform();

	public ConfigSelectLimit() {
	}

	public ConfigSelectLimit(SelectLimitTypes type, long randomSeed) {
		this.type = type;
		this.randomSeed = randomSeed;
	}

	@Override
	public void checkValidity() {

	}

	public static ConfigSelectLimit selectBestN() {
		return new ConfigSelectLimit(SelectLimitTypes.BEST_N,-1);
	}

	public static ConfigSelectLimit selectRandom(long seed) {
		return new ConfigSelectLimit(SelectLimitTypes.RANDOM,seed);
	}

	public static ConfigSelectLimit selectUniform(double inverseRegionScale) {
		ConfigSelectLimit config = new ConfigSelectLimit(SelectLimitTypes.UNIFORM_BEST,-1);
		config.uniform.inverseRegionScale = inverseRegionScale;
		return config;
	}
}
