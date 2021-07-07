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

package boofcv.alg.filter.convolve.noborder;

import boofcv.alg.filter.convolve.CompareToStandardConvolution;
import boofcv.struct.convolve.Kernel1D_S32;
import boofcv.struct.convolve.Kernel2D_S32;
import boofcv.struct.image.GrayI16;
import boofcv.struct.image.GrayS16;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;
import pabeles.concurrency.GrowArray;

import java.lang.reflect.Method;

/**
 * @author Peter Abeles
 */
public class TestConvolveImageUnrolled_SB_S16_I16_Div extends BoofStandardJUnit {
	CompareToStandardConvolution compareToStandard = new CompareToStandardConvolution(ConvolveImageUnrolled_SB_S16_I16_Div.class);

	@Test void convolve() throws NoSuchMethodException {
		for (int i = 0; i < GenerateConvolvedUnrolled_SB.numUnrolled; i++) {
			Method m = ConvolveImageUnrolled_SB_S16_I16_Div.class.getMethod("convolve",
					Kernel2D_S32.class, GrayS16.class, GrayI16.class , int.class, GrowArray.class);

			compareToStandard.compareMethod(m, "convolve", i + 1);
		}
	}

	@Test void horizontal() throws NoSuchMethodException {
		for (int i = 0; i < GenerateConvolvedUnrolled_SB.numUnrolled; i++) {
			Method m = ConvolveImageUnrolled_SB_S16_I16_Div.class.getMethod("horizontal",
					Kernel1D_S32.class, GrayS16.class, GrayI16.class, int.class);

			compareToStandard.compareMethod(m, "horizontal", i + 1);
		}
	}

	@Test void vertical() throws NoSuchMethodException {
		for (int i = 0; i < GenerateConvolvedUnrolled_SB.numUnrolled; i++) {
			Method m = ConvolveImageUnrolled_SB_S16_I16_Div.class.getMethod("vertical",
					Kernel1D_S32.class, GrayS16.class, GrayI16.class, int.class, GrowArray.class);

			compareToStandard.compareMethod(m, "vertical", i + 1);
		}
	}
}
