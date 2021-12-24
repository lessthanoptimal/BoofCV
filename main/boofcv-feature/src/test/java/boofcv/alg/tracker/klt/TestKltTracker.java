/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.tracker.klt;

import boofcv.BoofTesting;
import boofcv.alg.filter.derivative.GradientSobel;
import boofcv.alg.interpolate.InterpolateRectangle;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.BorderIndex1D_Extend;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.border.ImageBorder1D_F32;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestKltTracker extends BoofStandardJUnit {

	int imageWidth = 40;
	int imageHeight = 50;

	GrayF32 image = new GrayF32(imageWidth, imageHeight);
	GrayF32 derivX = new GrayF32(imageWidth, imageHeight);
	GrayF32 derivY = new GrayF32(imageWidth, imageHeight);

	/**
	 * Process the same features in two different sets of image. only difference is that one is a sub image
	 * results should be identical
	 */
	@Test void testSubImages() {
		ImageMiscOps.fillUniform(image, rand, 0, 100);
		GradientSobel.process(image, derivX, derivY, new ImageBorder1D_F32(BorderIndex1D_Extend::new));

		KltTracker<GrayF32, GrayF32> trackerA = createDefaultTracker();
		trackerA.setImage(image, derivX, derivY);

		KltTracker<GrayF32, GrayF32> trackerB = createDefaultTracker();
		GrayF32 image = BoofTesting.createSubImageOf(this.image);
		GrayF32 derivX = BoofTesting.createSubImageOf(this.derivX);
		GrayF32 derivY = BoofTesting.createSubImageOf(this.derivY);
		trackerB.setImage(image, derivX, derivY);

		for (int y = 0; y < imageHeight; y += 4) {
			for (int x = 0; x < imageWidth; x += 4) {
				KltFeature featureA = new KltFeature(3);
				KltFeature featureB = new KltFeature(3);

				featureA.setPosition(x, y);
				featureB.setPosition(x, y);

				trackerA.setDescription(featureA);
				trackerB.setDescription(featureB);

				float dx = rand.nextFloat()*2 - 1;
				float dy = rand.nextFloat()*2 - 1;

				featureA.setPosition(x + dx, y + dy);
				featureB.setPosition(x + dx, y + dy);

				KltTrackFault faultA = trackerA.track(featureA);
				KltTrackFault faultB = trackerB.track(featureB);

				assertSame(faultA, faultB);

				if (faultA == KltTrackFault.SUCCESS) {
					assertEquals(featureB.x, featureA.x);
					assertEquals(featureB.y, featureA.y);
				}
			}
		}
	}

	/**
	 * Create a description of a feature next to the border then place the feature just outside of the image
	 * and see if it can track to its original position.
	 */
	@Test void testTracking_border1() {

		ImageMiscOps.fillUniform(image, rand, 0, 100);
		GradientSobel.process(image, derivX, derivY, new ImageBorder1D_F32(BorderIndex1D_Extend::new));

		KltTracker<GrayF32, GrayF32> tracker = createDefaultTracker();
		tracker.setImage(image, derivX, derivY);
		int r = 4;
		KltFeature feature = new KltFeature(r);

		// lower right border, but fully inside the image
		feature.setPosition(imageWidth - r - 1, imageHeight - r - 1);
		tracker.setDescription(feature);
		// put it partially outside the image
		feature.setPosition(imageWidth - r + 0.1f, imageHeight - r + 0.5f);

		// see if it got sucked back
		assertSame(KltTrackFault.SUCCESS, tracker.track(feature));
		assertEquals(imageWidth - r - 1, feature.x, 0.01);
		assertEquals(imageHeight - r - 1, feature.y, 0.01);

		// same thing but with the top left image
		feature.setPosition(r, r);
		tracker.setDescription(feature);
		// put it partially outside the image
		feature.setPosition(r - 0.5f, r - 1.1f);

		// see if it got sucked back
		assertSame(KltTrackFault.SUCCESS, tracker.track(feature));
		assertEquals(r, feature.x, 0.01);
		assertEquals(r, feature.y, 0.01);
	}

	/**
	 * Place a feature on the border then put it inside the image. See if it moves towards the border
	 */
	@Test void testTracking_border2() {
		ImageMiscOps.fillUniform(image, rand, 1, 100);
		GradientSobel.process(image, derivX, derivY, new ImageBorder1D_F32(BorderIndex1D_Extend::new));

		KltTracker<GrayF32, GrayF32> tracker = createDefaultTracker();
		tracker.setImage(image, derivX, derivY);

		int r = 3;
		KltFeature feature = new KltFeature(r);

		// just outside the image
		feature.setPosition(imageWidth - r - 1 + 1, imageHeight - r - 1 + 1);
		tracker.setDescription(feature);
		// now fully inside the image
		feature.setPosition(imageWidth - r - 1, imageHeight - r - 1);

		// see if it got sucked back
		assertSame(KltTrackFault.SUCCESS, tracker.track(feature));
		assertEquals(imageWidth - r - 1 + 1, feature.x, 0.01);
		assertEquals(imageHeight - r - 1 + 1, feature.y, 0.01);

		// same thing but with the top left image
		feature.setPosition(r - 1, 1);
		tracker.setDescription(feature);
		// put it fully inside the image
		feature.setPosition(r, r);

		// see if it got sucked back
		assertSame(KltTrackFault.SUCCESS, tracker.track(feature));
		assertEquals(r - 1, feature.x, 0.01);
		assertEquals(1, feature.y, 0.01);
	}

	/**
	 * Set description should fail if a feature is entirely outside the image
	 */
	@Test void setDescription_outsideFail() {
		KltTracker<GrayF32, GrayF32> tracker = createDefaultTracker();
		tracker.setImage(image, derivX, derivY);
		KltFeature feature = new KltFeature(3);
		feature.setPosition(-100, 200);

		assertFalse(tracker.setDescription(feature));
	}

	/**
	 * Compares the border algorithm to the inner algorithm
	 */
	@Test void setDescription_compare() {

		ImageMiscOps.fillUniform(image, rand, 0, 100);
		GradientSobel.process(image, derivX, derivY, new ImageBorder1D_F32(BorderIndex1D_Extend::new));

		KltTracker<GrayF32, GrayF32> tracker = createDefaultTracker();
		tracker.setImage(image, derivX, derivY);

		KltFeature featureA = new KltFeature(3);
		KltFeature featureB = new KltFeature(3);

		featureA.setPosition(20.6f, 25.1f);
		featureB.setPosition(20.6f, 25.1f);

		tracker.setAllowedBounds(featureA);
		tracker.internalSetDescription(featureA, derivX, derivY);
		tracker.internalSetDescriptionBorder(featureB, derivX, derivY);

		for (int i = 0; i < featureA.desc.data.length; i++) {
			assertEquals(featureB.desc.data[i], featureA.desc.data[i]);
			assertEquals(featureB.derivX.data[i], featureA.derivX.data[i]);
			assertEquals(featureB.derivY.data[i], featureA.derivY.data[i]);
		}
	}

	/**
	 * When placed outside the image pixels should be NaN
	 */
	@Test void setDescription_borderNaN() {
		KltTracker<GrayF32, GrayF32> tracker = createDefaultTracker();
		tracker.setImage(image, derivX, derivY);

		KltFeature feature = new KltFeature(3);
		feature.setPosition(2, 1);
		tracker.setDescription(feature);

		int numNaN = 0;
		for (int i = 0; i < feature.desc.data.length; i++) {
			if (Float.isNaN(feature.desc.data[i])) {
				numNaN++;
			}
		}

		assertEquals(19, numNaN);
	}

	/**
	 * Pass in a feature with a small determinant and see if it returns a fault.
	 */
	@Test void detectBadFeature() {
		KltTracker<GrayF32, GrayF32> tracker = createDefaultTracker();
		tracker.setImage(image, derivX, derivY);
		KltFeature feature = new KltFeature(2);

		// put a feature right on the corner
		feature.setPosition(20, 20);
		// Gxx, Gyy, and Gxy will all be zero, which is bad

		// update the feature's position
		tracker.setImage(image, derivX, derivY);
		assertNotSame(tracker.track(feature), KltTrackFault.SUCCESS);
	}

	@Test void compare_computeGandE_border_toInsideImage() {
		ImageMiscOps.fillUniform(image, rand, 0, 100);
		GradientSobel.process(image, derivX, derivY, new ImageBorder1D_F32(BorderIndex1D_Extend::new));

		KltTracker<GrayF32, GrayF32> tracker = createDefaultTracker();
		tracker.setImage(image, derivX, derivY);
		KltFeature feature = new KltFeature(2);

		feature.setPosition(20, 22);
		tracker.setDescription(feature);
		tracker.currDesc.reshape(5, 5);
		// need to compute E from a shifted location or else it will be zero
		tracker.computeE(feature, 21, 23);

		float expectedGxx = feature.Gxx;
		float expectedGxy = feature.Gxy;
		float expectedGyy = feature.Gyy;
		float expectedEx = tracker.Ex;
		float expectedEy = tracker.Ey;

		assertTrue(0 != expectedGxx);
		assertTrue(0 != expectedEy);

		assertEquals(25, tracker.computeGandE_border(feature, 20, 22));

		assertEquals(expectedGxx, tracker.Gxx, 1e-8);
		assertEquals(expectedGxy, tracker.Gxy, 1e-8);
		assertEquals(expectedGyy, tracker.Gyy, 1e-8);

		assertEquals(25, tracker.computeGandE_border(feature, 21, 23));
		assertEquals(expectedEx, tracker.Ex, 1e-8);
		assertEquals(expectedEy, tracker.Ey, 1e-8);
	}

	@Test void isDescriptionComplete() {
		KltTracker<GrayF32, GrayF32> tracker = createDefaultTracker();

		KltFeature f = new KltFeature(2);
		tracker.lengthFeature = 25;

		assertTrue(tracker.isDescriptionComplete(f));

		for (int i = 0; i < f.desc.data.length; i++) {
			f.desc.data[i] = Float.NaN;
			assertFalse(tracker.isDescriptionComplete(f));
			f.desc.data[i] = 0;
			assertTrue(tracker.isDescriptionComplete(f));
		}
	}

	@Test void isFullyInside() {
		KltTracker<GrayF32, GrayF32> tracker = createDefaultTracker();

		KltFeature f = new KltFeature(2);

		tracker.image = new GrayF32(imageWidth, imageHeight);
		tracker.setAllowedBounds(f);

		assertTrue(tracker.isFullyInside(imageWidth/2, imageHeight/2));
		assertTrue(tracker.isFullyInside(2, 2));
		assertTrue(tracker.isFullyInside(imageWidth - 3, imageHeight - 3));

		// check border cases
		assertFalse(tracker.isFullyInside(1.99f, imageHeight/2));
		assertFalse(tracker.isFullyInside(imageWidth/2f, 1.99f));
		assertFalse(tracker.isFullyInside(imageWidth - 2.99f, imageHeight - 3));
		assertFalse(tracker.isFullyInside(imageWidth - 3, imageHeight - 2.99f));

		// negative numbers
		assertFalse(tracker.isFullyInside(-imageWidth/2, imageHeight/2));
		assertFalse(tracker.isFullyInside(imageWidth/2, -imageHeight/2));
	}

	@Test void isFullyOutside() {
		KltTracker<GrayF32, GrayF32> tracker = createDefaultTracker();

		int r = 2;
		KltFeature f = new KltFeature(r);

		tracker.image = new GrayF32(imageWidth, imageHeight);
		tracker.setAllowedBounds(f);

		assertFalse(tracker.isFullyOutside(-r, -r));
		assertFalse(tracker.isFullyOutside(imageWidth/2, imageHeight/2));
		assertFalse(tracker.isFullyOutside(imageWidth + r - 1, imageHeight + r - 1));

		assertTrue(tracker.isFullyOutside(imageWidth/2, 1000));
		assertTrue(tracker.isFullyOutside(1000, imageHeight/2));
		assertTrue(tracker.isFullyOutside(-r - 0.001f, -r));
		assertTrue(tracker.isFullyOutside(-r, -r - 0.001f));
		assertTrue(tracker.isFullyOutside(imageWidth + r - 0.999f, imageHeight + r - 1));
		assertTrue(tracker.isFullyOutside(imageWidth + r - 1, imageHeight + r - 0.999f));
	}

	public static KltTracker<GrayF32, GrayF32> createDefaultTracker() {
		ConfigKlt config = new ConfigKlt();
		config.maxPerPixelError = 10;
		config.maxIterations = 30;
		config.minDeterminant = 0.01f;
		config.minPositionDelta = 0.001f;

		InterpolateRectangle<GrayF32> interp1 = FactoryInterpolation.bilinearRectangle(GrayF32.class);
		InterpolateRectangle<GrayF32> interp2 = FactoryInterpolation.bilinearRectangle(GrayF32.class);

		return new KltTracker<>(interp1, interp2, config);
	}
}
