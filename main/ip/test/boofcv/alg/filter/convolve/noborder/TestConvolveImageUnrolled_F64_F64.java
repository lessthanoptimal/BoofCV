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

package boofcv.alg.filter.convolve.noborder;

import boofcv.alg.filter.convolve.CompareToStandardConvolution;
import boofcv.struct.convolve.Kernel1D_F64;
import boofcv.struct.convolve.Kernel2D_F64;
import boofcv.struct.image.GrayF64;
import org.junit.Test;

import java.lang.reflect.Method;

/**
 * @author Peter Abeles
 */
public class TestConvolveImageUnrolled_F64_F64 {
	CompareToStandardConvolution compareToStandard = new CompareToStandardConvolution(ConvolveImageUnrolled_F64_F64.class);

	@Test
	public void convolve() throws NoSuchMethodException {
		for (int i = 0; i < GenerateConvolvedUnrolled.numUnrolled; i++) {
			Method m = ConvolveImageUnrolled_F64_F64.class.getMethod("convolve",
					Kernel2D_F64.class, GrayF64.class, GrayF64.class );

			compareToStandard.compareMethod(m, "convolve", i + 1);
		}
	}

	@Test
	public void horizontal() throws NoSuchMethodException {
		for (int i = 0; i < GenerateConvolvedUnrolled.numUnrolled; i++) {
			Method m = ConvolveImageUnrolled_F64_F64.class.getMethod("horizontal",
					Kernel1D_F64.class, GrayF64.class, GrayF64.class);

			compareToStandard.compareMethod(m, "horizontal", i + 1);
		}
	}

	@Test
	public void vertical() throws NoSuchMethodException {
		for (int i = 0; i < GenerateConvolvedUnrolled.numUnrolled; i++) {
			Method m = ConvolveImageUnrolled_F64_F64.class.getMethod("vertical",
					Kernel1D_F64.class, GrayF64.class, GrayF64.class);

			compareToStandard.compareMethod(m, "vertical", i + 1);
		}
	}
}
