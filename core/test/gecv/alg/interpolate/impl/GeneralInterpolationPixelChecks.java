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

import gecv.alg.interpolate.InterpolatePixel;
import gecv.struct.image.ImageBase;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * Several standardized tests that ensure correct implementations of {@link gecv.alg.interpolate.InterpolatePixel}.
 *
 * @author Peter Abeles
 */
public abstract class GeneralInterpolationPixelChecks< T extends ImageBase> {
	protected Random rand = new Random(0xff34);

	protected int width = 320;
	protected int height = 240;


	protected abstract T createImage( int width , int height );

	protected abstract void randomize( T image );

	protected abstract InterpolatePixel<T> wrap( T image );


	/**
	 * Checks to see if inBounds correctly determines if a point is in bounds with positive
	 * and negative examples
	 */
	@Test
	public void inBounds() {
		T img = createImage(width, height);
		randomize(img);

		GecvTesting.checkSubImage(this, "inBounds", false, img);
	}

	public void inBounds(T img) {
		InterpolatePixel<T> interp = wrap(img);

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
		T img = createImage(width, height);
		randomize(img);

		GecvTesting.checkSubImage(this, "get", false, img);
	}

	public void get(T img) {
		InterpolatePixel<T> interp = wrap(img);

		assertEquals(compute(img, 10, 10), interp.get(10, 10), 1e-5f);
		assertEquals(compute(img, 10.1f, 10), interp.get(10.1f, 10), 1e-5f);
		assertEquals(compute(img, 10, 10.6f), interp.get(10, 10.6f), 1e-5f);
		assertEquals(compute(img, 10.8f, 10.6f), interp.get(10.8f, 10.6f), 1e-5f);
	}

	/**
	 * Compute a bilinear interpolation manually
	 */
	protected abstract float compute(T img, float x, float y);

	/**
	 * Sees if get throws an exception if it is out of bounds
	 */
	@Test(expected = IllegalArgumentException.class)
	public void get_outside() {
		T img = createImage(width, height);

		InterpolatePixel<T> interp = wrap(img);

		interp.get(500, 10);
	}

	/**
	 * Compare get_unsafe against the value returned by get()
	 */
	@Test
	public void get_unsafe() {
		T img = createImage(width, height);
		randomize(img);

		GecvTesting.checkSubImage(this, "get_unsafe", false, img);
	}

	public void get_unsafe(T img) {
		InterpolatePixel<T> interp = wrap(img);

		assertEquals(interp.get(10, 10), interp.get_unsafe(10, 10), 1e-6);
		assertEquals(interp.get(10.1f, 10), interp.get_unsafe(10.1f, 10), 1e-6);
		assertEquals(interp.get(10, 10.6f), interp.get_unsafe(10, 10.6f), 1e-6);
		assertEquals(interp.get(10.8f, 10.6f), interp.get_unsafe(10.8f, 10.6f), 1e-6);
	}

	@Test
	public void getImage() {
		T img = createImage(width, height);
		InterpolatePixel<T> interp = wrap(img);
		assertTrue(img == interp.getImage());
	}

	@Test
	public void getBorderOffsets() {
		T img = createImage(width, height);
		InterpolatePixel<T> interp = wrap(img);

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
