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

package gecv.alg.interpolate.impl;

import gecv.core.image.UtilImageFloat32;
import gecv.struct.image.ImageFloat32;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;


/**
 * @author Peter Abeles
 */
public class TestBilinearPixel_F32 {

	Random rand = new Random(0xff34);

	int width = 320;
	int height = 240;

	/**
	 * Checks to see if inBounds correctly determines if a point is in bounds with positive
	 * and negative examples
	 */
	@Test
	public void inBounds() {
		ImageFloat32 img = new ImageFloat32(width, height);
		UtilImageFloat32.randomize(img, rand, 0, 100);

		GecvTesting.checkSubImage(this, "inBounds", false, img);
	}

	public void inBounds(ImageFloat32 img) {
		BilinearPixel_F32 interp = new BilinearPixel_F32(img);

		assertTrue(interp.inBounds(0, 0));
		assertTrue(interp.inBounds(10, 0));
		assertTrue(interp.inBounds(0, 10));
		assertTrue(interp.inBounds(0.001f, 0.002f));
		assertTrue(interp.inBounds(width - 1.1f, height - 1.1f));

		assertFalse(interp.inBounds(width - 0.1f, height - 0.1f));
		assertFalse(interp.inBounds(-0.1f, -0.1f));
	}

	/**
	 * Checks value returned by get()
	 */
	@Test
	public void get() {
		ImageFloat32 img = new ImageFloat32(width, height);
		UtilImageFloat32.randomize(img, rand, 0, 100);

		GecvTesting.checkSubImage(this, "get", false, img);
	}

	public void get(ImageFloat32 img) {
		BilinearPixel_F32 interp = new BilinearPixel_F32(img);

		assertEquals(compute(img, 10, 10), interp.get(10, 10), 1e-5f);
		assertEquals(compute(img, 10.1f, 10), interp.get(10.1f, 10), 1e-5f);
		assertEquals(compute(img, 10, 10.6f), interp.get(10, 10.6f), 1e-5f);
		assertEquals(compute(img, 10.8f, 10.6f), interp.get(10.8f, 10.6f), 1e-5f);
	}

	/**
	 * Compute a bilinear interpolation manually
	 */
	private float compute(ImageFloat32 img, float x, float y) {
		int gX = (int) x;
		int gY = (int) y;

		float v0 = img.get(gX, gY);
		float v1 = img.get(gX + 1, gY);
		float v2 = img.get(gX, gY + 1);
		float v3 = img.get(gX + 1, gY + 1);

		x %= 1f;
		y %= 1f;

		float a = 1f - x;
		float b = 1f - y;

		return a * b * v0 + x * b * v1 + a * y * v2 + x * y * v3;
	}

	/**
	 * Sees if get throws an exception if it is out of bounds
	 */
	@Test(expected = IllegalArgumentException.class)
	public void get_outside() {
		ImageFloat32 img = new ImageFloat32(width, height);

		BilinearPixel_F32 interp = new BilinearPixel_F32(img);

		interp.get(500, 10);
	}

	/**
	 * Compare get_unsafe against the value returned by get()
	 */
	@Test
	public void get_unsafe() {
		ImageFloat32 img = new ImageFloat32(width, height);
		UtilImageFloat32.randomize(img, rand, 0, 100);

		GecvTesting.checkSubImage(this, "get_unsafe", false, img);
	}

	public void get_unsafe(ImageFloat32 img) {
		BilinearPixel_F32 interp = new BilinearPixel_F32(img);

		assertEquals(interp.get(10, 10), interp.get_unsafe(10, 10), 1e-6);
		assertEquals(interp.get(10.1f, 10), interp.get_unsafe(10.1f, 10), 1e-6);
		assertEquals(interp.get(10, 10.6f), interp.get_unsafe(10, 10.6f), 1e-6);
		assertEquals(interp.get(10.8f, 10.6f), interp.get_unsafe(10.8f, 10.6f), 1e-6);
	}

	@Test
	public void getImage() {
		ImageFloat32 img = new ImageFloat32(width, height);
		BilinearPixel_F32 interp = new BilinearPixel_F32(img);
		assertTrue(img == interp.getImage());
	}

	@Test
	public void getBorderOffsets() {
		ImageFloat32 img = new ImageFloat32(width, height);
		BilinearPixel_F32 interp = new BilinearPixel_F32(img);

		int[] border = interp.getBorderOffsets();

		// top border
		if (border[0] > 0) {
			try {
				interp.get(40, border[0] - 1);
				fail("Should have thrown exception");
			} catch (RuntimeException e) {
			}
		} else {
			interp.get(40, 0);
		}

		// right border
		if (border[1] > 0) {
			try {
				interp.get(width - border[1], 40);
				fail("Should have thrown exception");
			} catch (RuntimeException e) {
			}
		} else {
			interp.get(width - 1, 40);
		}

		// bottom border
		if (border[2] > 0) {
			try {
				interp.get(40, height - border[2]);
				fail("Should have thrown exception");
			} catch (RuntimeException e) {
			}
		} else {
			interp.get(40, height - 1);
		}

		// left border
		if (border[3] > 0) {
			try {
				interp.get(border[3] - 1, 40);
				fail("Should have thrown exception");
			} catch (RuntimeException e) {
			}
		} else {
			interp.get(0, 40);
		}
	}
}
