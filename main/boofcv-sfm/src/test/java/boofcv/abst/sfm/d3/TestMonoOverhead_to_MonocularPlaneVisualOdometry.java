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

package boofcv.abst.sfm.d3;

import boofcv.abst.feature.detect.interest.ConfigPointDetector;
import boofcv.abst.feature.detect.interest.PointDetectorTypes;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.tracker.klt.ConfigPKlt;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.ConfigDiscreteLevels;

/**
 * @author Peter Abeles
 */
public class TestMonoOverhead_to_MonocularPlaneVisualOdometry extends CheckVisualOdometryMonoPlaneSim<GrayU8> {

	public TestMonoOverhead_to_MonocularPlaneVisualOdometry() {
		super(GrayU8.class, -20, 0.04);

		setAlgorithm(createAlgorithm());
	}

	protected MonocularPlaneVisualOdometry<GrayU8> createAlgorithm() {

		ConfigPKlt config = new ConfigPKlt();
		config.pyramidLevels = ConfigDiscreteLevels.levels(4);
		config.templateRadius = 3;
		ConfigPointDetector configDetector = new ConfigPointDetector();
		configDetector.type = PointDetectorTypes.SHI_TOMASI;
		configDetector.general.maxFeatures = 600;
		configDetector.general.radius = 3;
		configDetector.general.threshold = 1;

		PointTracker<GrayU8> tracker = FactoryPointTracker.klt(config, configDetector,
				GrayU8.class, GrayS16.class);

		double cellSize = 0.015;
		double ransacTol = 0.2;

		return FactoryVisualOdometry.monoPlaneOverhead(
				cellSize, 25, 0.5, ransacTol, 300, 2, 30, 0.5, 0.3, tracker, ImageType.single(GrayU8.class));
	}
}
