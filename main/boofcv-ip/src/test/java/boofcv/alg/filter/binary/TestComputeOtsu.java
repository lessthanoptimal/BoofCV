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

package boofcv.alg.filter.binary;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestComputeOtsu {

	Random rand = new Random(234);
	/**
	 * Compares to standard Otsu
	 */
	@Test
	public void otsu() {
		ComputeOtsu computeOtsu = new ComputeOtsu(false,true);

		for (int i = 0; i < 100; i++) {
			int histogram[] = new int[ 256 ];
			int total = 0;
			for (int j = 0; j < histogram.length; j++) {
				total += histogram[j] = rand.nextInt(400);
			}

			int expected = GThresholdImageOps.computeOtsu(histogram,histogram.length,total);
			computeOtsu.compute(histogram,histogram.length,total);
			int found = (int)computeOtsu.threshold;

			assertEquals(expected,found);
		}
	}

	/**
	 * Compares to standard Otsu
	 */
	@Test
	public void otsu2() {
		ComputeOtsu computeOtsu = new ComputeOtsu(true,true);

		for (int i = 0; i < 100; i++) {
			int histogram[] = new int[ 256 ];
			int total = 0;
			for (int j = 0; j < histogram.length; j++) {
				total += histogram[j] = rand.nextInt(400);
			}

			int expected = GThresholdImageOps.computeOtsu2(histogram,histogram.length,total);
			computeOtsu.compute(histogram,histogram.length,total);
			int found = (int)computeOtsu.threshold;

			assertEquals(expected,found);
		}
	}

}