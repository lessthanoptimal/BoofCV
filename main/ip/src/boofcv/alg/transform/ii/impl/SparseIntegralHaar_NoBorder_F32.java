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

package boofcv.alg.transform.ii.impl;

import boofcv.alg.transform.ii.SparseIntegralGradient_NoBorder;
import boofcv.struct.image.GrayF32;
import boofcv.struct.sparse.GradientValue_F32;


/**
 * Computes the gradient Haar wavelet from an integral image.  Does not check for border conditions.
 *
 * @author Peter Abeles
 */
public class SparseIntegralHaar_NoBorder_F32
		extends SparseIntegralGradient_NoBorder<GrayF32, GradientValue_F32>
{

	private GradientValue_F32 ret = new GradientValue_F32();

	@Override
	public void setWidth(double width) {
		super.setWidth(width);
		w = 2*r;
		x0 = y0 = -r;
		x1 = y1 = r;
	}

	@Override
	public GradientValue_F32 compute(int x, int y) {

		int horizontalOffset = x-r;
		int indexSrc1 = input.startIndex + (y-r)*input.stride + horizontalOffset;
		int indexSrc2 = input.startIndex + y*input.stride + horizontalOffset;
		int indexSrc3 = input.startIndex + (y+r)*input.stride + horizontalOffset;


		float p0 = input.data[indexSrc1];
		float p1 = input.data[indexSrc1+r];
		float p2 = input.data[indexSrc1+w];
		float p3 = input.data[indexSrc2];
		float p5 = input.data[indexSrc2+w];
		float p6 = input.data[indexSrc3];
		float p7 = input.data[indexSrc3+r];
		float p8 = input.data[indexSrc3+w];


		float left = p7-p1-p6+p0;
		float right = p8-p2-p7+p1;
		float top = p5-p2-p3+p0;
		float bottom = p8-p5-p6+p3;

		ret.x = right-left;
		ret.y = bottom-top;

		return ret;
	}

	@Override
	public Class<GradientValue_F32> getGradientType() {
		return GradientValue_F32.class;
	}
}
