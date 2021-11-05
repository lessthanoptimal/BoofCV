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

package boofcv.alg.tracker.klt;

import boofcv.misc.BoofMiscOps;
import boofcv.struct.ConfigLength;
import boofcv.struct.Configuration;
import boofcv.struct.pyramid.ConfigDiscreteLevels;

/**
 * Configuration class for {@link PyramidKltTracker}.
 *
 * @author Peter Abeles
 */
public class ConfigPKlt implements Configuration {
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

	/** Specifies the number of layers in the pyramid */
	public ConfigDiscreteLevels pyramidLevels = ConfigDiscreteLevels.minSize(40);

	/**
	 * If true it will prune tracks which come too close to each other. The default behavior is to
	 * prune tracks will higher feature IDs.
	 */
	public boolean pruneClose = false;

	/**
	 * Specifies the maximum number of features it can track. If fixed at 0 then there is no limit. If relative
	 * then it's relative to the total number of pixels in the image.
	 *
	 * <p>NOTE: {@link boofcv.abst.tracker.PointTrackerKltPyramid} will manage the number of detections and will
	 * override {@link boofcv.abst.feature.detect.interest.ConfigGeneralDetector#maxFeatures}.</p>
	 */
	public ConfigLength maximumTracks = ConfigLength.relative(0.002, 50);

	public ConfigPKlt() {}

	public ConfigPKlt( int templateRadius ) {
		this.templateRadius = templateRadius;
	}

	public static ConfigPKlt levels( int levels ) {
		ConfigPKlt config = new ConfigPKlt();
		config.pyramidLevels = ConfigDiscreteLevels.levels(levels);
		return config;
	}

	@Override
	public void checkValidity() {
		config.checkValidity();
		pyramidLevels.checkValidity();
		maximumTracks.checkValidity();
		BoofMiscOps.checkTrue(templateRadius >= 0); // 0 = 1 pixel wide. Still technically valid
		// toleranceFB is valid for all values
	}

	public ConfigPKlt setTo( ConfigPKlt src ) {
		this.config.setTo(src.config);
		this.toleranceFB = src.toleranceFB;
		this.templateRadius = src.templateRadius;
		this.pyramidLevels.setTo(src.pyramidLevels);
		this.pruneClose = src.pruneClose;
		this.maximumTracks.setTo(src.maximumTracks);
		return this;
	}

	public ConfigPKlt copy() {
		var ret = new ConfigPKlt();
		ret.setTo(this);
		return ret;
	}
}
