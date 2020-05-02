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

package boofcv.factory.sfm;

import boofcv.abst.feature.detect.interest.PointDetectorTypes;
import boofcv.factory.feature.describe.ConfigDescribeRegionPoint;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.factory.geo.ConfigBundleAdjustment;
import boofcv.factory.geo.EnumPNP;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.struct.Configuration;
import boofcv.struct.pyramid.ConfigDiscreteLevels;

/**
 * Configuration for {@link boofcv.alg.sfm.d3.VisOdomDualTrackPnP}
 *
 * @author Peter Abeles
 */
public class ConfigStereoDualTrackPnP implements Configuration {

	/** Used to track features in each camera independently */
	public ConfigPointTracker tracker = new ConfigPointTracker();

	/** Feature descriptor for stereo association */
	public ConfigDescribeRegionPoint stereoDescribe = new ConfigDescribeRegionPoint();

	// TODO add in fancier sanity checks for stereo association

	/** Tolerance for matching stereo features along epipolar line in Pixels */
	public double epipolarTol = 1.5;

	/** Configuration for Bundle Adjustment */
	public ConfigBundleAdjustment sba = new ConfigBundleAdjustment();

	/** Maximum number of iterations to do with sparse bundle adjustment. &le; 0 to disable. */
	public int bundleIterations = 1;
	/**
	 * Maximum number of features optimized in bundle adjustment per key frame. This is a very good way to limit
	 * the amount of CPU used. If not positive then unlimited. &le; 0 to disable.
	 */
	public int bundleMaxFeaturesPerFrame = 200;
	/**
	 * Minimum number of observations a track must have before it is included in bundle adjustment. Has to be
	 * >= 2 and it's strongly recommended that this is set to 3 or higher. Due to ambiguity along epipolar lines
	 * there can be lots of false positives with just two views. With three views there is a unique solution and that
	 * tends to remove most false positives.
	 */
	public int bundleMinObservations = 3;
	/** Drop tracks if they have been outliers for this many frames in a row */
	public int dropOutlierTracks = 2;
	/** Maximum number of key frames it will save. Must be at least 4*/
	public int maxKeyFrames = 8;
	/** Number of RANSAC iterations to perform when estimating motion using PNP */
	public int ransacIterations = 500;
	/** RANSAC inlier tolerance in Pixels */
	public double ransacInlierTol = 1.5;
	/** Seed for the random number generator used by RANSAC */
	public long ransacSeed = 0xDEADBEEF;
	/** Number of iterations to perform when refining the initial frame-to-frame motion estimate. Disable &le; 0 */
	public int refineIterations = 25;
	/** Which PNP solution to use */
	public EnumPNP pnp = EnumPNP.P3P_GRUNERT;
	/** Specifies when a new key frame is created */
	public ConfigKeyFrameManager keyframes = new ConfigKeyFrameManager();

	{
		tracker.typeTracker = ConfigPointTracker.TrackerType.KLT;
		tracker.klt.pyramidLevels = ConfigDiscreteLevels.minSize(40);
		tracker.klt.pruneClose = true;
		tracker.klt.toleranceFB = 3.0;
		tracker.klt.templateRadius = 4;
		tracker.detDesc.typeDetector = ConfigDetectInterestPoint.DetectorType.POINT;
		tracker.detDesc.detectPoint.type = PointDetectorTypes.SHI_TOMASI;
		tracker.detDesc.detectPoint.shiTomasi.radius = 4;
		tracker.detDesc.detectPoint.general.radius = 5;

		stereoDescribe.type = ConfigDescribeRegionPoint.DescriptorType.BRIEF;
	}

	@Override
	public void checkValidity() {
		if( maxKeyFrames < 4 )
			throw new IllegalArgumentException("There must be at least 4 key frames");

		tracker.checkValidity();
		stereoDescribe.checkValidity();
		keyframes.checkValidity();
	}
}
