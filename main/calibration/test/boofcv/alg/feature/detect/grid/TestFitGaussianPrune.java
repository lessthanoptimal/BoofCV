/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.grid;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestFitGaussianPrune {

	/**
	 * Given an approximately Gaussian distribute, remove some simple outliers
	 */
	@Test
	public void basicTest() {
		IntensityHistogram hist = new IntensityHistogram(100,200);
		
		hist.histogram[1] = 2;
		hist.histogram[2] = 5;
		hist.histogram[3] = 12;
		hist.histogram[4] = 20;
		hist.histogram[5] = 40;
		hist.histogram[6] = 45;
		hist.histogram[7] = 40;
		hist.histogram[8] = 20;
		hist.histogram[9] = 12;
		hist.histogram[10] = 5;
		hist.histogram[11] = 2;
		hist.histogram[50] = 2;
		hist.histogram[60] = 1;
		hist.histogram[80] = 6000; // should never be seen by the alg

		
		FitGaussianPrune alg = new FitGaussianPrune(20,3,2);

		alg.process(hist,0,70);

		// the mean should be approximately the distributions mean
		assertEquals(6*2,alg.getMean(),1);
		// see if it closed the index boundary as expected
		assertEquals(1,alg.indexLow);
		assertEquals(11,alg.indexHigh);
	}
}
