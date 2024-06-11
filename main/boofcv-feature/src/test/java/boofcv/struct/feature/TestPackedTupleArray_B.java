/*
 * Copyright (c) 2024, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.feature;

import boofcv.struct.PackedArray;
import boofcv.struct.packed.GenericPackedArrayChecks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class TestPackedTupleArray_B extends GenericPackedArrayChecks<TupleDesc_B> {
	int DOF = 32;

	@Override protected PackedArray<TupleDesc_B> createAlg() {
		return new PackedTupleArray_B(DOF);
	}

	@Override protected TupleDesc_B createRandomPoint() {
		var point = new TupleDesc_B(DOF);
		point.data[0] = rand.nextInt();
		return point;
	}

	@Override protected void checkEquals( TupleDesc_B a, TupleDesc_B b ) {
		for (int i = 0; i < a.data.length; i++) {
			assertEquals(a.data[i], b.data[i]);
		}
	}

	@Override protected void checkNotEquals( TupleDesc_B a, TupleDesc_B b ) {
		for (int i = 0; i < a.data.length; i++) {
			if (a.data[i] != b.data[i])
				return;
		}
		fail("The tuples are identical");
	}

	// not implemented yet
	@Test void setTo_Standard(){}
}
