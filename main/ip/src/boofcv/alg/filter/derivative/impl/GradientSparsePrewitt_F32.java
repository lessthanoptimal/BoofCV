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

package boofcv.alg.filter.derivative.impl;

import boofcv.core.image.border.ImageBorder_F32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.sparse.GradientValue_F32;
import boofcv.struct.sparse.SparseImageGradient;

/**
 * Sparse computation of the Prewitt gradient operator.
 *
 * @author Peter Abeles
 */
public class GradientSparsePrewitt_F32 implements SparseImageGradient<GrayF32,GradientValue_F32> {

	// image being processed
	GrayF32 input;
	// specifies how the image border is handled
	ImageBorder_F32 border;
	// storage for computed gradient
	GradientValue_F32 gradient = new GradientValue_F32();

	/**
	 * Specifies how border pixels are handled.  If null then the border is not handled.
	 * @param border how borders are handled
	 */
	public GradientSparsePrewitt_F32(ImageBorder_F32 border) {
		this.border = border;
	}

	@Override
	public GradientValue_F32 compute(int x, int y) {
		float a00,a01,a02,a10,a12,a20,a21,a22;
		if( x >= 1 && y >= 1 && x < input.width - 1 && y < input.height - 1 ) {
			int s = input.stride;
			int tl = input.startIndex + input.stride*(y-1) + x-1;

			a00 = input.data[tl       ];
			a01 = input.data[tl+1     ];
			a02 = input.data[tl+2     ];
			a10 = input.data[tl   + s ];
			a12 = input.data[tl+2 + s ];
			a20 = input.data[tl   + 2*s];
			a21 = input.data[tl+1 + 2*s];
			a22 = input.data[tl+2 + 2*s];
		} else {
			a00 = border.get(x-1,y-1);
			a01 = border.get(x  ,y-1);
			a02 = border.get(x+1,y-1);
			a10 = border.get(x-1,y  );
			a12 = border.get(x+1,y  );
			a20 = border.get(x-1,y+1);
			a21 = border.get(x  ,y+1);
			a22 = border.get(x+1,y+1);
		}
		gradient.y = -(a00 + a01 + a02);
		gradient.y += (a20 + a21 + a22);

		gradient.x = -(a00 + a10 + a20);
		gradient.x += (a02 + a12 + a22);

		return gradient;
	}

	@Override
	public Class<GradientValue_F32> getGradientType() {
		return GradientValue_F32.class;
	}

	@Override
	public void setImage(GrayF32 input) {
		this.input = input;
		if( border != null ) {
			border.setImage(input);
		}
	}

	@Override
	public boolean isInBounds(int x, int y) {
		return border != null || x >= 1 && y >= 1 && x < input.width - 1 && y < input.height - 1;
	}
}
