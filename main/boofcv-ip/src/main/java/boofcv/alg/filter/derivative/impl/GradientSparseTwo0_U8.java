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

package boofcv.alg.filter.derivative.impl;

import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.sparse.GradientValue_I32;
import boofcv.struct.sparse.SparseImageGradient;

/**
 * Sparse computation of the two-0 gradient operator.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class GradientSparseTwo0_U8 implements SparseImageGradient<GrayU8,GradientValue_I32> {

	// image being processed
	GrayU8 input;
	// specifies how the image border is handled
	ImageBorder_S32<GrayU8> border;
	// storage for computed gradient
	GradientValue_I32 gradient = new GradientValue_I32();

	/**
	 * Specifies how border pixels are handled. If null then the border is not handled.
	 * @param border how borders are handled
	 */
	public GradientSparseTwo0_U8(ImageBorder_S32<GrayU8> border) {
		this.border = border;
	}

	@Override
	public GradientValue_I32 compute(int x, int y) {
		int a00,a10,a01;
		if( x >= 0 && y >= 0 && x < input.width - 1 && y < input.height - 1 ) {
			int s = input.stride;
			int tl = input.startIndex + input.stride*y + x;

			a00 = input.data[tl    ] & 0xFF;
			a10 = input.data[tl  +s] & 0xFF;
			a01 = input.data[tl+1  ] & 0xFF;
		} else {
			a00 = border.get(x  ,y  );
			a10 = border.get(x  ,y+1);
			a01 = border.get(x+1,y  );
		}
		gradient.y = a10-a00;
		gradient.x = a01-a00;

		return gradient;
	}

	@Override
	public Class<GradientValue_I32> getGradientType() {
		return GradientValue_I32.class;
	}

	@Override
	public void setImage(GrayU8 input) {
		this.input = input;
		if( border != null ) {
			border.setImage(input);
		}
	}

	@Override
	public boolean isInBounds(int x, int y) {
		return border != null || (x >= 0 && y >= 0 && x < input.width - 1 && y < input.height - 1);
	}
}
