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

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestGradientSobel_Naive {

	Random rand = new Random(0xfeed);

	private final int width = 4;
	private final int height = 5;

	/**
	 * Compare the results to a hand computed value
	 */
	@Test
	public void compareToKnown_I8() {
		GrayU8 img = new GrayU8(width, height);
		ImageMiscOps.fillUniform(img, rand, 0, 100);

		GrayS16 derivX = new GrayS16(width, height);
		GrayS16 derivY = new GrayS16(width, height);

		BoofTesting.checkSubImage(this, "compareToKnown_I8", true, img, derivX, derivY);
	}

	public void compareToKnown_I8(GrayU8 img, GrayS16 derivX, GrayS16 derivY) {
		GradientSobel_Naive.process(img, derivX, derivY);

		int dX = -((img.get(0, 2) + img.get(0, 0)) + img.get(0, 1) * 2);
		dX += (img.get(2, 2) + img.get(2, 0)) + img.get(2, 1) * 2;

		int dY = -((img.get(2, 0) + img.get(0, 0)) + img.get(1, 0) * 2);
		dY += (img.get(2, 2) + img.get(0, 2)) + img.get(1, 2) * 2;

		assertEquals(dX, derivX.get(1, 1), 1e-6);
		assertEquals(dY, derivY.get(1, 1), 1e-6);
	}

	/**
	 * Compare the results to a hand computed value
	 */
	@Test
	public void compareToKnown_F32() {
		GrayF32 img = new GrayF32(width, height);
		ImageMiscOps.fillUniform(img, rand, 0, 255);

		GrayF32 derivX = new GrayF32(width, height);
		GrayF32 derivY = new GrayF32(width, height);

		BoofTesting.checkSubImage(this, "compareToKnown_F32", true, img, derivX, derivY);

	}

	public void compareToKnown_F32(GrayF32 img, GrayF32 derivX, GrayF32 derivY) {
		GradientSobel_Naive.process(img, derivX, derivY);

		float dX = -((img.get(0, 2) + img.get(0, 0)) * 0.25f + img.get(0, 1) * 0.5f);
		dX += (img.get(2, 2) + img.get(2, 0)) * 0.25f + img.get(2, 1) * 0.5f;

		float dY = -((img.get(2, 0) + img.get(0, 0)) * 0.25f + img.get(1, 0) * 0.5f);
		dY += (img.get(2, 2) + img.get(0, 2)) * 0.25f + img.get(1, 2) * 0.5f;

		assertEquals(dX, derivX.get(1, 1), 1e-6);
		assertEquals(dY, derivY.get(1, 1), 1e-6);
	}
}
