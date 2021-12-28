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

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for searches which follow a repetable pattern
 *
 * @author Peter Abeles
 */
@SuppressWarnings("NullAway.Init")
public abstract class ConfigGeneratorPatternSearchBase<Config extends Configuration> extends ConfigGenerator<Config> {
	/**
	 * Specifies how to discretize a continuous range
	 */
	Map<String, Discretization> pathToRule = new HashMap<>();

	/**
	 * If a grid search is requested, the number of discrete values a range is broken up into.
	 */
	@Getter @Setter int rangeDiscretization = 10;

	/** Copied from baseline at initialization. Modified while searching the grid */
	protected Config configurationWork;

	protected ConfigGeneratorPatternSearchBase( long seed, Class<Config> type ) {
		super(seed, type);
	}

	/**
	 * Returns the number of possible states a parameter has
	 */
	@Override protected int getNumberOfStates( Parameter p ) {
		if (p.getStateSize() != 0)
			return p.getStateSize();

		switch (pathToRule.getOrDefault(p.path, Discretization.DEFAULT)) {
			case DEFAULT -> {
				// If the number of unique values is smaller then the default discretization use that instead.
				if (p instanceof RangeOfIntegers) {
					RangeOfIntegers pp = (RangeOfIntegers)p;
					int uniqueValues = pp.idx1 - pp.idx0 + 1;
					return Math.min(uniqueValues, rangeDiscretization);
				}
				return rangeDiscretization;
			}
			case INTEGER_VALUES -> {
				double val0 = ((Number)p.selectValue(0.0)).doubleValue();
				double val1 = ((Number)p.selectValue(1.0)).doubleValue();
				return (int)(val1 - val0 + 1); // +1 because both extents are inclusive
			}
			default -> throw new RuntimeException("Unknown rule");
		}
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

	/**
	 * Specifies how continuous ranges should be discretized
	 */
	public void setDiscretizationRule( String path, Discretization rule ) {
		pathToRule.put(path, rule);
	}

	public enum Discretization {
		/** Breaks it up into a fixed number of values */
		DEFAULT,
		/** breaks it up using the number of whole integers that lie within the range */
		INTEGER_VALUES
	}
}
