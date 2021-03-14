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

import boofcv.struct.TestConfigGenerator.ConfigDummyA;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestConfigGeneratorRandom extends BoofStandardJUnit {
	@Test void generate() {
		var alg = new ConfigGeneratorRandom<>(100, 0xBEEF, ConfigDummyA.class);

		// Modify two parameters
		alg.setOfIntegers("valueInt", -2, 1, 3, 10);
		alg.rangeOfFloats("next.valueFloat", -2.0, 5.0);
		alg.initialize();

		assertEquals(100, alg.numTrials);

		// Generate the configs
		List<ConfigDummyA> configs = new ArrayList<>();
		int trial = 1;
		while (alg.hasNext()) {
			configs.add(alg.next());
			assertEquals(trial++, alg.getTrial());
			assertSame(configs.get(configs.size() - 1), alg.getConfiguration());
		}

		// Make sure the expected number was created
		assertEquals(100, configs.size());

		// We will look at some of the statistics to ensure it's correct
		int intMin = Integer.MAX_VALUE;
		int intMax = Integer.MIN_VALUE;

		double floatMin = Double.MAX_VALUE;
		double floatMax = -Double.MAX_VALUE;

		for (int i = 1; i < configs.size(); i++) {
			// make sure they are not the same instance
			assertNotSame(configs.get(i - 1), configs.get(i));

			// Look at the values it sampled
			ConfigDummyA c = configs.get(i);
			intMin = Math.min(intMin, c.valueInt);
			intMax = Math.max(intMax, c.valueInt);
			floatMin = Math.min(floatMin, c.next.valueFloat);
			floatMax = Math.max(floatMax, c.next.valueFloat);
		}

		// see if there was the expected value range
		assertEquals(-2, intMin);
		assertEquals(10, intMax);
		assertEquals(-2, floatMin, 1.0);
		assertEquals(5.0, floatMax, 1.0);
	}

	/**
	 * Makes sure toStringState() contains the actual current state.
	 */
	@Test void toStringState() {
		var alg = new ConfigGeneratorRandom<>(100, 0xBEEF, ConfigDummyA.class);
		alg.rangeOfIntegers("valueInt", -2, 10);
		alg.rangeOfFloats("next.valueFloat", -2.0, 5.0);

		alg.initialize();
		while (alg.hasNext()) {
			TestConfigGenerator.ConfigDummyA config = alg.next();

			String state = alg.toStringState();
			String[] lines = state.split("\n");
			for (String line : lines) {
				if (line.startsWith("valueInt")) {
					int value = Integer.parseInt(line.split(",")[1]);
					assertEquals(config.valueInt, value);
				} else if (line.startsWith("next.valueFloat")) {
					float value = Float.parseFloat(line.split(",")[1]);
					assertEquals(config.next.valueFloat, value);
				}
			}
		}
	}
}
