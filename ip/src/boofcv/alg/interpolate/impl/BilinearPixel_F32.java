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

package boofcv.alg.interpolate.impl;

import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.struct.image.ImageFloat32;


/**
 * <p>
 * Performs bilinear interpolation to extract values between pixels in an image.  When a boundary is encountered
 * the number of pixels used to interpolate is automatically reduced.
 * </p>
 *
 * <p>
 * NOTE: This code was automatically generated using {@link GenerateBilinearPixel}.
 * </p>
 *
 * @author Peter Abeles
 */
public class BilinearPixel_F32 implements InterpolatePixel<ImageFloat32> {

	private ImageFloat32 orig;

	private float data[];
	private int stride;
	private int width;
	private int height;

	public BilinearPixel_F32() {
	}

	public BilinearPixel_F32(ImageFloat32 orig) {
		setImage(orig);
	}

	@Override
	public void setImage(ImageFloat32 image) {
		this.orig = image;
		this.data = orig.data;
		this.stride = orig.getStride();
		this.width = orig.getWidth();
		this.height = orig.getHeight();
	}

	@Override
	public ImageFloat32 getImage() {
		return orig;
	}

	@Override
	public float get_unsafe(float x, float y) {
		int xt = (int) x;
		int yt = (int) y;
		float ax = x - xt;
		float ay = y - yt;

		int index = orig.startIndex + yt * stride + xt;

		int dx = xt == width - 1 ? 0 : 1;
		int dy = yt == height - 1 ? 0 : stride;

		float val = (1.0f - ax) * (1.0f - ay) * (data[index] ); // (x,y)
		val += ax * (1.0f - ay) * (data[index + dx] ); // (x+1,y)
		val += ax * ay * (data[index + dx + dy] ); // (x+1,y+1)
		val += (1.0f - ax) * ay * (data[index + dy] ); // (x,y+1)

		return val;
	}

	@Override
	public float get(float x, float y) {
		int xt = (int) x;
		int yt = (int) y;

		if (xt < 0 || yt < 0 || xt >= width || yt >= height)
			throw new IllegalArgumentException("Point is outside of the image");

		float ax = x - xt;
		float ay = y - yt;

		int index = orig.startIndex + yt * stride + xt;

		// throw allows borders to be interpolated gracefully by double counting appropriate pixels
		int dx = xt == width - 1 ? 0 : 1;
		int dy = yt == height - 1 ? 0 : stride;

		float val = (1.0f - ax) * (1.0f - ay) * (data[index] ); // (x,y)
		val += ax * (1.0f - ay) * (data[index + dx] ); // (x+1,y)
		val += ax * ay * (data[index + dx + dy] ); // (x+1,y+1)
		val += (1.0f - ax) * ay * (data[index + dy] ); // (x,y+1)

		return val;
	}

}
