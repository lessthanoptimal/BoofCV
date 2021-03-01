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

import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.feature.TupleDesc_B;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.LArrayAccessor;
import pabeles.concurrency.GrowArray;

import java.util.Arrays;

/**
 * Concurrent implementation of {@link ComputeMedianTuple_B}
 *
 * @author Peter Abeles
 */
public class ComputeMedianTuple_MT_B extends ComputeMedianTuple_B {

	/**
	 * Minimum list size for it to use concurrent code. If a list is small it will run slower than the single
	 * thread version. By default this is zero since the optimal value is use case specific.
	 */
	@Getter @Setter int minimumForConcurrent = 0;

	GrowArray<ThreadData> threadData;

	public ComputeMedianTuple_MT_B( int DOF ) {
		super(DOF);

		threadData = new GrowArray<>(ThreadData::new);
	}

	@Override
	protected void countBitsInEachCluster( LArrayAccessor<TupleDesc_B> points, DogArray_I32 assignments ) {
		if (points.size() < minimumForConcurrent) {
			super.countBitsInEachCluster(points, assignments);
			return;
		}
		int numClusters = super.assignmentCounts.size;

		// Compute the sum of all points in each cluster
		BoofConcurrency.loopBlocks(0, points.size(), threadData, ( data, idx0, idx1 ) -> {
			final TupleDesc_B tuple = data.point;
			final DogArray<int[]> bitCounts = data.bitCounts;
			final DogArray_I32 assignmentCounts = data.assignmentCounts;

			assignmentCounts.resize(numClusters, 0);
			bitCounts.resize(numClusters);
			for (int i = 0; i < bitCounts.size; i++) {
				Arrays.fill(bitCounts.data[i], 0);
			}

			// Compute the sum of all points in each cluster
			for (int pointIdx = idx0; pointIdx < idx1; pointIdx++) {
				// See which cluster this point was assigned to and increment its counter
				int clusterIdx = assignments.get(pointIdx);
				assignmentCounts.data[clusterIdx]++;

				points.getCopy(pointIdx, tuple);

				// Increment the counter for each "true" bit in the tuple
				int[] bitCount = bitCounts.get(clusterIdx);
				for (int i = 0; i < dof; i++) {
					if (!tuple.isBitTrue(i))
						continue;
					bitCount[i]++;
				}
			}
		});

		// stitch all the threads back together
		for (int threadIdx = 0; threadIdx < threadData.size(); threadIdx++) {
			ThreadData data = threadData.get(threadIdx);
			for (int clusterIdx = 0; clusterIdx < numClusters; clusterIdx++) {
				super.assignmentCounts.data[clusterIdx] += data.assignmentCounts.data[clusterIdx];

				int[] allCounts = super.bitCounts.get(clusterIdx);
				int[] threadCounts = data.bitCounts.get(clusterIdx);

				for (int bitIdx = 0; bitIdx < dof; bitIdx++) {
					allCounts[bitIdx] += threadCounts[bitIdx];
				}
			}
		}
	}

	class ThreadData {
		DogArray<int[]> bitCounts = new DogArray<>(()->new int[dof]);
		TupleDesc_B point = new TupleDesc_B(dof);
		DogArray_I32 assignmentCounts = new DogArray_I32();
	}
}
