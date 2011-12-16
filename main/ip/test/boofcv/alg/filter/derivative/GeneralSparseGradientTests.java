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

package boofcv.alg.filter.derivative;

import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.deriv.GradientValue;
import boofcv.struct.deriv.SparseImageGradient;
import boofcv.struct.image.ImageSingleBand;
import boofcv.testing.BoofTesting;

import java.util.Random;

import static org.junit.Assert.assertEquals;


/**
 * Standard tests for implementers of {@link SparseImageGradient}.
 *
 * @author Peter Abeles
 */
public abstract class GeneralSparseGradientTests
<T extends ImageSingleBand, D extends ImageSingleBand,G extends GradientValue>
{
	Random rand = new Random(12342);
	int width = 30;
	int height = 40;

	public Class<T> inputType;
	public Class<D> derivType;

	T input;
	D derivX;
	D derivY;


	// the algorithm being tests
	SparseImageGradient<T,G>  alg;

	// size of the image border
	int borderRadius;

	protected GeneralSparseGradientTests(Class<T> inputType, Class<D> derivType,
										 int borderRadius ) {
		this.inputType = inputType;
		this.derivType = derivType;
		this.borderRadius = borderRadius;

		input = GeneralizedImageOps.createSingleBand(inputType, width, height);
		derivX = GeneralizedImageOps.createSingleBand(derivType, width, height);
		derivY = GeneralizedImageOps.createSingleBand(derivType, width, height);

		GeneralizedImageOps.randomize(input,rand,0,100);
		imageGradient(input,derivX,derivY);
	}

	/**
	 * Perform all tests
	 *
	 * @param includeBorder Should it include tests that test along the image
	 * border.
	 */
	public void allTests( boolean includeBorder ) {
		testCenterImage();
		testSubSample();
		if( includeBorder )
			testBorder();
	}

	/**
	 * Test to see if it produces identical results for pixels inside the image
	 */
	public void testCenterImage() {
		for( int y = borderRadius; y < height-borderRadius; y++ ) {
			for( int x = borderRadius; x < width-borderRadius; x++ ) {
				G g = sparseGradient(input,x,y);
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
	public void testSubSample() {
		T subImage = BoofTesting.createSubImageOf(input);

		for( int y = borderRadius; y < height-borderRadius; y++ ) {
			for( int x = borderRadius; x < width-borderRadius; x++ ) {
				G g = sparseGradient(subImage,x,y);
				double expectedX = GeneralizedImageOps.get(derivX,x,y);
				double expectedY = GeneralizedImageOps.get(derivY,x,y);

				assertEquals(expectedX,g.getX(),1e-4);
				assertEquals(expectedY,g.getY(),1e-4);
			}
		}
	}

	/**
	 * Compute the input along the image border and see if has the expected results
	 */
	public void testBorder() {
		for( int y = 0; y < height; y++ ) {
			if( y < borderRadius || y >= height-borderRadius) {
				for( int x = 0; x < width; x++ ) {
					if( y < borderRadius || y >= height-borderRadius) {
						G g = sparseGradient(input,x,y);
						double expectedX = GeneralizedImageOps.get(derivX,x,y);
						double expectedY = GeneralizedImageOps.get(derivY,x,y);

						assertEquals(expectedX,g.getX(),1e-4);
						assertEquals(expectedY,g.getY(),1e-4);
					}
				}
			}
		}
	}

	protected abstract void imageGradient( T input , D derivX , D derivY);

	protected abstract G sparseGradient( T input , int x , int y );
}
