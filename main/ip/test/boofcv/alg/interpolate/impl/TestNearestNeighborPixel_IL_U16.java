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
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.InterleavedU16;

/**
 * @author Peter Abeles
 */
public class TestNearestNeighborPixel_IL_U16 extends GeneralChecksInterpolationPixelMB<InterleavedU16> {

	@Override
	protected InterleavedU16 createImage(int width, int height, int numBands) {
		return new InterleavedU16(width,height,numBands);
	}

	@Override
	protected InterpolatePixelMB<InterleavedU16> wrap(InterleavedU16 image, int minValue, int maxValue) {
		return new NearestNeighborPixel_IL_U16(image);
	}

	@Override
	protected <SB extends ImageGray> InterpolatePixelS<SB>
	wrapSingle(SB image, int minValue, int maxValue) {
		return (InterpolatePixelS)new NearestNeighborPixel_U16((GrayU16)image);
	}

	@Override
	protected void compute(InterleavedU16 img, float x, float y, float[] pixel) {
		int xx = (int)x;
		int yy = (int)y;

		int tmp[] = new int[ pixel.length ];
		img.get(xx,yy,tmp);
		for (int i = 0; i < tmp.length; i++) {
			pixel[i] = tmp[i];
		}
	}
}
