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

package boofcv.alg.interpolate.impl;

import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.ImageSingleBand;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * Several standardized tests that ensure correct implementations of {@link boofcv.alg.interpolate.InterpolatePixel}.
 *
 * @author Peter Abeles
 */
public abstract class GeneralInterpolationPixelChecks< T extends ImageSingleBand> {
	protected Random rand = new Random(0xff34);

	protected int width = 320;
	protected int height = 240;

	protected boolean exceptionOutside = true;

	protected abstract T createImage( int width , int height );

	protected abstract InterpolatePixel<T> wrap(T image, int minValue, int maxValue);

	/**
	 * Checks value returned by get() against values computed using
	 * an alternative approach.
	 */
	@Test
	public void get() {
		T img = createImage(width, height);
		GImageMiscOps.fillUniform(img, rand, 0, 100);

		BoofTesting.checkSubImage(this, "get", false, img);
	}

	public void get(T img) {
		InterpolatePixel<T> interp = wrap(img, 0, 100);

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
		GImageMiscOps.fillUniform(img, rand, 0, 100);

		BoofTesting.checkSubImage(this, "get_edges", false, img);
	}

	public void get_edges(T img) {
		InterpolatePixel<T> interp = wrap(img, 0, 100);

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
	 * Compute the interpolation manually using independently written code.  For
	 * example, easy to write but inefficient.
	 */
	protected abstract float compute(T img, float x, float y);

	/**
	 * Sees if get throws an exception if it is out of bounds
	 */
	public void get_outside() {
		T img = createImage(width, height);

		InterpolatePixel<T> interp = wrap(img, 0, 100);

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
		GImageMiscOps.fillUniform(img, rand, 0, 100);

		BoofTesting.checkSubImage(this, "get_unsafe", false, img);
	}

	public void get_unsafe(T img) {
		InterpolatePixel<T> interp = wrap(img, 0, 100);

		assertEquals(interp.get(10, 10), interp.get_unsafe(10, 10), 1e-6);
		assertEquals(interp.get(10.1f, 10), interp.get_unsafe(10.1f, 10), 1e-6);
		assertEquals(interp.get(10, 10.6f), interp.get_unsafe(10, 10.6f), 1e-6);
		assertEquals(interp.get(10.8f, 10.6f), interp.get_unsafe(10.8f, 10.6f), 1e-6);
	}

	@Test
	public void getImage() {
		T img = createImage(width, height);
		InterpolatePixel<T> interp = wrap(img, 0, 100);
		assertTrue(img == interp.getImage());
	}

	/**
	 * Scans through the whole image and for each pixel which is "safe" it compares the safe
	 * value to the unsafe value.
	 */
	@Test
	public void isInSafeBounds() {
		T img = createImage(width, height);
		GImageMiscOps.fillUniform(img, rand, 0, 100);
		InterpolatePixel<T> interp = wrap(img, 0, 100);

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				if( interp.isInSafeBounds(x,y)) {
					float a = interp.get(x,y);
					float b = interp.get_unsafe(x,y);
					assertEquals(a,b,1e-4);
				}
			}
		}
	}

	/**
	 * Try the same get at a few border points and see if anything blows up
	 */
	@Test
	public void checkSafeGetAlongBorder() {
		T img = createImage(width, height);
		GImageMiscOps.fillUniform(img, rand, 0, 100);
		InterpolatePixel<T> interp = wrap(img, 0, 100);

		// will it blow up?
		interp.get(0,0);
		interp.get(width/2,0);
		interp.get(0,height/2);
		interp.get(width-1,height-1);
		interp.get(width/2,height-1);
		interp.get(width-1,height/2);
	}

	/**
	 * Interpolates the whole image and sees if the values returned are within the specified bounds
	 */
	@Test
	public void checkPixelValueBoundsHonored() {
		T img = createImage(20, 30);
		GImageMiscOps.fillUniform(img, rand, 0, 100);
		InterpolatePixel<T> interp = wrap(img, 0, 100);

		for( int y = 0; y < img.height; y++ ) {
			for( int x = 0; x < img.width; x++ ) {
				float v = interp.get(x,y);
				assertTrue( v >= 0 && v <= 100 );
			}
		}
	}
}
