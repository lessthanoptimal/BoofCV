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
import boofcv.factory.feature.detect.selector.ConfigSelectLimit;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ConfigLength;
import boofcv.struct.Configuration;
import boofcv.struct.pyramid.ConfigDiscreteLevels;

/**
 * Configuration for {@link SelectFramesForReconstruction3D}
 */
public class ConfigSelectFrames3D implements Configuration {

	/**
	 * Number of recent frames it will save and consider when it needs to select a new key frame.
	 * 0=current frame only.
	 */
	public int historyLength = 5;

	/** Error for what is considered significant motion. Increase to skip for frames. Units: Pixels */
	public double motionInlierPx = 2.0;

	/**
	 * Ratio of outliers over all points. Used for quickly testing to see if there could be
	 * 3D motion. 0.0 = always true for 3D, 1.0 means 100% outliers for it to be 3D.
	 */
	public double thresholdQuick = 0.1;

	/** How much better 3D needs to be than homography. 0.0 = equal or worse. 1.0 = 2x better. 2.0 = 3x better. */
	public double threshold3D = 0.5;

	/** Minimum number of features in an image before all hope is lost */
	public int minimumPairs = 100;

	/** Number of iterations it uses when performing robust fitting */
	public int robustIterations = 50;

	/**
	 * How much more numerous associated features need to be than tracks to be considered better. A value less than
	 * 1.0 turn off this check. 1.5 means 50% better.
	 */
	public double skipEvidenceRatio = 1.5;

	/** A new keyframe can't be made until the motion is greater than this. Relative to max(width,height) */
	public final ConfigLength minTranslation = ConfigLength.relative(0.01, 0);

	/** Force keyframe if motion is more than this pixels. Relative to max(width,height) */
	public final ConfigLength maxTranslation = ConfigLength.relative(0.15, 20);

	/** Radius of the region used to compute the description. Might be ignored by descriptor. */
	public final ConfigLength featureRadius = ConfigLength.fixed(10);

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
		tracker.klt.maximumTracks.setRelative(0.002,800);
		tracker.typeTracker = ConfigPointTracker.TrackerType.KLT;
		// force detection to be spread out some
		tracker.detDesc.detectPoint.general.selector.setTo(ConfigSelectLimit.selectUniform(3.0));

		// best compromise between speed and stability
		describe.type = ConfigDescribeRegionPoint.DescriptorType.SURF_STABLE;
		// Video sequence, improve results by assuming there are not huge jumps in location
		associate.maximumDistancePixels.setRelative(0.25, 50);
		associate.greedy.forwardsBackwards = true;
		associate.greedy.scoreRatioThreshold = 0.9;
		associate.type = ConfigAssociate.AssociationType.GREEDY;
	}

	@Override public void checkValidity() {
		BoofMiscOps.checkTrue(historyLength >= 0);
		BoofMiscOps.checkTrue(motionInlierPx >= 0);
		BoofMiscOps.checkTrue(thresholdQuick >= 0.0 && thresholdQuick <= 1.0);
		BoofMiscOps.checkTrue(threshold3D >= 0.0);
		BoofMiscOps.checkTrue(minimumPairs >= 1);
		BoofMiscOps.checkTrue(robustIterations >= 1);
		BoofMiscOps.checkTrue(skipEvidenceRatio >= 0);

		minTranslation.checkValidity();
		maxTranslation.checkValidity();
		featureRadius.checkValidity();

		tracker.checkValidity();
		describe.checkValidity();
		associate.checkValidity();
	}

	public void setTo( ConfigSelectFrames3D src ) {
		this.historyLength = src.historyLength;
		this.motionInlierPx = src.motionInlierPx;
		this.thresholdQuick = src.thresholdQuick;
		this.threshold3D = src.threshold3D;
		this.minimumPairs = src.minimumPairs;
		this.robustIterations = src.robustIterations;
		this.skipEvidenceRatio = src.skipEvidenceRatio;
		this.minTranslation.setTo(src.minTranslation);
		this.maxTranslation.setTo(src.maxTranslation);
		this.featureRadius.setTo(src.featureRadius);
		this.tracker.setTo(src.tracker);
		this.describe.setTo(src.describe);
		this.associate.setTo(src.associate);
	}
}
