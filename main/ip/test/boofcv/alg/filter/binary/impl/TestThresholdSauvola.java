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

package boofcv.alg.filter.binary.impl;

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import boofcv.testing.BoofTesting;
import org.junit.Test;

/**
 * @author Peter Abeles
 */
public class TestThresholdSauvola {

	/**
	 * Provide it a simple input image with obvious thresholding.  There will be regions of white space
	 * which exceed its radius.
	 */
	@Test
	public void simple() {
		int radius = 5;
		ImageUInt8 expected = new ImageUInt8(30,35);

		for (int y = radius; y < expected.height-radius; y++) {
			expected.set(20,y,1);
			expected.set(21,y,1);
			expected.set(22,y,1);
		}

		ImageFloat32 input = new ImageFloat32(expected.width,expected.height);
		for (int i = 0; i < input.width * input.height; i++) {
			input.data[i] = expected.data[i] == 0 ? 255 : 0;
		}

		ImageUInt8 found = new ImageUInt8(expected.width,expected.height);

		ThresholdSauvola alg = new ThresholdSauvola(radius,0.5f,true);

		alg.process(input,found);

		BoofTesting.assertEqualsInner(expected, found, 0, radius, radius, false);

		alg.setDown(false);
		alg.process(input, found);
		BinaryImageOps.invert(expected, expected);

		BoofTesting.assertEqualsInner(expected, found, 0, radius, radius, false);
	}
}