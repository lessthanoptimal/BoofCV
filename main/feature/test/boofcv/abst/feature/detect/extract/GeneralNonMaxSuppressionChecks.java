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

package boofcv.abst.feature.detect.extract;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import boofcv.testing.BoofTesting;
import georegression.struct.point.Point2D_I16;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public abstract class GeneralNonMaxSuppressionChecks {

	Random rand = new Random(123);

	int width = 20;
	int height = 25;

	NonMaxSuppression alg;

	ImageFloat32 image = new ImageFloat32(width,height);
	QueueCorner candidatesMin = new QueueCorner(10);
	QueueCorner candidatesMax = new QueueCorner(10);

	QueueCorner foundMax = new QueueCorner(10);
	QueueCorner foundMin = new QueueCorner(10);

	public abstract NonMaxSuppression createAlg();

	public void testAll() {
		getUsesCandidates();
		setIgnoreBorder_basic();
		setIgnoreBorder_harder();
		setThreshold();
		setSearchRadius();
		canDetectBorder();
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

	@Test
	public void setIgnoreBorder_basic() {
		setIgnoreBorder_basic( true );
		setIgnoreBorder_basic( false );
	}

	public void setIgnoreBorder_basic( boolean checkMax ) {
		init();

		float a = checkMax ? 1 : -1;
		int r = alg.canDetectBorder() ? 0 : alg.getSearchRadius();

		// place features inside the image at the border of where they can be processed
		setPixel(r,r, checkMax, 10*a);
		setPixel(width-r-1,height-r-1, checkMax, 10*a);

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
	public void setIgnoreBorder_harder() {
		setIgnoreBorder_harder(true);
		setIgnoreBorder_harder(false);
	}

	public void setIgnoreBorder_harder( boolean checkMax ) {
		init();

		float a = checkMax ? 1 : -1;
		int r = alg.canDetectBorder() ? 1 : alg.getSearchRadius()+1;

		// place features inside the image at the border of where they can be processed
		setPixel(r,r, checkMax, 10*a);
		setPixel(width-r-1,height-r-1, checkMax, 10*a);
		// then even more intense features right next to them outside of where they should be processed
		setPixel(r-1,r, checkMax, 15*a);
		setPixel(r,r-1, checkMax, 15*a);
		setPixel(width-r,height-r-1, checkMax, 15*a);
		setPixel(width-r-1,height-r, checkMax, 15*a);

		// If it doesn't find both of those features then its actually looking outside the ignore border
		foundMin.reset(); foundMax.reset();
		alg.setIgnoreBorder(1);
		alg.process(image, candidatesMin, candidatesMax, foundMin, foundMax);
		if( checkMax ) {
			checkDetectedSize(0,2);
		} else {
			checkDetectedSize(2,0);
		}

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
	public void canDetectBorder() {
		canDetectBorder(false);
		canDetectBorder(true);
	}

	public void canDetectBorder(boolean checkMax) {
		init();

		float a = checkMax ? 1 : -1;

		// place two features along the image border
		setPixel(0,0, checkMax, 10*a);
		setPixel(width-1,height-1, checkMax, 10*a);

		// the solution should depend on its capabilities
		foundMin.reset(); foundMax.reset();
		alg.process(image, candidatesMin, candidatesMax, foundMin, foundMax);
		if( foundMin.size == 2 || foundMax.size == 2 )
			assertTrue(alg.canDetectBorder());
		else if( alg.canDetectMinimums() ) {
			if( !checkMax )
				if( foundMin.size == 0 )
					assertTrue(!alg.canDetectBorder());
				else
					fail("Unexpected number of found");
		} else if( alg.canDetectMaximums() ) {
			if( checkMax )
				if( foundMax.size == 0 )
					assertTrue(!alg.canDetectBorder());
				else
					fail("Unexpected number of found");
		}
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
		ImageFloat32 sub = BoofTesting.createSubImageOf(image);
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
