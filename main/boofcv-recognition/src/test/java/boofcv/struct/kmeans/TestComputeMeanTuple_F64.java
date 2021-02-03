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

import boofcv.struct.feature.TupleDesc_F64;
import org.ddogleg.clustering.ComputeMeanClusters;
import org.ddogleg.struct.DogArray_F64;

/**
 * @author Peter Abeles
 */
class TestComputeMeanTuple_F64 extends GenericComputeMeanClustersChecks<TupleDesc_F64> {

	int DOF = 3;

	@Override public ComputeMeanClusters<TupleDesc_F64> createAlg() {
		return new ComputeMeanTuple_F64();
	}

	@Override public PackedArray<TupleDesc_F64> createArray() {
		return new PackedTupleArray_F64(DOF);
	}

	@Override public void pointToDoubleArray( TupleDesc_F64 src, DogArray_F64 dst ) {
		dst.resize(DOF);
		for (int i = 0; i < DOF; i++) {
			dst.set(i, dst.get(i));
		}
	}

	@Override public TupleDesc_F64 randomPoint() {
		return new TupleDesc_F64(DOF);
	}
}