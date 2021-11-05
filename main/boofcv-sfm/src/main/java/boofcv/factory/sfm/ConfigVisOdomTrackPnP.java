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

import boofcv.factory.geo.ConfigBundleAdjustment;
import boofcv.factory.geo.ConfigRansac;
import boofcv.factory.geo.EnumPNP;
import boofcv.misc.ConfigConverge;
import boofcv.struct.Configuration;

/**
 * Base class for visual odometry algorithms based on {@link boofcv.abst.tracker.PointTracker}.
 *
 * @author Peter Abeles
 */
public class ConfigVisOdomTrackPnP implements Configuration {

	/** Configuration for Bundle Adjustment */
	public ConfigBundleAdjustment bundle = new ConfigBundleAdjustment();
	/** Convergence criteria for bundle adjustment. Set max iterations to &le; 0 to disable */
	public ConfigConverge bundleConverge = new ConfigConverge(1e-3, 1e-3, 1);
	/**
	 * Maximum number of features optimized in bundle adjustment per key frame. This is a very good way to limit
	 * the amount of CPU used. If not positive then unlimited. &le; 0 to disable.
	 */
	public int bundleMaxFeaturesPerFrame = 200;
	/**
	 * Minimum number of observations a track must have before it is included in bundle adjustment. Has to be
	 * &ge; 2 and it's strongly recommended that this is set to 3 or higher. Due to ambiguity along epipolar lines
	 * there can be lots of false positives with just two views. With three views there is a unique solution and that
	 * tends to remove most false positives.
	 */
	public int bundleMinObservations = 3;
	/** Drop tracks if they have been outliers for this many frames in a row */
	public int dropOutlierTracks = 2;
	/** Maximum number of key frames it will save. Must be at least 4 */
	public int maxKeyFrames = 5;
	/** Configuration for RANSAC. Used to robustly estimate frame-to-frame motion */
	public ConfigRansac ransac = new ConfigRansac(500, 1.5);
	/** Number of iterations to perform when refining the initial frame-to-frame motion estimate. Disable &le; 0 */
	public int refineIterations = 25;
	/** Which PNP solution to use */
	public EnumPNP pnp = EnumPNP.P3P_GRUNERT;
	/** Specifies when a new key frame is created */
	public ConfigKeyFrameManager keyframes = new ConfigKeyFrameManager();

	@Override
	public void checkValidity() {
		bundleConverge.checkValidity();
		keyframes.checkValidity();

		if (bundleMinObservations < 2)
			throw new IllegalArgumentException("bundleMinObservations must be >= 2");
	}

	public ConfigVisOdomTrackPnP setTo( ConfigVisOdomTrackPnP src ) {
		this.bundle.setTo(src.bundle);
		this.bundleConverge.setTo(src.bundleConverge);
		this.bundleMaxFeaturesPerFrame = src.bundleMaxFeaturesPerFrame;
		this.bundleMinObservations = src.bundleMinObservations;
		this.dropOutlierTracks = src.dropOutlierTracks;
		this.maxKeyFrames = src.maxKeyFrames;
		this.ransac.setTo(src.ransac);
		this.refineIterations = src.refineIterations;
		this.pnp = src.pnp;
		this.keyframes.setTo(src.keyframes);
		return this;
	}
}
