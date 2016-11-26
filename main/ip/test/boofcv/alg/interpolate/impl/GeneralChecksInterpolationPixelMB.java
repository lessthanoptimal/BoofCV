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

import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageInterleaved;
import boofcv.struct.image.ImageMultiBand;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * Several standardized tests that ensure correct implementations of {@link InterpolatePixelS}.
 *
 * @author Peter Abeles
 */
public abstract class GeneralChecksInterpolationPixelMB< T extends ImageMultiBand> {
	protected Random rand = new Random(0xff34);

	protected int width = 320;
	protected int height = 240;
	int numBands = 2;

	float tmp0[] = new float[numBands];
	float tmp1[] = new float[numBands];


	protected boolean exceptionOutside = true;

	protected abstract T createImage( int width , int height , int numBands );

	protected abstract InterpolatePixelMB<T> wrap(T image, int minValue, int maxValue);

	/**
	 * Creates the equivalent single band interpolation algorithm. If none exist then return null
	 */
	protected abstract<SB extends ImageGray>
	InterpolatePixelS<SB> wrapSingle(SB image, int minValue, int maxValue);

	/**
	 * Checks value returned by get() against values computed using
	 * an alternative approach.
	 */
	@Test
	public void get() {
		T img = createImage(width, height,numBands);
		GImageMiscOps.fillUniform(img, rand, 0, 100);

		BoofTesting.checkSubImage(this, "get", false, img);
	}

	public void get(T img) {
		InterpolatePixelMB<T> interp = wrap(img, 0, 100);

		compareGet(10, 10, img, interp);
		compareGet(10.1f, 10, img, interp);
		compareGet(10, 10.6f, img, interp);
		compareGet(10.8f, 10.6f, img, interp);
	}

	private void compareGet(float x, float y, T img, InterpolatePixelMB<T> interp) {
		compute(img, x, y, tmp0);
		interp.get(x, y, tmp1);
		for (int i = 0; i < numBands; i++) {
			assertEquals(tmp0[i],tmp1[i],1e-5f);
		}
	}

	private void compareFast(float x, float y, T img, InterpolatePixelMB<T> interp) {
		compute(img, x, y, tmp0);
		interp.get_fast(x, y, tmp1);
		for (int i = 0; i < numBands; i++) {
			assertEquals(tmp0[i],tmp1[i],1e-5f);
		}
	}

	/**
	 * See if accessing the image edge causes it to blow up.
	 */
	@Test
	public void get_edges() {
		T img = createImage(width, height,numBands);
		GImageMiscOps.fillUniform(img, rand, 0, 100);

		BoofTesting.checkSubImage(this, "get_edges", false, img);
	}

	public void get_edges(T img) {
		InterpolatePixelMB<T> interp = wrap(img, 0, 100);

		int borderX0 = interp.getFastBorderX();
		int borderX1 = interp.getFastBorderX();
		int borderY0 = interp.getFastBorderY();
		int borderY1 = interp.getFastBorderY();

		compareGet(width - borderX1 - 1, height / 2, img, interp);
		compareGet(borderX0, height / 2, img, interp);
		compareGet(width / 2, height - borderY1 - 1, img, interp);
		compareGet(width / 2, borderY0, img, interp);
		compareGet(borderX0, borderY0, img, interp);
		compareGet(width - borderX1 - 1, height - borderY1 - 1, img, interp);
	}


	/**
	 * Compute the interpolation manually using independently written code.  For
	 * example, easy to write but inefficient.
	 */
	protected abstract void compute(T img, float x, float y , float pixel[] );

	/**
	 * Sees if get throws an exception if it is out of bounds
	 */
	@Test
	public void get_outside_noborder() {
		T img = createImage(width, height, numBands);

		InterpolatePixelMB<T> interp = wrap(img, 0, 100);

		checkOutside(interp,-0.1f,0);
		checkOutside(interp,0,-0.1f);
		checkOutside(interp,width-0.99f,0);
		checkOutside(interp,0,height-0.99f);
	}

	private void checkOutside(InterpolatePixelMB<T> interp, float x , float y) {
		try {
			interp.get(x, y, tmp0);
			if( exceptionOutside )
				fail("Didn't throw an exception when accessing an outside pixel");
		} catch( RuntimeException e ) {}
	}


	/**
	 * Compare get_fast against the value returned by get()
	 */
	@Test
	public void get_fast() {
		T img = createImage(width, height, numBands);
		GImageMiscOps.fillUniform(img, rand, 0, 100);

		BoofTesting.checkSubImage(this, "get_fast", false, img);
	}

	public void get_fast(T img) {
		InterpolatePixelMB<T> interp = wrap(img, 0, 100);

		compareFast(10, 10, img, interp);
		compareFast(10.1f, 10, img, interp);
		compareFast(10, 10.6f, img, interp);
		compareFast(10.8f, 10.6f, img, interp);
	}


	/**
	 * If a border is specified it should handle everything just fine
	 */
	@Test
	public void get_outside_border() {
		T img = createImage(width, height, numBands);
		GImageMiscOps.fillUniform(img, rand, 0, 100);

		BoofTesting.checkSubImage(this, "get_outside_border", false, img);
	}
	public void get_outside_border(T img) {
		InterpolatePixelMB<T> interp = wrap(img, 0, 100);

		ImageBorder<T> border = (ImageBorder)FactoryImageBorder.interleavedValue((Class) img.getClass(), 5);
		interp.setBorder(border);
		interp.setImage(img);

		// outside the image it should work just fine
		for (int i = 0; i < numBands; i++) {
			tmp1[i] = 5;
		}
		interp.get(-10, 23, tmp0);
		for (int i = 0; i < numBands; i++) { assertEquals(tmp0[i],tmp1[i],1e-4); }
		interp.get(5, 2330, tmp1);
		for (int i = 0; i < numBands; i++) { assertEquals(tmp0[i],tmp1[i],1e-4); }
	}

	@Test
	public void getImage() {
		T img = createImage(width, height, numBands);
		InterpolatePixelMB<T> interp = wrap(img, 0, 100);
		assertTrue(img == interp.getImage());
	}

	/**
	 * Scans through the whole image and for each pixel which is "safe" it compares the safe
	 * value to the unsafe value.
	 */
	@Test
	public void isInFastBounds() {
		T img = createImage(width, height, numBands);
		GImageMiscOps.fillUniform(img, rand, 0, 100);
		InterpolatePixelMB<T> interp = wrap(img, 0, 100);

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				if( interp.isInFastBounds(x, y)) {
					interp.get(x, y, tmp0);
					interp.get_fast(x, y, tmp1);
					for (int i = 0; i < numBands; i++) {
						assertEquals(tmp0[i],tmp1[i],1e-4);
					}

				}
			}
		}
	}

	/**
	 * Pixels out of the image are clearly not in the fast bounds
	 */
	@Test
	public void isInFastBounds_outOfBounds() {
		T img = createImage(width, height, numBands);
		InterpolatePixelMB<T> interp = wrap(img, 0, 100);

		assertFalse(interp.isInFastBounds(-0.1f,0));
		assertFalse(interp.isInFastBounds(0, -0.1f));
		assertFalse(interp.isInFastBounds(width-0.99f,0));
		assertFalse(interp.isInFastBounds(0,height-0.99f));
	}

	@Test
	public void getFastBorder() {
		T img = createImage(width, height, numBands);
		InterpolatePixelMB<T> interp = wrap(img, 0, 100);

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
		T img = createImage(20, 30, numBands);
		GImageMiscOps.fillUniform(img, rand, 0, 100);
		InterpolatePixelMB<T> interp = wrap(img, 0, 100);
		interp.setBorder((ImageBorder)FactoryImageBorder.interleavedValue((ImageInterleaved)img, 0));

		for( int off = 0; off < 5; off++ ) {
			float frac = off/5.0f;

			for( int y = 0; y < img.height; y++ ) {
				for( int x = 0; x < img.width; x++ ) {
					interp.get(x + frac, y + frac, tmp0);
					for (int i = 0; i < numBands; i++) {
						assertTrue( tmp0[i] >= 0 && tmp0[i] <= 100 );
					}
				}
			}
		}
	}

	/**
	 * Should produce identical results when given a sub-image.
	 */
	@Test
	public void checkSubImage() {
		T imgA = createImage(30, 40, numBands);
		GImageMiscOps.fillUniform(imgA, rand, 0, 100);

		InterpolatePixelMB<T> interpA = wrap(imgA, 0, 100);

		T imgB = BoofTesting.createSubImageOf(imgA);
		InterpolatePixelMB<T> interpB = wrap(imgB, 0, 100);

		interpA.setBorder((ImageBorder)FactoryImageBorder.interleavedValue((ImageInterleaved)imgA, 0));
		interpB.setBorder((ImageBorder) FactoryImageBorder.interleavedValue((ImageInterleaved) imgB, 0));

		for (int y = 0; y < 40; y++) {
			for (int x = 0; x < 30; x++) {

				float dx = rand.nextFloat()*2-1f;
				float dy = rand.nextFloat()*2-1f;

				float xx = x + dx;
				float yy = y + dy;

				// ,make sure it is inside the image bound
				if( yy < 0 ) yy = 0; else if( yy > 39 ) yy = 39;
				if( xx < 0 ) xx = 0; else if( xx > 29 ) xx = 29;

				interpA.get(xx, yy, tmp0);
				interpB.get(xx, yy, tmp1);

				for (int i = 0; i < numBands; i++) {
					assertTrue("( " + x + " , " + y + " )", tmp0[i] == tmp1[i]);
				}
			}
		}
	}

	/**
	 * Compares interpolation to two single band images and sees if they produce nearly identical results
	 */
	@Test
	public void compareToSingleBand() {

		T origMB = createImage(30, 40, 2);
		GImageMiscOps.fillUniform(origMB, rand, 0, 100);

		ImageDataType dataType = origMB.getImageType().getDataType();
		ImageGray band0 = GeneralizedImageOps.createSingleBand(dataType,origMB.width,origMB.height);
		ImageGray band1 = GeneralizedImageOps.createSingleBand(dataType,origMB.width,origMB.height);

		for (int y = 0; y < origMB.height; y++) {
			for (int x = 0; x < origMB.width; x++) {
				double val0 = GeneralizedImageOps.get(origMB,x,y,0);
				double val1 = GeneralizedImageOps.get(origMB,x,y,1);

				GeneralizedImageOps.set(band0,x,y,val0);
				GeneralizedImageOps.set(band1,x,y,val1);
			}
		}

		InterpolatePixelS interpBand0 = wrapSingle(band0,0,255);
		InterpolatePixelS interpBand1 = wrapSingle(band1,0,255);

		InterpolatePixelMB<T> interpMB = wrap(origMB,0,255);

		interpBand0.setBorder(FactoryImageBorder.genericValue(0,band0.getImageType()));
		interpBand1.setBorder(FactoryImageBorder.genericValue(0,band1.getImageType()));
		interpMB.setBorder(FactoryImageBorder.genericValue(0,interpMB.getImageType()));

		interpBand0.setImage(band0);
		interpBand1.setImage(band1);
		interpMB.setImage(origMB);


		float values[] = new float[2];
		for (int y = 0; y < origMB.height-1; y++) {
			for (int x = 0; x < origMB.width-1; x++) {
				float val0 = interpBand0.get(x+0.2f,y+0.3f);
				float val1 = interpBand1.get(x+0.2f,y+0.3f);

				interpMB.get(x+0.2f,y+0.3f,values);

				assertEquals(val0,values[0],1e-4f);
				assertEquals(val1,values[1],1e-4f);
			}
		}
	}
}
