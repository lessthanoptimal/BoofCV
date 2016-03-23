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

package boofcv.alg.filter.derivative;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.ImageBorder_F32;
import boofcv.core.image.border.ImageBorder_S32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import org.junit.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestHessianFromGradient {

	Random rand = new Random(234);

	int width = 20;
	int height = 25;

	@Test
	public void hessianPrewitt_I8() throws NoSuchMethodException {
		CompareHessianToConvolution validator = new CompareHessianToConvolution();
		validator.setTarget(HessianFromGradient.class.getMethod("hessianPrewitt",
				GrayS16.class,GrayS16.class, GrayS16.class, GrayS16.class, GrayS16.class, ImageBorder_S32.class ));

		validator.setKernel(0,GradientPrewitt.kernelDerivX_I32);
		validator.setKernel(1,GradientPrewitt.kernelDerivY_I32);

		GrayS16 derivX = new GrayS16(width,height);
		GrayS16 derivY = new GrayS16(width,height);
		ImageMiscOps.fillUniform(derivX, rand, -10, 10);
		ImageMiscOps.fillUniform(derivY, rand, -10, 10);
		GrayS16 derivXX = new GrayS16(width,height);
		GrayS16 derivYY = new GrayS16(width,height);
		GrayS16 derivXY = new GrayS16(width,height);

		validator.compare(derivX,derivY,derivXX,derivYY,derivXY);
	}

	@Test
	public void hessianPrewitt_F32() throws NoSuchMethodException {
		CompareHessianToConvolution validator = new CompareHessianToConvolution();
		validator.setTarget(HessianFromGradient.class.getMethod("hessianPrewitt",
				GrayF32.class,GrayF32.class, GrayF32.class, GrayF32.class, GrayF32.class, ImageBorder_F32.class ));

		validator.setKernel(0,GradientPrewitt.kernelDerivX_F32);
		validator.setKernel(1,GradientPrewitt.kernelDerivY_F32);

		GrayF32 derivX = new GrayF32(width,height);
		GrayF32 derivY = new GrayF32(width,height);
		ImageMiscOps.fillUniform(derivX, rand, -10, 10);
		ImageMiscOps.fillUniform(derivY, rand, -10, 10);
		GrayF32 derivXX = new GrayF32(width,height);
		GrayF32 derivYY = new GrayF32(width,height);
		GrayF32 derivXY = new GrayF32(width,height);

		validator.compare(derivX,derivY,derivXX,derivYY,derivXY);
	}

	@Test
	public void hessianSobel_I8() throws NoSuchMethodException {
		CompareHessianToConvolution validator = new CompareHessianToConvolution();
		validator.setTarget(HessianFromGradient.class.getMethod("hessianSobel",
				GrayS16.class,GrayS16.class, GrayS16.class, GrayS16.class, GrayS16.class,ImageBorder_S32.class ));

		validator.setKernel(0,GradientSobel.kernelDerivX_I32);
		validator.setKernel(1,GradientSobel.kernelDerivY_I32);

		GrayS16 derivX = new GrayS16(width,height);
		GrayS16 derivY = new GrayS16(width,height);
		ImageMiscOps.fillUniform(derivX, rand, -10, 10);
		ImageMiscOps.fillUniform(derivY, rand, -10, 10);
		GrayS16 derivXX = new GrayS16(width,height);
		GrayS16 derivYY = new GrayS16(width,height);
		GrayS16 derivXY = new GrayS16(width,height);

		validator.compare(derivX,derivY,derivXX,derivYY,derivXY);
	}

	@Test
	public void hessianSobel_F32() throws NoSuchMethodException {
		CompareHessianToConvolution validator = new CompareHessianToConvolution();
		validator.setTarget(HessianFromGradient.class.getMethod("hessianSobel",
				GrayF32.class,GrayF32.class, GrayF32.class, GrayF32.class, GrayF32.class,ImageBorder_F32.class ));

		validator.setKernel(0,GradientSobel.kernelDerivX_F32);
		validator.setKernel(1,GradientSobel.kernelDerivY_F32);

		GrayF32 derivX = new GrayF32(width,height);
		GrayF32 derivY = new GrayF32(width,height);
		ImageMiscOps.fillUniform(derivX, rand, -10, 10);
		ImageMiscOps.fillUniform(derivY, rand, -10, 10);
		GrayF32 derivXX = new GrayF32(width,height);
		GrayF32 derivYY = new GrayF32(width,height);
		GrayF32 derivXY = new GrayF32(width,height);

		validator.compare(derivX,derivY,derivXX,derivYY,derivXY);
	}

	@Test
	public void hessianThree_I8() throws NoSuchMethodException {
		CompareHessianToConvolution validator = new CompareHessianToConvolution();
		validator.setTarget(HessianFromGradient.class.getMethod("hessianThree",
				GrayS16.class,GrayS16.class, GrayS16.class, GrayS16.class, GrayS16.class,ImageBorder_S32.class ));

		validator.setKernel(0,GradientThree.kernelDeriv_I32,true);
		validator.setKernel(1,GradientThree.kernelDeriv_I32,false);

		GrayS16 derivX = new GrayS16(width,height);
		GrayS16 derivY = new GrayS16(width,height);
		ImageMiscOps.fillUniform(derivX, rand, -10, 10);
		ImageMiscOps.fillUniform(derivY, rand, -10, 10);
		GrayS16 derivXX = new GrayS16(width,height);
		GrayS16 derivYY = new GrayS16(width,height);
		GrayS16 derivXY = new GrayS16(width,height);

		validator.compare(derivX,derivY,derivXX,derivYY,derivXY);
	}

	@Test
	public void hessianThree_F32() throws NoSuchMethodException {
		CompareHessianToConvolution validator = new CompareHessianToConvolution();
		validator.setTarget(HessianFromGradient.class.getMethod("hessianThree",
				GrayF32.class,GrayF32.class, GrayF32.class, GrayF32.class, GrayF32.class,ImageBorder_F32.class ));

		validator.setKernel(0,GradientThree.kernelDeriv_F32,true);
		validator.setKernel(1,GradientThree.kernelDeriv_F32,false);

		GrayF32 derivX = new GrayF32(width,height);
		GrayF32 derivY = new GrayF32(width,height);
		ImageMiscOps.fillUniform(derivX, rand, -10, 10);
		ImageMiscOps.fillUniform(derivY, rand, -10, 10);
		GrayF32 derivXX = new GrayF32(width,height);
		GrayF32 derivYY = new GrayF32(width,height);
		GrayF32 derivXY = new GrayF32(width,height);

		validator.compare(derivX,derivY,derivXX,derivYY,derivXY);
	}
}
