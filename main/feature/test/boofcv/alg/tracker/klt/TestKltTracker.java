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

package boofcv.alg.tracker.klt;

import boofcv.alg.filter.derivative.GradientSobel;
import boofcv.alg.interpolate.InterpolateRectangle;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.BorderIndex1D_Extend;
import boofcv.core.image.border.ImageBorder1D_F32;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageFloat32;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestKltTracker {

	int imageWidth = 40;
	int imageHeight = 50;

	ImageFloat32 image;
	ImageFloat32 derivX;
	ImageFloat32 derivY;

	/**
	 * Create an artificial image with a corner and create a feature over the corner.  Then move the corner and
	 * see if the KLT tracker can accurate track that feature.
	 */
	@Test
	public void testCornerTracking() {

		checkMovementSub(3, 0, 0);
		checkMovementSub(3, 1, 0);
		checkMovementSub(3, -1, 0);
		checkMovementSub(3, 0, 1);
		checkMovementSub(3, 0, -1);
		checkMovementSub(3, 1, 1);
		checkMovementSub(3, -1, -1);
		checkMovementSub(3, 2, 2);
		checkMovementSub(3, -2, -2);
		checkMovementSub(3, 2, -2);
		checkMovementSub(3, -2, 2);

		checkMovementSub(2, -1, 1);
	}

	/**
	 * Check its ability to estimate the feature's motion for regular and sub-images
	 */
	private void checkMovementSub(int radius, int deltaX, int deltaY) {
		image = new ImageFloat32(imageWidth, imageHeight);
		derivX = new ImageFloat32(imageWidth, imageHeight);
		derivY = new ImageFloat32(imageWidth, imageHeight);

		checkMovement(radius, deltaX, deltaY);

		image = BoofTesting.createSubImageOf(image);
		derivX = BoofTesting.createSubImageOf(derivX);
		derivY = BoofTesting.createSubImageOf(derivY);

		checkMovement(radius, deltaX, deltaY);
	}

	/**
	 * Estimate the feature's motion
	 *
	 * @param radius feature's radius
	 * @param deltaX motion in x direction
	 * @param deltaY motion in y direction
	 */
	private void checkMovement(int radius, int deltaX, int deltaY) {
		ImageMiscOps.fill(image, 0);
		ImageMiscOps.fillRectangle(image, 100, 20, 20, imageWidth-20, imageHeight-20);
		GradientSobel.process(image, derivX, derivY, new ImageBorder1D_F32(BorderIndex1D_Extend.class));

		KltTracker<ImageFloat32, ImageFloat32> tracker = createDefaultTracker();
		tracker.setImage(image, derivX, derivY);
		KltFeature feature = new KltFeature(radius);

		// put a feature right on the corner
		feature.setPosition(20, 20);
		tracker.setDescription(feature);

		// move the rectangle a bit
		ImageMiscOps.fill(image, 0);
		ImageMiscOps.fillRectangle(image, 100, 20 + deltaX, 20 + deltaY, imageWidth, imageHeight);
		GradientSobel.process(image, derivX, derivY, new ImageBorder1D_F32(BorderIndex1D_Extend.class));

		// update the feature's position
		tracker.setImage(image, derivX, derivY);
		assertTrue(tracker.track(feature) == KltTrackFault.SUCCESS);

		// see if it moved with the corner
		assertEquals(20 + deltaX, feature.x, 0.1f);
		assertEquals(20 + deltaY, feature.y, 0.1f);
	}

	/**
	 * Make sure it uses the
	 */
	@Test
	public void testBorder() {
		image = new ImageFloat32(imageWidth, imageHeight);
		derivX = new ImageFloat32(imageWidth, imageHeight);
		derivY = new ImageFloat32(imageWidth, imageHeight);

		KltTracker<ImageFloat32, ImageFloat32> tracker = createDefaultTracker();
		tracker.setImage(image, derivX, derivY);
		KltFeature feature = new KltFeature(2);

		// put a feature right on the corner
		feature.setPosition(imageWidth/2, imageHeight/2);
		tracker.setDescription(feature);

		// this should make the feature be out of bounds
		tracker.getConfig().forbiddenBorder=10000;

		// update the feature's position
		tracker.setImage(image, derivX, derivY);
		assertTrue(tracker.track(feature) == KltTrackFault.OUT_OF_BOUNDS);
	}

	/**
	 * Passes in a feature which is out of bands and sees if a fault happens.
	 */
	@Test
	public void handleOutOfBounds() {
		image = new ImageFloat32(imageWidth, imageHeight);
		derivX = new ImageFloat32(imageWidth, imageHeight);
		derivY = new ImageFloat32(imageWidth, imageHeight);

		KltTracker<ImageFloat32, ImageFloat32> tracker = createDefaultTracker();
		tracker.setImage(image, derivX, derivY);
		KltFeature feature = new KltFeature(2);

		// put a feature right on the corner
		feature.setPosition(imageWidth/2, imageHeight/2);
		tracker.setDescription(feature);
		feature.setPosition(imageWidth, imageHeight);

		// update the feature's position
		tracker.setImage(image, derivX, derivY);
		assertTrue(tracker.track(feature) == KltTrackFault.OUT_OF_BOUNDS);
	}

	/**
	 * Pass in a feature with a small determinant and see if it returns a fault.
	 */
	@Test
	public void detectBadFeature() {
		image = new ImageFloat32(imageWidth, imageHeight);
		derivX = new ImageFloat32(imageWidth, imageHeight);
		derivY = new ImageFloat32(imageWidth, imageHeight);
		KltTracker<ImageFloat32, ImageFloat32> tracker = createDefaultTracker();
		tracker.setImage(image, derivX, derivY);
		KltFeature feature = new KltFeature(2);

		// put a feature right on the corner
		feature.setPosition(20, 20);
		// Gxx, Gyy, and Gxy will all be zero, which is bad

		// update the feature's position
		tracker.setImage(image, derivX, derivY);
		assertTrue(tracker.track(feature) != KltTrackFault.SUCCESS);
	}

	public static KltTracker<ImageFloat32, ImageFloat32> createDefaultTracker() {
		KltConfig config = new KltConfig();
		config.forbiddenBorder = 1;
		config.maxPerPixelError = 10;
		config.maxIterations = 30;
		config.minDeterminant = 0.01f;
		config.minPositionDelta = 0.01f;

		InterpolateRectangle<ImageFloat32> interp = FactoryInterpolation.bilinearRectangle(ImageFloat32.class);

		return new KltTracker<ImageFloat32, ImageFloat32>(interp, interp, config);
	}
}
