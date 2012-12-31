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

package boofcv.alg.filter.derivative.impl;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestGradientSobel_Outer {

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
				ImageUInt8 img = new ImageUInt8(w, h);
				ImageMiscOps.fillUniform(img, new Random(0xfeed), 0, 100);

				ImageSInt16 derivX = new ImageSInt16(w, h);
				ImageSInt16 derivY = new ImageSInt16(w, h);

				ImageSInt16 derivX2 = new ImageSInt16(w, h);
				ImageSInt16 derivY2 = new ImageSInt16(w, h);

				GradientSobel_Naive.process(img, derivX2, derivY2);
				GradientSobel_Outer.process_I8(img, derivX, derivY);

				BoofTesting.assertEquals(derivX2, derivX, 0);
				BoofTesting.assertEquals(derivY2, derivY, 0);
			}
		}
	}

	@Test
	public void process_I8_sub_naive() {
		for( int offY = 0; offY < 3; offY++ ) {
			for( int offX = 0; offX < 3; offX++ ) {
				int w = width+offX; int h = height+offY;
				ImageUInt8 img = new ImageUInt8(w, h);
				ImageMiscOps.fillUniform(img, new Random(0xfeed), 0, 100);

				ImageSInt16 derivX = new ImageSInt16(w, h);
				ImageSInt16 derivY = new ImageSInt16(w, h);

				BoofTesting.checkSubImage(this, "process_I8_sub_naive", true, img, derivX, derivY);
			}
		}
	}

	public void process_I8_sub_naive(ImageUInt8 img, ImageSInt16 derivX, ImageSInt16 derivY) {
		ImageSInt16 derivX2 = new ImageSInt16(derivX.width, derivX.height);
		ImageSInt16 derivY2 = new ImageSInt16(derivX.width, derivX.height);

		GradientSobel_Naive.process(img, derivX2, derivY2);
		GradientSobel_Outer.process_I8_sub(img, derivX, derivY);

		BoofTesting.assertEquals(derivX2, derivX, 0);
		BoofTesting.assertEquals(derivY2, derivY, 0);
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
				ImageMiscOps.fillUniform(img, rand, 0f, 255f);

				ImageFloat32 derivX = new ImageFloat32(w, h);
				ImageFloat32 derivY = new ImageFloat32(w, h);

				ImageFloat32 derivX2 = new ImageFloat32(w, h);
				ImageFloat32 derivY2 = new ImageFloat32(w, h);

				GradientSobel_Naive.process(img, derivX2, derivY2);
				GradientSobel_Outer.process_F32(img, derivX, derivY);

				BoofTesting.assertEquals(derivX2, derivX, 1e-4f);
				BoofTesting.assertEquals(derivY2, derivY, 1e-4f);
			}
		}
	}
}
