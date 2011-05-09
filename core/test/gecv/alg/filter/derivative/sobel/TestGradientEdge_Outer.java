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

package gecv.alg.filter.derivative.sobel;

import gecv.alg.drawing.impl.BasicDrawing_I8;
import gecv.core.image.UtilImageFloat32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt16;
import gecv.struct.image.ImageInt8;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestGradientEdge_Outer {

	Random rand = new Random(234);

	int width = 200;
	int height = 250;

	/**
	 * See if the same results are returned by the simple naive algorithm
	 */
	@Test
	public void process_I8_naive() {

		for( int offY = 0; offY < 3; offY++ ) {
			for( int offX = 0; offX < 3; offX++ ) {
				int w = width+offX; int h = height+offY;
				ImageInt8 img = new ImageInt8(w, h);
				BasicDrawing_I8.randomize(img, new Random(0xfeed));

				ImageInt16 derivX = new ImageInt16(w, h, true);
				ImageInt16 derivY = new ImageInt16(w, h, true);

				ImageInt16 derivX2 = new ImageInt16(w, h, true);
				ImageInt16 derivY2 = new ImageInt16(w, h, true);

				GradientSobel_Naive.process_I8(img, derivX2, derivY2);
				GradientSobel_Outer.process_I8(img, derivX, derivY);

				GecvTesting.assertEquals(derivX2, derivX, 0);
				GecvTesting.assertEquals(derivY2, derivY, 0);
			}
		}
	}

	@Test
	public void process_I8_sub_naive() {
		for( int offY = 0; offY < 3; offY++ ) {
			for( int offX = 0; offX < 3; offX++ ) {
				int w = width+offX; int h = height+offY;
				ImageInt8 img = new ImageInt8(w, h);
				BasicDrawing_I8.randomize(img, new Random(0xfeed));

				ImageInt16 derivX = new ImageInt16(w, h, true);
				ImageInt16 derivY = new ImageInt16(w, h, true);

				GecvTesting.checkSubImage(this, "process_I8_sub_naive", true, img, derivX, derivY);
			}
		}
	}

	public void process_I8_sub_naive(ImageInt8 img, ImageInt16 derivX, ImageInt16 derivY) {
		ImageInt16 derivX2 = new ImageInt16(derivX.width, derivX.height, true);
		ImageInt16 derivY2 = new ImageInt16(derivX.width, derivX.height, true);

		GradientSobel_Naive.process_I8(img, derivX2, derivY2);
		GradientSobel_Outer.process_I8_sub(img, derivX, derivY);

		GecvTesting.assertEquals(derivX2, derivX, 0);
		GecvTesting.assertEquals(derivY2, derivY, 0);
	}

	/**
	 * See if the same results are returned by ImageByte2D equivalent
	 */
	@Test
	public void process_F32_naive() {
		for( int offY = 0; offY < 3; offY++ ) {
			for( int offX = 0; offX < 3; offX++ ) {
				int w = width+offX; int h = height+offY;
				ImageFloat32 img = new ImageFloat32(w, h);
				UtilImageFloat32.randomize(img, rand, 0f, 255f);

				ImageFloat32 derivX = new ImageFloat32(w, h);
				ImageFloat32 derivY = new ImageFloat32(w, h);

				ImageFloat32 derivX2 = new ImageFloat32(w, h);
				ImageFloat32 derivY2 = new ImageFloat32(w, h);

				GradientSobel_Naive.process_F32(img, derivX2, derivY2);
				GradientSobel_Outer.process_F32(img, derivX, derivY);

				GecvTesting.assertEquals(derivX2, derivX, 0, 1e-4f);
				GecvTesting.assertEquals(derivY2, derivY, 0, 1e-4f);
			}
		}
	}
}
