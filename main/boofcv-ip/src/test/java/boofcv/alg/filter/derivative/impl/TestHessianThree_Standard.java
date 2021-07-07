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

import boofcv.alg.filter.derivative.CompareDerivativeToConvolution;
import boofcv.alg.filter.derivative.HessianThree;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

/**
 * @author Peter Abeles
 */
public class TestHessianThree_Standard extends BoofStandardJUnit {
	int width = 20;
	int height = 25;

	@Test void compareToConvolve_I8() throws NoSuchMethodException {
		CompareDerivativeToConvolution validator = new CompareDerivativeToConvolution();
		validator.setTarget(HessianThree_Standard.class.getMethod("process",
				GrayU8.class, GrayS16.class, GrayS16.class, GrayS16.class));

		validator.setKernel(0,HessianThree.kernelXXYY_I32,true);
		validator.setKernel(1,HessianThree.kernelXXYY_I32,false);
		validator.setKernel(2,HessianThree.kernelCross_I32);

		GrayU8 input = new GrayU8(width,height);
		ImageMiscOps.fillUniform(input, rand, 0, 10);
		GrayS16 derivXX = new GrayS16(width,height);
		GrayS16 derivYY = new GrayS16(width,height);
		GrayS16 derivXY = new GrayS16(width,height);

		validator.compare(false,input,derivXX,derivYY,derivXY);
	}

	@Test void compareToConvolve_F32() throws NoSuchMethodException {
		CompareDerivativeToConvolution validator = new CompareDerivativeToConvolution();
		validator.setTarget(HessianThree_Standard.class.getMethod("process",
				GrayF32.class, GrayF32.class, GrayF32.class, GrayF32.class ));

		validator.setKernel(0,HessianThree.kernelXXYY_F32,true);
		validator.setKernel(1,HessianThree.kernelXXYY_F32,false);
		validator.setKernel(2,HessianThree.kernelCross_F32);

		GrayF32 input = new GrayF32(width,height);
		ImageMiscOps.fillUniform(input, rand, 0, 10);
		GrayF32 derivXX = new GrayF32(width,height);
		GrayF32 derivYY = new GrayF32(width,height);
		GrayF32 derivXY = new GrayF32(width,height);

		validator.compare(false,input,derivXX,derivYY,derivXY);
	}
}
