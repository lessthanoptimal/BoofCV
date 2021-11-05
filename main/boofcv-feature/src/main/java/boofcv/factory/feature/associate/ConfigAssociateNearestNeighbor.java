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

package boofcv.factory.feature.associate;

import boofcv.alg.feature.associate.AssociateNearestNeighbor_ST;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link AssociateNearestNeighbor_ST}.
 *
 * @author Peter Abeles
 */
public class ConfigAssociateNearestNeighbor implements Configuration {
	/**
	 * If true then the score which represents the distance between two features is squared. If this flag
	 * is true then when the score ratio is computed the square root will be used.
	 */
	public boolean distanceIsSquared = true;
	// should be not be in the config? seems very error prone. have flag in the score function

	/**
	 * If less than one then the best two matches are found the ratio is defined as the distance of the best
	 * divided by the distance of the second best. Matches are only accepted if less than this ratio
	 */
	public double scoreRatioThreshold = 0.8;

	/**
	 * If more than zero then this is the maximum allowed error/distance between two features for a match to
	 * be accepted
	 */
	public double maxErrorThreshold = -1;

	/**
	 * The maximum number of nodes it will search in a KD-Tree. Setting a limit will improve speed at the cost
	 * of accuracy.
	 */
	public int maxNodesSearched = Integer.MAX_VALUE;

	@Override
	public void checkValidity() {
		if (scoreRatioThreshold <= 0)
			throw new IllegalArgumentException("Ratio must be more than zero");
	}

	public ConfigAssociateNearestNeighbor setTo( ConfigAssociateNearestNeighbor src ) {
		this.distanceIsSquared = src.distanceIsSquared;
		this.scoreRatioThreshold = src.scoreRatioThreshold;
		this.maxErrorThreshold = src.maxErrorThreshold;
		this.maxNodesSearched = src.maxNodesSearched;
		return this;
	}
}
