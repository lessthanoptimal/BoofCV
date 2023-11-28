/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.bundle;

import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestEvaluateBundleScene extends BoofStandardJUnit {
	/**
	 * Give it a known count of inlier error and see if it returns the expected results
	 */
	@Test void checkInlierStats() {
		var alg = new EvaluateBundleScene();

		// Greatly simplify the code and generate exact residuals we want to test
		alg.functionResiduals = new BundleAdjustmentMetricResidualFunction() {
			@Override public void configure( SceneStructureMetric structure, SceneObservations observations ) {}

			@Override public void process( double[] output ) {
				for (int i = 1, count = 0; i <= 5; i++) {
					// Want the norm to be equal to i
					double r = Math.sqrt(i*i/2.0);

					for (int j = 0; j < i*2; j++, count++) {
						// give the residual different size, but the set magnitude
						output[count] = r*(j%2 == 0 ? -1 : 1);
					}
				}
			}

			@Override public int getNumOfOutputsM() {
				return 2*(1 + 2 + 3 + 4 + 5);
			}
		};

		alg.addInliers(0.5, 1.5, 2.5, 3.5, 4.5, 5.5);
		alg.evaluate(null, null);

		int expected = 0;
		for (int i = 0; i < 6; i++) {
			expected += i;
			assertEquals(expected, alg.inlierStats.get(i).count);
		}
	}
}
