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

import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.Tuple2;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestConfigGeneratorVector extends BoofStandardJUnit {
	@Test void generate() {
		var alg = new ConfigGeneratorVector<>(0xBEEF, TestConfigGenerator.ConfigDummyA.class);

		int range = 10;
		alg.rangeDiscretization = range;

		// Modify two parameters
		alg.setOfIntegers("valueInt", -2, 1, 3, 10);
		alg.rangeOfFloats("next.valueFloat", -2.0, 5.0);
		alg.initialize();

		assertEquals(4 + 10, alg.numTrials);

		// Generate the configs
		List<TestConfigGenerator.ConfigDummyA> configs = new ArrayList<>();
		int trial = 1;
		while (alg.hasNext()) {
			configs.add(alg.next());
			assertEquals(trial++, alg.getTrial());
			assertSame(configs.get(configs.size() - 1), alg.getConfiguration());
		}

		// Make sure the expected number was created
		assertEquals(4 + 10, configs.size());

		// manually check a few parameters
		assertEquals(-2, configs.get(0).valueInt);
		assertEquals(0.0, configs.get(0).next.valueFloat);
		assertEquals(10, configs.get(3).valueInt);
		assertEquals(0.0, configs.get(3).next.valueFloat);
		assertEquals(0, configs.get(5).valueInt);
		assertEquals(-2.0 + 7.0/(range - 1), configs.get(5).next.valueFloat, UtilEjml.TEST_F32);

		// We will look at some of the statistics to ensure it's correct
		int intMin = Integer.MAX_VALUE;
		int intMax = Integer.MIN_VALUE;

		double floatMin = Double.MAX_VALUE;
		double floatMax = -Double.MAX_VALUE;

		for (int i = 0; i < configs.size(); i++) {
			// make sure they are not the same instance
			if (i > 0)
				assertNotSame(configs.get(i - 1), configs.get(i));

			// Look at the values it sampled
			TestConfigGenerator.ConfigDummyA c = configs.get(i);
			intMin = Math.min(intMin, c.valueInt);
			intMax = Math.max(intMax, c.valueInt);
			floatMin = Math.min(floatMin, c.next.valueFloat);
			floatMax = Math.max(floatMax, c.next.valueFloat);
		}

		// see if there was the expected value range
		assertEquals(-2, intMin);
		assertEquals(10, intMax);
		assertEquals(-2, floatMin, UtilEjml.TEST_F32);
		assertEquals(5.0, floatMax, UtilEjml.TEST_F32);
	}

	/**
	 * Modify the baseline and see if that has the expected results
	 */
	@Test void modifiedBaseline() {
		var alg = new ConfigGeneratorVector<>(0xBEEF, TestConfigGenerator.ConfigDummyA.class);

		// Modify two parameters
		alg.setOfIntegers("valueInt", -2, 1, 3, 10);
		alg.getConfigurationBase().valueFloat = 123.0f; // this should stick
		alg.initialize();
		alg.getConfigurationBase().valueFloat = 321.0f; // this should be ignored

		// Generate the configs
		int trials = 0;
		while (alg.hasNext()) {
			assertEquals(123.0f, alg.next().valueFloat, UtilEjml.TEST_F32);
			trials++;
		}
		assertTrue(trials > 0);
	}

	@Test void Discretization_Integers() {
		var alg = new ConfigGeneratorVector<>(0xBEEF, TestConfigGenerator.ConfigDummyA.class);

		// Give it a very large number that will be easy to recognize
		alg.rangeDiscretization = 10000;

		// Modify two parameters
		alg.rangeOfIntegers("valueInt", -2, 10);
		alg.rangeOfFloats("next.valueFloat", -2.0, 5.0);
		alg.setDiscretizationRule("valueInt", ConfigGeneratorVector.Discretization.INTEGER_VALUES);
		alg.setDiscretizationRule("next.valueFloat", ConfigGeneratorVector.Discretization.INTEGER_VALUES);
		alg.initialize();

		assertEquals(13 + 8, alg.getNumTrials());

		// Sanity check to see if each value appears the expected number of times
		int[] countInt = new int[13];
		int[] countFloat = new int[8];

		while (alg.hasNext()) {
			TestConfigGenerator.ConfigDummyA config = alg.next();
			countInt[config.valueInt + 2]++;
			countFloat[(int)config.next.valueFloat + 2]++;
		}

		checkHistogram(countInt);
		checkHistogram(countFloat);
	}

	/**
	 * Every value should be sampled once when it was the target, but the default value many times
	 */
	private void checkHistogram( int[] counts ) {
		int totalOne = 0;
		for (int count : counts) {
			if (count == 1) {
				totalOne++;
			}
		}
		assertEquals(counts.length - 1, totalOne);
	}

	/**
	 * Makes sure toStringState() contains the actual current state.
	 */
	@Test void toStringState() {
		var alg = new ConfigGeneratorVector<>(0xBEEF, TestConfigGenerator.ConfigDummyA.class);
		alg.rangeOfIntegers("valueInt", -2, 10);
		alg.rangeOfFloats("next.valueFloat", -2.0, 5.0);
		alg.setDiscretizationRule("valueInt", ConfigGeneratorVector.Discretization.INTEGER_VALUES);
		alg.setDiscretizationRule("next.valueFloat", ConfigGeneratorVector.Discretization.INTEGER_VALUES);

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

	@Test void getNumberOfStates() {
		var alg = new ConfigGeneratorVector<>(0xBEEF, TestConfigGenerator.ConfigDummyA.class);

		// This will be larger than the first variable but smaller than the second
		alg.rangeDiscretization = 20;

		// smaller than the default discretization. It should be limited by the max values
		alg.rangeOfIntegers("valueInt", -2, 10);
		// Much larger than the default. It should use the default
		alg.rangeOfIntegers("next.valueInt", -2, 2000);

		assertEquals(13, alg.getNumberOfStates(alg.parameters.get(0)));
		assertEquals(20, alg.getNumberOfStates(alg.parameters.get(1)));
	}

	@Test void getParameterCounts() {
		var alg = new ConfigGeneratorVector<>(0xBEEF, TestConfigGenerator.ConfigDummyA.class);
		alg.rangeDiscretization = 20;
		alg.rangeOfIntegers("valueInt", -2, 10);
		alg.rangeOfFloats("next.valueFloat", -2.0, 5.0);

		List<Tuple2<String, Integer>> counts = alg.getParameterCounts();
		assertEquals("valueInt", counts.get(0).d0);
		assertEquals(13, counts.get(0).getD1().intValue());
		assertEquals("next.valueFloat", counts.get(1).d0);
		assertEquals(20, counts.get(1).getD1().intValue());
	}
}
