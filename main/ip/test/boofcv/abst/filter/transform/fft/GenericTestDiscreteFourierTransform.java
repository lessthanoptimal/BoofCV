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

package boofcv.abst.filter.transform.fft;

import boofcv.abst.transform.fft.DiscreteFourierTransform;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.GImageStatistics;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageInterleaved;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public abstract class GenericTestDiscreteFourierTransform<T extends ImageGray, I extends ImageInterleaved> {

	protected Random rand = new Random(234);

	boolean subimage;
	double tolerance;

	protected GenericTestDiscreteFourierTransform(boolean subimage, double tolerance) {
		this.subimage = subimage;
		this.tolerance = tolerance;
	}

	public abstract DiscreteFourierTransform<T,I> createAlgorithm();

	public abstract T createImage( int width , int height );

	public abstract I createTransform( int width , int height );

	/**
	 * Check correctness by having it convert an image to and from
	 */
	@Test
	public void forwardsBackwards() {

		for( int h = 2; h < 10; h++ ) {
			for( int w = 1; w < 10; w++ ) {
				checkForwardsBackwards(w,h);
			}
		}

		checkForwardsBackwards(64,64);
		checkForwardsBackwards(71,97);
	}

	protected void checkForwardsBackwards( int width , int height ) {
		T input = createImage(width,height);
		I transform = createTransform(width,height);
		T found = createImage(width,height);

		GImageMiscOps.fillUniform(input,rand,-20,20);

		DiscreteFourierTransform<T,I> alg = createAlgorithm();

		alg.forward(input,transform);
		alg.inverse(transform, found);

		BoofTesting.assertEquals(input, found, tolerance);
	}

	/**
	 * The zero frequency should be the average image intensity
	 */
	@Test
	public void zeroFrequency() {
		T input = createImage(20,25);
		I transform = createTransform(20,25);

		GImageMiscOps.fillUniform(input,rand,-20,20);
		double value = GImageStatistics.sum(input);
		// NOTE: the value probably depends on when the scaling is invoked.  Must need to be more robust here

		DiscreteFourierTransform<T,I> alg = createAlgorithm();

		alg.forward(input, transform);

		// imaginary component should be zero
		assertEquals(0,GeneralizedImageOps.get(transform,0,0,1),tolerance);
		// this should be the average value
		assertEquals(value, GeneralizedImageOps.get(transform, 0, 0,0), tolerance);
	}

	/**
	 * Call the same instance multiples times with images of the same size
	 */
	@Test
	public void multipleCalls_sameSize() {
		checkMultipleCalls(new int[]{52, 52, 52});
	}

	/**
	 * Call the same instance multiple times with images of different sizes
	 */
	@Test
	public void multipleCalls_differentSizes() {
		checkMultipleCalls(new int[]{1,10,100});
	}

	private void checkMultipleCalls(int[] sizes) {
		DiscreteFourierTransform<T,I> alg = createAlgorithm();

		for( int s : sizes ) {
			T input = createImage(s,s+1);
			I transform = createTransform(s,s+1);
			T found = createImage(s,s+1);

			GImageMiscOps.fillUniform(input,rand,-20,20);

			alg.forward(input,transform);
			alg.inverse(transform, found);

			BoofTesting.assertEquals(input, found, tolerance);
		}
	}

	/**
	 * See if the fourier transform is the expected one for even sizes images
	 */
	@Test
	public void format_even() {
		T input = createImage(10,1);
		I transform = createTransform(10,1);
		GImageMiscOps.fillUniform(input,rand,-20,20);

		DiscreteFourierTransform<T,I> alg = createAlgorithm();

		alg.forward(input,transform);
		assertEquals( GeneralizedImageOps.get(transform,4,0,0),GeneralizedImageOps.get(transform,6,0,0),tolerance);
		assertEquals( GeneralizedImageOps.get(transform,4,0,1),-GeneralizedImageOps.get(transform,6,0,1),tolerance);
	}

	/**
	 * See if the fourier transform is the expected one for odd sizes images
	 */
	@Test
	public void format_odd() {
		T input = createImage(7,1);
		I transform = createTransform(7,1);
		GImageMiscOps.fillUniform(input,rand,-20,20);

		DiscreteFourierTransform<T,I> alg = createAlgorithm();

		alg.forward(input,transform);
		assertEquals( GeneralizedImageOps.get(transform,3,0,0),GeneralizedImageOps.get(transform,4,0,0),tolerance);
		assertEquals( GeneralizedImageOps.get(transform,3,0,1),-GeneralizedImageOps.get(transform,4,0,1),tolerance);
	}

	@Test
	public void subimage() {
		int w = 20;
		int h = 32;
		T input = createImage(w,h);
		I transform = createTransform(w,h);
		T found = createImage(w,h);

		GImageMiscOps.fillUniform(input,rand,-20,20);

		DiscreteFourierTransform<T,I> alg = createAlgorithm();
		alg.forward(input,transform);
		alg.inverse(transform, found);

		T inputSub = BoofTesting.createSubImageOf(input);
		I transformSub = BoofTesting.createSubImageOf(transform);
		T foundSub = BoofTesting.createSubImageOf(found);

		if( subimage ) {
			alg.forward(inputSub,transformSub);
			alg.inverse(transformSub, foundSub);

			BoofTesting.assertEquals(transform,transformSub,tolerance);
			BoofTesting.assertEquals(found,transformSub,tolerance);
		} else {
			// should throw an exception if sub-images are passed in
			try {
				alg.forward(inputSub,transformSub);
				fail("Should have thrown an exception");
			} catch( IllegalArgumentException ignore ) {}

			// should throw an exception if sub-images are passed in
			try {
				alg.inverse(transformSub, foundSub);
				fail("Should have thrown an exception");
			} catch( IllegalArgumentException ignore ) {}
		}
	}

	@Test
	public void checkDoNotModifyInputs() {
		int w = 20;
		int h = 32;
		T input = createImage(w,h);
		I transform = createTransform(w,h);
		T found = createImage(w,h);

		GImageMiscOps.fillUniform(input,rand,-20,20);

		DiscreteFourierTransform<T,I> alg = createAlgorithm();
		T inputOrig = (T)input.clone();
		alg.forward(input, transform);
		I transformOrig = (I)transform.clone();
		alg.inverse(transform, found);

		// by default nothing should be modified
		assertFalse(alg.isModifyInputs());
		BoofTesting.assertEquals(input,inputOrig,tolerance);
		BoofTesting.assertEquals(transform,transformOrig,tolerance);
	}

	/**
	 * It should produce identical results with the modify flag set to true
	 */
	@Test
	public void checkSameResultsWithModify() {
		for( int h = 1; h < 10; h++ ) {
			for( int w = 1; w < 10; w++ ) {
				checkSameResultsWithModify(w, h);
			}
		}

		checkSameResultsWithModify(64, 64);
		checkSameResultsWithModify(71, 97);
	}

	protected void checkSameResultsWithModify( int width , int height ) {
		T input = createImage(width,height);
		I transform = createTransform(width,height);
		T found = createImage(width,height);

		GImageMiscOps.fillUniform(input,rand,-20,20);

		DiscreteFourierTransform<T,I> alg = createAlgorithm();
		assertFalse(alg.isModifyInputs());

		alg.forward(input,transform);
		alg.inverse(transform, found);

		I transformM = createTransform(width,height);
		T foundM = createImage(width,height);

		alg.setModifyInputs(true);
		assertTrue(alg.isModifyInputs());

		alg.forward(input,transformM);
		alg.inverse(transformM, foundM);

		BoofTesting.assertEquals(found,foundM,tolerance);
	}

	/**
	 * Makes sure it only accepts images which are the correct size
	 */
	@Test
	public void inputImageSize() {
		int width = 20;
		int height = 25;
		T input = createImage(width,height);

		DiscreteFourierTransform<T,I> alg = createAlgorithm();

		try {
			alg.forward(input,createTransform(width-1,height) );
			fail("Should have thrown an exception");
		} catch( IllegalArgumentException ignore ){}

		try {
			alg.inverse(createTransform(width-1,height),input );
			fail("Should have thrown an exception");
		} catch( IllegalArgumentException ignore ){}
	}
}
