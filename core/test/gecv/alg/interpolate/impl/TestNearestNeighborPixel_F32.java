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
import gecv.core.image.UtilImageFloat32;
import gecv.struct.image.ImageFloat32;

/**
 * @author Peter Abeles
 */
public class TestNearestNeighborPixel_F32 extends GeneralInterpolationPixelChecks<ImageFloat32>
{
	@Override
	protected ImageFloat32 createImage(int width, int height) {
		return new ImageFloat32(width, height);
	}

	@Override
	protected void randomize(ImageFloat32 image) {
		UtilImageFloat32.randomize(image, rand, 0, 100);
	}

	@Override
	protected InterpolatePixel<ImageFloat32> wrap(ImageFloat32 image) {
		return new NearestNeighborPixel_F32(image);
	}

	/**
	 * Compute a bilinear interpolation manually
	 */
	@Override
	protected float compute(ImageFloat32 img, float x, float y) {
		return img.get((int)x,(int)y);
	}
}
