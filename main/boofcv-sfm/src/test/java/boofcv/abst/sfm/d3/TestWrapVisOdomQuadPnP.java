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

import boofcv.factory.sfm.ConfigStereoQuadPnP;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.struct.image.GrayF32;

/**
 * @author Peter Abeles
 */
public class TestWrapVisOdomQuadPnP extends CheckVisualOdometryStereoSim<GrayF32> {

	public TestWrapVisOdomQuadPnP() {
		super(GrayF32.class, 0.3);
	}

	@Override
	public StereoVisualOdometry<GrayF32> createAlgorithm() {
		ConfigStereoQuadPnP config = new ConfigStereoQuadPnP();
		return FactoryVisualOdometry.stereoQuadPnP(config, GrayF32.class);
	}
}
