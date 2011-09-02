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

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestFitByMeanStatistics {
	@Test
	public void metric_and_prune() {
		List<Double> inliers = new ArrayList<Double>();

		for (int i = 0; i < 200; i++) {
			inliers.add((double) i);
		}

		// randomize the inputs
		Collections.sort(inliers);

		FitByMeanStatistics<double[],Double> fit = new FitByMeanStatistics<double[],Double>(1);

		fit.init(new DistanceFromMeanModel(), inliers);

		fit.computeStatistics();

		assertEquals(99.5, fit.getErrorMetric(), 1e-8);

		fit.prune();

		assertEquals(158, inliers.size());
	}
}
