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

import gecv.alg.filter.convolve.ConvolveImage;
import gecv.core.image.UtilImageInt8;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.image.ImageInt16;
import gecv.struct.image.ImageInt8;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestConvolveBox_I8_I16 {

	Random rand = new Random(0xFF);

	int width = 7;
	int height = 8;

	@Test
	public void horizontal() {
		ImageInt8 input = new ImageInt8(width, height);
		ImageInt16 outputA = new ImageInt16(width, height);
		ImageInt16 outputB = new ImageInt16(width, height);

		UtilImageInt8.randomize(input, rand);

		GecvTesting.checkSubImage(this, "horizontal", true, input, outputA, outputB);
	}

	public void horizontal(ImageInt8 input, ImageInt16 outputA, ImageInt16 outputB) {
		for (int border = 0; border < 2; border++) {
			for (int i = 1; i <= 2; i++) {
				Kernel1D_I32 kernel = new Kernel1D_I32(2 * i + 1);

				for (int j = 0; j < kernel.width; j++) {
					kernel.data[j] = 1;
				}

				ConvolveImageStandard.horizontal(kernel, input, outputA, border == 0);
				ConvolveBox_I8_I16.horizontal(input, outputB, i, border == 0);
				GecvTesting.assertEquals(outputA, outputB, 0);
			}
		}
	}

	@Test
	public void vertical() {
		ImageInt8 input = new ImageInt8(width, height);
		ImageInt16 outputA = new ImageInt16(width, height);
		ImageInt16 outputB = new ImageInt16(width, height);

		UtilImageInt8.randomize(input, rand);

		GecvTesting.checkSubImage(this, "vertical", true, input, outputA, outputB);
	}

	public void vertical(ImageInt8 input, ImageInt16 outputA, ImageInt16 outputB) {
		for (int border = 0; border < 2; border++) {
			for (int i = 1; i <= 2; i++) {
				Kernel1D_I32 kernel = new Kernel1D_I32(2 * i + 1);

				for (int j = 0; j < kernel.width; j++) {
					kernel.data[j] = 1;
				}

				ConvolveImageStandard.vertical(kernel, input, outputA, border == 0);
				ConvolveBox_I8_I16.vertical(input, outputB, i, border == 0);
				GecvTesting.assertEquals(outputA, outputB, 0);
			}
		}
	}
}
