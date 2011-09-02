/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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
import boofcv.struct.deriv.GradientValue_F32;
import boofcv.struct.image.ImageFloat32;


/**
 * Computes the gradient from an integral image.  Does not check for border conditions.
 *
 * @author Peter Abeles
 */
public class SparseIntegralGradient_NoBorder_F32 
		extends SparseIntegralGradient_NoBorder<ImageFloat32, GradientValue_F32>
{

	private GradientValue_F32 ret = new GradientValue_F32();

	public SparseIntegralGradient_NoBorder_F32(int radius) {
		super(radius);
	}

	@Override
	public GradientValue_F32 compute(int x, int y) {

		int horizontalOffset = x-r-1;
		int indexSrc1 = ii.startIndex + (y-r-1)*ii.stride + horizontalOffset;
		int indexSrc2 = indexSrc1 + r*ii.stride;
		int indexSrc3 = indexSrc2 + ii.stride;
		int indexSrc4 = indexSrc3 + r*ii.stride;

		float p0 = ii.data[indexSrc1];
		float p1 = ii.data[indexSrc1+r];
		float p2 = ii.data[indexSrc1+r+1];
		float p3 = ii.data[indexSrc1+w];
		float p11 = ii.data[indexSrc2];
		float p4 = ii.data[indexSrc2+w];
		float p10 = ii.data[indexSrc3];
		float p5 = ii.data[indexSrc3+w];
		float p9 = ii.data[indexSrc4];
		float p8 = ii.data[indexSrc4+r];
		float p7 = ii.data[indexSrc4+r+1];
		float p6 = ii.data[indexSrc4+w];

		float left = p8-p9-p1+p0;
		float right = p6-p7-p3+p2;
		float top = p4-p11-p3+p0;
		float bottom = p6-p9-p5+p10;

		ret.x = right-left;
		ret.y = bottom-top;

		return ret;
	}
}
