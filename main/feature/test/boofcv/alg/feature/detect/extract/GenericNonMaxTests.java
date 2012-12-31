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

package boofcv.alg.feature.detect.extract;


import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import boofcv.testing.BoofTesting;
import georegression.struct.point.Point2D_I16;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Standard tests for non-maximum suppression algorithms
 *
 * @author Peter Abeles
 */
public abstract class GenericNonMaxTests {

	Random rand = new Random(2134);

	int width = 30;
	int height = 40;

	private QueueCorner found = new QueueCorner(100);
	private ImageFloat32 intensity = new ImageFloat32(width, height);


	boolean strict;

	protected GenericNonMaxTests(boolean strict) {
		this.strict = strict;
	}


	private void findLocalMaximums(ImageFloat32 intensity, float threshold, int radius,
								   int border) {
		found.reset();
		findMaximums(intensity, threshold, radius, border, found);
	}

	public abstract void findMaximums(ImageFloat32 intensity,
									  float threshold,
									  int radius,
									  int border, QueueCorner found);


	public void reset() {
		ImageMiscOps.fill(intensity, 0);
	}

	public void allStandard() {
		if (strict)
			testStrictRule();
		else
			testNotStrictRule();
		testRadius();
		testThreshold();
		exclude_MAX_VALUE();
		testSubimage();
		checkIgnoreBorder();
		negativeValuedIntensity();
		compareToNaive();
	}

	public void testSubimage() {
		reset();

		intensity.set(5, 5, 30);
		intensity.set(5, 8, 35);
		intensity.set(5, 12, 31);

		// see how many features it finds at various sizes
		findLocalMaximums(intensity, 5, 2, 0);
		assertEquals(3, found.size);

		ImageFloat32 sub = BoofTesting.createSubImageOf(intensity);
		findLocalMaximums(sub, 5, 2, 0);
		assertEquals(3, found.size);
	}

	public void testThreshold() {
		reset();

		intensity.set(5, 5, 30);
		intensity.set(5, 8, 35);
		intensity.set(5, 12, 31);

		// see how many features it finds at various sizes
		findLocalMaximums(intensity, 5, 2, 0);
		assertEquals(3, found.size);

		found.reset();
		findLocalMaximums(intensity, 35, 2, 0);
		assertEquals(1, found.size);

	}

	public void testRadius() {
		reset();

		intensity.set(5, 5, 30);
		intensity.set(5, 8, 35);
		intensity.set(5, 12, 31);

		// see how many features it finds at various sizes
		findLocalMaximums(intensity, 5, 2, 0);
		assertEquals(3, found.size);

		findLocalMaximums(intensity, 5, 3, 0);
		assertEquals(2, found.size);

		findLocalMaximums(intensity, 5, 4, 0);
		assertEquals(1, found.size);
	}

	public void testStrictRule() {
		reset();

		intensity.set(3, 5, 30);
		intensity.set(5, 7, 30);
		intensity.set(7, 7, 30);

		// none of these points are a strict maximum
		findLocalMaximums(intensity, 5, 2, 0);
		assertEquals(0, found.size);
	}

	public void testNotStrictRule() {
		reset();

		intensity.set(3, 5, 30);
		intensity.set(3, 6, 30);
		intensity.set(4, 5, 30);

		// none of these points are a strict maximum and all should be returned
		findLocalMaximums(intensity, 5, 2, 0);
		assertEquals(3, found.size);
	}

	public void checkIgnoreBorder() {
		reset();
		int radius = 3;

		intensity.set(0, 0, 100);
		intensity.set(radius, radius, 50);

		// it should consider the 0,0 pixel
		findLocalMaximums(intensity, 2, radius, radius);
		assertEquals(0, found.size);

		// no ignore border
		findLocalMaximums(intensity, 2, radius, 0);
		assertEquals(1, found.size);

		// sanity check
		intensity.set(0, 0, 0);
		findLocalMaximums(intensity, 2, radius, radius);
		assertEquals(1, found.size);
	}

	/**
	 * The entire intensity image is filled with negative values.  See if it can still find
	 * the peak easily
	 */
	public void negativeValuedIntensity() {
		ImageMiscOps.fill(intensity, -Float.MAX_VALUE);

		intensity.set(15, 20, -1000);

		findLocalMaximums(intensity, -1e16f, 2, 0);

		assertEquals(1, found.size);
		assertEquals(15, found.data[0].x);
		assertEquals(20, found.data[0].y);
	}

	public void exclude_MAX_VALUE() {
		reset();

		intensity.set(15, 20, Float.MAX_VALUE);
		intensity.set(20, 21, Float.MAX_VALUE);
		intensity.set(10, 25, Float.MAX_VALUE);
		intensity.set(11, 24, 10);
		intensity.set(25, 35, 10);

		findLocalMaximums(intensity, 5, 2, 0);

		// only one feature should be found.  The rest should be MAX_VALUE or too close to MAX_VALUE
		assertEquals(1, found.size);
		assertEquals(25, found.data[0].x);
		assertEquals(35, found.data[0].y);
	}

	/**
	 * Compares output against naive algorithm.  Checks for compliance with sub-images
	 */
	public void compareToNaive() {
		ImageFloat32 inten = new ImageFloat32(30, 40);

		QueueCorner naiveCorners = new QueueCorner(inten.getWidth() * inten.getHeight());

		for (int useSubImage = 0; useSubImage <= 1; useSubImage++) {
			// make sure it handles sub images correctly
			if (useSubImage == 1) {
				ImageFloat32 larger = new ImageFloat32(inten.width + 10, inten.height + 8);
				inten = larger.subimage(5, 5, inten.width + 5, inten.height + 5);
			}

			for (int nonMaxWidth = 3; nonMaxWidth <= 9; nonMaxWidth += 2) {
				int radius = nonMaxWidth / 2;
				NonMaxExtractorNaive reg = new NonMaxExtractorNaive(strict);
				reg.setSearchRadius(radius);
				reg.setThreshold(0.6f);

				for (int i = 0; i < 10; i++) {
					ImageMiscOps.fillUniform(inten, rand, 0, 10);


					// detect the corners
					findLocalMaximums(inten, 0.6f, radius, 0);
					naiveCorners.reset();
					reg.process(inten, naiveCorners);

					// check the number of corners
					assertTrue(found.size() > 0);

					assertEquals(naiveCorners.size(), found.size());

					for (int j = 0; j < naiveCorners.size(); j++) {
						Point2D_I16 b = naiveCorners.get(j);

						boolean foundMatch = false;
						for (int k = 0; k < found.size(); k++) {
							Point2D_I16 a = found.get(k);

							if (a.x == b.x && a.y == b.y) {
								foundMatch = true;
								break;
							}
						}

						assertTrue(foundMatch);
					}
				}
			}
		}
	}
}
