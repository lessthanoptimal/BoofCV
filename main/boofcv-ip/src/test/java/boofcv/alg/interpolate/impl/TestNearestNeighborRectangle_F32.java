/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.interpolate.impl;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.InterpolateRectangle;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.GrayF32;


public class TestNearestNeighborRectangle_F32 extends GeneralBilinearRectangleChecks<GrayF32>{


	public TestNearestNeighborRectangle_F32() {
		super(GrayF32.class);
	}

	@Override
	public InterpolatePixelS<GrayF32> createPixelInterpolate() {
		return FactoryInterpolation.nearestNeighborPixelS(imageType);
	}

	@Override
	public InterpolateRectangle<GrayF32> createRectangleInterpolate() {
		return FactoryInterpolation.nearestNeighborRectangle(imageType);
	}

	@Override
	protected GrayF32 createImage(int width, int height) {
		return new GrayF32(width,height);
	}
}
