/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.feature.TupleDesc_F32;
import boofcv.struct.feature.TupleDesc_F64;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestScoreAssociateEuclideanSq {
	@Nested
	class F32  extends StandardScoreAssociationChecks<TupleDesc_F32> {
		public F32() {super(MatchScoreType.NORM_ERROR);}

		@Override
		public ScoreAssociation<TupleDesc_F32> createScore() { return new ScoreAssociateEuclideanSq.F32(); }

		@Override
		public TupleDesc_F32 createDescription() {
			var a = new TupleDesc_F32(5);
			for (int i = 0; i < a.size(); i++)
				a.value[i] = rand.nextFloat() * 2;

			return a;
		}

		@Test
		public void compareToExpected() {
			var score = new ScoreAssociateEuclideanSq.F32();

			var a = new TupleDesc_F32(5);
			var b = new TupleDesc_F32(5);

			a.value = new float[]{1, 2, 3, 4, 5};
			b.value = new float[]{2, -1, 7, -8, 10};

			assertEquals(195, score.score(a, b), 1e-4);
		}
	}

	@Nested
	class F64  extends StandardScoreAssociationChecks<TupleDesc_F64> {
		public F64() {super(MatchScoreType.NORM_ERROR);}

		@Override
		public ScoreAssociation<TupleDesc_F64> createScore() { return new ScoreAssociateEuclideanSq.F64(); }

		@Override
		public TupleDesc_F64 createDescription() {
			var a = new TupleDesc_F64(5);
			for (int i = 0; i < a.size(); i++)
				a.value[i] = rand.nextDouble() * 2;

			return a;
		}

		@Test
		public void compareToExpected() {
			var score = new ScoreAssociateEuclideanSq.F64();

			var a = new TupleDesc_F64(5);
			var b = new TupleDesc_F64(5);

			a.value=new double[]{1,2,3,4,5};
			b.value=new double[]{2,-1,7,-8,10};

			assertEquals(195,score.score(a,b),1e-4);
		}
	}
}