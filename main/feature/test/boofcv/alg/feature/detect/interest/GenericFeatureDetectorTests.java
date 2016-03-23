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

package boofcv.alg.feature.detect.interest;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.GrayF32;
import georegression.geometry.UtilPoint2D_I32;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Provides VERY basic tests for feature detectors
 *
 * @author Peter Abeles
 */
public abstract class GenericFeatureDetectorTests {
	Random rand = new Random(234);

	int width = 80;
	int height = 90;

	int r = 2;

	double scaleTolerance = 1e-4;

	/**
	 * If the maximum number of features is set to a negative number or zero then
	 * it should return the maximum number of features possible
	 */
	@Test
	public void checkNegativeMaxFeatures() {
		GrayF32 input = new GrayF32(width,height);

		// give it a bunch of features that any of the detectors should be able to see
		GImageMiscOps.fillRectangle(input,100,10,10,15,15);
		GImageMiscOps.fillRectangle(input, 100, 30, 10, 35, 15);
		GImageMiscOps.fillRectangle(input,100,10,30,15,35);
		GImageMiscOps.fillRectangle(input,100,30,30,35,35);

		// limit it to "one" feature
		Object alg = createDetector(1);
		int firstFound = detectFeature(input, alg);

		// tell it to return everything it can find
		 alg = createDetector(0);
		int secondFound = detectFeature(input, alg);

		assertTrue(firstFound+" "+secondFound,secondFound>firstFound);
	}

	/**
	 * Checks to see if features are flushed after multiple calls
	 */
	@Test
	public void checkFlushFeatures() {
		GrayF32 input = new GrayF32(width,height);

		// provide a rectangle and circular feature
		GImageMiscOps.fillRectangle(input,20,5,5,25,25);
		drawCircle(input, 10, 10, r * 2);

		Object alg = createDetector(50);
		int firstFound = detectFeature(input, alg);
		int secondFound = detectFeature(input, alg);

		// make sure at least one feature was found
		assertTrue(firstFound>0);
		// if features are not flushed then the secondFound should be twice as large
		assertEquals(firstFound, secondFound);
	}

	/**
	 * Give it a blank image and one with random noise.  The blank image should have very very few features
	 */
	@Test
	public void compareBlankImage() {
		GrayF32 input = new GrayF32(width,height);

		Object alg = createDetector(-1);
		int firstFound = detectFeature(input, alg);

		renderCheckered(input);

		int secondFound = detectFeature(input, alg);

		assertTrue(firstFound < secondFound);
	}

	/**
	 * Multiple calls to the same input should return the same results
	 */
	@Test
	public void checkMultipleCalls() {
		GrayF32 input = new GrayF32(width,height);
		renderCheckered(input);

		Object alg = createDetector(50);

		int firstFound = detectFeature(input, alg);
		int secondFound = detectFeature(input, alg);

		assertEquals(firstFound, secondFound);
	}

	/**
	 * Creates a new feature detector.  Max feature detector isn't a hard max.
	 *
	 * @param maxFeatures Used to adjust the number of detected features.  -1 indicates all.
	 * @return New feature detector
	 */
	protected abstract Object createDetector( int maxFeatures );

	/**
	 * Returns the number of detected features in the image.
	 */
	protected abstract int detectFeature(GrayF32 input, Object detector);

	private void drawCircle(GrayF32 img , int c_x , int c_y , double r ) {

		for( int y = 0; y < img.height; y++ ) {
			for( int x = 0; x < img.width; x++ ) {
				double d = UtilPoint2D_I32.distance(x,y,c_x,c_y);
				if( d <= r ) {
					img.set(x,y,0);
				}
			}
		}
	}

	protected void renderCheckered(GrayF32 input) {
		boolean moo = true;
		for( int y = 0; y < input.height; y += 10 ) {
			int h = Math.min(10,input.height-y);
			boolean boo = moo;
			for( int x = 0; x < input.width; x += 10 ) {
				int w = Math.min(10,input.width-x);

				if( boo )
					GImageMiscOps.fillRectangle(input, 50, x, y, w, h);
				boo = !boo;
			}
			moo = !moo;
		}

		GImageMiscOps.addUniform(input,rand,-5,5);
	}
}
