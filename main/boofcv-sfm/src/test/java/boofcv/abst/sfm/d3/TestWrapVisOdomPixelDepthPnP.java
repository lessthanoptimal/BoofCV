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
import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.tracker.klt.ConfigPKlt;
import boofcv.factory.feature.disparity.ConfigDisparityBM;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.factory.sfm.ConfigVisOdomDepthPnP;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.struct.image.GrayF32;
import boofcv.struct.pyramid.ConfigDiscreteLevels;

/**
 * @author Peter Abeles
 */
public class TestWrapVisOdomPixelDepthPnP extends CheckVisualOdometryStereoSim<GrayF32> {

	public TestWrapVisOdomPixelDepthPnP() {
		super(GrayF32.class);
	}

	@Override
	public StereoVisualOdometry<GrayF32> createAlgorithm() {
		var configBM = new ConfigDisparityBM();
		configBM.disparityMin = 0;
		configBM.disparityRange = 60;
		configBM.regionRadiusX = 2;
		configBM.regionRadiusY = 2;
		configBM.maxPerPixelError = 30;
		configBM.texture = -1;
		configBM.validateRtoL = 1;
		configBM.subpixel = true;

		var configKLT = new ConfigPKlt();
		configKLT.pyramidLevels = ConfigDiscreteLevels.levels(4);
		configKLT.templateRadius = 3;

		var configPnP = new ConfigVisOdomDepthPnP();
		configPnP.ransacIterations = 200;

		ConfigPointDetector configDetector = new ConfigPointDetector();
		configDetector.general.maxFeatures = 600;
		configDetector.general.radius = 3;
		configDetector.general.threshold = 1;
		configDetector.shiTomasi.radius = 3;

		StereoDisparitySparse<GrayF32> disparity = FactoryStereoDisparity.sparseRectifiedBM(configBM, GrayF32.class);
		PointTracker<GrayF32> tracker = FactoryPointTracker.klt(configKLT, configDetector,
				GrayF32.class, GrayF32.class);

		return FactoryVisualOdometry.stereoDepthPnP(configPnP,disparity,tracker,GrayF32.class);
	}

}
