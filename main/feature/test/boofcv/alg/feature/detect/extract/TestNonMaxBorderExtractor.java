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

package boofcv.alg.feature.detect.extract;

import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import boofcv.testing.BoofTesting;
import georegression.struct.point.Point2D_I16;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestNonMaxBorderExtractor {

	int width = 30;
	int height = 40;

	int minSeparation = 5;

	/**
	 * Put features at strategic places inside the image and see if it can detect all of them
	 */
	@Test
	public void checkDetect() {
		// check top,bottom,left,right regions
		checkDetectPoint(10, 0);
		checkDetectPoint(10, height - 1);
		checkDetectPoint(1, 10);
		checkDetectPoint(width - 2, 10);
	}

	/**
	 * See if it ignores features at MAX_VALUE
	 */
	@Test
	public void checkMaxValue() {
		// check top,bottom,left,right regions
		checkIgnoreMax(10, 0);
		checkIgnoreMax(10, height - 1);
		checkIgnoreMax(1, 10);
		checkIgnoreMax(width - 2, 10);
	}

	/**
	 * Adjust the input border and see if it has the expected behavior
	 */
	@Test
	public void checkInputBorder() {
		ImageFloat32 inten = new ImageFloat32(width,height);
		inten.set(1,1,20);

		NonMaxBorderExtractor alg = new NonMaxBorderExtractor(minSeparation,5);

		QueueCorner corners = new QueueCorner(100);

		// test positive case with no input border
		alg.process(inten, corners);
		assertEquals(1, corners.size);

		// test negative case with the input border
		alg.setInputBorder(2);
		corners.reset();
		alg.process(inten, corners);
		assertEquals(0,corners.size);
	}

	/**
	 * See if it can process sub-images correctly
	 */
	@Test
	public void checkSubImage() {
		checkSubImage(10, 0);
		checkSubImage(10, height - 1);
		checkSubImage(1, 10);
		checkSubImage(width - 2, 10);
	}

	private void checkSubImage( int x , int y ) {
		ImageFloat32 inten = new ImageFloat32(width,height);
		inten.set(x,y,20);

		// create a sub image and see if stuff breaks
		inten = BoofTesting.createSubImageOf(inten);

		NonMaxBorderExtractor alg = new NonMaxBorderExtractor(minSeparation,5);

		QueueCorner corners = new QueueCorner(100);

		// test positive case with no input border
		alg.process(inten, corners);
		assertEquals(1,corners.size);
		Point2D_I16 p = corners.get(0);

		assertEquals(x,p.x);
		assertEquals(y,p.y);
	}

	private void checkDetectPoint( int x , int y ) {
		ImageFloat32 inten = new ImageFloat32(width,height);
		inten.set(x,y,20);

		NonMaxBorderExtractor alg = new NonMaxBorderExtractor(minSeparation,5);

		QueueCorner corners = new QueueCorner(100);

		alg.process(inten, corners);

		assertEquals(1,corners.size);
		Point2D_I16 p = corners.get(0);

		assertEquals(x,p.x);
		assertEquals(y,p.y);
	}

	private void checkIgnoreMax( int x , int y ) {
		ImageFloat32 inten = new ImageFloat32(width,height);
		inten.set(x,y,Float.MAX_VALUE);

		NonMaxBorderExtractor alg = new NonMaxBorderExtractor(minSeparation,5);

		QueueCorner corners = new QueueCorner(100);

		alg.process(inten, corners);

		assertEquals(0,corners.size);
	}

}
