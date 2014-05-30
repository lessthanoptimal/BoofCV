/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.interpolate.BilinearPixel;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;


/**
 * <p>
 * Implementation of {@link BilinearPixel} for a specific image type.
 * </p>
 *
 * <p>
 * NOTE: This code was automatically generated using {@link GenerateImplBilinearPixel}.
 * </p>
 *
 * @author Peter Abeles
 */
public class ImplBilinearPixel_U8 extends BilinearPixel<ImageUInt8> {

	public ImplBilinearPixel_U8() {
	}

	public ImplBilinearPixel_U8(ImageUInt8 orig) {
		setImage(orig);
	}
	@Override
	public float get_fast(float x, float y) {
		int xt = (int) x;
		int yt = (int) y;
		float ax = x - xt;
		float ay = y - yt;

		int index = orig.startIndex + yt * stride + xt;

		byte[] data = orig.data;

		float val = (1.0f - ax) * (1.0f - ay) * (data[index] & 0xFF); // (x,y)
		val += ax * (1.0f - ay) * (data[index + 1] & 0xFF); // (x+1,y)
		val += ax * ay * (data[index + 1 + stride] & 0xFF); // (x+1,y+1)
		val += (1.0f - ax) * ay * (data[index + stride] & 0xFF); // (x,y+1)

		return val;
	}

	@Override
	public float get(float x, float y) {
		if (x < 0 || y < 0 || x > width-1 || y > height-1)
			throw new IllegalArgumentException("Point is outside of the image "+x+" "+y);

		int xt = (int) x;
		int yt = (int) y;

		float ax = x - xt;
		float ay = y - yt;

		int index = orig.startIndex + yt * stride + xt;

		// allows borders to be interpolated gracefully by double counting appropriate pixels
		int dx = xt == width - 1 ? 0 : 1;
		int dy = yt == height - 1 ? 0 : stride;

		byte[] data = orig.data;

		float val = (1.0f - ax) * (1.0f - ay) * (data[index] & 0xFF); // (x,y)
		val += ax * (1.0f - ay) * (data[index + dx] & 0xFF); // (x+1,y)
		val += ax * ay * (data[index + dx + dy] & 0xFF); // (x+1,y+1)
		val += (1.0f - ax) * ay * (data[index + dy] & 0xFF); // (x,y+1)

		return val;
	}

	@Override
	public ImageType<ImageUInt8> getImageType() {
		return ImageType.single(ImageUInt8.class);
	}

}
