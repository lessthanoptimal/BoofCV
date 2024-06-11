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

class TestPackedTupleBigArray_S8 extends GenericPackedArrayChecks<TupleDesc_S8> {
	int DOF = 1;

	@Override protected PackedArray<TupleDesc_S8> createAlg() {
		return new PackedTupleBigArray_S8(DOF);
	}

	@Override protected TupleDesc_S8 createRandomPoint() {
		var point = new TupleDesc_S8(DOF);
		point.data[0] = (byte) rand.nextInt(256);
		return point;
	}

	@Override protected void checkEquals( TupleDesc_S8 a, TupleDesc_S8 b ) {
		for (int i = 0; i < DOF; i++) {
			assertEquals(a.data[i], b.data[i]);
		}
	}

	@Override protected void checkNotEquals( TupleDesc_S8 a, TupleDesc_S8 b ) {
		for (int i = 0; i < DOF; i++) {
			if (a.data[i] != b.data[i])
				return;
		}
		fail("The tuples are identical");
	}

	// not implemented yet
	@Test void setTo_Standard(){}
}
