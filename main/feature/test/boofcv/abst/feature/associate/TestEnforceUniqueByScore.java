/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestEnforceUniqueByScore {

	@Test
	public void generic() {
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
		assertTrue(unique.getMatches()==alg.getMatches());
		assertTrue(unique.calledProcess);
		assertEquals(unique.numSource, alg.numSource);
		assertEquals(unique.numDestination,alg.numDestination);
		assertEquals(checkSource,alg.uniqueSource());
		assertEquals(checkDestination,alg.uniqueDestination());
	}

	@Test
	public void checkUniqueFlags() {
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
		FastQueue<AssociatedIndex> matches = new FastQueue<>(AssociatedIndex.class, true);
		int numSource;
		int numDestination;

		public Dummy(MatchScoreType type, boolean checkSource, boolean checkDestination) {
			super(type, checkSource, checkDestination);
		}

		@Override
		public void process( FastQueue<AssociatedIndex> matches , int numSource , int numDestination ) {
			calledProcess = true;
			this.numSource = numSource;
			this.numDestination = numDestination;
		}

		@Override
		public FastQueue<AssociatedIndex> getMatches() {
			return matches;
		}
	}

	private static class DummyAssociate implements Associate {

		boolean calledAssociate = false;

		boolean uniqueSource;
		boolean uniqueDestination;

		GrowQueue_I32 unSrc = new GrowQueue_I32();
		GrowQueue_I32 unDst = new GrowQueue_I32();

		FastQueue<AssociatedIndex> matches = new FastQueue<>(AssociatedIndex.class, true);

		@Override
		public void associate() {
			calledAssociate = true;
		}

		@Override
		public FastQueue<AssociatedIndex> getMatches() {
			return matches;
		}

		@Override
		public GrowQueue_I32 getUnassociatedSource() {
			return unSrc;
		}

		@Override
		public GrowQueue_I32 getUnassociatedDestination() {
			return unDst;
		}

		@Override
		public void setThreshold(double score) {
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
	}
}
