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

package boofcv.alg.feature.associate;

import boofcv.struct.feature.TupleDesc_F64;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public abstract class GenericAssociateGreedyChecks extends BoofStandardJUnit {
	protected abstract AssociateGreedyBase<TupleDesc_F64> createAlgorithm();

	protected abstract void associate( AssociateGreedyBase<TupleDesc_F64> alg,
									   FastAccess<TupleDesc_F64> src, FastAccess<TupleDesc_F64> dst );

	@Test void basic() {
		DogArray<TupleDesc_F64> a = createData(1, 2, 3, 4);
		DogArray<TupleDesc_F64> b = createData(3, 4, 1, 40);

		AssociateGreedyBase<TupleDesc_F64> alg = createAlgorithm();
		alg.setMaxFitError(0.5);

		associate(alg, a, b);

		DogArray_I32 pairs = alg.getPairs();

		assertEquals(2, pairs.get(0));
		assertEquals(-1, pairs.get(1));
		assertEquals(0, pairs.get(2));
		assertEquals(1, pairs.get(3));

		DogArray_F64 fitScore = alg.getFitQuality();

		assertEquals(0, fitScore.get(0), 1e-5);
		assertEquals(0, fitScore.get(2), 1e-5);
		assertEquals(0, fitScore.get(3), 1e-5);
	}

	@Test void maxError() {
		DogArray<TupleDesc_F64> a = createData(1, 2, 3, 4);
		DogArray<TupleDesc_F64> b = createData(3, 4, 1.1, 40);

		// large margin for error
		AssociateGreedyBase<TupleDesc_F64> alg = createAlgorithm();
		alg.setMaxFitError(10);

		associate(alg, a, b);
		assertEquals(2, alg.getPairs().get(1));

		// small margin for error, no association
		alg = createAlgorithm();
		alg.setMaxFitError(0.1);
		associate(alg, a, b);
		assertEquals(-1, alg.getPairs().get(1));
	}

	@Test void backwards() {
		DogArray<TupleDesc_F64> a = createData(1, 2, 3, 8);
		DogArray<TupleDesc_F64> b = createData(3, 4, 1, 10);

		AssociateGreedyBase<TupleDesc_F64> alg = createAlgorithm();
		alg.backwardsValidation = true;
		alg.setMaxFitError(10);

		associate(alg, a, b);

		DogArray_I32 pairs = alg.getPairs();

		assertEquals(2, pairs.get(0));
		assertEquals(-1, pairs.get(1));
		assertEquals(0, pairs.get(2));
		assertEquals(3, pairs.get(3));

		DogArray_F64 fitScore = alg.getFitQuality();

		assertEquals(0, fitScore.get(0), 1e-5);
		assertEquals(0, fitScore.get(2), 1e-5);
		assertEquals(2, fitScore.get(3), 1e-5);
	}

	@Test void ratioTest() {
		// perfect match is a special case
		// [2] is a very good fit and should pass the ratio test, but is not zero
		DogArray<TupleDesc_F64> a = createData(1, 2, 3, 10);
		DogArray<TupleDesc_F64> b = createData(1, 2, 3.01, 4);

		AssociateGreedyBase<TupleDesc_F64> alg = createAlgorithm();

		// ratio test is turned off by default
		associate(alg, a, b);
		DogArray_I32 pairs = alg.getPairs();
		for (int i = 0; i < 4; i++) {
			assertEquals(i, pairs.get(i));
		}

		// set it so that 4 will be rejected
		alg.setRatioTest(0.1);
		associate(alg, a, b);
		pairs = alg.getPairs();
		assertEquals(0, pairs.get(0));
		assertEquals(1, pairs.get(1));
		assertEquals(2, pairs.get(2));
		assertEquals(-1, pairs.get(3));
	}

	protected DogArray<TupleDesc_F64> createData( double... values ) {
		DogArray<TupleDesc_F64> ret = new DogArray<>(() -> new TupleDesc_F64(1));
		ret.resize(values.length);

		for (int i = 0; i < values.length; i++) {
			ret.get(i).setTo(values[i]);
		}

		return ret;
	}
}
