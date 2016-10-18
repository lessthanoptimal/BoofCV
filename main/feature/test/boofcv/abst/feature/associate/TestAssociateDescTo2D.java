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

import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.MatchScoreType;
import boofcv.struct.feature.TupleDesc_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestAssociateDescTo2D {

	@Test
	public void basic() {
		Dummy dummy = new Dummy();

		AssociateDescTo2D<TupleDesc_F64> alg = new AssociateDescTo2D<>(dummy);

		FastQueue<TupleDesc_F64> listSrc = new FastQueue<>(10, TupleDesc_F64.class, false);
		FastQueue<TupleDesc_F64> listDst = new FastQueue<>(10, TupleDesc_F64.class, false);

		alg.setSource(null,listSrc);
		alg.setDestination(null,listDst);
		alg.associate();
		alg.setThreshold(10.5);

		assertTrue(listSrc == dummy.listSrc);
		assertTrue(listDst == dummy.listDst);
		assertTrue(dummy.calledAssociate);
		assertTrue(dummy.matches == alg.getMatches());
		assertTrue(dummy.unassociatedSrc == alg.getUnassociatedSource());
		assertTrue(10.5 == dummy.threshold);
		assertTrue(MatchScoreType.CORRELATION==alg.getScoreType());
	}

	private static class Dummy implements AssociateDescription<TupleDesc_F64> {

		public FastQueue<TupleDesc_F64> listSrc;
		public FastQueue<TupleDesc_F64> listDst;
		public boolean calledAssociate = false;
		public FastQueue<AssociatedIndex> matches = new FastQueue<>(10, AssociatedIndex.class, false);
		public GrowQueue_I32 unassociatedSrc = new GrowQueue_I32(10);
		public GrowQueue_I32 unassociatedDst = new GrowQueue_I32(10);
		public double threshold;

		@Override
		public void setSource(FastQueue<TupleDesc_F64> listSrc) {
			this.listSrc = listSrc;
		}

		@Override
		public void setDestination(FastQueue<TupleDesc_F64> listDst) {
			this.listDst = listDst;
		}

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
			return unassociatedSrc;
		}

		@Override
		public GrowQueue_I32 getUnassociatedDestination() {
			return unassociatedDst;
		}

		@Override
		public void setThreshold(double score) {
			threshold = score;
		}

		@Override
		public MatchScoreType getScoreType() {
			return MatchScoreType.CORRELATION;
		}

		@Override
		public boolean uniqueSource() {
			return false;
		}

		@Override
		public boolean uniqueDestination() {
			return false;
		}
	}

}
