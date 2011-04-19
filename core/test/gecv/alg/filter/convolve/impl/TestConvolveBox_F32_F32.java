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

import gecv.core.image.UtilImageFloat32;
import gecv.core.image.UtilImageInt8;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt32;
import gecv.struct.image.ImageInt8;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestConvolveBox_F32_F32 {

	Random rand = new Random(0xFF);

	int width = 7;
	int height = 8;

	@Test
	public void horizontal() {
		ImageFloat32 input = new ImageFloat32(width, height);
		ImageFloat32 outputA = new ImageFloat32(width, height);
		ImageFloat32 outputB = new ImageFloat32(width, height);

		UtilImageFloat32.randomize(input, rand, 0, 10);

		GecvTesting.checkSubImage(this, "horizontal", true, input, outputA, outputB);
	}

	public void horizontal(ImageFloat32 input, ImageFloat32 outputA, ImageFloat32 outputB) {
		for (int border = 0; border < 2; border++) {
			for (int i = 1; i <= 2; i++) {
				Kernel1D_F32 kernel = new Kernel1D_F32(2 * i + 1);

				for (int j = 0; j < kernel.width; j++) {
					kernel.data[j] = 1;
				}

				ConvolveImageStandard.horizontal(kernel, input, outputA, border == 0);
				ConvolveBox_F32_F32.horizontal(input, outputB, i, border == 0);
				GecvTesting.assertEquals(outputA, outputB, 0, 1e-4f);
			}
		}
	}

	@Test
	public void vertical() {
		ImageFloat32 input = new ImageFloat32(width, height);
		ImageFloat32 outputA = new ImageFloat32(width, height);
		ImageFloat32 outputB = new ImageFloat32(width, height);

		UtilImageFloat32.randomize(input, rand, 0, 10);

		GecvTesting.checkSubImage(this, "vertical", true, input, outputA, outputB);
	}

	public void vertical(ImageFloat32 input, ImageFloat32 outputA, ImageFloat32 outputB) {
		for (int border = 0; border < 2; border++) {
			for (int i = 1; i <= 2; i++) {
				Kernel1D_F32 kernel = new Kernel1D_F32(2 * i + 1);

				for (int j = 0; j < kernel.width; j++) {
					kernel.data[j] = 1;
				}

				ConvolveImageStandard.vertical(kernel, input, outputA, border == 0);
				ConvolveBox_F32_F32.vertical(input, outputB, i, border == 0);
				GecvTesting.assertEquals(outputA, outputB, 0, 1e-4f);
			}
		}
	}
}
