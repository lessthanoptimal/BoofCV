/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.numerics.fitting.modelset.distance;

import gecv.numerics.fitting.modelset.DistanceFromModel;
import gecv.numerics.fitting.modelset.GenericModelSetTests;
import gecv.numerics.fitting.modelset.ModelFitter;
import gecv.numerics.fitting.modelset.ModelMatcher;
import org.junit.Test;

/**
 * @author Peter Abeles
 */
public class TestStatisticalDistanceModelMatcher extends GenericModelSetTests {
	@Test
	public void standardTests() {
		configure(0.9, 0.1, false);
		performSimpleModelFit();
		runMultipleTimes();
	}

	@Override
	public ModelMatcher<Double> createModelMatcher(DistanceFromModel<Double> distance,
												   ModelFitter<Double> fitter,
												   int minPoints, double fitThreshold) {
		return new StatisticalDistanceModelMatcher<Double>(5, 0, 0, 10000, minPoints,
				StatisticalDistance.PERCENTILE,
				0.95, fitter, distance);
	}
}
