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
import boofcv.struct.feature.TupleDesc_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.clustering.ComputeMeanClusters;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.LArrayAccessor;
import pabeles.concurrency.GrowArray;

/**
 * Concurrent implementation of {@link ComputeMeanTuple_F64}
 *
 * @author Peter Abeles
 */
public class ComputeMeanTuple_MT_F64 extends ComputeMeanTuple_F64 {

	/**
	 * Minimum list size for it to use concurrent code. If a list is small it will run slower than the single
	 * thread version. By default this is zero since the optimal value is use case specific.
	 */
	@Getter @Setter int minimumForConcurrent = 0;

	final int tupleDof;

	GrowArray<ThreadData> threadData;

	public ComputeMeanTuple_MT_F64( int numElements) {
		tupleDof = numElements;
		threadData = new GrowArray<>(ThreadData::new);
	}

	@Override public void process( LArrayAccessor<TupleDesc_F64> points,
								   DogArray_I32 assignments,
								   FastAccess<TupleDesc_F64> clusters) {
		// see if it should run the single thread version instead
		if (points.size() < minimumForConcurrent) {
			super.process(points, assignments, clusters);
			return;
		}

		if (assignments.size != points.size())
			throw new IllegalArgumentException("Points and assignments need to be the same size");

		// Compute the sum of all points in each cluster
		BoofConcurrency.loopBlocks(0, points.size(), threadData, (data,idx0,idx1)->{
			final TupleDesc_F64 tuple = data.point;
			final DogArray<TupleDesc_F64> sums = data.clusterSums;
			sums.resize(clusters.size);
			for (int i = 0; i < sums.size; i++) {
				sums.get(i).fill(0.0);
			}
			final DogArray_I32 counts = data.counts;
			counts.resetResize(sums.size, 0);

			for (int pointIdx = idx0; pointIdx < idx1; pointIdx++) {
				points.getCopy(pointIdx, tuple);
				final double[] point = tuple.data;

				int clusterIdx = assignments.get(pointIdx);
				counts.data[clusterIdx]++;
				double[] cluster = sums.get(clusterIdx).data;
				for (int i = 0; i < point.length; i++) {
					cluster[i] += point[i];
				}
			}
		});

		// Stitch results from threads back together
		counts.resetResize(clusters.size, 0);
		for (int i = 0; i < clusters.size; i++) {
			clusters.get(i).fill(0.0);
		}
		for (int threadIdx = 0; threadIdx < threadData.size(); threadIdx++) {
			ThreadData data = threadData.get(threadIdx);
			for (int clusterIdx = 0; clusterIdx < clusters.size; clusterIdx++) {
				TupleDesc_F64 a = data.clusterSums.get(clusterIdx);
				TupleDesc_F64 b = clusters.get(clusterIdx);

				for (int i = 0; i < b.size(); i++) {
					b.data[i] += a.data[i];
				}
				counts.data[clusterIdx] += data.counts.data[clusterIdx];
			}
		}

		// Divide to get the average value in each cluster
		for (int clusterIdx = 0; clusterIdx < clusters.size; clusterIdx++) {
			double[] cluster = clusters.get(clusterIdx).data;
			double divisor = counts.get(clusterIdx);
			for (int i = 0; i < cluster.length; i++) {
				cluster[i] /= divisor;
			}
		}
	}

	@Override public ComputeMeanClusters<TupleDesc_F64> newInstanceThread() {
		return new ComputeMeanTuple_MT_F64(tupleDof);
	}

	class ThreadData {
		TupleDesc_F64 point = new TupleDesc_F64(tupleDof);
		DogArray_I32 counts = new DogArray_I32();
		DogArray<TupleDesc_F64> clusterSums = new DogArray<>(()->new TupleDesc_F64(tupleDof));
	}
}
