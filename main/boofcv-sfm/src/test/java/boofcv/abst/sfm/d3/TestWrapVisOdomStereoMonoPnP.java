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

import boofcv.abst.disparity.StereoDisparitySparse;
import boofcv.abst.feature.detect.interest.ConfigPointDetector;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.tracker.klt.ConfigPKlt;
import boofcv.factory.disparity.ConfigDisparityBM;
import boofcv.factory.disparity.FactoryStereoDisparity;
import boofcv.factory.feature.describe.ConfigDescribeRegion;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.factory.sfm.ConfigVisOdomTrackPnP;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.struct.image.GrayF32;
import boofcv.struct.pyramid.ConfigDiscreteLevels;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Nested;

/**
 * @author Peter Abeles
 */
public class TestWrapVisOdomStereoMonoPnP extends BoofStandardJUnit {

	ConfigDisparityBM configBM = new ConfigDisparityBM();
	ConfigVisOdomTrackPnP configPnP = new ConfigVisOdomTrackPnP();

	{
		configBM.disparityMin = 0;
		configBM.disparityRange = 60;
		configBM.regionRadiusX = 2;
		configBM.regionRadiusY = 2;
		configBM.maxPerPixelError = 30;
		configBM.texture = -1;
		configBM.validateRtoL = 1;
		configBM.subpixel = true;

		configPnP.ransac.iterations = 200;
	}

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

			var configKLT = new ConfigPKlt();
			configKLT.pyramidLevels = ConfigDiscreteLevels.levels(4);
			configKLT.templateRadius = 3;

			ConfigPointDetector configDetector = new ConfigPointDetector();
			configDetector.general.maxFeatures = 600;
			configDetector.general.radius = 3;
			configDetector.general.threshold = 1;
			configDetector.shiTomasi.radius = 3;

			StereoDisparitySparse<GrayF32> disparity = FactoryStereoDisparity.sparseRectifiedBM(configBM, GrayF32.class);
			PointTracker<GrayF32> tracker = FactoryPointTracker.klt(configKLT, configDetector,
					GrayF32.class, GrayF32.class);

			return FactoryVisualOdometry.stereoMonoPnP(configPnP, disparity, tracker, GrayF32.class);
		}
	}

	@Nested
	public class TrackerDDA extends CheckVisualOdometryStereoSim<GrayF32> {

		public TrackerDDA() {
			super(GrayF32.class);
		}

		@Override
		public StereoVisualOdometry<GrayF32> createAlgorithm() {

			var configDDA = new ConfigPointTracker();
			configDDA.typeTracker = ConfigPointTracker.TrackerType.DDA;
			configDDA.detDesc.typeDetector = ConfigDetectInterestPoint.Type.POINT;
			configDDA.detDesc.detectPoint.general.maxFeatures = 300;
			configDDA.detDesc.detectPoint.scaleRadius = 12;
			configDDA.detDesc.detectPoint.general.radius = 3;
			configDDA.detDesc.detectPoint.general.threshold = 0;
			configDDA.detDesc.detectPoint.shiTomasi.radius = 2;
			configDDA.detDesc.typeDescribe = ConfigDescribeRegion.Type.BRIEF;
			configDDA.detDesc.describeBrief.fixed = true;

			StereoDisparitySparse<GrayF32> disparity = FactoryStereoDisparity.sparseRectifiedBM(configBM, GrayF32.class);
			PointTracker<GrayF32> tracker = FactoryPointTracker.tracker(configDDA, GrayF32.class, GrayF32.class);

			return FactoryVisualOdometry.stereoMonoPnP(configPnP, disparity, tracker, GrayF32.class);
		}
	}
}
