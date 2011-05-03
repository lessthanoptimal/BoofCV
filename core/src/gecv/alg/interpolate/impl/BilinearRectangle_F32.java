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

import gecv.alg.interpolate.InterpolateRectangle;
import gecv.struct.image.ImageFloat32;


/**
 * Performs bilinear interpolation to extract values between pixels in an image.
 *
 * @author Peter Abeles
 */
public class BilinearRectangle_F32 implements InterpolateRectangle<ImageFloat32> {

	private ImageFloat32 orig;

	private float data[];
	private int stride;

	public BilinearRectangle_F32(ImageFloat32 image) {
		setImage(image);
	}

	public BilinearRectangle_F32() {
	}

	@Override
	public void setImage(ImageFloat32 image) {
		this.orig = image;
		this.data = orig.data;
		this.stride = orig.getStride();
	}

	@Override
	public ImageFloat32 getImage() {
		return orig;
	}

	@Override
	public void region(float lt_x, float tl_y, float[] results, int regWidth, int regHeight) {
		int xt = (int) lt_x;
		int yt = (int) tl_y;
		float ax = lt_x - xt;
		float ay = tl_y - yt;

		float bx = 1.0f - ax;
		float by = 1.0f - ay;

		float a0 = bx * by;
		float a1 = ax * by;
		float a2 = ax * ay;
		float a3 = bx * ay;

		// make sure it is in bounds
		if (xt + regWidth >= orig.width || yt + regHeight >= orig.height) {
			throw new RuntimeException("reguested region is out of bounds");
		}

		// perform the interpolation while reducing the number of times the image needs to be accessed
		int indexResults = 0;
		for (int i = 0; i < regHeight; i++) {
			int index = orig.startIndex + (yt + i) * stride + xt;

			float XY = data[index];
			float Xy = data[index + stride];

			int indexEnd = index + regWidth;
			// for( int j = 0; j < regWidth; j++, index++ ) {
			for (; index < indexEnd; index++) {
				float xY = data[index + 1];
				float xy = data[index + stride + 1];

				float val = a0 * XY + a1 * xY + a2 * xy + a3 * Xy;

				results[indexResults++] = val;
				XY = xY;
				Xy = xy;
			}
		}
	}
}
