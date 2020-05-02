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
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.factory.feature.detect.selector.SelectLimitTypes;
import boofcv.factory.feature.disparity.ConfigDisparityBM;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.struct.pyramid.ConfigDiscreteLevels;

/**
 * Configuration for {@link boofcv.abst.sfm.d3.WrapVisOdomPixelDepthPnP}. Stereo visual odometry where features
 * are tracked only in the left camera. The right camera is only used for the initial depth estimate of a new
 * feature and ignored otherwise.
 *
 * @author Peter Abeles
 */
public class ConfigStereoMonoPnP extends ConfigVisOdomTrackPnP {
	/** Tracker configuration for left camera */
	public ConfigPointTracker tracker = new ConfigPointTracker();

	/** Configuration for stereo disparity calculation */
	public ConfigDisparityBM disparity = new ConfigDisparityBM();

	{
		// Give it default settings which should work reasonably well for images from 800x600 to 320x240
		ConfigPointTracker config = new ConfigPointTracker();
		config.klt.toleranceFB = 3;
		config.klt.pruneClose = true;
		config.klt.config.maxIterations = 25;
		config.klt.templateRadius = 4;
		config.klt.pyramidLevels = ConfigDiscreteLevels.minSize(40);

		config.detDesc.typeDetector = ConfigDetectInterestPoint.DetectorType.POINT;
		config.detDesc.detectPoint.type = PointDetectorTypes.SHI_TOMASI;
		config.detDesc.detectPoint.shiTomasi.radius = 3;
		config.detDesc.detectPoint.general.threshold = 1.0f;
		config.detDesc.detectPoint.general.radius = 5;
		config.detDesc.detectPoint.general.maxFeatures = 300;
		config.detDesc.detectPoint.general.selector.type = SelectLimitTypes.BEST_N;
	}

	@Override
	public void checkValidity() {
		super.checkValidity();
		tracker.checkValidity();
		disparity.checkValidity();
	}
}
