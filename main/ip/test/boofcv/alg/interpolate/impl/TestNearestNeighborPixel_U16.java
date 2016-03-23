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

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.struct.image.GrayU16;

/**
 * @author Peter Abeles
 */
public class TestNearestNeighborPixel_U16 extends GeneralChecksInterpolationPixelS<GrayU16>
{
	@Override
	protected GrayU16 createImage(int width, int height) {
		return new GrayU16(width, height);
	}

	@Override
	protected InterpolatePixelS<GrayU16> wrap(GrayU16 image, int minValue, int maxValue) {
		return new NearestNeighborPixel_U16(image);
	}

	/**
	 * Compute a bilinear interpolation manually
	 */
	@Override
	protected float compute(GrayU16 img, float x, float y) {
		return img.get((int)x,(int)y);
	}
}
