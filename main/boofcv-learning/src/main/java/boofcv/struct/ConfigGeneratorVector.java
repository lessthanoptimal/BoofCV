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
import org.ddogleg.struct.Tuple2;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link ConfigGenerator} that samples the configuration space using along each degree of
 * freedom (a parameter) independently. useful when you think you've found an optimal answer but want to see which
 * parameters influence its performance the most. When going through all the permutations, only the parameter being
 * searched will deviate from its default value.
 *
 * @author Peter Abeles
 */
public class ConfigGeneratorVector<Config extends Configuration> extends ConfigGeneratorPatternSearchBase<Config> {

	public ConfigGeneratorVector( long seed, Class<Config> type ) {
		super(seed, type);
	}

	/**
	 * Provides more configuration info over default.
	 */
	@Override public String toStringSettings() {
		String ret = "Vector:\n";

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

		numTrials = 0;
		for (int i = 0; i < parameters.size(); i++) {
			numTrials += getNumberOfStates(parameters.get(i));
			if (numTrials < 0)
				throw new IllegalArgumentException("Too many possible states in the vector search." +
						" Exceeded size of an int. Try random sampling instead?");
		}
	}

	/**
	 * Systematically goes through all configs in a grid type search. Each time it's called just one field in
	 * configBase is modified and a copy is returned.
	 */
	@Override public Config next() {

		// Creates a new config and assigns it to have the same value as configBase
		configCurrent = BoofMiscOps.copyConfig(configurationWork);

		try {
			int firstTrialInParameter = 0;
			for (int paramIdx = 0; paramIdx < parameters.size(); paramIdx++) {
				Parameter p = parameters.get(paramIdx);
				int numStates = getNumberOfStates(p);

				// See if this is the parameter being manipulated now
				if (firstTrialInParameter + numStates > trial) {
					// What's the discretized state in the parameter
					int state = trial - firstTrialInParameter;

					// Assign this parameter a new value
					double fraction = numStates == 1 ? 0.0 : state/(double)(numStates - 1);
					assignValue(configCurrent, p.getPath(), p.selectValue(fraction));
					break;
				}
				firstTrialInParameter += numStates;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		trial++;
		return configCurrent;
	}

	/**
	 * Returns a list of each parameter being examined and the number of counts. The order it's returned will
	 * match the order it's processed in.
	 */
	public List<Tuple2<String, Integer>> getParameterCounts() {
		var list = new ArrayList<Tuple2<String, Integer>>();

		for (int paramIdx = 0; paramIdx < parameters.size(); paramIdx++) {
			Parameter p = parameters.get(paramIdx);
			list.add(new Tuple2<>(p.path, getNumberOfStates(p)));
		}
		return list;
	}
}
