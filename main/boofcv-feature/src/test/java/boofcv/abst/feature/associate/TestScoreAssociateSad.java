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

import boofcv.struct.feature.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestScoreAssociateSad {
	@Nested
	public class F32 extends StandardScoreAssociationChecks<TupleDesc_F32> {

		public F32() { super(MatchScoreType.NORM_ERROR); }

		@Override
		public ScoreAssociation<TupleDesc_F32> createScore() { return new ScoreAssociateSad.F32(); }

		@Override
		public TupleDesc_F32 createDescription() {
			var a = new TupleDesc_F32(5);
			for (int i = 0; i < a.size(); i++)
				a.value[i] = rand.nextFloat() * 2;

			return a;
		}

		@Test
		public void compareToExpected() {
			var scorer = new ScoreAssociateSad.F32();

			var a = new TupleDesc_F32(5);
			var b = new TupleDesc_F32(5);

			a.value = new float[]{1.1f, 2, 3, 4.5f, 5};
			b.value = new float[]{-1, 2, 6.1f, 3, 6};

			assertEquals(7.7, scorer.score(a, b), 1e-2);
		}
	}

	@Nested
	public class F64 extends StandardScoreAssociationChecks<TupleDesc_F64> {
		public F64() { super(MatchScoreType.NORM_ERROR); }

		@Override
		public ScoreAssociation<TupleDesc_F64> createScore() { return new ScoreAssociateSad.F64(); }

		@Override
		public TupleDesc_F64 createDescription() {
			var a = new TupleDesc_F64(5);
			for( int i = 0; i < a.size(); i++ )
				a.value[i] = rand.nextDouble()*2;

			return a;
		}

		@Test
		public void compareToExpected() {
			var scorer = new ScoreAssociateSad.F64();

			var a = new TupleDesc_F64(5);
			var b = new TupleDesc_F64(5);

			a.value=new double[]{1.1,2,3,4.5,5};
			b.value=new double[]{-1,2,6.1,3,6};

			assertEquals(7.7,scorer.score(a,b),1e-2);
		}
	}

	@Nested
	public class U8 extends StandardScoreAssociationChecks<TupleDesc_U8> {
		public U8() { super(MatchScoreType.NORM_ERROR); }

		@Override
		public ScoreAssociation<TupleDesc_U8> createScore() { return new ScoreAssociateSad.U8(); }

		@Override
		public TupleDesc_U8 createDescription() {
			TupleDesc_U8 a = new TupleDesc_U8(5);
			for( int i = 0; i < a.size(); i++ )
				a.value[i] = (byte)rand.nextInt(200);

			return a;
		}

		@Test
		public void compareToExpected() {
			var scorer = new ScoreAssociateSad.U8();

			var a = new TupleDesc_U8(5);
			var b = new TupleDesc_U8(5);

			a.value=new byte[]{1,2,3,4,(byte)200};
			b.value=new byte[]{(byte)245,2,6,3,6};

			assertEquals(442,scorer.score(a,b),1e-2);
		}
	}

	@Nested
	public class S8 extends StandardScoreAssociationChecks<TupleDesc_S8> {
		public S8() { super(MatchScoreType.NORM_ERROR); }

		@Override
		public ScoreAssociation<TupleDesc_S8> createScore() { return new ScoreAssociateSad.S8(); }

		@Override
		public TupleDesc_S8 createDescription() {
			var a = new TupleDesc_S8(5);
			for( int i = 0; i < a.size(); i++ )
				a.value[i] = (byte)(rand.nextInt(200)-100);

			return a;
		}

		@Test
		public void compareToExpected() {
			var scorer = new ScoreAssociateSad.U8();

			var a = new TupleDesc_U8(5);
			var b = new TupleDesc_U8(5);

			a.value=new byte[]{1,2,3,4,(byte)200};
			b.value=new byte[]{(byte)245,2,6,3,6};

			assertEquals(442,scorer.score(a,b),1e-2);
		}
	}
}
