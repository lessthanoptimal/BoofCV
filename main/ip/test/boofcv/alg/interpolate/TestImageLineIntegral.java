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

package boofcv.alg.interpolate;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.FactoryGImageGray;
import boofcv.struct.image.GrayU8;
import org.junit.Before;
import org.junit.Test;

import static java.lang.Math.sqrt;
import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestImageLineIntegral {

	ImageLineIntegral alg;
	double tol = 1e-6;

	@Before
	public void init() {
		alg = new ImageLineIntegral();
	}
	
	/**
	 * The same two points are passed in for the end points
	 */
	@Test
	public void zeroLengthLine() {
		GrayU8 img = new GrayU8(10,15);
		img.set(6,6,100);

		alg.setImage(FactoryGImageGray.wrap(img));

		checkSolution(6,6,6,6, 0);
		checkSolution(6.1,6.1,6.1,6.1, 0);
	}

	/**
	 * Tests an integral inside a single pixel where x or y slope is zero
	 */
	@Test
	public void inside_SlopeZero() {
		GrayU8 img = new GrayU8(10,15);
		img.set(6,6,100);

		alg.setImage(FactoryGImageGray.wrap(img));

		checkSolution(6.5, 6, 6.5, 7, 100);
		checkSolution(6.5, 6, 6.5, 6.9, 0.9 * 100);
		checkSolution(6.5, 6.1, 6.5, 7.0, 0.9 * 100);

		checkSolution(6, 6.5, 7, 6.5, 100);
		checkSolution(6, 6.5, 6.9, 6.5, 0.9 * 100);
		checkSolution(6.1, 6.5, 7, 6.5, 0.9 * 100);
	}

	/**
	 * Test cases where the slope for x or y is zero across multiple pixels
	 */
	@Test
	public void across_SlopeZero() {
		GrayU8 img = new GrayU8(10,15);
		img.set(6,6,100);
		img.set(6,7,50);
		img.set(6,8,10);
		img.set(7,6,50);
		img.set(8,6,10);

		alg.setImage(FactoryGImageGray.wrap(img));

		checkSolution(6.5, 6, 6.5, 8, 150);
		checkSolution(6.5, 6, 6.5, 7.5, 125);
		checkSolution(6.5, 6.5, 6.5, 8, 100);
		checkSolution(6.5, 6.5, 6.5, 7.5, 75);
		checkSolution(6.5, 6, 6.5, 8.5, 155);

		checkSolution(6,   6.5, 8  , 6.5, 150);
		checkSolution(6,   6.5, 7.5, 6.5, 125);
		checkSolution(6.5, 6.5, 8  , 6.5, 100);
		checkSolution(6.5, 6.5, 7.5, 6.5, 75);
		checkSolution(6,   6.5, 8.5, 6.5, 155);
	}

	/**
	 * Test cases where the slope for x or y is zero across multiple pixels and sampling is done near the image
	 * border
	 */
	@Test
	public void nearBorder_SlopeZero() {
		GrayU8 img = new GrayU8(10,15);
		img.set(0,0,100);
		img.set(9,14,120);

		alg.setImage(FactoryGImageGray.wrap(img));

		checkSolution(0.5, 0, 0.5, 0.5, 50);
		checkSolution(9.5, 14.5, 9.5, 15, 0.5*120);

		checkSolution(0, 0.5, 0.5, 0.5, 50);
		checkSolution(9.5, 14.5, 10.0, 14.5, 0.5*120);
	}

	/**
	 * See if it calculates the integral correctly inside a single pixel
	 */
	@Test
	public void inside_nonZero() {
		GrayU8 img = new GrayU8(10,15);
		ImageMiscOps.fill(img,255);
		img.set(6,6,100);

		alg.setImage(FactoryGImageGray.wrap(img));

		// entirely inside
		double r = Math.sqrt(0.1*0.1 + 0.2*0.2);
		checkSolution(6.1, 6.2, 6.2, 6.4, r*100);
		checkSolution(6.2, 6.1, 6.4, 6.2, r*100);

		// one entire diagonal
		checkSolution(6, 6, 7, 7, sqrt(2) * 100);
		checkSolution(6, 7, 7, 6, sqrt(2) * 100);
	}

	/**
	 * Move across multiple squares
	 */
	@Test
	public void across_nonZero() {
		GrayU8 img = new GrayU8(10,15);
		ImageMiscOps.fill(img, 255);
		img.set(6,6,100);
		img.set(6,7,200);
		img.set(6,8,140);
		img.set(7,6,150);
		img.set(8,6,175);
		img.set(7,7,50);

		alg.setImage(FactoryGImageGray.wrap(img));

		// two entire diagonal at 45 degrees
		checkSolution(6, 6, 8, 8, sqrt(2) * (100+50));
		checkSolution(6, 8, 8, 6, sqrt(2) * (200+150));

		// two squares horizontal and vertical
		double r = Math.sqrt(1+4)/2;
		checkSolution(6, 6, 8, 7, r * (100+150));
		checkSolution(6, 6, 7, 8, r * (100+200));

		// three squares horizontal and vertical
		r = Math.sqrt(1+9)/3;
		checkSolution(6, 6, 9, 7, r * (100+150+175));
		checkSolution(6, 6, 7, 9, r * (100+200+140));

		// now try some which don't start and stop at a corner
		double dy = 0.5/3;

		checkSolution(6.5, 6+dy, 8.5, 7-dy, (r/2)*100+r*150+(r/2)*175);
		checkSolution(6+dy, 6.5, 7-dy, 8.5, (r/2)*100+r*200+(r/2)*140);

	}

	/**
	 * See if it handles borders correctly with both slopes are not zero
	 */
	@Test
	public void nearBorder_nonZero() {
		GrayU8 img = new GrayU8(2,2);
		img.set(0,0,100);
		img.set(0,1,200);
		img.set(1,0,140);
		img.set(1,1,150);

		alg.setImage(FactoryGImageGray.wrap(img));

		checkSolution(0, 0, 2, 2, sqrt(2) * (100+150));
		checkSolution(2, 0, 0, 2, sqrt(2) * (200+140));

		// two squares horizontal and vertical
		double r = Math.sqrt(1+4)/2;
		checkSolution(0, 0, 2, 1, r * (100+140));
		checkSolution(0, 0, 1, 2, r * (100+200));
	}

	@Test
	public void isInside() {
		GrayU8 img = new GrayU8(12,14);
		alg.setImage(FactoryGImageGray.wrap(img));
		assertTrue(alg.isInside(0,0));
		assertTrue(alg.isInside(12,14));
		assertTrue(alg.isInside(12,0));
		assertTrue(alg.isInside(0,14));

		assertFalse(alg.isInside(-0.0001, 0));
		assertFalse(alg.isInside(0, -0.00001));
		assertFalse(alg.isInside(12.000001, 14));
		assertFalse(alg.isInside(12, 14.0001));
		assertFalse(alg.isInside(12.0001, 0));
		assertFalse(alg.isInside(0, 14.00001));

	}

	private void checkSolution(double x0, double y0, double x1, double y1, double expected) {
		// ind solution in the orward direction
		double found = alg.compute(x0, y0, x1, y1);
		assertEquals(expected,found,tol);

		// should be the same in the reverse direction
		found = alg.compute(x1, y1, x0, y0);
		assertEquals(expected,found,tol);
	}
}