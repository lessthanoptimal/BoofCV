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

import boofcv.alg.interpolate.NearestNeighborPixelS;
import boofcv.core.image.border.ImageBorder_S32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.ImageType;


/**
 * <p>
 * Performs nearest neighbor interpolation to extract values between pixels in an image.
 * </p>
 *
 * <p>
 * NOTE: This code was automatically generated using {@link GenerateNearestNeighborPixel}.
 * </p>
 *
 * @author Peter Abeles
 */
public class NearestNeighborPixel_S32 extends NearestNeighborPixelS<GrayS32> {

	private int data[];
	public NearestNeighborPixel_S32() {
	}

	public NearestNeighborPixel_S32(GrayS32 orig) {

		setImage(orig);
	}
	@Override
	public void setImage(GrayS32 image) {
		super.setImage(image);
		this.data = orig.data;
	}

	@Override
	public float get_fast(float x, float y) {
		return data[ orig.startIndex + ((int)y)*stride + (int)x];
	}

	public float get_border(float x, float y) {
		return ((ImageBorder_S32)border).get((int)Math.floor(x),(int)Math.floor(y));
	}

	@Override
	public float get(float x, float y) {
		if (x < 0 || y < 0 || x > width-1 || y > height-1 )
			return get_border(x,y);
		int xx = (int)x;
		int yy = (int)y;

		return data[ orig.startIndex + yy*stride + xx];
	}

	@Override
	public ImageType<GrayS32> getImageType() {
		return ImageType.single(GrayS32.class);
	}

}
