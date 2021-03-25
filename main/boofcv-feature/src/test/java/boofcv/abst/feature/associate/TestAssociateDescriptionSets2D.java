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

import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.FastAccess;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestAssociateDescriptionSets2D extends BoofStandardJUnit {
	@Test
	void matchedOnlyWithinSets() {
		AssociateDescriptionSets2D<TupleDesc_F64> alg = createAlgorithm();
		alg.initializeSets(2);
		// all points are the same coordinate. Zero distance from each other so that only desc matters
		alg.addSource(c(5.0), 5, 5, 0);
		alg.addSource(c(6.0), 5, 5, 1);
		alg.addSource(c(7.0), 5, 5, 1);
		alg.addSource(c(8.0), 5, 5, 0);
		alg.addSource(c(9.0), 5, 5, 0);

		// intentionally set the scores so that the best match is in the wrong set
		alg.addDestination(c(5.2), 5, 5, 1);
		alg.addDestination(c(5.9), 5, 5, 0);
		alg.addDestination(c(7.9), 5, 5, 1);
		alg.addDestination(c(7.9), 5, 5, 0);
		alg.addDestination(c(9.9), 5, 5, 0);

		alg.initializeAssociator(100, 100);
		alg.associate();

		assertEquals(5, alg.getMatches().size);
//		FastAccess<AssociatedIndex> matches = alg.getMatches();

		assertTrue(checkForMatch(0, 1, alg.getMatches()));
		assertTrue(checkForMatch(1, 0, alg.getMatches()));
		assertTrue(checkForMatch(2, 2, alg.getMatches()));
		assertTrue(checkForMatch(3, 3, alg.getMatches()));
		assertTrue(checkForMatch(4, 4, alg.getMatches()));
	}

	private boolean checkForMatch( int src, int dst, FastAccess<AssociatedIndex> matches ) {
		for (AssociatedIndex a : matches.toList()) {
			if (a.src == src && a.dst == dst)
				return true;
		}
		return false;
	}

	protected static TupleDesc_F64 c( double value ) {
		TupleDesc_F64 s = new TupleDesc_F64(1);
		s.data[0] = value;
		return s;
	}

	protected AssociateDescriptionSets2D<TupleDesc_F64> createAlgorithm() {
		return new AssociateDescriptionSets2D<>(new AssociateDescTo2D<>(FactoryAssociation.greedy(
				null, new ScoreAssociateEuclideanSq.F64())));
	}

	@Nested
	public class StandardAssociate extends StandardAssociateDescription2DChecks<TupleDesc_F64> {
		protected StandardAssociate() {
			super(TupleDesc_F64.class);
		}

		@Override
		public AssociateDescription2D<TupleDesc_F64> createAssociate2D() {
			return new AssociateDescTo2D<>(FactoryAssociation.greedy(null, new ScoreAssociateEuclideanSq.F64()));
		}

		@Override
		protected TupleDesc_F64 c( double value ) {
			return TestAssociateDescriptionSets.c(value);
		}
	}
}
