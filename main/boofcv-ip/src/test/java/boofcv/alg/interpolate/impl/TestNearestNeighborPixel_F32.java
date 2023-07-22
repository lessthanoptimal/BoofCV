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
import boofcv.struct.image.GrayF32;

public class TestNearestNeighborPixel_F32 extends GeneralChecksInterpolationPixelS<GrayF32>
{
	@Override
	protected GrayF32 createImage(int width, int height) {
		return new GrayF32(width, height);
	}

	@Override
	protected InterpolatePixelS<GrayF32> wrap(GrayF32 image, int minValue, int maxValue) {
		return new NearestNeighborPixel_F32(image);
	}

	/**
	 * Compute a bilinear interpolation manually
	 */
	@Override
	protected float compute(GrayF32 img, float x, float y) {
		return img.get((int)x,(int)y);
	}
}
