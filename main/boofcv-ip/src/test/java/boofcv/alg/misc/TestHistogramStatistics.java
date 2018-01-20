/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.misc;

import org.ejml.UtilEjml;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestHistogramStatistics {
	@Test
	public void variance() {
		int histogram[] = new int[]{0,3,10,3,6};

		double mean = HistogramStatistics.mean(histogram,4);
		double found = HistogramStatistics.variance(histogram,mean,4);
		assertEquals(0.375,found, UtilEjml.TEST_F64);
	}

	@Test
	public void count() {
		int histogram[] = new int[]{0,3,10,3,6};

		int found = HistogramStatistics.count(histogram,4);
		assertEquals(16,found);
	}

	@Test
	public void mean() {
		int histogram[] = new int[]{0,3,10,3,6};

		double found = HistogramStatistics.mean(histogram,4);
		assertEquals((3+10*2+3*3)/(16.0),found, UtilEjml.TEST_F64);
	}

	@Test
	public void percentile() {
		int histogram[] = new int[]{0,3,10,3,6};

		int found = HistogramStatistics.percentile(histogram,0,4);
		assertEquals(0,found, UtilEjml.TEST_F64);
		found = HistogramStatistics.percentile(histogram,1.0,4);
		assertEquals(3,found, UtilEjml.TEST_F64);
		found = HistogramStatistics.percentile(histogram,0.2,4);
		assertEquals(1,found, UtilEjml.TEST_F64);
		found = HistogramStatistics.percentile(histogram,0.6,4);
		assertEquals(2,found, UtilEjml.TEST_F64);
	}
}