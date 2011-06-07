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

package gecv.numerics.fitting.modelset.ransac;

import gecv.numerics.fitting.modelset.DistanceFromModel;
import gecv.numerics.fitting.modelset.GenericModelSetTests;
import gecv.numerics.fitting.modelset.ModelFitter;
import gecv.numerics.fitting.modelset.ModelMatcher;
import org.junit.Test;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class TestSimpleScoreRansac extends GenericModelSetTests {

	@Test
	public void performStandardTests() {
		configure(0.8, 0.2, true);
		performSimpleModelFit();
		runMultipleTimes();
	}

	@Override
	public ModelMatcher<double[],Double> createModelMatcher(DistanceFromModel<double[],Double> distance,
												   ModelFitter<double[],Double> fitter,
												   int minPoints, double fitThreshold) {


		MyScorer scorer = new MyScorer();

		return new SimpleScoreRansac<double[],Double>(3443, fitter, distance, scorer, 300, 2,
				fitThreshold, minPoints, fitThreshold, 0.0);
	}

	private static class MyScorer implements RansacFitScore<double[],Double> {

		@Override
		public double computeFitScore(List<Double> samples,
									  DistanceFromModel<double[],Double> modelDistance) {
			double total = 0;

			for (Double d : samples) {
				total += modelDistance.computeDistance(d);
			}

			return total;
		}
	}
}
