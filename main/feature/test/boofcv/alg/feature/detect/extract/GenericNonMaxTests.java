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
import boofcv.alg.misc.PixelMath;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_I16;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Compares the results against a naive algorithm and other basic tests.  More rigorous tests are part of the abstracted
 * {@link boofcv.abst.feature.detect.extract.NonMaxSuppression} compliance tests.
 *
 * @author Peter Abeles
 */
public abstract class GenericNonMaxTests {

	Random rand = new Random(2134);

	int width = 30;
	int height = 40;

	protected QueueCorner foundMinimum = new QueueCorner(100);
	protected QueueCorner foundMaximum = new QueueCorner(100);
	protected ImageFloat32 intensity = new ImageFloat32(width, height);


	boolean canDetectMin;
	boolean canDetectMax;

	boolean strict;

	protected GenericNonMaxTests(boolean strict, boolean canDetectMin, boolean canDetectMax ) {
		this.strict = strict;
		this.canDetectMin = canDetectMin;
		this.canDetectMax = canDetectMax;
	}


	private void findLocalPeaks(ImageFloat32 intensity, float threshold, int radius, int border) {
		foundMinimum.reset();
		foundMaximum.reset();
		findMaximums(intensity, threshold, radius, border, foundMinimum, foundMaximum);
	}

	public abstract void findMaximums(ImageFloat32 intensity, float threshold, int radius, int border,
									  QueueCorner foundMinimum, QueueCorner foundMaximum );


	public void reset() {
		ImageMiscOps.fill(intensity, 0);
	}

	public void allStandard() {
		checkDetectionRule();
		compareToNaive();
	}

	@Test
	public void checkDetectionRule() {
		if (strict)
			testStrictRule();
		else
			testNotStrictRule();
	}

	public void testStrictRule() {
		reset();

		intensity.set(3, 5, 30);
		intensity.set(5, 7, 30);
		intensity.set(7, 7, 30);

		intensity.set(2, 5, -30);
		intensity.set(4, 7, -30);
		intensity.set(6, 7, -30);

		// none of these points are a strict maximum
		findLocalPeaks(intensity, 5, 2, 0);
		assertEquals(0, foundMinimum.size);
		assertEquals(0, foundMaximum.size);
	}

	public void testNotStrictRule() {
		reset();

		intensity.set(3, 5, 30);
		intensity.set(3, 6, 30);
		intensity.set(4, 5, 30);

		intensity.set(2, 5, -30);
		intensity.set(4, 7, -30);
		intensity.set(6, 7, -30);


		// none of these points are a strict maximum and all should be returned
		findLocalPeaks(intensity, 5, 2, 0);
		if( canDetectMin)
			assertEquals(3, foundMinimum.size);
		if( canDetectMax)
			assertEquals(3, foundMaximum.size);
	}

	/**
	 * Compares output against naive algorithm.  Checks for compliance with sub-images
	 */
	@Test
	public void compareToNaive() {
		ImageFloat32 inten = new ImageFloat32(30, 40);

		QueueCorner naiveMin = new QueueCorner(inten.getWidth() * inten.getHeight());
		QueueCorner naiveMax = new QueueCorner(inten.getWidth() * inten.getHeight());

		for (int useSubImage = 0; useSubImage <= 1; useSubImage++) {
			// make sure it handles sub images correctly
			if (useSubImage == 1) {
				ImageFloat32 larger = new ImageFloat32(inten.width + 10, inten.height + 8);
				inten = larger.subimage(5, 5, inten.width + 5, inten.height + 5, null);
			}

			for (int nonMaxWidth = 3; nonMaxWidth <= 9; nonMaxWidth += 2) {
				int radius = nonMaxWidth / 2;
				NonMaxExtractorNaive reg = new NonMaxExtractorNaive(strict);
				reg.setSearchRadius(radius);
				reg.setThreshold(0.6f);

				for (int i = 0; i < 10; i++) {
					ImageMiscOps.fillGaussian(inten, rand, 0, 3, -100, 100);


					// detect the corners
					findLocalPeaks(inten, 0.6f, radius, 0);
					naiveMin.reset();naiveMax.reset();
					reg.process(inten, naiveMax);
					PixelMath.invert(inten, inten);
					reg.process(inten, naiveMin);

					// check the number of corners
					if( canDetectMin ) {
						assertTrue(foundMinimum.size() > 0);
						assertEquals(naiveMin.size(), foundMinimum.size());
						checkSamePoints(naiveMin,foundMinimum);
					}
					if( canDetectMax ) {
						assertTrue(foundMaximum.size() > 0);
						assertEquals(naiveMax.size(), foundMaximum.size());
						checkSamePoints(naiveMax,foundMaximum);
					}
				}
			}
		}
	}

	private void checkSamePoints( QueueCorner list0 , QueueCorner list1 ) {
		for (int j = 0; j < list0.size(); j++) {
			Point2D_I16 b = list0.get(j);

			boolean foundMatch = false;
			for (int k = 0; k < list1.size(); k++) {
				Point2D_I16 a = list1.get(k);

				if (a.x == b.x && a.y == b.y) {
					foundMatch = true;
					break;
				}
			}

			assertTrue(foundMatch);
		}
	}
}
