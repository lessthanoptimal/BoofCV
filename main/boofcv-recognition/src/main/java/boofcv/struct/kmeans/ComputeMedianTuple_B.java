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
		this.bitCounts = new DogArray<>(() -> new int[DOF]);
		this.dof = DOF;
	}

	@Override public void process( LArrayAccessor<TupleDesc_B> points,
								   DogArray_I32 assignments,
								   FastAccess<TupleDesc_B> clusters ) {

		if (assignments.size != points.size())
			throw new IllegalArgumentException("Points and assignments need to be the same size");

		// set the number of points in each cluster to zero and zero the clusters
		assignmentCounts.resetResize(clusters.size, 0);
		bitCounts.resize(clusters.size);
		for (int i = 0; i < bitCounts.size; i++) {
			Arrays.fill(bitCounts.get(i), 0);
		}

		countBitsInEachCluster(points, assignments);

		countsToBits(clusters);
	}

	/**
	 * Goes through each point and counts the number of bits are true in each cluster its assigned to
	 */
	protected void countBitsInEachCluster( LArrayAccessor<TupleDesc_B> points, DogArray_I32 assignments ) {
		// Compute the sum of all points in each cluster
		for (int pointIdx = 0; pointIdx < points.size(); pointIdx++) {
			// See which cluster this point was assigned to and increment its counter
			int clusterIdx = assignments.get(pointIdx);
			assignmentCounts.data[clusterIdx]++;

			TupleDesc_B tuple = points.getTemp(pointIdx);

			// Increment the counter for each "true" bit in the tuple
			int[] bitCount = bitCounts.get(clusterIdx);
			int bit = 0;
			while (bit + 32 < dof) {
				// Unroll for speed
				int value = tuple.data[bit/32];
				if ((value & 0x00000001) != 0) bitCount[bit]++;
				if ((value & 0x00000002) != 0) bitCount[bit + 1]++;
				if ((value & 0x00000004) != 0) bitCount[bit + 2]++;
				if ((value & 0x00000008) != 0) bitCount[bit + 3]++;
				if ((value & 0x00000010) != 0) bitCount[bit + 4]++;
				if ((value & 0x00000020) != 0) bitCount[bit + 5]++;
				if ((value & 0x00000040) != 0) bitCount[bit + 6]++;
				if ((value & 0x00000080) != 0) bitCount[bit + 7]++;
				if ((value & 0x00000100) != 0) bitCount[bit + 8]++;
				if ((value & 0x00000200) != 0) bitCount[bit + 9]++;
				if ((value & 0x00000400) != 0) bitCount[bit + 10]++;
				if ((value & 0x00000800) != 0) bitCount[bit + 11]++;
				if ((value & 0x00001000) != 0) bitCount[bit + 12]++;
				if ((value & 0x00002000) != 0) bitCount[bit + 13]++;
				if ((value & 0x00004000) != 0) bitCount[bit + 14]++;
				if ((value & 0x00008000) != 0) bitCount[bit + 15]++;
				if ((value & 0x00010000) != 0) bitCount[bit + 16]++;
				if ((value & 0x00020000) != 0) bitCount[bit + 17]++;
				if ((value & 0x00040000) != 0) bitCount[bit + 18]++;
				if ((value & 0x00080000) != 0) bitCount[bit + 19]++;
				if ((value & 0x00100000) != 0) bitCount[bit + 20]++;
				if ((value & 0x00200000) != 0) bitCount[bit + 21]++;
				if ((value & 0x00400000) != 0) bitCount[bit + 22]++;
				if ((value & 0x00800000) != 0) bitCount[bit + 23]++;
				if ((value & 0x01000000) != 0) bitCount[bit + 24]++;
				if ((value & 0x02000000) != 0) bitCount[bit + 25]++;
				if ((value & 0x04000000) != 0) bitCount[bit + 26]++;
				if ((value & 0x08000000) != 0) bitCount[bit + 27]++;
				if ((value & 0x10000000) != 0) bitCount[bit + 28]++;
				if ((value & 0x20000000) != 0) bitCount[bit + 29]++;
				if ((value & 0x40000000) != 0) bitCount[bit + 30]++;
				if ((value & 0x80000000) != 0) bitCount[bit + 31]++;
				bit += 32;
			}
			// handle the remainder if it doesn't align with 32-bit integers
			for (; bit < dof; bit++) {
				if (!tuple.isBitTrue(bit))
					continue;
				bitCount[bit]++;
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
				cluster.setBit(i, bitCount[i] > threshold);
			}
		}
	}

	@Override public ComputeMeanClusters<TupleDesc_B> newInstanceThread() {
		return new ComputeMedianTuple_B(dof);
	}
}
