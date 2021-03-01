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
import boofcv.factory.struct.FactoryTupleDesc;
import boofcv.struct.feature.*;
import org.ddogleg.clustering.ComputeMeanClusters;
import org.ddogleg.clustering.ConfigKMeans;
import org.ddogleg.clustering.FactoryClustering;
import org.ddogleg.clustering.PointDistance;
import org.ddogleg.clustering.kmeans.StandardKMeans;

/**
 * Factory for creating classes related to clustering of {@link TupleDesc} data structures
 *
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class FactoryTupleCluster {
	/**
	 * Creates a K-means clistering algorithm
	 *
	 * @param config Configuration for K-Means clustering
	 * @param minimumForThreads Performance tuning parameter. A single threaded variant is used if the number of descriptors is less than this number.
	 * @param numElements Number of elements in the tuple
	 * @param tupleType Type of tuple
	 * @return Clustering algorithm
	 */
	public static <TD extends TupleDesc<TD>>
	StandardKMeans<TD> kmeans( ConfigKMeans config, int minimumForThreads, int numElements, Class<TD> tupleType ) {
		config.checkValidity();

		if (BoofConcurrency.isUseConcurrent()) {
			return FactoryClustering.kMeans_MT(config, minimumForThreads,
					createMeanClusters(numElements, minimumForThreads, tupleType),
					createDistance(tupleType),
					() -> FactoryTupleDesc.createTuple(numElements, tupleType));
		} else {
			return FactoryClustering.kMeans(config,
					createMeanClusters(numElements, minimumForThreads, tupleType),
					createDistance(tupleType),
					() -> FactoryTupleDesc.createTuple(numElements, tupleType));
		}
	}

	public static <TD extends TupleDesc<TD>>
	ComputeMeanClusters<TD> createMeanClusters( int numElements, int minimumForThreads, Class<TD> type ) {
		if (type == TupleDesc_F64.class) {
			if (BoofConcurrency.isUseConcurrent()) {
				var mean = new ComputeMeanTuple_MT_F64(numElements);
				mean.setMinimumForConcurrent(minimumForThreads);
				return (ComputeMeanClusters<TD>)mean;
			} else {
				return (ComputeMeanClusters<TD>)new ComputeMeanTuple_F64();
			}
		} else if (type == TupleDesc_F32.class) {
			if (BoofConcurrency.isUseConcurrent()) {
				var mean = new ComputeMeanTuple_MT_F32(numElements);
				mean.setMinimumForConcurrent(minimumForThreads);
				return (ComputeMeanClusters<TD>)mean;
			} else {
				return (ComputeMeanClusters<TD>)new ComputeMeanTuple_F32();
			}
		} else if (type == TupleDesc_U8.class) {
			if (BoofConcurrency.isUseConcurrent()) {
				var mean = new ComputeMeanTuple_MT_U8(numElements);
				mean.setMinimumForConcurrent(minimumForThreads);
				return (ComputeMeanClusters<TD>)mean;
			} else {
				return (ComputeMeanClusters<TD>)new ComputeMeanTuple_U8(numElements);
			}
		} else if (type == TupleDesc_B.class) {
//			if (BoofConcurrency.isUseConcurrent()) {
//				var mean = new ComputeMeanTuple_MT_U8(numElements);
//				mean.setMinimumForConcurrent(minimumForThreads);
//				return (ComputeMeanClusters<TD>)mean;
//			} else {
				return (ComputeMeanClusters<TD>)new ComputeMedianTuple_B(numElements);
//			}
		}
		throw new IllegalArgumentException("Unknown");
	}

	public static <TD extends TupleDesc<TD>>
	PointDistance<TD> createDistance(Class<TD> tuple) {
		if (tuple== TupleDesc_F64.class)
			return (PointDistance<TD>)new TuplePointDistanceEuclideanSq.F64();
		else if (tuple == TupleDesc_F32.class)
			return (PointDistance<TD>)new TuplePointDistanceEuclideanSq.F32();
		else if (tuple == TupleDesc_U8.class)
			return (PointDistance<TD>)new TuplePointDistanceEuclideanSq.U8();
		else if (tuple == TupleDesc_B.class)
			return (PointDistance<TD>)new TuplePointDistanceHamming();

		throw new IllegalArgumentException("Add appropriate distance");
	}
}
