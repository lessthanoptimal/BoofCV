/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.numerics.fitting.modelset.distance;

import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestFitByMeanStatistics {

	Random rand = new Random(234);

	@Test
	public void metric_and_prune() {
		LinkedList<PointIndex<Double>> inliers = new LinkedList<PointIndex<Double>>();

		for (int i = 0; i < 200; i++) {
			inliers.add(new PointIndex<Double>((double) i,i));
		}

		// randomize the inputs
		Collections.shuffle(inliers,rand);

		FitByMeanStatistics<double[],Double> fit = new FitByMeanStatistics<double[],Double>(1);

		fit.init(new DistanceFromMeanModel(), inliers);

		fit.computeStatistics();

		assertEquals(99.5, fit.getErrorMetric(), 1e-8);

		fit.prune();

		assertEquals(158, inliers.size());
	}
}
