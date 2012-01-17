/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.derivative;

import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.deriv.GradientValue;
import boofcv.struct.deriv.SparseImageGradient;
import boofcv.struct.image.ImageSingleBand;
import boofcv.testing.BoofTesting;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Standard tests for implementers of {@link SparseImageGradient}.
 *
 * @author Peter Abeles
 */
public abstract class GeneralSparseGradientTests
<T extends ImageSingleBand, D extends ImageSingleBand,G extends GradientValue>
{
	protected Random rand = new Random(12342);
	protected int width = 30;
	protected int height = 40;

	public Class<T> inputType;
	public Class<D> derivType;

	protected T input;
	// "true" derivative.  Used to validate results
	D derivX;
	D derivY;

	// the algorithm being tests
	protected SparseImageGradient<T,G>  alg;

	// size of the image border
	int borderRadius;

	protected GeneralSparseGradientTests(Class<T> inputType, Class<D> derivType,
										 int borderRadius ) {
		this.alg = alg;
		this.inputType = inputType;
		this.derivType = derivType;
		this.borderRadius = borderRadius;

		input = GeneralizedImageOps.createSingleBand(inputType, width, height);
		derivX = GeneralizedImageOps.createSingleBand(derivType, width, height);
		derivY = GeneralizedImageOps.createSingleBand(derivType, width, height);

		GeneralizedImageOps.randomize(input,rand,0,100);
	}

	/**
	 * Perform all tests
	 *
	 * @param includeBorder Should it include tests that test along the image
	 * border.
	 */
	public void allTests( boolean includeBorder ) {
		if( alg == null )
			throw new RuntimeException("must setup alg!");
		testCenterImage();
		testSubImage();
		isInBounds();
		if( includeBorder )
			testBorder();
	}

	/**
	 * Test to see if it produces identical results for pixels inside the image
	 */
	public void testCenterImage() {
		imageGradient(input,derivX,derivY);
		alg.setImage(input);
		for( int y = borderRadius; y < height-borderRadius; y++ ) {
			for( int x = borderRadius; x < width-borderRadius; x++ ) {
				G g = alg.compute(x,y);
				double expectedX = GeneralizedImageOps.get(derivX,x,y);
				double expectedY = GeneralizedImageOps.get(derivY,x,y);

				assertEquals(expectedX,g.getX(),1e-4);
				assertEquals(expectedY,g.getY(),1e-4);
			}
		}

	}

	/**
	 * Provide a sub-image as an input image and see if it produces the expected results
	 */
	public void testSubImage() {
		imageGradient(input,derivX,derivY);
		T subImage = BoofTesting.createSubImageOf(input);
		alg.setImage(subImage);

		for( int y = borderRadius; y < height-borderRadius; y++ ) {
			for( int x = borderRadius; x < width-borderRadius; x++ ) {
				G g = alg.compute(x,y);
				double expectedX = GeneralizedImageOps.get(derivX,x,y);
				double expectedY = GeneralizedImageOps.get(derivY,x,y);

				assertEquals(expectedX,g.getX(),1e-4);
				assertEquals(expectedY,g.getY(),1e-4);
			}
		}
	}

	/**
	 * See if it recognizes that it's along the border
	 */
	public void isInBounds() {
		alg.setImage(input);

		assertTrue(alg.isInBounds(width/2,height/2));
		
		assertTrue(!alg.isInBounds(0,0));
		assertTrue(!alg.isInBounds(width/2,0));
		assertTrue(!alg.isInBounds(width/2,height-1));
		assertTrue(!alg.isInBounds(0,height/2));
		assertTrue(!alg.isInBounds(width-1,height/2));
	}
	
	/**
	 * Compute the input along the image border and see if has the expected results
	 */
	public void testBorder() {
		imageGradient(input,derivX,derivY);
		alg.setImage(input);
		for( int y = 0; y < height; y++ ) {
			if( y < borderRadius || y >= height-borderRadius) {
				for( int x = 0; x < width; x++ ) {
					if( y < borderRadius || y >= height-borderRadius) {
						G g = alg.compute(x,y);
						double expectedX = GeneralizedImageOps.get(derivX,x,y);
						double expectedY = GeneralizedImageOps.get(derivY,x,y);

						assertEquals(expectedX,g.getX(),1e-4);
						assertEquals(expectedY,g.getY(),1e-4);
					}
				}
			}
		}
	}

	/**
	 * Compute the image gradient.  Should not be computed using the exact same code as
	 *  the gradient operator
	 */
	protected abstract void imageGradient( T input , D derivX , D derivY);

}
