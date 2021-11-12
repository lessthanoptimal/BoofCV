/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.interpolate.NearestNeighborPixelMB;
import boofcv.struct.border.ImageBorder_IL_S32;
import boofcv.struct.image.InterleavedS32;

import javax.annotation.Generated;


/**
 * <p>
 * Performs nearest neighbor interpolation to extract values between pixels in an image.
 * </p>
 *
 * <p>DO NOT MODIFY. Automatically generated code created by GenerateNearestNeighborPixel_IL</p>
 *
 * @author Peter Abeles
 */
@Generated("boofcv.alg.interpolate.impl.GenerateNearestNeighborPixel_IL")
public class NearestNeighborPixel_IL_S32 extends NearestNeighborPixelMB<InterleavedS32> {

	private int[] pixel = new int[3];
	public NearestNeighborPixel_IL_S32() {}

	public NearestNeighborPixel_IL_S32(InterleavedS32 orig) {
		setImage(orig);
	}
	@Override
	public void setImage(InterleavedS32 image) {
		super.setImage(image);
		int N = image.getImageType().getNumBands();
		if( pixel.length != N )
			pixel = new int[ N ];
	}

		@Override
	public void get(float x, float y, float[] values) {
		if (x < 0 || y < 0 || x > width-1 || y > height-1 ) {
			((ImageBorder_IL_S32)border).get((int) Math.floor(x), (int) Math.floor(y), pixel);
			for (int i = 0; i < pixel.length; i++) {
				values[i] = pixel[i];
			}
		} else {
			get_fast(x,y,values);
		}
	}

	@Override
	public void get_fast(float x, float y, float[] values) {
		int xx = (int)x;
		int yy = (int)y;

		orig.unsafe_get(xx,yy,pixel);
		for (int i = 0; i < pixel.length; i++) {
			values[i] = pixel[i];
		}
	}

	@Override
	public InterpolatePixelMB<InterleavedS32> copy() {
		var out = new NearestNeighborPixel_IL_S32();
		out.setBorder(border.copy());
		return out;
	}
}
