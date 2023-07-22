/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.filter.derivative;

import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

public class TestImageHessianDirect_SB extends BoofStandardJUnit {

	int width = 20;
	int height = 30;

	/**
	 * See if it throws an exception or not
	 */
	@Test void testNoException() {
		GrayF32 input = new GrayF32(width, height);
		GrayF32 derivXX = new GrayF32(width, height);
		GrayF32 derivYY = new GrayF32(width, height);
		GrayF32 derivXY = new GrayF32(width, height);

		ImageHessianDirect_SB<GrayF32, GrayF32> alg = new ImageHessianDirect_SB.Sobel<>(GrayF32.class, GrayF32.class);

		alg.process(input, derivXX, derivYY, derivXY);
	}
}
