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

package boofcv.struct;

import boofcv.misc.BoofMiscOps;

/**
 * Implementation of {@link ConfigGenerator} that randomly samples each parameter using a uniform distribution
 *
 * @author Peter Abeles
 */
public class ConfigGeneratorRandom<Config extends Configuration> extends ConfigGenerator<Config> {
	public ConfigGeneratorRandom( int numTrials, long seed, Class<Config> type ) {
		super(seed, type);
		this.numTrials = numTrials;
	}

	/**
	 * Provides more configuration info over default.
	 */
	@Override public String toStringSettings() {
		return "Random:\n" + super.toStringSettings();
	}

	@Override public Config next() {
		trial++;
		// randomly assign states to all parameters
		try {
			for (int i = 0; i < parameters.size(); i++) {
				Parameter p = parameters.get(i);
				assignValue(configurationBase, p.getPath(), p.selectValue(rand.nextDouble()));
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// Creates a new config and assigns it to have the same value as configBase
		configCurrent = BoofMiscOps.copyConfig(configurationBase);
		return configCurrent;
	}
}
