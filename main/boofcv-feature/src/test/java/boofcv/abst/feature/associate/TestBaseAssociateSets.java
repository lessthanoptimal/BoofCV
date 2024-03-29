/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.MatchScoreType;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestBaseAssociateSets extends BoofStandardJUnit {
	/**
	 * Ensures that flags in associate are correctly passed through
	 */
	@Test
	void checkFlagsPassedThrough() {
		var associate = new MockAssociate();
		var alg = new MockAssociateSets(associate);

		alg.setMaxScoreThreshold(5.0);
		assertEquals(5.0, associate.maxScore, 0.0);
		associate.uniqueSrc = false;
		assertFalse(alg.uniqueSource());
		associate.uniqueSrc = true;
		assertTrue(alg.uniqueSource());
		associate.uniqueDst = false;
		assertFalse(alg.uniqueDestination());
		associate.uniqueDst = true;
		assertTrue(alg.uniqueDestination());
		assertEquals(MatchScoreType.NORM_ERROR, alg.getScoreType());
	}

	private static class MockAssociate implements Associate<TupleDesc_F64> {

		public int calledAssociate;
		public double maxScore;
		public boolean uniqueSrc;
		public boolean uniqueDst;
		public FastAccess<AssociatedIndex> matches = new FastArray<>(AssociatedIndex.class);
		public DogArray_I32 unassociatedSrc = new DogArray_I32();
		public DogArray_I32 unassociatedDst = new DogArray_I32();

		// @formatter:off
		@Override public void associate() {calledAssociate++;}
		@Override public FastAccess<AssociatedIndex> getMatches() {return matches;}
		@Override public DogArray_I32 getUnassociatedSource() {return unassociatedSrc;}
		@Override public DogArray_I32 getUnassociatedDestination() {return unassociatedDst;}
		@Override public void setMaxScoreThreshold(double score) {this.maxScore = score;}
		@Override public MatchScoreType getScoreType() {return MatchScoreType.NORM_ERROR;}
		@Override public boolean uniqueSource() {return uniqueSrc;}
		@Override public boolean uniqueDestination() { return uniqueDst; }
		@Override public Class<TupleDesc_F64> getDescriptionType() {return TupleDesc_F64.class;}
		// @formatter:on
	}

	private static class MockAssociateSets extends BaseAssociateSets<TupleDesc_F64> {
		public MockAssociateSets( Associate<TupleDesc_F64> associator ) {
			super(associator);
		}

		@Override public void associate() {}
	}
}
