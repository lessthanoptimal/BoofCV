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
public class TestHistogramTwoPeaks {

	/**
	 * Give it two clearly defined peaks with a few local minimums
	 */
	@Test
	public void simpleTest() {
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
		hist.histogram[70] = 2;
		hist.histogram[71] = 4;
		hist.histogram[72] = 8;
		hist.histogram[73] = 12;
		hist.histogram[74] = 20;
		hist.histogram[75] = 23;
		hist.histogram[76] = 20;
		hist.histogram[77] = 12;
		hist.histogram[78] = 6;
		hist.histogram[79] = 7;// local minimum
		hist.histogram[80] = 2;

		HistogramTwoPeaks alg = new HistogramTwoPeaks(5);
		
		alg.computePeaks(hist);
		
		assertEquals(6*2+0.5,alg.peakLow,1);
		assertEquals(75*2+0.5,alg.peakHigh,1);

	}
}
