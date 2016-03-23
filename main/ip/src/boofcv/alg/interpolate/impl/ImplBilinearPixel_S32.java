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

import boofcv.alg.interpolate.BilinearPixelS;
import boofcv.core.image.border.ImageBorder_S32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.ImageType;


/**
 * <p>
 * Implementation of {@link BilinearPixelS} for a specific image type.
 * </p>
 *
 * <p>
 * NOTE: This code was automatically generated using {@link GenerateImplBilinearPixel}.
 * </p>
 *
 * @author Peter Abeles
 */
public class ImplBilinearPixel_S32 extends BilinearPixelS<GrayS32> {

	public ImplBilinearPixel_S32() {
	}

	public ImplBilinearPixel_S32(GrayS32 orig) {

		setImage(orig);
	}
	@Override
	public float get_fast(float x, float y) {
		int xt = (int) x;
		int yt = (int) y;
		float ax = x - xt;
		float ay = y - yt;

		int index = orig.startIndex + yt * stride + xt;

		int[] data = orig.data;

		float val = (1.0f - ax) * (1.0f - ay) * (data[index] ); // (x,y)
		val += ax * (1.0f - ay) * (data[index + 1] ); // (x+1,y)
		val += ax * ay * (data[index + 1 + stride] ); // (x+1,y+1)
		val += (1.0f - ax) * ay * (data[index + stride] ); // (x,y+1)

		return val;
	}

	public float get_border(float x, float y) {
		float xf = (float)Math.floor(x);
		float yf = (float)Math.floor(y);
		int xt = (int) xf;
		int yt = (int) yf;
		float ax = x - xf;
		float ay = y - yf;

		ImageBorder_S32 border = (ImageBorder_S32)this.border;

		float val = (1.0f - ax) * (1.0f - ay) * border.get(xt,yt); // (x,y)
		val += ax * (1.0f - ay) *  border.get(xt + 1, yt);; // (x+1,y)
		val += ax * ay *  border.get(xt + 1, yt + 1);; // (x+1,y+1)
		val += (1.0f - ax) * ay *  border.get(xt,yt+1);; // (x,y+1)

		return val;
	}

	@Override
	public float get(float x, float y) {
		if (x < 0 || y < 0 || x > width-2 || y > height-2)
			return get_border(x,y);

		return get_fast(x,y);
	}

	@Override
	public ImageType<GrayS32> getImageType() {
		return ImageType.single(GrayS32.class);
	}

}
