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

package boofcv.abst.feature.detect.extract;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofTesting;
import georegression.struct.point.Point2D_I16;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public abstract class GeneralNonMaxSuppressionChecks {

	Random rand = new Random(123);

	int width = 20;
	int height = 25;

	NonMaxSuppression alg;

	GrayF32 image = new GrayF32(width,height);
	QueueCorner candidatesMin = new QueueCorner(10);
	QueueCorner candidatesMax = new QueueCorner(10);

	QueueCorner foundMax = new QueueCorner(10);
	QueueCorner foundMin = new QueueCorner(10);

	public abstract NonMaxSuppression createAlg();

	public void testAll() {
		getUsesCandidates();
		setIgnoreBorder_basic();
		setIgnoreBorder_conflict();
		setThreshold();
		setSearchRadius();
		ignoreMAX_VALUE();
		checkSubImage();
	}

	public void init() {
		alg = createAlg();

		alg.setIgnoreBorder(0);
		alg.setSearchRadius(1);
		alg.setThresholdMinimum(-5);
		alg.setThresholdMaximum(5);

		ImageMiscOps.fill(image,0);
		if( alg.getUsesCandidates() ) {
			candidatesMin.reset();
			candidatesMax.reset();
		} else {
			candidatesMin = null;
			candidatesMax = null;
		}
	}

	@Test
	public void getUsesCandidates() {
		init();

		if( !alg.getUsesCandidates() )
			return;

		// add maximums and minimums, some will be in the candidates list and others not
		setPixel(2, 4, true, 10);
		image.set(2,2,10);

		setPixel(3, 4, false, -10);
		image.set(3,2,-9);

		// with the candidate list only (2,4) should be found
		foundMin.reset();foundMax.reset();
		alg.process(image,candidatesMin, candidatesMax,foundMin,foundMax);
		checkDetectedSize(1,1);

		if( alg.canDetectMaximums() )
			checkFor(2,4,foundMax);
		if( alg.canDetectMinimums() )
			checkFor(3,4,foundMin);

		// All points from the list, none should be found
		candidatesMin.reset();
		candidatesMax.reset();
		foundMin.reset(); foundMax.reset();
		alg.process(image,candidatesMin, candidatesMax,foundMin,foundMax);
		assertEquals(0, foundMin.size);
		assertEquals(0, foundMax.size);

		// since it got two different answers that were dependent on the candidate list it passes the test
	}

	/**
	 * Makes sure that the border just defines the region in which an exteme can be found.  If a pixel is within
	 * the exclusion zone and larger magnitude than a near by pixel inside, the inside pixel can't be an exteme
	 */
	@Test
	public void setIgnoreBorder_conflict() {
		if( alg.canDetectMaximums() )
			setIgnoreBorder_conflict(true);
		if( alg.canDetectMinimums() )
			setIgnoreBorder_conflict(false);
	}

	private void setIgnoreBorder_conflict( boolean checkMax ) {
		init();
		alg.setSearchRadius(1);

		float sign = checkMax ? 1 : -1;

		setPixel(0, 1, checkMax , sign*90);
		setPixel(1, 1, checkMax , sign*30);

		// with no border (0,1) should be a peak
		foundMin.reset();foundMax.reset();
		alg.process(image,candidatesMin, candidatesMax,foundMin,foundMax);
		if( checkMax ) {
			assertEquals(0, foundMin.size);
			assertEquals(1, foundMax.size);
		} else {
			assertEquals(1, foundMin.size);
			assertEquals(0, foundMax.size);
		}

		// now with a border there should be no maximum.  30 gets knocked out because 90 is next to it
		foundMin.reset();foundMax.reset();
		alg.setIgnoreBorder(1);
		alg.process(image,candidatesMin, candidatesMax,foundMin,foundMax);
		assertEquals(0, foundMin.size);
		assertEquals(0, foundMax.size);
	}

	@Test
	public void setIgnoreBorder_basic() {
		setIgnoreBorder_basic( true );
		setIgnoreBorder_basic( false );
	}

	public void setIgnoreBorder_basic( boolean checkMax ) {
		init();

		float a = checkMax ? 1 : -1;

		// place features inside the image at the border of where they can be processed
		setPixel(0,0, checkMax, 10*a);
		setPixel(width-1,height-1, checkMax, 10*a);

		// should find both of them
		foundMin.reset();foundMax.reset();
		alg.process(image,candidatesMin, candidatesMax,foundMin,foundMax);
		if( checkMax ) {
			checkDetectedSize(0, 2);
		} else {
			checkDetectedSize(2, 0);
		}

		// now it shouldn't find them
		foundMin.reset(); foundMax.reset();
		alg.setIgnoreBorder(1);
		alg.process(image, candidatesMin, candidatesMax, foundMin, foundMax);
		assertEquals(0, foundMin.size);
		assertEquals(0, foundMax.size);
	}

	@Test
	public void setThreshold() {
		init();

		setPixel(5,6, true, 10);
		setPixel(5,4, false, -10);
		foundMin.reset(); foundMax.reset();
		alg.process(image, candidatesMin, candidatesMax, foundMin, foundMax);
		checkDetectedSize(1, 1);

		// shouldn't find it after the threshold is set above its values
		alg.setThresholdMinimum(-20);
		alg.setThresholdMaximum(20);
		foundMin.reset(); foundMax.reset();
		alg.process(image, candidatesMin, candidatesMax, foundMin, foundMax);
		checkDetectedSize(0, 0);
	}


	@Test
	public void setSearchRadius() {
		init();

		setPixel(5, 6, true, 9);
		setPixel(7,6, true, 10);
		setPixel(6, 6, false, -9);
		setPixel(8,6, false, -10);

		// should find them both on the first pass
		foundMin.reset(); foundMax.reset();
		alg.setSearchRadius(1);
		alg.process(image, candidatesMin, candidatesMax, foundMin, foundMax);
		checkDetectedSize(2, 2);

		// only one of the aftear the search radius is expanded
		alg.setSearchRadius(2);
		foundMin.reset(); foundMax.reset();
		alg.process(image, candidatesMin, candidatesMax, foundMin, foundMax);
		checkDetectedSize(1, 1);
	}

	@Test
	public void ignoreMAX_VALUE() {
		init();

		setPixel(4,5, true, Float.MAX_VALUE);
		setPixel(4,6, false, -Float.MAX_VALUE);
		foundMin.reset(); foundMax.reset();
		alg.process(image, candidatesMin, candidatesMax, foundMin, foundMax);
		checkDetectedSize(0,0);

		// sanity check
		resetCandidates();
		setPixel(4, 5, true, 10);
		setPixel(4, 6, false, -10);
		foundMin.reset(); foundMax.reset();
		alg.process(image, candidatesMin, candidatesMax, foundMin, foundMax);
		checkDetectedSize(1,1);
	}

	private void resetCandidates() {
		if( candidatesMin != null )
			candidatesMin.reset();
		if( candidatesMax != null )
			candidatesMax.reset();
	}

	/**
	 * When processing a sub-image it should produce the same results as when processing a regular image
	 */
	@Test
	public void checkSubImage() {
		init();

		ImageMiscOps.fillGaussian(image,rand,0,2,-100,100);
		// make every pixel as a candidate since I'm not sure which ones are extremes.
		for( int i = 0; i < image.height; i++ )
			for( int j = 0; j < image.width; j++ ) {
				if( candidatesMin != null )
					candidatesMin.add(j,i);
				if( candidatesMax != null )
					candidatesMax.add(j,i);
			}

		// the original input image
		foundMin.reset(); foundMax.reset();
		alg.process(image, candidatesMin, candidatesMax, foundMin, foundMax);
		int origMin = foundMin.size;
		int origMax = foundMax.size;

		// if sub-images are correctly handled this should produce identical results
		GrayF32 sub = BoofTesting.createSubImageOf(image);
		foundMin.reset(); foundMax.reset();
		alg.process(sub, candidatesMin, candidatesMax, foundMin, foundMax);
		int subMin = foundMin.size;
		int subMax = foundMax.size;

		assertEquals(origMin,subMin);
		assertEquals(origMax,subMax);
	}

	public void checkDetectedSize( int min , int max ) {
		if( alg.canDetectMaximums() ) {
			assertEquals(max,foundMax.size);
		} else {
			assertEquals(0,foundMax.size);
		}
		if( alg.canDetectMinimums() ) {
			assertEquals(min,foundMin.size);
		} else {
			assertEquals(0,foundMin.size);
		}
	}

	public void checkFor( int x , int y , QueueCorner list) {
		int numFound = 0;

		for( int i = 0; i < list.size; i++ ) {
			Point2D_I16 p = list.get(i);
			if( p.x == x && p.y == y )
				numFound++;
		}
		if( numFound > 1 )
			fail("Found multiple points of "+x+" "+y);
		else if( numFound == 0 )
			fail("Point "+x+" "+y+" was not found");
	}

	public void setPixel(int x, int y, boolean isMax, float intensity) {
		image.set(x,y,intensity);
		if( alg.getUsesCandidates() ) {
			if( isMax )
				candidatesMax.add(x,y);
			else
				candidatesMin.add(x,y);
		}
	}
}
