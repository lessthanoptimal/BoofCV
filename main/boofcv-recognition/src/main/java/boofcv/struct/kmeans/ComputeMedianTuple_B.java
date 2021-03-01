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
import org.ddogleg.clustering.ComputeMeanClusters;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.LArrayAccessor;

import java.util.Arrays;

/**
 * Update cluster assignments for {@link TupleDesc_B} descriptors.
 *
 * @author Peter Abeles
 */
public class ComputeMedianTuple_B implements ComputeMeanClusters<TupleDesc_B> {
	// Number of times each label was seen
	protected final DogArray_I32 assignmentCounts = new DogArray_I32();
	// Number of times each bit was 1
	protected final DogArray<int[]> bitCounts;
	// degree-of-freedom Number of elements in the tuple
	protected final int dof;

	public ComputeMedianTuple_B( int DOF ) {
		this.bitCounts = new DogArray<>(()->new int[DOF]);
		this.dof = DOF;
	}

	@Override public void process( LArrayAccessor<TupleDesc_B> points,
								   DogArray_I32 assignments,
								   FastAccess<TupleDesc_B> clusters) {

		if (assignments.size != points.size())
			throw new IllegalArgumentException("Points and assignments need to be the same size");

		// set the number of points in each cluster to zero and zero the clusters
		assignmentCounts.resize(clusters.size, 0);
		bitCounts.resize(clusters.size);
		for (int i = 0; i < bitCounts.size; i++) {
			Arrays.fill(bitCounts.get(i),0);
		}

		countBitsInEachCluster(points, assignments);

		countsToBits(clusters);
	}

	protected void countBitsInEachCluster( LArrayAccessor<TupleDesc_B> points, DogArray_I32 assignments ) {
		// Compute the sum of all points in each cluster
		for (int pointIdx = 0; pointIdx < points.size(); pointIdx++) {
			// See which cluster this point was assigned to and increment its counter
			int clusterIdx = assignments.get(pointIdx);
			assignmentCounts.data[clusterIdx]++;

			TupleDesc_B tuple = points.getTemp(pointIdx);

			// Increment the counter for each "true" bit in the tuple
			int[] bitCount = bitCounts.get(clusterIdx);
			for (int i = 0; i < dof; i++) {
				if (!tuple.isBitTrue(i))
					continue;
				bitCount[i]++;
			}
		}
	}

	protected void countsToBits( FastAccess<TupleDesc_B> clusters ) {
		// If 50% of a bit was observed to be true for a cluster, set that bit to true
		for (int clusterIdx = 0; clusterIdx < clusters.size; clusterIdx++) {
			int[] bitCount = bitCounts.get(clusterIdx);
			// If more than 1/2 the points in this cluster had a positive value for a bit then the output will be 1
			int threshold = assignmentCounts.get(clusterIdx)/2;

			TupleDesc_B cluster = clusters.get(clusterIdx);
			// shouldn't be necessary, but this way we know if there are extra bits in the array they are all zero
			Arrays.fill(cluster.data,0);
			for (int i = 0; i < dof; i++) {
				cluster.setBit(i,bitCount[i]>threshold);
			}
		}
	}

	@Override public ComputeMeanClusters<TupleDesc_B> newInstanceThread() {
		return new ComputeMedianTuple_B(dof);
	}
}
