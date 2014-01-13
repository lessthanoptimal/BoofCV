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

package boofcv.alg.weights;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestWeightDistanceSqGaussian_F32 {

	/**
	 * See if it has some of the features of a gaussian
	 */
	@Test
	public void basic() {
		WeightDistanceSqGaussian_F32 alg = new WeightDistanceSqGaussian_F32(2);

		// should always decrease in value
		float prev = alg.weight(0);
		for( int i = 1; i < 10; i++ ) {
			float dist = 0.2f*i;
			float found = alg.weight(dist);

			assertTrue( found < prev );
			prev = found;
		}

		// likelihood of something 6 stdevs away is practically zero
		// remember, the input is distance squared.
		assertEquals(0,alg.weight(12*12),1e-4);
	}

}
