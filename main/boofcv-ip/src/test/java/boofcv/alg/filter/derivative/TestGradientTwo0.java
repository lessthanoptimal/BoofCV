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

package boofcv.alg.filter.derivative;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.border.ImageBorder_F32;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

public class TestGradientTwo0 extends BoofStandardJUnit {

	int width = 5;
	int height = 7;

	@Test void testStandard() {
		StandardGradientChecks standard = new StandardGradientChecks();
		standard.secondDerivativeTest(GradientTwo0.class,3);
	}

	@Test void compareToConvolve_I8() throws NoSuchMethodException {
		CompareDerivativeToConvolution validator = new CompareDerivativeToConvolution();
		validator.setTarget(GradientTwo0.class.getMethod("process",
				GrayU8.class, GrayS16.class, GrayS16.class, ImageBorder_S32.class ));

		validator.setKernel(0, GradientTwo0.kernelDeriv_I32,true);
		validator.setKernel(1, GradientTwo0.kernelDeriv_I32,false);

		GrayU8 input = new GrayU8(width,height);
		ImageMiscOps.fillUniform(input, rand, 0, 10);
		GrayS16 derivX = new GrayS16(width,height);
		GrayS16 derivY = new GrayS16(width,height);

		validator.compare(input,derivX,derivY);
	}

	@Test void compareToConvolve_I16() throws NoSuchMethodException {
		CompareDerivativeToConvolution validator = new CompareDerivativeToConvolution();
		validator.setTarget(GradientTwo0.class.getMethod("process",
				GrayS16.class, GrayS16.class, GrayS16.class, ImageBorder_S32.class ));

		validator.setKernel(0, GradientTwo0.kernelDeriv_I32,true);
		validator.setKernel(1, GradientTwo0.kernelDeriv_I32,false);

		GrayS16 input = new GrayS16(width,height);
		ImageMiscOps.fillUniform(input, rand, 0, 10);
		GrayS16 derivX = new GrayS16(width,height);
		GrayS16 derivY = new GrayS16(width,height);

		validator.compare(input,derivX,derivY);
	}

@	Test
	public void compareToConvolve_F32() throws NoSuchMethodException {
		CompareDerivativeToConvolution validator = new CompareDerivativeToConvolution();
		validator.setTarget(GradientTwo0.class.getMethod("process",
				GrayF32.class, GrayF32.class, GrayF32.class, ImageBorder_F32.class ));

		validator.setKernel(0, GradientTwo0.kernelDeriv_F32,true);
		validator.setKernel(1, GradientTwo0.kernelDeriv_F32,false);

		GrayF32 input = new GrayF32(width,height);
		ImageMiscOps.fillUniform(input, rand, 0, 10);
		GrayF32 derivX = new GrayF32(width,height);
		GrayF32 derivY = new GrayF32(width,height);

		validator.compare(input,derivX,derivY);
	}

}
