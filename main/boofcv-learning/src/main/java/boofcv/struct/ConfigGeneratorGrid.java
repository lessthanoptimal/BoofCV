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
import org.ddogleg.struct.DogArray_I32;

/**
 * Implementation of {@link ConfigGenerator} that samples the configuration space using a grid pattern. This will
 * be exhaustive and can take a considerable amount of time.
 *
 * @author Peter Abeles
 */
public class ConfigGeneratorGrid<Config extends Configuration> extends ConfigGeneratorPatternSearchBase<Config> {

	// TODO Support sampling on a log scale

	// used for grid search
	DogArray_I32 parameterStates = new DogArray_I32();

	public ConfigGeneratorGrid( long seed, Class<Config> type ) {
		super(seed, type);
	}

	/**
	 * Provides more configuration info over default.
	 */
	@Override public String toStringSettings() {
		String ret = "Grid:\n";

		ret += "discretization," + rangeDiscretization + "\n";
		ret += super.toStringSettings();
		ret += "rules:\n";
		for (String key : pathToRule.keySet()) { // lint:forbidden ignore_line
			ret += key + "," + pathToRule.get(key) + "\n";
		}

		return ret;
	}

	@Override public void initialize() {
		super.initialize();

		configurationWork = BoofMiscOps.copyConfig(configurationBase);

		// Initialize every parameter to the first legal value
		parameterStates.resetResize(parameters.size(), 0);

		numTrials = 1;
		for (int i = 0; i < parameters.size(); i++) {
			numTrials *= getNumberOfStates(parameters.get(i));
			if (numTrials < 0)
				throw new IllegalArgumentException("Too many possible states in the grid search." +
						" Exceeded size of an int. Try random sampling instead?");
		}

		try {
			// Initialize all parameters in the base config to their 0.0 state
			for (int i = 0; i < parameters.size(); i++) {
				Parameter p = parameters.get(i);
				assignValue(configurationWork, p.getPath(), p.selectValue(0.0));
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Systematically goes through all configs in a grid type search. Each time it's called just one field in
	 * configBase is modified and a copy is returned.
	 */
	@Override public Config next() {
		trial++;

		// Creates a new config and assigns it to have the same value as configBase
		configCurrent = BoofMiscOps.copyConfig(configurationWork);

		try {
			boolean finished = false;
			// Iterate through the grid. Lower indexed parameters are incremented first.
			for (int i = 0; i < parameters.size() && !finished; i++) {
				Parameter p = parameters.get(i);
				int state = parameterStates.get(i);
				int numStates = getNumberOfStates(p);

				// See if it's outside of the allowed range. if so reset to zero and move to the next parameter
				state += 1;
				if (state >= numStates) {
					parameterStates.set(i, 0);
					state = 0;
				} else {
					parameterStates.set(i, state);
					finished = true;
				}

				// Modify this parameter and increment the state
				double fraction = numStates == 1 ? 0.0 : state/(double)(numStates - 1);
				assignValue(configurationWork, p.getPath(), p.selectValue(fraction));
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return configCurrent;
	}
}
