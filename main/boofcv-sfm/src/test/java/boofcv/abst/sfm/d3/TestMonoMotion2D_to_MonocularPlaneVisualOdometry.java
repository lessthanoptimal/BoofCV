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

package boofcv.abst.sfm.d3;

import boofcv.abst.feature.detect.interest.PointDetectorTypes;
import boofcv.factory.sfm.ConfigPlanarTrackPnP;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.struct.image.GrayU8;
import boofcv.struct.pyramid.ConfigDiscreteLevels;

/**
 * @author Peter Abeles
 */
public class TestMonoMotion2D_to_MonocularPlaneVisualOdometry extends CheckVisualOdometryMonoPlaneSim<GrayU8> {

	public TestMonoMotion2D_to_MonocularPlaneVisualOdometry() {
		super(GrayU8.class, -20, 0.04);   // angle selected to include ground points and points far away
		setAlgorithm(createAlgorithm());
	}

	protected MonocularPlaneVisualOdometry<GrayU8> createAlgorithm() {
		var config = new ConfigPlanarTrackPnP();
		config.tracker.typeTracker = ConfigPointTracker.TrackerType.KLT;
		config.tracker.klt.pyramidLevels = ConfigDiscreteLevels.levels(4);
		config.tracker.klt.templateRadius = 3;

		config.tracker.detDesc.detectPoint.type = PointDetectorTypes.SHI_TOMASI;
		config.tracker.detDesc.detectPoint.general.maxFeatures = 600;
		config.tracker.detDesc.detectPoint.general.radius = 3;
		config.tracker.detDesc.detectPoint.general.threshold = 1;

		config.thresholdAdd = 50;
		config.thresholdRetire = 2;
		config.ransac.iterations = 300;
		config.ransac.inlierThreshold = 1.5;

		return FactoryVisualOdometry.monoPlaneInfinity(config, GrayU8.class);
	}
}
