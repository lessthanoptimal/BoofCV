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

package gecv.alg.filter.derivative;

import gecv.alg.drawing.impl.BasicDrawing_I8;
import gecv.alg.filter.convolve.ConvolveImageNoBorder;
import gecv.core.image.UtilImageFloat32;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestGradientThree {

	Random rand = new Random(234);

	int width = 5;
	int height = 7;

//	@Test
//	public void checkInputShape() {
//		GenericDerivativeTests.checkImageDimensionValidation(new GradientThree(), 2);
//	}

	@Test
	public void compareToConvolve_I8() throws NoSuchMethodException {
		CompareDerivativeToConvolution validator = new CompareDerivativeToConvolution();
		validator.setTarget(GradientThree.class.getMethod("process",
				ImageUInt8.class, ImageSInt16.class, ImageSInt16.class, boolean.class ));

		validator.setKernel(0,GradientThree.kernelDeriv_I32,true);
		validator.setKernel(1,GradientThree.kernelDeriv_I32,false);

		ImageUInt8 input = new ImageUInt8(width,height);
		BasicDrawing_I8.randomize(input, rand, 0, 10);
		ImageSInt16 derivX = new ImageSInt16(width,height);
		ImageSInt16 derivY = new ImageSInt16(width,height);

		validator.compare(input,derivX,derivY);
	}

@	Test
	public void compareToConvolve_F32() throws NoSuchMethodException {
		CompareDerivativeToConvolution validator = new CompareDerivativeToConvolution();
		validator.setTarget(GradientThree.class.getMethod("process",
				ImageFloat32.class, ImageFloat32.class, ImageFloat32.class, boolean.class ));

		validator.setKernel(0,GradientThree.kernelDeriv_F32,true);
		validator.setKernel(1,GradientThree.kernelDeriv_F32,false);

		ImageFloat32 input = new ImageFloat32(width,height);
		UtilImageFloat32.randomize(input, rand, 0, 10);
		ImageFloat32 derivX = new ImageFloat32(width,height);
		ImageFloat32 derivY = new ImageFloat32(width,height);

		validator.compare(input,derivX,derivY);
	}

	@Test
	public void derivX_I8() {
		ImageUInt8 img = new ImageUInt8(width, height);
		BasicDrawing_I8.randomize(img, rand, 0, 10);

		ImageSInt16 derivX = new ImageSInt16(width, height);
		ImageSInt16 convX = new ImageSInt16(width, height);

		GecvTesting.checkSubImage(this, "derivX_I8", true, img, derivX, convX);
	}

	public void derivX_I8(ImageUInt8 img, ImageSInt16 derivX, ImageSInt16 convX) {
		GradientThree.derivX_I8(img, derivX);

		// compare to the equivalent convolution
		Kernel1D_I32 kernel = new Kernel1D_I32(3, -1, 0, 1);
		ConvolveImageNoBorder.horizontal(kernel, img, convX, true);

		GecvTesting.assertEquals(derivX, convX, 1);
	}

	@Test
	public void derivY_I8() {
		ImageUInt8 img = new ImageUInt8(width, height);
		BasicDrawing_I8.randomize(img, rand, 0, 10);

		ImageSInt16 derivY = new ImageSInt16(width, height);
		ImageSInt16 convY = new ImageSInt16(width, height);

		GecvTesting.checkSubImage(this, "derivY_I8", true, img, derivY, convY);
	}

	public void derivY_I8(ImageUInt8 img, ImageSInt16 derivY, ImageSInt16 convY) {
		GradientThree.derivY_I8(img, derivY);

		// compare to the equivalent convolution
		Kernel1D_I32 kernel = new Kernel1D_I32(3, -1, 0, 1);
		ConvolveImageNoBorder.vertical(kernel, img, convY, true);

		GecvTesting.assertEquals(derivY, convY, 1);
	}

	@Test
	public void derivX_F32() {
		ImageFloat32 img = new ImageFloat32(width, height);
		UtilImageFloat32.randomize(img, rand, 0, 10);

		ImageFloat32 derivX = new ImageFloat32(width, height);
		ImageFloat32 convX = new ImageFloat32(width, height);

		GecvTesting.checkSubImage(this, "derivX_F32", true, img, derivX, convX);
	}

	public void derivX_F32(ImageFloat32 img, ImageFloat32 derivX, ImageFloat32 convX) {
		GradientThree.derivX_F32(img, derivX);

		// compare to the equivalent convolution
		Kernel1D_F32 kernel = new Kernel1D_F32(3, -0.5f, 0f, 0.5f);
		ConvolveImageNoBorder.horizontal(kernel, img, convX, true);

		GecvTesting.assertEquals(derivX, convX, 0, 1);
	}

	@Test
	public void derivY_F32() {
		ImageFloat32 img = new ImageFloat32(width, height);
		UtilImageFloat32.randomize(img, rand, 0, 10);

		ImageFloat32 derivY = new ImageFloat32(width, height);
		ImageFloat32 convY = new ImageFloat32(width, height);

		GecvTesting.checkSubImage(this, "derivY_F32", true, img, derivY, convY);
	}

	public void derivY_F32(ImageFloat32 img, ImageFloat32 derivY, ImageFloat32 convY) {
		GradientThree.derivY_F32(img, derivY);

		// compare to the equivalent convolution
		Kernel1D_F32 kernel = new Kernel1D_F32(3, -0.5f, 0, 0.5f);
		ConvolveImageNoBorder.vertical(kernel, img, convY, true);

		GecvTesting.assertEquals(derivY, convY, 0, 1);
	}
}
