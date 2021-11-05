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

package boofcv.factory.sfm;

import boofcv.factory.geo.ConfigRansac;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.Configuration;

/**
 * Configuration for visual odometry by assuming a flat plane using PnP style approach.
 *
 * @author Peter Abeles
 */
public class ConfigPlanarTrackPnP implements Configuration {
	/** hen the inlier set is less than this number new features are detected */
	public int thresholdAdd;
	/** discard tracks after they have not been in the inlier set for this many updates in a row */
	public int thresholdRetire = 2;
	/** maximum allowed pixel error. Used for determining which tracks are inliers/outliers */
	public double thresholdPixelError = 1.5;

	/** Configuration for RANSAC. Used to robustly estimate frame-to-frame motion */
	public ConfigRansac ransac = new ConfigRansac(500, 1.5);

	/** Tracker configuration for left camera */
	public ConfigPointTracker tracker = new ConfigPointTracker();

	@Override public void checkValidity() {
		tracker.checkValidity();
		ransac.checkValidity();
		BoofMiscOps.checkTrue(thresholdAdd > 0);
		BoofMiscOps.checkTrue(thresholdRetire >= 0);
		BoofMiscOps.checkTrue(thresholdPixelError >= 0);
	}

	public ConfigPlanarTrackPnP setTo( ConfigPlanarTrackPnP src ) {
		this.thresholdAdd = src.thresholdAdd;
		this.thresholdRetire = src.thresholdRetire;
		this.thresholdPixelError = src.thresholdPixelError;
		this.tracker.setTo(src.tracker);
		this.ransac.setTo(src.ransac);
		return this;
	}
}
