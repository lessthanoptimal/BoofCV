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

import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.struct.Configuration;

/**
 * Configuration for visual odometry from RGB-D image using PnP style approach.
 *
 * @author Peter Abeles
 */
public class ConfigRgbDepthTrackPnP implements Configuration {
	/** Configuration for building and optimizing a local scene */
	public ConfigVisOdomTrackPnP scene = new ConfigVisOdomTrackPnP();

	/** Tracker configuration for left camera */
	public ConfigPointTracker tracker = new ConfigPointTracker();

	/** Used to adjust units in the depth image to something more manageable. E.g. millimeters to meters */
	public double depthScale = 1.0;

	@Override public void checkValidity() {
		scene.checkValidity();
		tracker.checkValidity();
	}

	public ConfigRgbDepthTrackPnP setTo( ConfigRgbDepthTrackPnP src ) {
		this.scene.setTo(src.scene);
		this.tracker.setTo(src.tracker);
		this.depthScale = src.depthScale;
		return this;
	}
}
