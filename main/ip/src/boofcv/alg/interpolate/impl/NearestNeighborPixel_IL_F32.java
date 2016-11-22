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

import boofcv.alg.interpolate.NearestNeighborPixelMB;
import boofcv.core.image.border.ImageBorder_IL_F32;
import boofcv.struct.image.InterleavedF32;


/**
 * <p>
 * Performs nearest neighbor interpolation to extract values between pixels in an image.
 * </p>
 *
 * <p>
 * TODO write auto generate code for this
 * </p>
 *
 * @author Peter Abeles
 */
public class NearestNeighborPixel_IL_F32 extends NearestNeighborPixelMB<InterleavedF32> {

	public NearestNeighborPixel_IL_F32() {
	}

	public NearestNeighborPixel_IL_F32(InterleavedF32 orig) {

		setImage(orig);
	}

	@Override
	public void get(float x, float y, float[] values) {
		if (x < 0 || y < 0 || x > width-1 || y > height-1 )
			((ImageBorder_IL_F32)border).get((int) Math.floor(x), (int) Math.floor(y), values);
		else {
			get_fast(x,y,values);
		}
	}

	@Override
	public void get_fast(float x, float y, float[] values) {
		int xx = (int)x;
		int yy = (int)y;

		orig.unsafe_get(xx,yy,values);
	}

}
