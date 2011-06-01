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

package gecv.alg.filter.derivative.impl;

import gecv.alg.misc.ImageTestingOps;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestGradientSobel_UnrolledOuter {
	Random rand = new Random(234);

	int width = 200;
	int height = 250;

	/**
	 * See if the same results are returned by ImageByte2D equivalent
	 */
	@Test
	public void process_I8_naive() {
		for( int offY = 0; offY < 3; offY++ ) {
			for( int offX = 0; offX < 3; offX++ ) {
				int w = width+offX; int h = height+offY;
				ImageUInt8 img = new ImageUInt8(w, h);
				ImageTestingOps.randomize(img, new Random(0xfeed), 0, 100);

				ImageSInt16 derivX = new ImageSInt16(w, h);
				ImageSInt16 derivY = new ImageSInt16(w, h);

				ImageSInt16 derivX2 = new ImageSInt16(w, h);
				ImageSInt16 derivY2 = new ImageSInt16(w, h);

				GradientSobel_Naive.process(img, derivX2, derivY2);
				GradientSobel_UnrolledOuter.process_I8(img, derivX, derivY);

				GecvTesting.assertEquals(derivX2, derivX, 0);
				GecvTesting.assertEquals(derivY2, derivY, 0);
			}
		}
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
				ImageTestingOps.randomize(img, rand, 0f, 255f);

				ImageFloat32 derivX = new ImageFloat32(w, h);
				ImageFloat32 derivY = new ImageFloat32(w, h);

				ImageFloat32 derivX2 = new ImageFloat32(w, h);
				ImageFloat32 derivY2 = new ImageFloat32(w, h);

				GradientSobel_Naive.process(img, derivX2, derivY2);
				GradientSobel_UnrolledOuter.process_F32(img, derivX, derivY);

				GecvTesting.assertEquals(derivX2, derivX, 0, 1e-4f);
				GecvTesting.assertEquals(derivY2, derivY, 0, 1e-4f);
			}
		}
	}

	@Test
	public void process_F32_sub_naive() {
		for( int offY = 0; offY < 3; offY++ ) {
			for( int offX = 0; offX < 3; offX++ ) {
				int w = width+offX; int h = height+offY;
				ImageFloat32 img = new ImageFloat32(w, h);
				ImageTestingOps.randomize(img, rand, 0f, 255f);

				ImageFloat32 derivX = new ImageFloat32(w, h);
				ImageFloat32 derivY = new ImageFloat32(w, h);

				GecvTesting.checkSubImage(this, "process_F32_sub_naive", true, img, derivX, derivY);
			}
		}
	}

	public void process_F32_sub_naive(ImageFloat32 img, ImageFloat32 derivX, ImageFloat32 derivY) {
		ImageFloat32 derivX2 = new ImageFloat32(derivX.width, derivX.height);
		ImageFloat32 derivY2 = new ImageFloat32(derivX.width, derivX.height);

		GradientSobel_Naive.process(img, derivX2, derivY2);
		GradientSobel_UnrolledOuter.process_F32_sub(img, derivX, derivY);

		GecvTesting.assertEquals(derivX2, derivX, 0, 1e-4f);
		GecvTesting.assertEquals(derivY2, derivY, 0, 1e-4f);
	}
}
