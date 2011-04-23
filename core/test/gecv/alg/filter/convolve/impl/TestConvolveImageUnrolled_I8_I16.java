/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.filter.convolve.impl;

import gecv.alg.filter.convolve.TestConvolveImage;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.image.ImageInt16;
import gecv.struct.image.ImageInt8;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestConvolveImageUnrolled_I8_I16 {
	@Test
	public void horizontal() throws NoSuchMethodException {
		Random rand = new Random(234);

		for (int i = 0; i < GenerateConvolvedUnrolled.numUnrolled; i++) {
			Method m = ConvolveImageUnrolled_I8_I16.class.getMethod("horizontal",
					Kernel1D_I32.class, ImageInt8.class, ImageInt16.class, boolean.class);

			TestConvolveImage.checkAgainstStandard(m, "horizontal", 5, 7, i + 1, rand);
		}
	}

	@Test
	public void vertical() throws NoSuchMethodException {
		Random rand = new Random(234);

		for (int i = 0; i < GenerateConvolvedUnrolled.numUnrolled; i++) {
			Method m = ConvolveImageUnrolled_I8_I16.class.getMethod("vertical",
					Kernel1D_I32.class, ImageInt8.class, ImageInt16.class, boolean.class);

			TestConvolveImage.checkAgainstStandard(m, "vertical", 5, 7, i + 1, rand);
		}
	}
}
