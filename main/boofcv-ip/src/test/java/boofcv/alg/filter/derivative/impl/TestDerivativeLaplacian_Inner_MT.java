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

import boofcv.BoofTesting;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

class TestDerivativeLaplacian_Inner_MT extends BoofStandardJUnit {

	private final int width = 100;
	private final int height = 120;

	@Test void U8_S16() {
		GrayU8 input = new GrayU8(width, height);
		ImageMiscOps.fillUniform(input, rand, 0, 100);

		GrayS16 expected = new GrayS16(width, height);
		GrayS16 found = expected.createSameShape();

		DerivativeLaplacian_Inner.process(input, expected);
		DerivativeLaplacian_Inner_MT.process(input, found);

		BoofTesting.assertEqualsInner(expected,found,0,1,1,false);
	}

	@Test void F32_F32() {
		GrayF32 input = new GrayF32(width, height);
		ImageMiscOps.fillUniform(input, rand, 0, 100);

		GrayF32 expected = new GrayF32(width, height);
		GrayF32 found = expected.createSameShape();

		DerivativeLaplacian_Inner.process(input, expected);
		DerivativeLaplacian_Inner_MT.process(input, found);

		BoofTesting.assertEqualsInner(expected,found, UtilEjml.TEST_F32,1,1,false);
	}

	@Test void U8_F32() {
		GrayU8 input = new GrayU8(width, height);
		ImageMiscOps.fillUniform(input, rand, 0, 100);

		GrayF32 expected = new GrayF32(width, height);
		GrayF32 found = expected.createSameShape();

		DerivativeLaplacian_Inner.process(input, expected);
		DerivativeLaplacian_Inner_MT.process(input, found);

		BoofTesting.assertEqualsInner(expected,found, UtilEjml.TEST_F32,1,1,false);
	}
}

