/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.descriptor.DescriptorDistance;
import boofcv.struct.feature.MatchScoreType;
import boofcv.struct.feature.TupleDesc_B;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestScoreAssociateHamming_B extends StandardScoreAssociationChecks<TupleDesc_B>{

	public TestScoreAssociateHamming_B() {
		super(MatchScoreType.NORM_ERROR);
	}

	/**
	 * Generate random descriptions and see two hamming distance calculations return the same result.
	 */
	@Test
	public void testRandom() {
		ScoreAssociateHamming_B scorer = new ScoreAssociateHamming_B();

		TupleDesc_B a = new TupleDesc_B(512);
		TupleDesc_B b = new TupleDesc_B(512);

		for( int numTries = 0; numTries < 20; numTries++ ) {
			for(int i = 0; i < a.value.length; i++ ) {
				a.value[i] = rand.nextInt();
				b.value[i] = rand.nextInt();
			}

			int expected = DescriptorDistance.hamming(a,b);

			assertEquals(expected,scorer.score(a,b),1e-4);
		}
	}

	@Override
	public ScoreAssociation<TupleDesc_B> createScore() {
		return new ScoreAssociateHamming_B();
	}

	@Override
	public TupleDesc_B createDescription() {
		TupleDesc_B a = new TupleDesc_B(5);
		for(int i = 0; i < a.value.length; i++ )
			a.value[i] = rand.nextInt();

		return a;
	}
}
