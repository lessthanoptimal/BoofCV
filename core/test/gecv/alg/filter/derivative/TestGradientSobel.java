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

import gecv.alg.filter.convolve.ConvolveImage;
import gecv.core.image.UtilImageFloat32;
import gecv.core.image.UtilImageInt8;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt16;
import gecv.struct.image.ImageInt8;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestGradientSobel {
	Random rand = new Random(234);

	int width = 5;
	int height = 7;

	@Test
	public void checkInputShape() {
		GecvTesting.checkImageDimensionValidation(new GradientSobel(), 2);
	}

	@Test
	public void process_I8() {
		ImageInt8 img = new ImageInt8(width, height);
		UtilImageInt8.randomize(img, rand, 0, 10);

		ImageInt16 derivX = new ImageInt16(width, height);
		ImageInt16 derivY = new ImageInt16(width, height);

		GecvTesting.checkSubImage(this, "process_I8", true, img, derivX, derivY);
	}

	public void process_I8(ImageInt8 img, ImageInt16 derivX, ImageInt16 derivY) {
		GradientSobel.process_I8(img, derivX, derivY);

		// compare to the equivalent convolution
		Kernel1D_I32 kernel1 = new Kernel1D_I32(3, 1, 2, 1);
		Kernel1D_I32 kernel2 = new Kernel1D_I32(3, -1, 0, 1);

		ImageInt16 temp = new ImageInt16(width, height);
		ImageInt16 convX = new ImageInt16(width, height);
		ImageInt16 convY = new ImageInt16(width, height);

		ConvolveImage.horizontal(kernel1, img, temp, true);
		ConvolveImage.vertical(kernel2, temp, convY, true);

		ConvolveImage.vertical(kernel1, img, temp, true);
		ConvolveImage.horizontal(kernel2, temp, convX, true);

		GecvTesting.assertEquals(derivX, convX, 1);
		GecvTesting.assertEquals(derivY, convY, 1);
	}

	@Test
	public void process_F32() {
		ImageFloat32 img = new ImageFloat32(width, height);
		UtilImageFloat32.randomize(img, rand, 0, 10);

		ImageFloat32 derivX = new ImageFloat32(width, height);
		ImageFloat32 derivY = new ImageFloat32(width, height);

		GecvTesting.checkSubImage(this, "process_F32", true, img, derivX, derivY);
	}

	public void process_F32(ImageFloat32 img, ImageFloat32 derivX, ImageFloat32 derivY) {
		GradientSobel.process_F32(img, derivX, derivY);

		// compare to the equivalent convolution
		Kernel1D_F32 kernel1 = new Kernel1D_F32(3, 0.25f, 0.5f, 0.25f);
		Kernel1D_F32 kernel2 = new Kernel1D_F32(3, -1, 0, 1);

		ImageFloat32 temp = new ImageFloat32(width, height);
		ImageFloat32 convX = new ImageFloat32(width, height);
		ImageFloat32 convY = new ImageFloat32(width, height);

		ConvolveImage.horizontal(kernel1, img, temp, true);
		ConvolveImage.vertical(kernel2, temp, convY, true);

		ConvolveImage.vertical(kernel1, img, temp, true);
		ConvolveImage.horizontal(kernel2, temp, convX, true);

		GecvTesting.assertEquals(derivX, convX, 1, 1e-4f);
		GecvTesting.assertEquals(derivY, convY, 1, 1e-4f);
	}
}
