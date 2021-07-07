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

import boofcv.alg.feature.associate.AssociateUniqueByScoreAlg;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.MatchScoreType;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestEnforceUniqueByScore extends BoofStandardJUnit {

	@Test void generic() {
		generic(true,false);
		generic(false,true);
		generic(true,true);
	}

	public void generic( boolean checkSource, boolean checkDestination ) {

		Dummy unique = new Dummy(MatchScoreType.NORM_ERROR,checkSource,checkDestination);
		DummyAssociate associate = new DummyAssociate();

		EnforceUniqueByScore alg = new EnforceUniqueByScore(associate,checkSource,checkDestination);
		alg.uniqueByScore = unique;
		alg.numSource=5;
		alg.numDestination=6;

		alg.associate();
		assertSame(unique.getMatches(), alg.getMatches());
		assertTrue(unique.calledProcess);
		assertEquals(unique.numSource, alg.numSource);
		assertEquals(unique.numDestination,alg.numDestination);
		assertEquals(checkSource,alg.uniqueSource());
		assertEquals(checkDestination,alg.uniqueDestination());
	}

	@Test void checkUniqueFlags() {
		DummyAssociate associate = new DummyAssociate();

		associate.uniqueSource=true;associate.uniqueDestination=false;
		EnforceUniqueByScore alg = new EnforceUniqueByScore(associate,true,false);
		assertTrue(alg.uniqueSource());
		assertFalse(alg.uniqueDestination());
		assertFalse(alg.uniqueByScore.checkSource());
		assertFalse(alg.uniqueByScore.checkDestination());

		associate.uniqueSource=true;associate.uniqueDestination=false;
		alg = new EnforceUniqueByScore(associate,true,true);
		assertTrue(alg.uniqueSource());
		assertTrue(alg.uniqueDestination());
		assertFalse(alg.uniqueByScore.checkSource());
		assertTrue(alg.uniqueByScore.checkDestination());

		associate.uniqueSource=false;associate.uniqueDestination=false;
		alg = new EnforceUniqueByScore(associate,true,true);
		assertTrue(alg.uniqueSource());
		assertTrue(alg.uniqueDestination());
		assertTrue(alg.uniqueByScore.checkSource());
		assertTrue(alg.uniqueByScore.checkDestination());

		associate.uniqueSource=false;associate.uniqueDestination=true;
		alg = new EnforceUniqueByScore(associate,false,true);
		assertFalse(alg.uniqueSource());
		assertTrue(alg.uniqueDestination());
		assertFalse(alg.uniqueByScore.checkSource());
		assertFalse(alg.uniqueByScore.checkDestination());

	}

	private static class Dummy extends AssociateUniqueByScoreAlg {

		boolean calledProcess = false;
		DogArray<AssociatedIndex> matches = new DogArray<>(AssociatedIndex::new);
		int numSource;
		int numDestination;

		public Dummy(MatchScoreType type, boolean checkSource, boolean checkDestination) {
			super(type, checkSource, checkDestination);
		}

		@Override
		public void process(FastAccess<AssociatedIndex> matches , int numSource , int numDestination ) {
			calledProcess = true;
			this.numSource = numSource;
			this.numDestination = numDestination;
		}

		@Override
		public DogArray<AssociatedIndex> getMatches() {
			return matches;
		}
	}

	private static class DummyAssociate implements Associate<Object> {
		boolean calledAssociate = false;

		boolean uniqueSource;
		boolean uniqueDestination;

		DogArray_I32 unSrc = new DogArray_I32();
		DogArray_I32 unDst = new DogArray_I32();

		DogArray<AssociatedIndex> matches = new DogArray<>(AssociatedIndex::new);

		@Override
		public void associate() {
			calledAssociate = true;
		}

		@Override
		public DogArray<AssociatedIndex> getMatches() {
			return matches;
		}

		@Override
		public DogArray_I32 getUnassociatedSource() {
			return unSrc;
		}

		@Override
		public DogArray_I32 getUnassociatedDestination() {
			return unDst;
		}

		@Override
		public void setMaxScoreThreshold(double score) {
		}

		@Override
		public MatchScoreType getScoreType() {
			return MatchScoreType.NORM_ERROR;
		}

		@Override
		public boolean uniqueSource() {
			return uniqueSource;
		}

		@Override
		public boolean uniqueDestination() {
			return uniqueDestination;
		}

		@Override public Class<Object> getDescriptionType() {
			return Object.class;
		}
	}
}
