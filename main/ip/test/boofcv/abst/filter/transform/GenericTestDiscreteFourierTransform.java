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

package boofcv.abst.filter.transform;

import boofcv.abst.transform.fft.DiscreteFourierTransform;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.GImageStatistics;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageSingleBand;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public abstract class GenericTestDiscreteFourierTransform<T extends ImageSingleBand> {

	protected Random rand = new Random(234);

	boolean subimage;
	double tolerance;

	protected GenericTestDiscreteFourierTransform(boolean subimage, double tolerance) {
		this.subimage = subimage;
		this.tolerance = tolerance;
	}

	public abstract DiscreteFourierTransform<T> createAlgorithm();

	public abstract T createImage( int width , int height );

	/**
	 * Check correctness by having it convert an image to and from
	 */
	@Test
	public void forwardsBackwards() {

		for( int h = 1; h < 10; h++ ) {
			for( int w = 1; w < 10; w++ ) {
				checkForwardsBackwards(w,h);
			}
		}

		checkForwardsBackwards(64,64);
		checkForwardsBackwards(71,97);
	}

	protected void checkForwardsBackwards( int width , int height ) {
		T input = createImage(width,height);
		T transform = createImage(width*2,height*2);
		T found = createImage(width,height);

		GImageMiscOps.fillUniform(input,rand,-20,20);

		DiscreteFourierTransform<T> alg = createAlgorithm();

		alg.forward(input,transform);
		alg.inverse(transform, found);

		BoofTesting.assertEquals(input,found,tolerance);
	}

	/**
	 * The zero frequency should be the average image intensity
	 */
	@Test
	public void zeroFrequency() {
		T input = createImage(20,25);
		T transform = createImage(20*2,25*2);

		GImageMiscOps.fillUniform(input,rand,-20,20);
		double value = GImageStatistics.sum(input);
		// NOTE: the value probably depends on when the scaling is invoked.  Must need to be more robust here

		DiscreteFourierTransform<T> alg = createAlgorithm();

		alg.forward(input,transform);

		// imaginary component should be zero
		assertEquals(0,GeneralizedImageOps.get(transform,1,0),tolerance);
		// this should be the average value
		assertEquals(value,GeneralizedImageOps.get(transform,0,0),tolerance);
	}

	/**
	 * Call the same instance multiples times with images of the same size
	 */
	@Test
	public void multipleCalls_sameSize() {
		checkMultipleCalls(new int[]{52,52,52});
	}

	/**
	 * Call the same instance multiple times with images of different sizes
	 */
	@Test
	public void multipleCalls_differentSizes() {
		checkMultipleCalls(new int[]{1,10,100});
	}

	private void checkMultipleCalls(int[] sizes) {
		DiscreteFourierTransform<T> alg = createAlgorithm();

		for( int s : sizes ) {
			T input = createImage(s,s+1);
			T transform = createImage(s*2,(s+1)*2);
			T found = createImage(s,s+1);

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
		// various image sizes
		fail("implement");
	}

	/**
	 * See if the fourier transform is the expected one for odd sizes images
	 */
	@Test
	public void format_odd() {
		// various image sizes
		fail("implement");
	}

	@Test
	public void subimage() {
		fail("implement");
	}

	@Test
	public void checkDoNotModifyInputs() {
		fail("implement");
	}

	@Test
	public void checkModifyFlags() {
		fail("implement");
	}
}
