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

import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.MatchScoreType;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestAssociateDescTo2D extends BoofStandardJUnit {

	@Test void basic() {
		Dummy dummy = new Dummy();

		AssociateDescTo2D<TupleDesc_F64> alg = new AssociateDescTo2D<>(dummy);
		alg.initialize(100,100);

		FastArray<TupleDesc_F64> listSrc = new FastArray<>(TupleDesc_F64.class);
		FastArray<TupleDesc_F64> listDst = new FastArray<>(TupleDesc_F64.class);

		alg.setSource(null,listSrc);
		alg.setDestination(null,listDst);
		alg.associate();
		alg.setMaxScoreThreshold(10.5);

		assertSame(listSrc, dummy.listSrc);
		assertSame(listDst, dummy.listDst);
		assertTrue(dummy.calledAssociate);
		assertSame(dummy.matches, alg.getMatches());
		assertSame(dummy.unassociatedSrc, alg.getUnassociatedSource());
		assertEquals(10.5, dummy.threshold);
		assertSame(MatchScoreType.CORRELATION, alg.getScoreType());
	}

	private static class Dummy extends AssociateDescriptionAbstract<TupleDesc_F64> {
		public FastAccess<TupleDesc_F64> listSrc;
		public FastAccess<TupleDesc_F64> listDst;
		public boolean calledAssociate = false;
		public FastArray<AssociatedIndex> matches = new FastArray<>(AssociatedIndex.class);
		public DogArray_I32 unassociatedSrc = new DogArray_I32(10);
		public DogArray_I32 unassociatedDst = new DogArray_I32(10);
		public double threshold;

		@Override
		public void setSource(FastAccess<TupleDesc_F64> listSrc) {
			this.listSrc = listSrc;
		}

		@Override
		public void setDestination(FastAccess<TupleDesc_F64> listDst) {
			this.listDst = listDst;
		}

		@Override
		public void associate() {
			calledAssociate = true;
		}

		@Override
		public FastAccess<AssociatedIndex> getMatches() {
			return matches;
		}

		@Override
		public DogArray_I32 getUnassociatedSource() {
			return unassociatedSrc;
		}

		@Override
		public DogArray_I32 getUnassociatedDestination() {
			return unassociatedDst;
		}

		@Override
		public void setMaxScoreThreshold(double score) {
			threshold = score;
		}

		@Override
		public MatchScoreType getScoreType() {
			return MatchScoreType.CORRELATION;
		}
	}

}
