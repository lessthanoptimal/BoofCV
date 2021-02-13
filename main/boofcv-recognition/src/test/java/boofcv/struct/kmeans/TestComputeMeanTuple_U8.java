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

//CUSTOM ignore org.ddogleg.struct.DogArray_F64;

import boofcv.struct.PackedArray;
import boofcv.struct.feature.PackedTupleArray_U8;
import boofcv.struct.feature.TupleDesc_U8;
import org.ddogleg.clustering.ComputeMeanClusters;
import org.ddogleg.struct.DogArray_F64;

/**
 * @author Peter Abeles
 */
class TestComputeMeanTuple_U8 extends GenericComputeMeanClustersChecks<TupleDesc_U8> {

	public TestComputeMeanTuple_U8() {
		tol = 1;
	}

	@Override public ComputeMeanClusters<TupleDesc_U8> createAlg() {
		return new ComputeMeanTuple_U8(DOF);
	}

	@Override public PackedArray<TupleDesc_U8> createArray() {
		return new PackedTupleArray_U8(DOF);
	}

	@Override public void pointToCommonArray( TupleDesc_U8 src, /**/DogArray_F64 dst ) {
		dst.resize(DOF);
		for (int i = 0; i < DOF; i++) {
			dst.set(i, src.get(i));
		}
	}

	@Override public TupleDesc_U8 randomPoint() {
		var tuple = new TupleDesc_U8(DOF);
		for (int i = 0; i < DOF; i++) {
			tuple.data[i] = (byte)rand.nextInt();
		}
		return tuple;
	}
}
