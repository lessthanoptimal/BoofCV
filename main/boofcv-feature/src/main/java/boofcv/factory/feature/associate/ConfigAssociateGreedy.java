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

import boofcv.alg.feature.associate.AssociateGreedyDesc;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link AssociateGreedyDesc}.
 *
 * @author Peter Abeles
 */
public class ConfigAssociateGreedy implements Configuration {

	/**
	 * If true then for a match to be accepted the two features must be each others mutually best match when
	 * associating in the forwards and backwards direction.
	 */
	public boolean forwardsBackwards = true;

	/**
	 * An association is only accepted if the ratio between the second best and best score is less than this value.
	 * Closer to zero is more strict and closer to 1.0 is less strict. Set to a value &ge; 1.0 to disable.
	 */
	public double scoreRatioThreshold = 1.0;

	/**
	 * If more than zero then this is the maximum allowed error/distance between two features for a match to
	 * be accepted
	 */
	public double maxErrorThreshold = -1.0;

	public ConfigAssociateGreedy( boolean forwardsBackwards, double maxErrorThreshold ) {
		this.forwardsBackwards = forwardsBackwards;
		this.maxErrorThreshold = maxErrorThreshold;
	}

	public ConfigAssociateGreedy( boolean forwardsBackwards, double scoreRatioThreshold, double maxErrorThreshold ) {
		this.forwardsBackwards = forwardsBackwards;
		this.scoreRatioThreshold = scoreRatioThreshold;
		this.maxErrorThreshold = maxErrorThreshold;
	}

	public ConfigAssociateGreedy( boolean forwardsBackwards ) {
		this.forwardsBackwards = forwardsBackwards;
	}

	public ConfigAssociateGreedy() {}

	@Override public void checkValidity() {
		if (scoreRatioThreshold < 0.0)
			throw new IllegalArgumentException("scoreRatioThreshold must be greater than or equal to 0");
	}

	public ConfigAssociateGreedy setTo( ConfigAssociateGreedy src ) {
		this.forwardsBackwards = src.forwardsBackwards;
		this.scoreRatioThreshold = src.scoreRatioThreshold;
		this.maxErrorThreshold = src.maxErrorThreshold;
		return this;
	}
}
