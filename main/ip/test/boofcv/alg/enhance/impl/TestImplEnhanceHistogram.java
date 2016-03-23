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

package boofcv.alg.enhance.impl;

import boofcv.alg.enhance.EnhanceImageOps;
import boofcv.alg.enhance.GEnhanceImageOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.GImageStatistics;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayI;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class TestImplEnhanceHistogram {

	int width = 15;
	int height = 20;
	Random rand = new Random(234);

	@Test
	public void applyTransform() {
		int numFound = 0;

		Method methods[] = ImplEnhanceHistogram.class.getMethods();
		for( int i = 0; i < methods.length; i++ ) {
			if( methods[i].getName().compareTo("applyTransform") != 0 )
				continue;

			numFound++;

			Class imageType = methods[i].getParameterTypes()[0];
			ImageGray input = GeneralizedImageOps.createSingleBand(imageType,width,height);
			ImageGray output = GeneralizedImageOps.createSingleBand(imageType,width,height);

			applyTransform( input , output );

			BoofTesting.checkSubImage(this,"applyTransform",true,input,output);
		}

		assertEquals(5,numFound);
	}

	public void applyTransform(ImageGray input , ImageGray output ) {
		int min = input.getDataType().isSigned() ? -10 : 0;
		int transform[] = new int[10-min];

		GImageMiscOps.fillUniform(input, rand, Math.min(min+1,0), 10);
		for( int i = min; i < 10; i++ )
			transform[i-min] = i*2;

		if(input.getDataType().isSigned() ) {
			BoofTesting.callStaticMethod(ImplEnhanceHistogram.class,"applyTransform",
					input,transform,-10,output);
		} else {
			BoofTesting.callStaticMethod(ImplEnhanceHistogram.class,"applyTransform",
				input,transform,output);
		}

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				double valueIn = GeneralizedImageOps.get(input,x,y);
				double valueOut = GeneralizedImageOps.get(output,x,y);

				assertEquals(2*valueIn,valueOut,1e-5);
			}
		}
	}

	/**
	 * Validate naive algorithm by comparing it against to the full image equalization that has been passed
	 * sub-images.
	 */
	@Test
	public void equalizeLocalNaive() {
		int numFound = 0;

		Method methods[] = ImplEnhanceHistogram.class.getMethods();
		for (Method method : methods) {
			if (method.getName().compareTo("equalizeLocalNaive") != 0)
				continue;

			numFound++;

			Class imageType = method.getParameterTypes()[0];
			GrayI input = (GrayI) GeneralizedImageOps.createSingleBand(imageType, width, height);
			GrayI output = (GrayI) GeneralizedImageOps.createSingleBand(imageType, width, height);

			equalizeLocalNaive(input, output);

			BoofTesting.checkSubImage(this, "equalizeLocalNaive", true, input, output);
		}

		assertEquals(2,numFound);
	}

	public void equalizeLocalNaive(GrayI input , GrayI output ) {

		GrayI tmp = GeneralizedImageOps.createSingleBand(input.getClass(),input.width, input.height);
		int transform[] = new int[10];
		int histogram[] = new int[10];

		GImageMiscOps.fillUniform(input,rand,0,10);

		for( int radius = 1; radius < 11; radius++ ) {
			BoofTesting.callStaticMethod(ImplEnhanceHistogram.class, "equalizeLocalNaive", input, radius, output, histogram);

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
					GrayI subIn = (GrayI)input.subimage(x0,y0,x1,y1, null);
					GrayI subOut = (GrayI)tmp.subimage(x0,y0,x1,y1, null);
					GImageStatistics.histogram(subIn,0, histogram);
					EnhanceImageOps.equalize(histogram, transform);
					GEnhanceImageOps.applyTransform(subIn, transform, 0,subOut);

					int expected = subOut.get(x-x0,y-y0);
					int found = output.get(x,y);

					assertEquals(x+" "+y,expected,found);
				}
			}
		}
	}

	@Test
	public void equalizeLocalInner() {
		int numFound = 0;

		Method methods[] = ImplEnhanceHistogram.class.getMethods();
		for (Method method : methods) {
			if (method.getName().compareTo("equalizeLocalNaive") != 0)
				continue;

			numFound++;

			Class imageType = method.getParameterTypes()[0];
			GrayI input = (GrayI) GeneralizedImageOps.createSingleBand(imageType, width, height);
			GrayI output = (GrayI) GeneralizedImageOps.createSingleBand(imageType, width, height);

			equalizeLocalInner(input, output);
			BoofTesting.checkSubImage(this, "equalizeLocalInner", true, input, output);
		}

		assertEquals(2,numFound);
	}

	public void equalizeLocalInner(GrayI input , GrayI found ) {
		GrayI expected = GeneralizedImageOps.createSingleBand(input.getClass(),input.width, input.height);
		int histogram[] = new int[10];

		GImageMiscOps.fillUniform(input,rand,0,10);

		for( int radius = 1; radius < 6; radius++ ) {
			// fill with zeros so it can be tested using checkBorderZero
			GImageMiscOps.fill(found,0);

			BoofTesting.callStaticMethod(ImplEnhanceHistogram.class, "equalizeLocalNaive", input, radius, expected, histogram);
			BoofTesting.callStaticMethod(ImplEnhanceHistogram.class, "equalizeLocalInner", input, radius, found, histogram);

			BoofTesting.assertEqualsInner(expected,found,1e-10,radius,radius,false);
			BoofTesting.checkBorderZero(found,radius);
		}
	}

	@Test
	public void equalizeLocalRow() {
		int numFound = 0;

		Method methods[] = ImplEnhanceHistogram.class.getMethods();
		for (Method method : methods) {
			if (method.getName().compareTo("equalizeLocalRow") != 0)
				continue;

			numFound++;

			Class imageType = method.getParameterTypes()[0];
			GrayI input = (GrayI) GeneralizedImageOps.createSingleBand(imageType, width, height);
			GrayI output = (GrayI) GeneralizedImageOps.createSingleBand(imageType, width, height);

			equalizeLocalRow(input, output);
			BoofTesting.checkSubImage(this, "equalizeLocalRow", true, input, output);
		}

		assertEquals(2,numFound);
	}

	public void equalizeLocalRow(GrayI input , GrayI found ) {
		GrayI expected = GeneralizedImageOps.createSingleBand(input.getClass(),input.width, input.height);
		int histogram[] = new int[10];
		int transform[] = new int[10];

		GImageMiscOps.fillUniform(input,rand,0,10);

		// check the top row
		for( int radius = 1; radius < 6; radius++ ) {
			// fill with zeros so it can be tested using checkBorderZero
			GImageMiscOps.fill(found,0);

			BoofTesting.callStaticMethod(ImplEnhanceHistogram.class,
					"equalizeLocalNaive", input, radius, expected, histogram);
			BoofTesting.callStaticMethod(ImplEnhanceHistogram.class,
					"equalizeLocalRow", input, radius, 0, found, histogram,transform);

			GrayI subExpected = (GrayI)expected.subimage(0,0,width,radius, null);
			GrayI subFound = (GrayI)found.subimage(0,0,width,radius, null);

			// check solution
			BoofTesting.assertEquals(subExpected,subFound,1e-10);
			checkZeroOutsideRows(found,0,radius);
		}

		// check the bottom row
		for( int radius = 1; radius < 6; radius++ ) {
			// fill with zeros so it can be tested using checkBorderZero
			GImageMiscOps.fill(found,0);

			int start = input.height-radius;

			BoofTesting.callStaticMethod(ImplEnhanceHistogram.class,
					"equalizeLocalNaive", input, radius, expected, histogram);
			BoofTesting.callStaticMethod(ImplEnhanceHistogram.class,
					"equalizeLocalRow", input, radius, start, found, histogram,transform);

			GrayI subExpected = (GrayI)expected.subimage(0,start,width,height, null);
			GrayI subFound = (GrayI)found.subimage(0,start,width,height, null);

			// check solution
			BoofTesting.assertEquals(subExpected,subFound,1e-10);
			checkZeroOutsideRows(found,start,height);
		}
	}

	private void checkZeroOutsideRows(GrayI image , int y0 , int y1) {
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
		int numFound = 0;

		Method methods[] = ImplEnhanceHistogram.class.getMethods();
		for (Method method : methods) {
			if (method.getName().compareTo("equalizeLocalCol") != 0)
				continue;

			numFound++;

			Class imageType = method.getParameterTypes()[0];
			GrayI input = (GrayI) GeneralizedImageOps.createSingleBand(imageType, width, height);
			GrayI output = (GrayI) GeneralizedImageOps.createSingleBand(imageType, width, height);

			equalizeLocalCol(input, output);
			BoofTesting.checkSubImage(this, "equalizeLocalCol", true, input, output);
		}

		assertEquals(2,numFound);
	}

	public void equalizeLocalCol(GrayI input , GrayI found ) {
		GrayI expected = GeneralizedImageOps.createSingleBand(input.getClass(),input.width, input.height);
		int histogram[] = new int[10];
		int transform[] = new int[10];

		GImageMiscOps.fillUniform(input,rand,1,10);

		// check the left column
		for( int radius = 1; radius < 6; radius++ ) {
			// fill with zeros so it can be tested using checkBorderZero
			GImageMiscOps.fill(found,0);

			BoofTesting.callStaticMethod(ImplEnhanceHistogram.class,
					"equalizeLocalNaive", input, radius, expected, histogram);
			BoofTesting.callStaticMethod(ImplEnhanceHistogram.class,
					"equalizeLocalCol", input, radius, 0, found, histogram, transform);

			GrayI subExpected = (GrayI)expected.subimage(0,radius,radius,height-radius-1, null);
			GrayI subFound = (GrayI)found.subimage(0,radius,radius,height-radius-1, null);

			// check solution
			BoofTesting.assertEquals(subExpected,subFound,1e-10);
			checkZeroOutsideColumns(found, 0, radius, radius);
		}

		// check the right column
		for( int radius = 1; radius < 6; radius++ ) {
			// fill with zeros so it can be tested using checkBorderZero
			GImageMiscOps.fill(found,0);

			int start = input.width-radius;

			BoofTesting.callStaticMethod(ImplEnhanceHistogram.class,
					"equalizeLocalNaive", input, radius, expected, histogram);
			BoofTesting.callStaticMethod(ImplEnhanceHistogram.class,
					"equalizeLocalCol", input, radius, start, found, histogram, transform);

			GrayI subExpected = (GrayI)expected.subimage(start,radius,width,height-radius-1, null);
			GrayI subFound = (GrayI)found.subimage(start,radius,width,height-radius-1, null);

			// check solution
			BoofTesting.assertEquals(subExpected,subFound,1e-10);
			checkZeroOutsideColumns(found, start, width, radius);
		}
	}

	private void checkZeroOutsideColumns(GrayI image , int x0 , int x1, int radius) {
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
}
