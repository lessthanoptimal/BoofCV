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

package boofcv.abst.feature.associate;

import boofcv.struct.feature.MatchScoreType;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * General tests for scoring associations.
 *
 * @author Peter Abeles
 */
public abstract class StandardScoreAssociationChecks<T> {

	Random rand = new Random(234);

	MatchScoreType expectedType;

	public StandardScoreAssociationChecks(MatchScoreType expectedType) {
		this.expectedType = expectedType;
	}

	public abstract ScoreAssociation<T> createScore();

	@Test
	public void checkScoreType() {
		ScoreAssociation<T> alg = createScore();

		assertTrue(expectedType == alg.getScoreType());
	}

	/**
	 * Create a description filled with random values
	 */
	public abstract T createDescription();


	@Test
	public void empiricalCheckOnType() {
		ScoreAssociation<T> alg = createScore();

		T descA = createDescription();
		T descB = createDescription();

		double scorePerfect = alg.score(descA,descA);
		double scoreNoise = alg.score(descA,descB);

		assertTrue(scorePerfect < scoreNoise);

		if( alg.getScoreType().isZeroBest() ) {
			assertTrue(scorePerfect == 0);
		} else {
			assertTrue(scorePerfect != 0);
		}
	}
}
