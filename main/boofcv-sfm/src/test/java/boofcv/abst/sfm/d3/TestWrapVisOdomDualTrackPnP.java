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

import boofcv.factory.feature.describe.ConfigDescribeRegion;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.factory.sfm.ConfigStereoDualTrackPnP;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.struct.image.GrayF32;
import boofcv.struct.pyramid.ConfigDiscreteLevels;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Nested;

/**
 * @author Peter Abeles
 */
public class TestWrapVisOdomDualTrackPnP extends BoofStandardJUnit {
	@Nested
	public class TrackerKlt extends CheckVisualOdometryStereoSim<GrayF32> {

		public TrackerKlt() {
			super(GrayF32.class);
		}

		@Override
		void singleBadFrame() {
			// skip this since KLT can't recover from bad frames since it overwrites the result
		}

		@Override
		public StereoVisualOdometry<GrayF32> createAlgorithm() {

			var config = new ConfigStereoDualTrackPnP();

			config.scene.ransac.iterations = 200;
			config.scene.ransac.inlierThreshold = 1.5;

			config.tracker.typeTracker = ConfigPointTracker.TrackerType.KLT;
			config.tracker.klt.pyramidLevels = ConfigDiscreteLevels.levels(4);
			config.tracker.klt.templateRadius = 3;
			config.tracker.detDesc.detectPoint.shiTomasi.radius = 3;
			config.tracker.detDesc.detectPoint.general.radius = 3;

			return FactoryVisualOdometry.stereoDualTrackerPnP(config, GrayF32.class);
		}
	}

	@Nested
	public class TrackerDDA extends CheckVisualOdometryStereoSim<GrayF32> {

		public TrackerDDA() {
			super(GrayF32.class);
		}

		@Override
		public StereoVisualOdometry<GrayF32> createAlgorithm() {

			var config = new ConfigStereoDualTrackPnP();

			config.scene.ransac.iterations = 200;
			config.scene.ransac.inlierThreshold = 1.5;

			config.tracker.typeTracker = ConfigPointTracker.TrackerType.DDA;
			config.tracker.detDesc.typeDetector = ConfigDetectInterestPoint.Type.POINT;
			config.tracker.detDesc.detectPoint.general.maxFeatures = 300;
			config.tracker.detDesc.detectPoint.scaleRadius = 12;
			config.tracker.detDesc.detectPoint.general.radius = 3;
			config.tracker.detDesc.detectPoint.general.threshold = 0;
			config.tracker.detDesc.detectPoint.shiTomasi.radius = 2;
			config.tracker.detDesc.typeDescribe = ConfigDescribeRegion.Type.BRIEF;
			config.tracker.detDesc.describeBrief.fixed = true;

			return FactoryVisualOdometry.stereoDualTrackerPnP(config, GrayF32.class);
		}
	}
}
