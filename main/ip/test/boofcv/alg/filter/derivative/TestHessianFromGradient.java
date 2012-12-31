/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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
import boofcv.core.image.border.ImageBorder_I32;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;
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
				ImageSInt16.class,ImageSInt16.class, ImageSInt16.class, ImageSInt16.class, ImageSInt16.class, ImageBorder_I32.class ));

		validator.setKernel(0,GradientPrewitt.kernelDerivX_I32);
		validator.setKernel(1,GradientPrewitt.kernelDerivY_I32);

		ImageSInt16 derivX = new ImageSInt16(width,height);
		ImageSInt16 derivY = new ImageSInt16(width,height);
		ImageMiscOps.fillUniform(derivX, rand, -10, 10);
		ImageMiscOps.fillUniform(derivY, rand, -10, 10);
		ImageSInt16 derivXX = new ImageSInt16(width,height);
		ImageSInt16 derivYY = new ImageSInt16(width,height);
		ImageSInt16 derivXY = new ImageSInt16(width,height);

		validator.compare(derivX,derivY,derivXX,derivYY,derivXY);
	}

	@Test
	public void hessianPrewitt_F32() throws NoSuchMethodException {
		CompareHessianToConvolution validator = new CompareHessianToConvolution();
		validator.setTarget(HessianFromGradient.class.getMethod("hessianPrewitt",
				ImageFloat32.class,ImageFloat32.class, ImageFloat32.class, ImageFloat32.class, ImageFloat32.class, ImageBorder_F32.class ));

		validator.setKernel(0,GradientPrewitt.kernelDerivX_F32);
		validator.setKernel(1,GradientPrewitt.kernelDerivY_F32);

		ImageFloat32 derivX = new ImageFloat32(width,height);
		ImageFloat32 derivY = new ImageFloat32(width,height);
		ImageMiscOps.fillUniform(derivX, rand, -10, 10);
		ImageMiscOps.fillUniform(derivY, rand, -10, 10);
		ImageFloat32 derivXX = new ImageFloat32(width,height);
		ImageFloat32 derivYY = new ImageFloat32(width,height);
		ImageFloat32 derivXY = new ImageFloat32(width,height);

		validator.compare(derivX,derivY,derivXX,derivYY,derivXY);
	}

	@Test
	public void hessianSobel_I8() throws NoSuchMethodException {
		CompareHessianToConvolution validator = new CompareHessianToConvolution();
		validator.setTarget(HessianFromGradient.class.getMethod("hessianSobel",
				ImageSInt16.class,ImageSInt16.class, ImageSInt16.class, ImageSInt16.class, ImageSInt16.class,ImageBorder_I32.class ));

		validator.setKernel(0,GradientSobel.kernelDerivX_I32);
		validator.setKernel(1,GradientSobel.kernelDerivY_I32);

		ImageSInt16 derivX = new ImageSInt16(width,height);
		ImageSInt16 derivY = new ImageSInt16(width,height);
		ImageMiscOps.fillUniform(derivX, rand, -10, 10);
		ImageMiscOps.fillUniform(derivY, rand, -10, 10);
		ImageSInt16 derivXX = new ImageSInt16(width,height);
		ImageSInt16 derivYY = new ImageSInt16(width,height);
		ImageSInt16 derivXY = new ImageSInt16(width,height);

		validator.compare(derivX,derivY,derivXX,derivYY,derivXY);
	}

	@Test
	public void hessianSobel_F32() throws NoSuchMethodException {
		CompareHessianToConvolution validator = new CompareHessianToConvolution();
		validator.setTarget(HessianFromGradient.class.getMethod("hessianSobel",
				ImageFloat32.class,ImageFloat32.class, ImageFloat32.class, ImageFloat32.class, ImageFloat32.class,ImageBorder_F32.class ));

		validator.setKernel(0,GradientSobel.kernelDerivX_F32);
		validator.setKernel(1,GradientSobel.kernelDerivY_F32);

		ImageFloat32 derivX = new ImageFloat32(width,height);
		ImageFloat32 derivY = new ImageFloat32(width,height);
		ImageMiscOps.fillUniform(derivX, rand, -10, 10);
		ImageMiscOps.fillUniform(derivY, rand, -10, 10);
		ImageFloat32 derivXX = new ImageFloat32(width,height);
		ImageFloat32 derivYY = new ImageFloat32(width,height);
		ImageFloat32 derivXY = new ImageFloat32(width,height);

		validator.compare(derivX,derivY,derivXX,derivYY,derivXY);
	}

	@Test
	public void hessianThree_I8() throws NoSuchMethodException {
		CompareHessianToConvolution validator = new CompareHessianToConvolution();
		validator.setTarget(HessianFromGradient.class.getMethod("hessianThree",
				ImageSInt16.class,ImageSInt16.class, ImageSInt16.class, ImageSInt16.class, ImageSInt16.class,ImageBorder_I32.class ));

		validator.setKernel(0,GradientThree.kernelDeriv_I32,true);
		validator.setKernel(1,GradientThree.kernelDeriv_I32,false);

		ImageSInt16 derivX = new ImageSInt16(width,height);
		ImageSInt16 derivY = new ImageSInt16(width,height);
		ImageMiscOps.fillUniform(derivX, rand, -10, 10);
		ImageMiscOps.fillUniform(derivY, rand, -10, 10);
		ImageSInt16 derivXX = new ImageSInt16(width,height);
		ImageSInt16 derivYY = new ImageSInt16(width,height);
		ImageSInt16 derivXY = new ImageSInt16(width,height);

		validator.compare(derivX,derivY,derivXX,derivYY,derivXY);
	}

	@Test
	public void hessianThree_F32() throws NoSuchMethodException {
		CompareHessianToConvolution validator = new CompareHessianToConvolution();
		validator.setTarget(HessianFromGradient.class.getMethod("hessianThree",
				ImageFloat32.class,ImageFloat32.class, ImageFloat32.class, ImageFloat32.class, ImageFloat32.class,ImageBorder_F32.class ));

		validator.setKernel(0,GradientThree.kernelDeriv_F32,true);
		validator.setKernel(1,GradientThree.kernelDeriv_F32,false);

		ImageFloat32 derivX = new ImageFloat32(width,height);
		ImageFloat32 derivY = new ImageFloat32(width,height);
		ImageMiscOps.fillUniform(derivX, rand, -10, 10);
		ImageMiscOps.fillUniform(derivY, rand, -10, 10);
		ImageFloat32 derivXX = new ImageFloat32(width,height);
		ImageFloat32 derivYY = new ImageFloat32(width,height);
		ImageFloat32 derivXY = new ImageFloat32(width,height);

		validator.compare(derivX,derivY,derivXX,derivYY,derivXY);
	}
}
