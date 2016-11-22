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

package boofcv.alg.interpolate.impl;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * Several standardized tests that ensure correct implementations of {@link boofcv.alg.interpolate.InterpolatePixelS}.
 *
 * @author Peter Abeles
 */
public abstract class GeneralChecksInterpolationPixelS< T extends ImageGray> {
	protected Random rand = new Random(0xff34);

	protected int width = 320;
	protected int height = 240;

	protected boolean exceptionOutside = true;

	protected abstract T createImage( int width , int height );

	protected abstract InterpolatePixelS<T> wrap(T image, int minValue, int maxValue);

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
		InterpolatePixelS<T> interp = wrap(img, 0, 100);

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
		InterpolatePixelS<T> interp = wrap(img, 0, 100);

		int borderX0 = interp.getFastBorderX();
		int borderX1 = interp.getFastBorderX();
		int borderY0 = interp.getFastBorderY();
		int borderY1 = interp.getFastBorderY();

		compare(interp,img, width-borderX1-1, height/2);
		compare(interp,img, borderX0, height/2);
		compare(interp,img, width/2, height-borderY1-1);
		compare(interp,img, width/2, borderY0);
		compare(interp,img, borderX0, borderY0);
		compare(interp,img, width - borderX1-1, height - borderY1-1);
	}

	protected void compare( InterpolatePixelS<T> interp , T img , float x , float y )
	{
		assertEquals(compute(img, x, y), interp.get(x, y), 1e-5f);
	}


	/**
	 * Compute the interpolation manually using independently written code.  For
	 * example, easy to write but inefficient.
	 */
	protected abstract float compute(T img, float x, float y);

	/**
	 * Sees if get throws an exception if it is out of bounds
	 */
	@Test
	public void get_outside_noborder() {
		T img = createImage(width, height);

		InterpolatePixelS<T> interp = wrap(img, 0, 100);

		checkOutside(interp,-0.1f,0);
		checkOutside(interp,0,-0.1f);
		checkOutside(interp,width-0.99f,0);
		checkOutside(interp,0,height-0.99f);
	}

	private void checkOutside(InterpolatePixelS<T> interp, float x , float y) {
		try {
			interp.get(x, y);
			if( exceptionOutside )
				fail("Didn't throw an exception when accessing an outside pixel");
		} catch( RuntimeException e ) {}
	}


	/**
	 * Compare get_fast against the value returned by get()
	 */
	@Test
	public void get_fast() {
		T img = createImage(width, height);
		GImageMiscOps.fillUniform(img, rand, 0, 100);

		BoofTesting.checkSubImage(this, "get_fast", false, img);
	}

	public void get_fast(T img) {
		InterpolatePixelS<T> interp = wrap(img, 0, 100);

		assertEquals(interp.get(10, 10), interp.get_fast(10, 10), 1e-6);
		assertEquals(interp.get(10.1f, 10), interp.get_fast(10.1f, 10), 1e-6);
		assertEquals(interp.get(10, 10.6f), interp.get_fast(10, 10.6f), 1e-6);
		assertEquals(interp.get(10.8f, 10.6f), interp.get_fast(10.8f, 10.6f), 1e-6);
	}


	/**
	 * If a border is specified it should handle everything just fine
	 */
	@Test
	public void get_outside_border() {
		T img = createImage(width, height);
		GImageMiscOps.fillUniform(img, rand, 0, 100);

		BoofTesting.checkSubImage(this, "get_outside_border", false, img);
	}
	public void get_outside_border(T img) {
		InterpolatePixelS<T> interp = wrap(img, 0, 100);

		ImageBorder<T> border = (ImageBorder)FactoryImageBorder.singleValue(img.getClass(), 5);
		interp.setBorder(border);
		interp.setImage(img);

		// outside the image it should work just fine
		assertEquals(5,interp.get(-10, 23),1e-6);
		assertEquals(5,interp.get(0,2330),1e-6);
	}

	@Test
	public void getImage() {
		T img = createImage(width, height);
		InterpolatePixelS<T> interp = wrap(img, 0, 100);
		assertTrue(img == interp.getImage());
	}

	/**
	 * Scans through the whole image and for each pixel which is "safe" it compares the safe
	 * value to the unsafe value.
	 */
	@Test
	public void isInFastBounds() {
		T img = createImage(width, height);
		GImageMiscOps.fillUniform(img, rand, 0, 100);
		InterpolatePixelS<T> interp = wrap(img, 0, 100);

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				if( interp.isInFastBounds(x, y)) {
					float a = interp.get(x,y);
					float b = interp.get_fast(x, y);
					assertEquals(a,b,1e-4);
				}
			}
		}
	}

	/**
	 * Pixels out of the image are clearly not in the fast bounds
	 */
	@Test
	public void isInFastBounds_outOfBounds() {
		T img = createImage(width, height);
		InterpolatePixelS<T> interp = wrap(img, 0, 100);

		assertFalse(interp.isInFastBounds(-0.1f,0));
		assertFalse(interp.isInFastBounds(0, -0.1f));
		assertFalse(interp.isInFastBounds(width-0.99f,0));
		assertFalse(interp.isInFastBounds(0,height-0.99f));
	}

	@Test
	public void getFastBorder() {
		T img = createImage(width, height);
		InterpolatePixelS<T> interp = wrap(img, 0, 100);

		// create a region with positive cases
		int x0 = interp.getFastBorderX();
		int x1 = width - interp.getFastBorderX();
		int y0 = interp.getFastBorderX();
		int y1 = height - interp.getFastBorderX();

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				if( x >= x0 && x < x1 && y >= y0 && y < y1 ) {
					assertTrue(interp.isInFastBounds(x,y));
				} else {
					// stuff outside of the border does not need to be outside the fast bounds
					// this is a crude test to avoid checking the bounds every time through a loop
//					assertFalse(interp.isInFastBounds(x,y));
				}
			}
		}
	}

	/**
	 * Interpolates the whole image and sees if the values returned are within the specified bounds
	 */
	@Test
	public void checkPixelValueBoundsHonored() {
		T img = createImage(20, 30);
		GImageMiscOps.fillUniform(img, rand, 0, 100);
		InterpolatePixelS<T> interp = wrap(img, 0, 100);
		interp.setBorder(FactoryImageBorder.singleValue(img, 0));

		for( int off = 0; off < 5; off++ ) {
			float frac = off/5.0f;

			for( int y = 0; y < img.height; y++ ) {
				for( int x = 0; x < img.width; x++ ) {
					float v = interp.get(x+frac,y+frac);
					assertTrue( v >= 0 && v <= 100 );
				}
			}
		}
	}

	/**
	 * Should produce identical results when given a sub-image.
	 */
	@Test
	public void checkSubImage() {
		T imgA = createImage(30, 40);
		GImageMiscOps.fillUniform(imgA, rand, 0, 100);

		InterpolatePixelS<T> interpA = wrap(imgA, 0, 100);

		T imgB = BoofTesting.createSubImageOf(imgA);
		InterpolatePixelS<T> interpB = wrap(imgB, 0, 100);

		interpA.setBorder(FactoryImageBorder.singleValue(imgA, 0));
		interpB.setBorder(FactoryImageBorder.singleValue(imgB, 0));

		for (int y = 0; y < 40; y++) {
			for (int x = 0; x < 30; x++) {

				float dx = rand.nextFloat()*2-1f;
				float dy = rand.nextFloat()*2-1f;

				float xx = x + dx;
				float yy = y + dy;

				// ,make sure it is inside the image bound
				if( yy < 0 ) yy = 0; else if( yy > 39 ) yy = 39;
				if( xx < 0 ) xx = 0; else if( xx > 29 ) xx = 29;

				assertTrue("( " + x + " , " + y + " )", interpA.get(xx, yy) == interpB.get(xx,yy));
			}
		}
	}
}
