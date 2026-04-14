/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

import boofcv.misc.BoofMiscOps;
import boofcv.struct.Configuration;
import boofcv.struct.pyramid.ConfigDiscreteLevels;

/// Configuration class for [EasyPyramidKlt].
public class ConfigEasyKlt implements Configuration {
	/// configuration for low level KLT tracker
	public ConfigKlt klt = new ConfigKlt();

	/// Forwards-Backwards validation tolerance. If set to a value ≥ 0 it will track features from the current
	/// frame to the previous frame and if the difference in location is greater than this amount the track
	/// will be dropped.
	public double toleranceFB = -1.0;

	/// The radius of a feature descriptor in layer. 3 is a reasonable number.
	public int templateRadius = 3;

	/// Specifies the number of layers in the pyramid
	public ConfigDiscreteLevels pyramidLevels = ConfigDiscreteLevels.minSize(40);

	/// If running a concurrent implementations, what's the minimum number of tracks for it to do parallel
	public int concurrentMinimumTracks = 20; // A guess and not tested

	public ConfigEasyKlt() {}

	public ConfigEasyKlt( int templateRadius ) {
		this.templateRadius = templateRadius;
	}

	public static ConfigEasyKlt levels( int levels ) {
		ConfigEasyKlt config = new ConfigEasyKlt();
		config.pyramidLevels = ConfigDiscreteLevels.levels(levels);
		return config;
	}

	@Override
	public void checkValidity() {
		klt.checkValidity();
		pyramidLevels.checkValidity();
		BoofMiscOps.checkTrue(templateRadius >= 0); // 0 = 1 pixel wide. Still technically valid
		// toleranceFB is valid for all values
	}

	public ConfigEasyKlt setTo( ConfigEasyKlt src ) {
		this.klt.setTo(src.klt);
		this.toleranceFB = src.toleranceFB;
		this.templateRadius = src.templateRadius;
		this.pyramidLevels.setTo(src.pyramidLevels);
		this.concurrentMinimumTracks = src.concurrentMinimumTracks;
		return this;
	}

	public ConfigEasyKlt copy() {
		var ret = new ConfigEasyKlt();
		ret.setTo(this);
		return ret;
	}
}
