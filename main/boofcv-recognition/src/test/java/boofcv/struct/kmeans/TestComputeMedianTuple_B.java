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

import boofcv.struct.PackedArray;
import boofcv.struct.feature.PackedTupleArray_B;
import boofcv.struct.feature.TupleDesc_B;
import org.ddogleg.clustering.ComputeMeanClusters;
import org.ddogleg.struct.DogArray_F64;

/**
 * @author Peter Abeles
 **/
public class TestComputeMedianTuple_B extends GenericComputeMeanClustersChecks<TupleDesc_B> {
	final static int DOF = 44;

	public TestComputeMedianTuple_B() {
		this.tol = 0.5; // in this case the mean/median should be within 0.5 of each other
	}

	@Override public ComputeMeanClusters<TupleDesc_B> createAlg() {
		return new ComputeMedianTuple_B(DOF);
	}

	@Override public PackedArray<TupleDesc_B> createArray() {
		return new PackedTupleArray_B(DOF);
	}

	@Override public void pointToCommonArray( TupleDesc_B src, DogArray_F64 dst ) {
		dst.resize(DOF,0.0);
		for (int i = 0; i < src.size(); i++) {
			dst.set(i, src.isBitTrue(i) ? 1.0 : 0.0);
		}
	}

	@Override public TupleDesc_B randomPoint() {
		var point = new TupleDesc_B(DOF);
		for (int i = 0; i < DOF; i++) {
			point.setBit(i, rand.nextBoolean());
		}
		return point;
	}
}
