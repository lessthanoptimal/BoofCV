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

import boofcv.alg.descriptor.KdTreeTuple_F64;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.struct.DogArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestAssociateNearestNeighbor_MT extends BoofStandardJUnit {
	@Test void compare() {
		DogArray<TupleDesc_F64> dataSrc = TestAssociateGreedyDesc_MT.createData(200);
		DogArray<TupleDesc_F64> dataDst = TestAssociateGreedyDesc_MT.createData(200);

		NearestNeighbor<TupleDesc_F64> exhaustive = FactoryNearestNeighbor.exhaustive(new KdTreeTuple_F64(1));

		AssociateNearestNeighbor<TupleDesc_F64> sequentialAlg =
				new AssociateNearestNeighbor_ST<>(exhaustive, TupleDesc_F64.class);
		AssociateNearestNeighbor<TupleDesc_F64> parallelAlg =
				new AssociateNearestNeighbor_MT<>(exhaustive, TupleDesc_F64.class);

		sequentialAlg.setSource(dataSrc); sequentialAlg.setDestination(dataDst);
		parallelAlg.setSource(dataSrc); parallelAlg.setDestination(dataDst);

		sequentialAlg.associate();
		parallelAlg.associate();

		DogArray<AssociatedIndex> matches0 = sequentialAlg.getMatches();
		DogArray<AssociatedIndex> matches1 = parallelAlg.getMatches();

		assertEquals(matches0.size, matches1.size);

		for (int i = 0; i < matches0.size; i++) {
			AssociatedIndex a = matches0.get(i);
			boolean matched = false;
			for (int j = 0; j < matches1.size; j++) {
				AssociatedIndex b = matches1.get(j);

				if (a.src == b.src && a.dst == b.dst && a.fitScore == b.fitScore) {
					matched = true;
				}
			}
			assertTrue(matched);
		}
	}
}