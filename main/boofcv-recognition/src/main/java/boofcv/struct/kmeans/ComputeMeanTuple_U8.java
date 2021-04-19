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

import boofcv.struct.feature.TupleDesc_U8;
import org.ddogleg.clustering.ComputeMeanClusters;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.LArrayAccessor;

import java.util.Arrays;

/**
 * Update cluster assignments for {@link TupleDesc_U8} descriptors.
 *
 * @author Peter Abeles
 */
public class ComputeMeanTuple_U8 implements ComputeMeanClusters<TupleDesc_U8> {
	final DogArray_I32 counts = new DogArray_I32();
	final DogArray<int[]> means;
	final int DOF;

	public ComputeMeanTuple_U8( int DOF ) {
		this.means = new DogArray<>(()->new int[DOF]);
		this.DOF = DOF;
	}

	@Override public void process( LArrayAccessor<TupleDesc_U8> points,
								   DogArray_I32 assignments,
								   FastAccess<TupleDesc_U8> clusters) {

		if (assignments.size != points.size())
			throw new IllegalArgumentException("Points and assignments need to be the same size");

		// set the number of points in each cluster to zero and zero the clusters
		counts.resetResize(clusters.size, 0);
		means.resize(clusters.size);
		for (int i = 0; i < means.size; i++) {
			Arrays.fill(means.get(i),0);
		}

		// Compute the sum of all points in each cluster
		for (int pointIdx = 0; pointIdx < points.size(); pointIdx++) {
			byte[] point = points.getTemp(pointIdx).data;

			int clusterIdx = assignments.get(pointIdx);
			counts.data[clusterIdx]++;
			int[] cluster = means.get(clusterIdx);
			for (int i = 0; i < point.length; i++) {
				cluster[i] += point[i]&0xFF;
			}
		}

		// Divide to get the average value in each cluster
		for (int clusterIdx = 0; clusterIdx < clusters.size; clusterIdx++) {
			int[] mean = means.get(clusterIdx);
			byte[] cluster = clusters.get(clusterIdx).data;
			double divisor = counts.get(clusterIdx);
			for (int i = 0; i < mean.length; i++) {
				cluster[i] = (byte)(mean[i]/divisor);
			}
		}
	}

	@Override public ComputeMeanClusters<TupleDesc_U8> newInstanceThread() {
		return new ComputeMeanTuple_U8(DOF);
	}
}
