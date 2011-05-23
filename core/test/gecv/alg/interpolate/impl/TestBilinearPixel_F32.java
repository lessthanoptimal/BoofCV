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

import gecv.alg.misc.ImageTestingOps;
import gecv.alg.interpolate.InterpolatePixel;
import gecv.struct.image.ImageFloat32;


/**
 * @author Peter Abeles
 */
public class TestBilinearPixel_F32 extends GeneralInterpolationPixelChecks<ImageFloat32>
{

	@Override
	protected ImageFloat32 createImage(int width, int height) {
		return new ImageFloat32(width, height);
	}

	@Override
	protected void randomize(ImageFloat32 image) {
		ImageTestingOps.randomize(image, rand, 0, 100);
	}

	@Override
	protected InterpolatePixel<ImageFloat32> wrap(ImageFloat32 image) {
		return new BilinearPixel_F32(image);
	}

	/**
	 * Compute a bilinear interpolation manually
	 */
	@Override
	protected float compute(ImageFloat32 img, float x, float y) {
		int gX = (int) x;
		int gY = (int) y;

		float v0 = img.get(gX, gY);
		float v1 = img.get(gX + 1, gY);
		float v2 = img.get(gX, gY + 1);
		float v3 = img.get(gX + 1, gY + 1);

		x %= 1f;
		y %= 1f;

		float a = 1f - x;
		float b = 1f - y;

		return a * b * v0 + x * b * v1 + a * y * v2 + x * y * v3;
	}
}
