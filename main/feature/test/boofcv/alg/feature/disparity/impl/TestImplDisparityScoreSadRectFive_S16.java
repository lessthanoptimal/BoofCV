/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.disparity.impl;

import boofcv.alg.feature.disparity.DisparityScoreWindowFive;
import boofcv.alg.feature.disparity.DisparitySelect;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;

/**
 * @author Peter Abeles
 */
public class TestImplDisparityScoreSadRectFive_S16 extends ChecksImplDisparityScoreSadRectFive<ImageSInt16,ImageUInt8> {

	public TestImplDisparityScoreSadRectFive_S16() {
		super(ImageSInt16.class, ImageUInt8.class);
	}

	@Override
	protected DisparityScoreWindowFive<ImageSInt16, ImageUInt8>
	createAlg(int minDisparity, int maxDisparity, int radiusX, int radiusY, DisparitySelect compDisp) {
		return new ImplDisparityScoreSadRectFive_S16<ImageUInt8>(minDisparity,maxDisparity,radiusX,radiusY,compDisp);
	}
}
