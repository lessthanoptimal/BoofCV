/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.segmentation.slic;

import boofcv.struct.ConnectRule;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.MultiSpectral;

/**
 * @author Peter Abeles
 */
public class TestSegmentSlic_MsF32 extends GeneralSegmentSlicColorChecks<MultiSpectral<ImageFloat32>> {

	public TestSegmentSlic_MsF32() {
		super(ImageType.ms(3, ImageFloat32.class));
	}

	@Override
	public SegmentSlic<MultiSpectral<ImageFloat32>> createAlg(int numberOfRegions, float m, int totalIterations, ConnectRule rule) {
		return new SegmentSlic_MsF32(numberOfRegions,m,totalIterations,rule,3);
	}

}
