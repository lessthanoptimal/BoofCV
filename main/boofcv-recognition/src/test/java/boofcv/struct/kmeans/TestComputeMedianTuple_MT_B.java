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

package boofcv.struct.kmeans;

import boofcv.struct.feature.TupleDesc_B;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.clustering.misc.ListAccessor;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestComputeMedianTuple_MT_B extends BoofStandardJUnit {
	@Test void compare() {
		int DOF = 510;
		int numClusters = 4;

		// Create a list of random points
		List<TupleDesc_B> list = new ArrayList<>();
		var assignments = new DogArray_I32(1000);
		for (int i = 0; i < 1000; i++) {
			assignments.add(rand.nextInt(numClusters));
			var t = new TupleDesc_B(DOF);
			for (int j = 0; j < t.data.length; j++) {
				t.data[j] = rand.nextInt();
			}
			list.add(t);
		}
		var points = new ListAccessor<>(list, ( src, dst)->dst.setTo(src), TupleDesc_B.class);

		var clustersSingle = new DogArray<>(()->new TupleDesc_B(DOF));
		var clustersMulti = new DogArray<>(()->new TupleDesc_B(DOF));
		clustersSingle.resize(numClusters);
		clustersMulti.resize(numClusters);

		var single = new ComputeMedianTuple_B(DOF);
		var multi = new ComputeMedianTuple_MT_B(DOF);

		single.process(points, assignments, clustersSingle);
		multi.process(points, assignments, clustersMulti);

		assertEquals(clustersSingle.size, clustersMulti.size);
		for (int i = 0; i < numClusters; i++) {
			assertArrayEquals(clustersSingle.get(i).data, clustersMulti.get(i).data);
		}
	}
}
