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
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.struct.pyramid.ConfigDiscreteLevels;

/**
 * Configuration for {@link boofcv.alg.sfm.d3.VisOdomDualTrackPnP}
 *
 * @author Peter Abeles
 */
public class ConfigStereoDualTrackPnP extends ConfigVisOdomTrackPnP
{
	/** Used to track features in each camera independently */
	public ConfigPointTracker tracker = new ConfigPointTracker();

	/** Feature descriptor for stereo association */
	public ConfigDescribeRegionPoint stereoDescribe = new ConfigDescribeRegionPoint();
	// TODO add in fancier sanity checks for stereo association

	/** Radius used when computing feature descriptors for stereo matching */
	public double stereoRadius = 11.0;

	/** Tolerance for matching stereo features along epipolar line in Pixels */
	public double epipolarTol = 1.5;

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
		stereoDescribe.brief.fixed = true;
	}

	@Override
	public void checkValidity() {
		super.checkValidity();
		if( maxKeyFrames < 4 )
			throw new IllegalArgumentException("There must be at least 4 key frames");

		tracker.checkValidity();
		stereoDescribe.checkValidity();
	}
}
