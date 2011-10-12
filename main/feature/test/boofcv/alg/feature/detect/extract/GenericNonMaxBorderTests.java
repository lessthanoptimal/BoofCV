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


import boofcv.alg.misc.ImageTestingOps;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import boofcv.testing.BoofTesting;
import georegression.struct.point.Point2D_I16;

import static org.junit.Assert.assertEquals;

/**
 * Standard tests for  non-maximum suppression algorithms just along the image border.
 *
 * @author Peter Abeles
 */
public abstract class GenericNonMaxBorderTests {

	protected int width = 30;
	protected int height = 40;

	protected  QueueCorner found = new QueueCorner(100);
	protected  ImageFloat32 intensity = new ImageFloat32(width,height);

	protected  boolean useStrictRule;

	public abstract void findLocalMaximums(ImageFloat32 intensity,
										   float threshold, int radius,
										   boolean useStrict, QueueCorner found);


	public void reset() {
		found.reset();
		ImageTestingOps.fill(intensity,0);
	}

	public void allStandard( boolean useStrictRule ) {
		if( useStrictRule )
			testStrictRule();
		else
			testRelaxedRule();

		this.useStrictRule = useStrictRule;

		checkDetect();
		checkMaxValue();
		checkSubImage();
	}

	public void testStrictRule() {
		reset();

		intensity.set(1,1,20);
		intensity.set(0,0,20);

		findLocalMaximums(intensity,10,3, true,found);
		assertEquals(0, found.size);
	}

	public void testRelaxedRule() {
		reset();

		intensity.set(1,1,20);
		intensity.set(0,0,20);

		findLocalMaximums(intensity,10,3, false,found);
		assertEquals(2, found.size);
	}

	/**
	 * Put features at strategic places inside the image and see if it can detect all of them
	 */
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
	public void checkMaxValue() {
		// check top,bottom,left,right regions
		checkIgnoreMax(10, 0);
		checkIgnoreMax(10, height - 1);
		checkIgnoreMax(1, 10);
		checkIgnoreMax(width - 2, 10);
	}

	/**
	 * See if it can process sub-images correctly
	 */
	public void checkSubImage() {
		checkSubImage(10, 0);
		checkSubImage(10, height - 1);
		checkSubImage(1, 10);
		checkSubImage(width - 2, 10);
	}

	private void checkSubImage( int x , int y ) {
		reset();
		intensity.set(x,y,20);

		// create a sub image and see if stuff breaks
		ImageFloat32 intensity = BoofTesting.createSubImageOf(this.intensity);

		findLocalMaximums(intensity,10,3, useStrictRule,found);

		// test positive case with no input border
		found.reset();
		findLocalMaximums(intensity, 10, 3, useStrictRule, found);
		assertEquals(1, found.size);
		Point2D_I16 p = found.get(0);

		assertEquals(x, p.x);
		assertEquals(y,p.y);
	}

	private void checkDetectPoint( int x , int y ) {
		reset();
		intensity.set(x, y, 20);

		findLocalMaximums(intensity, 10, 3, useStrictRule, found);

		assertEquals(1, found.size);
		Point2D_I16 p = found.get(0);

		assertEquals(x, p.x);
		assertEquals(y,p.y);
	}

	private void checkIgnoreMax( int x , int y ) {
		reset();
		intensity.set(x, y, Float.MAX_VALUE);

		findLocalMaximums(intensity, 10, 3, useStrictRule, found);

		assertEquals(0,found.size);
	}



}
