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

package boofcv.abst.tracker;

import boofcv.struct.ConfigLength;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link PointTrackerHybrid}.
 *
 * @author Peter Abeles
 */
public class ConfigTrackerHybrid implements Configuration {
	/**
	 * KLT tracks can drift to being on top of each other. This will prune a few if that happens
	 */
	public boolean pruneCloseTracks = false;

	/**
	 * If there are more than this number of unused tracks they will be randomly discarded. This is intended to
	 * prevent unbounded growth of unused tracks even if tracks are not explicity pruned.
	 */
	public int maxInactiveTracks = 500;

	/**
	 * It will attempt to respawn old dropped tracks when the number of active tracks drops below this value.
	 * The fractional part is relative to the number of tracks after the last spawn or most recent respawn.
	 */
	public ConfigLength thresholdRespawn = ConfigLength.relative(0.4, 50);

	/** Random seed */
	public long seed = 0xDEADBEEF;

	@Override public void checkValidity() {}

	public ConfigTrackerHybrid setTo( ConfigTrackerHybrid src ) {
		this.pruneCloseTracks = src.pruneCloseTracks;
		this.maxInactiveTracks = src.maxInactiveTracks;
		this.seed = src.seed;
		this.thresholdRespawn.setTo(src.thresholdRespawn);
		return this;
	}

	public ConfigTrackerHybrid copy() {
		return new ConfigTrackerHybrid().setTo(this);
	}
}
