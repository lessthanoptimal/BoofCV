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

import boofcv.alg.feature.associate.AssociateGreedy;
import boofcv.struct.feature.TupleDesc_F64;
import org.junit.jupiter.api.Nested;

/**
 * @author Peter Abeles
 */
class TestWrapAssociateGreedy {

	@Nested
	public class Normal extends StandardTests {
		@Override
		public AssociateDescription<TupleDesc_F64> createAssociate() {
			ScoreAssociateEuclidean_F64 score = new ScoreAssociateEuclidean_F64();
			AssociateGreedy<TupleDesc_F64> greedy = new AssociateGreedy<>(score, false);
			return new WrapAssociateGreedy<>(greedy);
		}
	}

	@Nested
	public class Backwards extends StandardTests {
		@Override
		public AssociateDescription<TupleDesc_F64> createAssociate() {
			ScoreAssociateEuclidean_F64 score = new ScoreAssociateEuclidean_F64();
			AssociateGreedy<TupleDesc_F64> greedy = new AssociateGreedy<>(score, true);
			return new WrapAssociateGreedy<>(greedy);
		}
	}

	@Nested
	public class RatioTest extends StandardTests {
		@Override
		public AssociateDescription<TupleDesc_F64> createAssociate() {
			ScoreAssociateEuclidean_F64 score = new ScoreAssociateEuclidean_F64();
			AssociateGreedy<TupleDesc_F64> greedy = new AssociateGreedy<>(score, false);
			return new WrapAssociateGreedy<>(greedy);
		}

		// Disable this test since it's not compatible with the ratio test
		@Override public void checkSetThreshold(){};
	}

	private static abstract class StandardTests extends StandardAssociateDescriptionChecks<TupleDesc_F64>
	{
		public StandardTests() {
			super(TupleDesc_F64.class);
			distanceIsSquared = false;
		}

		@Override
		protected TupleDesc_F64 c(double value) {
			return createFeature(value);
		}
	}

	private static TupleDesc_F64 createFeature(double value) {
		TupleDesc_F64 s = new TupleDesc_F64(1);
		s.value[0] = value;
		return s;
	}

}
