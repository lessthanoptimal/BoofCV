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

package boofcv.abst.feature.associate;

import boofcv.alg.feature.associate.DescriptorDistance;
import boofcv.struct.feature.TupleDesc_B;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestScoreAssociateHamming_B {

	Random rand = new Random(123);

	/**
	 * Generate random descriptions and see two hamming distance calculations return the same result.
	 */
	@Test
	public void testRandom() {
		ScoreAssociateHamming_B scorer = new ScoreAssociateHamming_B();

		TupleDesc_B a = new TupleDesc_B(512);
		TupleDesc_B b = new TupleDesc_B(512);

		for( int numTries = 0; numTries < 20; numTries++ ) {
			for( int i = 0; i < a.data.length; i++ ) {
				a.data[i] = rand.nextInt();
				b.data[i] = rand.nextInt();
			}

			int expected = DescriptorDistance.hamming(a,b);

			assertEquals(expected,scorer.score(a,b),1e-4);
		}
	}
}
