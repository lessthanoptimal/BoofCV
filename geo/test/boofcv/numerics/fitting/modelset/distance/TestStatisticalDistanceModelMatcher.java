/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.numerics.fitting.modelset.distance;

import boofcv.numerics.fitting.modelset.*;
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
	public ModelMatcher<double[],Double> createModelMatcher(DistanceFromModel<double[],Double> distance,
												   ModelFitter<double[],Double> fitter,
												   int minPoints, double fitThreshold) {
		return new StatisticalDistanceModelMatcher<double[],Double>(5, 0, 0, 10000, minPoints,
				StatisticalDistance.PERCENTILE,
				0.95, fitter, distance, new ArrayCodec());
	}

	private static class ArrayCodec implements ModelCodec<double[]>
	{

		@Override
		public double[] decode(double[] param, double[] outputModel) {
			return param;
		}

		@Override
		public double[] encode(double[] model, double[] param) {
			return model;
		}

		@Override
		public int getParamLength() {
			return 1;
		}
	}
}
