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

package gecv.alg.filter.derivative.three;

import gecv.alg.drawing.impl.BasicDrawing_I8;
import gecv.core.image.UtilImageFloat32;
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
public class TestGradientThree_Standard {

	private final Random rand = new Random(0xfeed);

	private final int width = 4;
	private final int height = 5;

	private static float A = 0.5f;

	@Test
	public void deriv_F32() {
		ImageFloat32 img = new ImageFloat32(width, height);
		UtilImageFloat32.randomize(img, rand, 0f, 1.0f);

		ImageFloat32 derivX = new ImageFloat32(width, height);
		ImageFloat32 derivY = new ImageFloat32(width, height);

		GecvTesting.checkSubImage(this, "deriv_F32", true, img, derivX, derivY);
	}

	public void deriv_F32(ImageFloat32 img, ImageFloat32 derivX, ImageFloat32 derivY) {
		ImageFloat32 derivX_a = new ImageFloat32(width, height);
		ImageFloat32 derivY_a = new ImageFloat32(width, height);

		GradientThree_Standard.derivX_F32(img, derivX_a);
		GradientThree_Standard.derivY_F32(img, derivY_a);
		GradientThree_Standard.deriv_F32(img, derivX, derivY);

		for (int i = 1; i < height - 1; i++) {
			for (int j = 1; j < width - 1; j++) {
				assertEquals(derivX.get(j, i), derivX_a.get(j, i), 1e-5);
				assertEquals(derivY.get(j, i), derivY_a.get(j, i), 1e-5);
			}
		}
	}

	/**
	 * Compare the results to a hand computed value
	 */
	@Test
	public void deriv_I8() {
		ImageUInt8 img = new ImageUInt8(width, height);
		BasicDrawing_I8.randomize(img, rand);

		ImageSInt16 derivX = new ImageSInt16(width, height);
		ImageSInt16 derivY = new ImageSInt16(width, height);

		GecvTesting.checkSubImage(this, "deriv_I8", true, img, derivX, derivY);
	}

	public void deriv_I8(ImageUInt8 img, ImageSInt16 derivX, ImageSInt16 derivY) {
		GradientThree_Standard.deriv_I8(img, derivX, derivY);

		int dX = img.get(2, 1) - img.get(0, 1);
		int dY = img.get(1, 2) - img.get(1, 0);

		assertEquals(dX, derivX.get(1, 1), 1e-6);
		assertEquals(dY, derivY.get(1, 1), 1e-6);
	}

	/**
	 * Compare the results to a hand computed value
	 */
	@Test
	public void derivX_I8() {
		ImageUInt8 img = new ImageUInt8(width, height);
		BasicDrawing_I8.randomize(img, rand);

		ImageSInt16 derivX = new ImageSInt16(width, height);

		GecvTesting.checkSubImage(this, "derivX_I8", true, img, derivX);
	}

	public void derivX_I8(ImageUInt8 img, ImageSInt16 derivX) {
		GradientThree_Standard.derivX_I8(img, derivX);

		int dX = img.get(2, 1) - img.get(0, 1);

		assertEquals(dX, derivX.get(1, 1), 1e-6);
	}

	/**
	 * Compare the results to a hand computed value
	 */
	@Test
	public void derivY_I8() {
		ImageUInt8 img = new ImageUInt8(width, height);
		BasicDrawing_I8.randomize(img, rand);

		ImageSInt16 derivY = new ImageSInt16(width, height);

		GecvTesting.checkSubImage(this, "derivY_I8", true, img, derivY);
	}

	public void derivY_I8(ImageUInt8 img, ImageSInt16 derivY) {
		GradientThree_Standard.derivY_I8(img, derivY);

		int dY = img.get(1, 2) - img.get(1, 0);

		assertEquals(dY, derivY.get(1, 1), 1e-6);
	}

	@Test
	public void derivX_F32() {
		ImageFloat32 img = new ImageFloat32(width, height);
		UtilImageFloat32.randomize(img, rand, 0f, 1.0f);

		ImageFloat32 derivX = new ImageFloat32(width, height);

		GecvTesting.checkSubImage(this, "derivX_F32", true, img, derivX);
	}

	public void derivX_F32(ImageFloat32 img, ImageFloat32 derivX) {
		GradientThree_Standard.derivX_F32(img, derivX);

		assertEquals((img.get(2, 0) - img.get(0, 0)) * A, derivX.get(1, 0), 1e-6);
		assertEquals((img.get(2, 1) - img.get(0, 1)) * A, derivX.get(1, 1), 1e-6);
		assertEquals((img.get(2, 2) - img.get(0, 2)) * A, derivX.get(1, 2), 1e-6);
		assertEquals((img.get(3, 0) - img.get(1, 0)) * A, derivX.get(2, 0), 1e-6);
	}

	@Test
	public void derivY_F32() {
		ImageFloat32 img = new ImageFloat32(width, height);
		UtilImageFloat32.randomize(img, rand, 0f, 1.0f);

		ImageFloat32 derivY = new ImageFloat32(width, height);

		GecvTesting.checkSubImage(this, "derivY_F32", true, img, derivY);
	}

	public void derivY_F32(ImageFloat32 img, ImageFloat32 derivY) {
		GradientThree_Standard.derivY_F32(img, derivY);

		assertEquals((img.get(0, 2) - img.get(0, 0)) * A, derivY.get(0, 1), 1e-6);
		assertEquals((img.get(1, 2) - img.get(1, 0)) * A, derivY.get(1, 1), 1e-6);
		assertEquals((img.get(2, 2) - img.get(2, 0)) * A, derivY.get(2, 1), 1e-6);
		assertEquals((img.get(0, 3) - img.get(0, 1)) * A, derivY.get(0, 2), 1e-6);
	}
}
