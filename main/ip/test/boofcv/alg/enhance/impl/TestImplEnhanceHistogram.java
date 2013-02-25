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

package boofcv.alg.enhance.impl;

import boofcv.alg.enhance.EnhanceImageOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.struct.image.ImageUInt8;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestImplEnhanceHistogram {

	int width = 15;
	int height = 20;
	Random rand = new Random(234);

	@Test
	public void applyTransform() {
		ImageUInt8 input = new ImageUInt8(width,height);
		int transform[] = new int[10];
		ImageUInt8 output = new ImageUInt8(width,height);

		ImageMiscOps.fillUniform(input,rand,0,10);
		for( int i = 0; i < 10; i++ )
			transform[i] = i*2;

		ImplEnhanceHistogram.applyTransform(input,transform,output);

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				assertEquals(2*input.get(x,y),output.get(x,y));
			}
		}
	}

	/**
	 * Validate naive algorithm by comparing it against to the full image equalization that has been passed
	 * sub-images.
	 */
	@Test
	public void equalizeLocalNaive() {
		ImageUInt8 input = new ImageUInt8(width,height);
		ImageUInt8 output = new ImageUInt8(width,height);
		ImageUInt8 tmp = new ImageUInt8(width,height);
		int transform[] = new int[10];
		int histogram[] = new int[10];

		ImageMiscOps.fillUniform(input,rand,0,10);

		for( int radius = 1; radius < 11; radius++ ) {
			ImplEnhanceHistogram.equalizeLocalNaive(input,radius,output,histogram);

			int width = 2*radius+1;

			for( int y = 0; y < height; y++ ) {
				int y0 = y - radius;
				int y1 = y + radius+1;
				if( y0 < 0 ) {
					y0 = 0; y1 = y0 +width;
					if( y1 > input.height )  y1 = input.height;
				} else if( y1 > input.height ) {
					y1 = input.height; y0 = y1 - width;
					if( y0 < 0 ) y0 = 0;
				}
				for( int x = 0; x < input.width; x++ ) {
					int x0 = x - radius;
					int x1 = x + radius+1;
					if( x0 < 0 ) {
						x0 = 0; x1 = x0 + width;
						if( x1 > input.width ) x1 = input.width;
					} else if( x1 > input.width ) {
						x1 = input.width; x0 = x1 - width;
						if( x0 < 0 ) x0 = 0;
					}

					// use the full image algorithm
					ImageUInt8 subIn = input.subimage(x0,y0,x1,y1);
					ImageUInt8 subOut = tmp.subimage(x0,y0,x1,y1);
					ImageStatistics.histogram(subIn,histogram);
					EnhanceImageOps.equalize(histogram, transform);
					EnhanceImageOps.applyTransform(subIn, transform, subOut);

					int expected = subOut.get(x-x0,y-y0);
					int found = output.get(x,y);

					assertEquals(x+" "+y,expected,found);
				}
			}
		}
	}

	@Test
	public void equalizeLocalInner() {
		ImageUInt8 input = new ImageUInt8(width,height);
		ImageUInt8 found = new ImageUInt8(width,height);
		ImageUInt8 expected = new ImageUInt8(width,height);
		int histogram[] = new int[10];

		ImageMiscOps.fillUniform(input,rand,0,10);

		for( int radius = 1; radius < 6; radius++ ) {
			// fill with zeros so it can be tested using checkBorderZero
			ImageMiscOps.fill(found,0);

			ImplEnhanceHistogram.equalizeLocalNaive(input,radius,expected,histogram);
			ImplEnhanceHistogram.equalizeLocalInner(input, radius, found, histogram);

			BoofTesting.assertEqualsInner(expected,found,1e-10,radius,radius,false);
			BoofTesting.checkBorderZero(found,radius);
		}
	}

	@Test
	public void equalizeLocalRow() {
		ImageUInt8 input = new ImageUInt8(width,height);
		ImageUInt8 found = new ImageUInt8(width,height);
		ImageUInt8 expected = new ImageUInt8(width,height);
		int histogram[] = new int[10];
		int transform[] = new int[10];

		ImageMiscOps.fillUniform(input,rand,0,10);

		// check the top row
		for( int radius = 1; radius < 6; radius++ ) {
			// fill with zeros so it can be tested using checkBorderZero
			ImageMiscOps.fill(found,0);

			ImplEnhanceHistogram.equalizeLocalNaive(input,radius,expected,histogram);
			ImplEnhanceHistogram.equalizeLocalRow(input, radius,0, found, histogram,transform);

			ImageUInt8 subExpected = expected.subimage(0,0,width,radius);
			ImageUInt8 subFound = found.subimage(0,0,width,radius);

			// check solution
			BoofTesting.assertEquals(subExpected,subFound,1e-10);
			checkZeroOutsideRows(found,0,radius);
		}

		// check the bottom row
		for( int radius = 1; radius < 6; radius++ ) {
			// fill with zeros so it can be tested using checkBorderZero
			ImageMiscOps.fill(found,0);

			int start = input.height-radius;

			ImplEnhanceHistogram.equalizeLocalNaive(input,radius,expected,histogram);
			ImplEnhanceHistogram.equalizeLocalRow(input, radius,start, found, histogram,transform);

			ImageUInt8 subExpected = expected.subimage(0,start,width,height);
			ImageUInt8 subFound = found.subimage(0,start,width,height);

			// check solution
			BoofTesting.assertEquals(subExpected,subFound,1e-10);
			checkZeroOutsideRows(found,start,height);
		}
	}

	private void checkZeroOutsideRows( ImageUInt8 image , int y0 , int y1) {
		for( int y = 0; y < height; y++ ) {
			if( y >= y0 && y < y1)
				continue;
			for( int x = 0; x < width; x ++ ) {
				assertEquals(x+" "+y,0,image.get(x,y));
			}
		}
	}

	@Test
	public void equalizeLocalCol() {
		ImageUInt8 input = new ImageUInt8(width,height);
		ImageUInt8 found = new ImageUInt8(width,height);
		ImageUInt8 expected = new ImageUInt8(width,height);
		int histogram[] = new int[10];
		int transform[] = new int[10];

		ImageMiscOps.fillUniform(input,rand,1,10);

		// check the left column
		for( int radius = 1; radius < 6; radius++ ) {
			// fill with zeros so it can be tested using checkBorderZero
			ImageMiscOps.fill(found,0);

			ImplEnhanceHistogram.equalizeLocalNaive(input,radius,expected,histogram);
			ImplEnhanceHistogram.equalizeLocalCol(input, radius, 0, found, histogram, transform);

			ImageUInt8 subExpected = expected.subimage(0,radius,radius,height-radius-1);
			ImageUInt8 subFound = found.subimage(0,radius,radius,height-radius-1);

			// check solution
			BoofTesting.assertEquals(subExpected,subFound,1e-10);
			checkZeroOutsideColumns(found, 0, radius, radius);
		}

		// check the right column
		for( int radius = 1; radius < 6; radius++ ) {
			// fill with zeros so it can be tested using checkBorderZero
			ImageMiscOps.fill(found,0);

			int start = input.width-radius;

			ImplEnhanceHistogram.equalizeLocalNaive(input,radius,expected,histogram);
			ImplEnhanceHistogram.equalizeLocalCol(input, radius, start, found, histogram, transform);

			ImageUInt8 subExpected = expected.subimage(start,radius,width,height-radius-1);
			ImageUInt8 subFound = found.subimage(start,radius,width,height-radius-1);

			// check solution
			BoofTesting.assertEquals(subExpected,subFound,1e-10);
			checkZeroOutsideColumns(found, start, width, radius);
		}
	}

	private void checkZeroOutsideColumns( ImageUInt8 image , int x0 , int x1, int radius) {
		for( int y = 0; y < height; y++ ) {
			if( y < radius || y >= height-radius) {
				continue;
			}

			for( int x = 0; x < width; x ++ ) {
				if( x >= x0 && x < x1)
					continue;
				assertEquals(x+" "+y,0,image.get(x,y));
			}
		}
	}

	@Test
	public void testAcrossTypes() {
		fail("generalize the class");
	}

	@Test
	public void testSubImages() {
		fail("sub images");
	}
}
