/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.interpolate.impl;

import gecv.alg.interpolate.InterpolatePixel;
import gecv.struct.image.ImageSInt16;

/**
 * @author Peter Abeles
 */
public class TestNearestNeighborPixel_S16 extends GeneralInterpolationPixelChecks<ImageSInt16>
{
	@Override
	protected ImageSInt16 createImage(int width, int height) {
		return new ImageSInt16(width, height);
	}

	@Override
	protected InterpolatePixel<ImageSInt16> wrap(ImageSInt16 image) {
		return new NearestNeighborPixel_S16(image);
	}

	/**
	 * Compute a bilinear interpolation manually
	 */
	@Override
	protected float compute(ImageSInt16 img, float x, float y) {
		return img.get((int)x,(int)y);
	}
}
