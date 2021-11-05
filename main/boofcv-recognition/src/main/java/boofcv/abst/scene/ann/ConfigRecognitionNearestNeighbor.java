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

package boofcv.abst.scene.ann;

import boofcv.alg.scene.bow.BowDistanceTypes;
import boofcv.struct.Configuration;
import org.ddogleg.clustering.ConfigKMeans;
import org.ddogleg.nn.ConfigNearestNeighborSearch;

/**
 * Configuration for {@link FeatureSceneRecognitionNearestNeighbor}.
 *
 * @author Peter Abeles
 */
public class ConfigRecognitionNearestNeighbor implements Configuration {
	/** Clustering algorithm used when learning the words */
	public final ConfigKMeans kmeans = new ConfigKMeans();

	/** Which Nearest Neighbor Algorithm will be used. */
	public final ConfigNearestNeighborSearch nearestNeighbor = new ConfigNearestNeighborSearch();

	/** Number of words in the dictionary */
	public int numberOfWords = 10_000;

	/** Specifies which norm to use. L1 should yield better results but is slower than L2 to compute. */
	public BowDistanceTypes distanceNorm = BowDistanceTypes.L1;

	/** Random number generator seed used when clustering */
	public long randSeed = 0xDEADBEEF;

	{
		// this is the only one that will be fast enough with high DOF feature descriptors
		nearestNeighbor.type = ConfigNearestNeighborSearch.Type.RANDOM_FOREST;
		nearestNeighbor.randomForest.maxNodesSearched = 200;

		// Finding the absolute "best" cluster doesn't seem to improve scoring significantly
		kmeans.reseedAfterIterations = 30;
		kmeans.maxIterations = 30;
		kmeans.maxReSeed = 0;
	}

	@Override public void checkValidity() {
		kmeans.checkValidity();
		nearestNeighbor.checkValidity();
	}

	public ConfigRecognitionNearestNeighbor setTo( ConfigRecognitionNearestNeighbor src ) {
		this.kmeans.setTo(src.kmeans);
		this.nearestNeighbor.setTo(src.nearestNeighbor);
		this.numberOfWords = src.numberOfWords;
		this.distanceNorm = src.distanceNorm;
		this.randSeed = src.randSeed;
		return this;
	}
}
