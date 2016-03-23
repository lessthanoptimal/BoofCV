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

package boofcv.alg.interpolate.impl;

import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.InterleavedF32;

/**
 * @author Peter Abeles
 */
public class TestNearestNeighborPixel_IL_F32 extends GeneralChecksInterpolationPixelMB<InterleavedF32> {

	@Override
	protected InterleavedF32 createImage(int width, int height, int numBands) {
		return new InterleavedF32(width,height,numBands);
	}

	@Override
	protected InterpolatePixelMB<InterleavedF32> wrap(InterleavedF32 image, int minValue, int maxValue) {
		return new NearestNeighborPixel_IL_F32(image);
	}

	@Override
	protected <SB extends ImageGray> InterpolatePixelS<SB>
	wrapSingle(SB image, int minValue, int maxValue) {
		return (InterpolatePixelS)new NearestNeighborPixel_F32((GrayF32)image);
	}

	@Override
	protected void compute(InterleavedF32 img, float x, float y, float[] pixel) {
		int xx = (int)x;
		int yy = (int)y;

		img.get(xx,yy,pixel);
	}
}
