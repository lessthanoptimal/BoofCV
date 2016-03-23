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

package boofcv.alg.filter.derivative.impl;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
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
				GrayU8 img = new GrayU8(w, h);
				ImageMiscOps.fillUniform(img, new Random(0xfeed), 0, 100);

				GrayS16 derivX = new GrayS16(w, h);
				GrayS16 derivY = new GrayS16(w, h);

				GrayS16 derivX2 = new GrayS16(w, h);
				GrayS16 derivY2 = new GrayS16(w, h);

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
				GrayU8 img = new GrayU8(w, h);
				ImageMiscOps.fillUniform(img, new Random(0xfeed), 0, 100);

				GrayS16 derivX = new GrayS16(w, h);
				GrayS16 derivY = new GrayS16(w, h);

				BoofTesting.checkSubImage(this, "process_I8_sub_naive", true, img, derivX, derivY);
			}
		}
	}

	public void process_I8_sub_naive(GrayU8 img, GrayS16 derivX, GrayS16 derivY) {
		GrayS16 derivX2 = new GrayS16(derivX.width, derivX.height);
		GrayS16 derivY2 = new GrayS16(derivX.width, derivX.height);

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
				GrayF32 img = new GrayF32(w, h);
				ImageMiscOps.fillUniform(img, rand, 0f, 255f);

				GrayF32 derivX = new GrayF32(w, h);
				GrayF32 derivY = new GrayF32(w, h);

				GrayF32 derivX2 = new GrayF32(w, h);
				GrayF32 derivY2 = new GrayF32(w, h);

				GradientSobel_Naive.process(img, derivX2, derivY2);
				GradientSobel_Outer.process_F32(img, derivX, derivY);

				BoofTesting.assertEquals(derivX2, derivX, 1e-4f);
				BoofTesting.assertEquals(derivY2, derivY, 1e-4f);
			}
		}
	}
}
