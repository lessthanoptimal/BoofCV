/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.interpolate.impl;

import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageBase;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * Several standardized tests that ensure correct implementations of {@link boofcv.alg.interpolate.InterpolatePixel}.
 *
 * @author Peter Abeles
 */
public abstract class GeneralInterpolationPixelChecks< T extends ImageBase> {
	protected Random rand = new Random(0xff34);

	protected int width = 320;
	protected int height = 240;

	protected boolean exceptionOutside = true;

	protected abstract T createImage( int width , int height );

	protected abstract InterpolatePixel<T> wrap( T image );

	/**
	 * Checks value returned by get()
	 */
	@Test
	public void get() {
		T img = createImage(width, height);
		GeneralizedImageOps.randomize(img, rand, 0,100);

		BoofTesting.checkSubImage(this, "get", false, img);
	}

	public void get(T img) {
		InterpolatePixel<T> interp = wrap(img);

		assertEquals(compute(img, 10, 10), interp.get(10, 10), 1e-5f);
		assertEquals(compute(img, 10.1f, 10), interp.get(10.1f, 10), 1e-5f);
		assertEquals(compute(img, 10, 10.6f), interp.get(10, 10.6f), 1e-5f);
		assertEquals(compute(img, 10.8f, 10.6f), interp.get(10.8f, 10.6f), 1e-5f);
	}

	/**
	 * See if accessing the image edge causes it to blow up.
	 */
	@Test
	public void get_edges() {
		T img = createImage(width, height);
		GeneralizedImageOps.randomize(img, rand, 0,100);

		BoofTesting.checkSubImage(this, "get_edges", false, img);
	}

	public void get_edges(T img) {
		InterpolatePixel<T> interp = wrap(img);

		compare(interp,img, width-0.01f, height/2);
		compare(interp,img, 0, height/2);
		compare(interp,img, width/2, height-0.01f);
		compare(interp,img, width/2, 0);
		compare(interp,img, 0, 0);
		compare(interp,img, width-0.01f, height-0.01f);
	}

	protected void compare( InterpolatePixel<T> interp , T img , float x , float y )
	{
		assertEquals(compute(img, x,y), interp.get(x, y), 1e-5f);
	}


	/**
	 * Compute a bilinear interpolation manually
	 */
	protected abstract float compute(T img, float x, float y);

	/**
	 * Sees if get throws an exception if it is out of bounds
	 */
	public void get_outside() {
		T img = createImage(width, height);

		InterpolatePixel<T> interp = wrap(img);

		try {
			interp.get(500, 10);
			if( exceptionOutside )
				fail("Didn't throw an exception when accessing an outside pixel");
		} catch( IllegalArgumentException e ) {

		}
	}


	/**
	 * Compare get_unsafe against the value returned by get()
	 */
	@Test
	public void get_unsafe() {
		T img = createImage(width, height);
		GeneralizedImageOps.randomize(img, rand, 0,100);

		BoofTesting.checkSubImage(this, "get_unsafe", false, img);
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
	public void isInSafeBounds() {
		// scan through whole image that is "safe"
		// compare get() to get_unsafe()
		fail("implement");
	}
}
