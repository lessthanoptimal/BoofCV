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

package boofcv.alg.feature.disparity.impl;

import boofcv.struct.image.GrayF32;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSelectRectSubpixel_F32_F32 extends ChecksSelectRectStandardBase<float[],GrayF32> {


	public TestSelectRectSubpixel_F32_F32() {
		super(float[].class,GrayF32.class);
	}

	@Override
	public ImplSelectRectStandardBase_F32<GrayF32> createSelector(int maxError, int rightToLeftTolerance, double texture) {
		return new SelectRectSubpixel.F32_F32(maxError, rightToLeftTolerance, texture);
	}

	/**
	 * Given different local error values see if it is closer to the value with a smaller error
	 */
	@Test
	public void addSubpixelBias() {

		GrayF32 img = new GrayF32(w,h);

		SelectRectSubpixel.F32_F32 alg = new SelectRectSubpixel.F32_F32(-1,-1,-1);

		alg.configure(img,0,20,2);
		alg.setLocalMax(20);

		// should be biased towards 4
		alg.columnScore[4] = 100;
		alg.columnScore[5] = 50;
		alg.columnScore[6] = 200;

		alg.setDisparity(4,5);
		assertTrue( img.data[4] < 5 && img.data[4] > 4);

		// now biased towards 6
		alg.columnScore[4] = 200;
		alg.columnScore[6] = 100;
		alg.setDisparity(4,5);
		assertTrue( img.data[4] < 6 && img.data[4] > 5);
	}
}
