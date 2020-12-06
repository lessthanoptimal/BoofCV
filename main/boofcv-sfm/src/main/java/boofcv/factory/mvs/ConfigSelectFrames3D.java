/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.mvs;

import boofcv.alg.mvs.video.SelectFramesForReconstruction3D;
import boofcv.factory.feature.associate.ConfigAssociate;
import boofcv.factory.feature.describe.ConfigDescribeRegionPoint;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.Configuration;
import boofcv.struct.pyramid.ConfigDiscreteLevels;

/**
 * Configuration for {@link SelectFramesForReconstruction3D}
 */
public class ConfigSelectFrames3D implements Configuration {

	/** Maximum number of "bad" frames it can skip. Larger number requires more memory. */
	public int maxFrameSkip = 5;

	/** Pixel error for what is considered significant motion. */
	public double motionInlier = 8.0;

	/** Catch all value that determines how different two values need to be before they are significant. 0 to 1. */
	public double significantFraction = 0.4;

	/** Minimum number of features in an image before all hope is lost */
	public int minimumFeatures = 20;

	/** Number of iterations it uses when performing robust fitting */
	public int robustIterations = 200;

	/** Configuration for frame-to-frame image tracker */
	public final ConfigPointTracker tracker = new ConfigPointTracker();

	/** Used to describe the area around a feature track */
	public final ConfigDescribeRegionPoint describe = new ConfigDescribeRegionPoint();

	/** Used to associate features between two images when recovering from a bad frame */
	public final ConfigAssociate associate = new ConfigAssociate();

	{
		// we need geometric diversity
		tracker.klt.pruneClose = true;
		// More adaptive to various image sizes
		tracker.klt.pyramidLevels.setTo(ConfigDiscreteLevels.minSize(40));
		// Get rid of more noise
		tracker.klt.toleranceFB = 2.0;
		// Improve stability of KLT tracks
		tracker.klt.templateRadius = 3;
		tracker.klt.config.maxIterations = 25;
		tracker.klt.maximumTracks = 800; // TODO make dependent on image size?
		tracker.typeTracker = ConfigPointTracker.TrackerType.KLT;

		// best compromise between speed and stability
		describe.type = ConfigDescribeRegionPoint.DescriptorType.SURF_STABLE;
	}

	@Override public void checkValidity() {
		BoofMiscOps.checkTrue(maxFrameSkip >= 0);
		BoofMiscOps.checkTrue(motionInlier >= 0);
		BoofMiscOps.checkTrue(significantFraction >= 0 && significantFraction <= 1.0);
		BoofMiscOps.checkTrue(minimumFeatures >= 1);
		BoofMiscOps.checkTrue(robustIterations >= 1);

		tracker.checkValidity();
		describe.checkValidity();
		associate.checkValidity();
	}

	public void setTo( ConfigSelectFrames3D src ) {
		this.maxFrameSkip = src.maxFrameSkip;
		this.motionInlier = src.motionInlier;
		this.significantFraction = src.significantFraction;
		this.minimumFeatures = src.minimumFeatures;
		this.robustIterations = src.robustIterations;
		this.tracker.setTo(src.tracker);
		this.describe.setTo(src.describe);
		this.associate.setTo(src.associate);
	}
}
