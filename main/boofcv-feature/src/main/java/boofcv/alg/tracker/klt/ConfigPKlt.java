/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.tracker.klt;

import boofcv.struct.Configuration;

/**
 * Configuration class for {@link PyramidKltTracker}.
 *
 * @author Peter Abeles
 */
public class ConfigPKlt implements Configuration
{
	/** configuration for low level KLT tracker */
	public ConfigKlt config = new ConfigKlt();

	/**
	 * Forwards-Backwards validation tolerance. If set to a value &ge; 0 it will track features from the current
	 * frame to the previous frame and if the difference in location is greater than this amount the track
	 * will be dropped.
	 */
	public double toleranceFB = -1.0;

	/** The radius of a feature descriptor in layer. 2 is a reasonable number. */
	public int templateRadius = 2;

	/** Scale factor for each layer in the pyramid */
	public int pyramidScaling[] = new int[]{1,2,4};

	/**
	 * If true it will prune tracks which come too close to each other. The default behavior is to
	 * prune tracks will higher feature IDs.
	 */
	public boolean pruneClose=false;

	public ConfigPKlt() {
	}

	public ConfigPKlt(int templateRadius) {
		this.templateRadius = templateRadius;
	}

	public ConfigPKlt(int templateRadius, int[] pyramidScaling) {
		this.templateRadius = templateRadius;
		this.pyramidScaling = pyramidScaling;
	}

	@Override
	public void checkValidity() {

	}
}
