/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.tracker.PointTrackerTwoPass;
import boofcv.alg.sfm.DepthSparse3D;
import boofcv.alg.tracker.klt.PkltConfig;
import boofcv.factory.feature.tracker.FactoryPointTrackerTwoPass;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;

/**
 * @author Peter Abeles
 */
public class TestVisOdomPixelDepthPnP_to_DepthVisualOdometry extends CheckVisualOdometryDepthSim<GrayU8,GrayU16> {

	public TestVisOdomPixelDepthPnP_to_DepthVisualOdometry() {
		super(GrayU8.class,GrayU16.class);

		setAlgorithm(createAlgorithm());
	}

	protected DepthVisualOdometry<GrayU8,GrayU16> createAlgorithm() {

		PkltConfig config = new PkltConfig();
		config.pyramidScaling = new int[]{1,2,4,8};
		config.templateRadius = 3;
		ConfigGeneralDetector configDetector = new ConfigGeneralDetector(600,3,1);

		PointTrackerTwoPass<GrayU8> tracker = FactoryPointTrackerTwoPass.klt(config, configDetector,
				GrayU8.class, GrayS16.class);

		DepthSparse3D<GrayU16> sparseDepth = new DepthSparse3D.I<>(depthUnits);

		return FactoryVisualOdometry.
				depthDepthPnP(1.5, 120, 2, 200, 50, false, sparseDepth, tracker, GrayU8.class, GrayU16.class);

	}
}
