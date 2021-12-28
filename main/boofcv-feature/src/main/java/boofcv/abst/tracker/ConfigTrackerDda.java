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

import boofcv.alg.tracker.dda.DetectDescribeAssociateTracker;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link DetectDescribeAssociateTracker}
 *
 * @author Peter Abeles
 */
public class ConfigTrackerDda implements Configuration {
	/** Update the description each time its successfully matched? */
	public boolean updateDescription = false;
	/**
	 * If there are more than this number of inactive tracks then inactive tracks will be randomly discarded until
	 * there is this many left.
	 */
	public int maxInactiveTracks = 500;

	/** Random seed */
	public long seed = 0xDEADBEEF;

	@Override public void checkValidity() {}

	public ConfigTrackerDda setTo( ConfigTrackerDda src ) {
		this.updateDescription = src.updateDescription;
		this.maxInactiveTracks = src.maxInactiveTracks;
		this.seed = src.seed;
		return this;
	}

	public ConfigTrackerDda copy() {
		return new ConfigTrackerDda().setTo(this);
	}
}
