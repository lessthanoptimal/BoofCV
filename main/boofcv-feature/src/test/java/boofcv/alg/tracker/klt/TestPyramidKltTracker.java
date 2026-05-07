/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.BorderIndex1D_Extend;
import boofcv.struct.border.ImageBorder1D_F32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestPyramidKltTracker extends PyramidKltTestBase {

	@BeforeEach
	@Override
	public void setup() {
		super.setup();
	}

	private void setTargetLocation( int x, int y ) {
		ImageMiscOps.fillUniform(image, rand, 0, 1);
		ImageMiscOps.fillRectangle(image, 100, x, y, 20, 20);
		pyramid.process(image);

		for (int i = 0; i < pyramid.getNumLayers(); i++) {
			GradientSobel.process(pyramid.getLayer(i), derivX[i], derivY[i], new ImageBorder1D_F32(BorderIndex1D_Extend::new));
		}
	}

	/// Test set description when the image is fully inside the image for all the pyramid layers
	@Test void setDescription() {
		// tell it to generate a feature inside directly on a pixel
		var feature = new PyramidKltFeature(pyramid.getNumLayers(), featureRadius);
		feature.setPosition(25, 20);
		tracker.setImage(pyramid, derivX, derivY, 1);
		assertTrue(tracker.setDescription(feature));

		// all the layers should have been set
		for (int i = 0; i < pyramid.getNumLayers(); i++) {
			assertTrue(feature.desc[i].Gxx != 0);
		}
	}

	/// Test set description when a feature partially inside and outside of the image at all levels
	@Test void setDescription_border() {
		// now tell it to set a description near the edge
		// only the first layer should be set
		var feature = new PyramidKltFeature(pyramid.getNumLayers(), featureRadius);
		feature.setPosition(featureRadius - 1, featureRadius - 1);
		tracker.setImage(pyramid, derivX, derivY, 1);
		assertTrue(tracker.setDescription(feature));
		for (int i = 0; i < pyramid.getNumLayers(); i++) {
			assertTrue(feature.desc[i].x != 0);
			assertTrue(feature.desc[i].y != 0);
			assertTrue(feature.desc[i].Gxx != 0.0f);
		}
	}

	/// Test set description when a feature is completely outside the image
	@Test void setDescription_outside() {
		// now tell it to set a description near the edge
		// only the first layer should be set
		var feature = new PyramidKltFeature(pyramid.getNumLayers(), featureRadius);
		feature.setPosition(-featureRadius - 1, -featureRadius - 1);
		tracker.setImage(pyramid, derivX, derivY, 1);
		assertFalse(tracker.setDescription(feature));
	}

	/// Test positive examples of tracking when there should be no fault at any point.
	/// Only a small offset easily done with a single layer tracker
	@Test void track_smallOffset() {
		// set the feature right on the corner
		var feature = new PyramidKltFeature(pyramid.getNumLayers(), featureRadius);
		feature.setPosition(cornerX, cornerY);
		tracker.setImage(pyramid, derivX, derivY, 1);
		tracker.setDescription(feature);

		// now move the corner away from the feature
		feature.setPosition(cornerX - 1.3f, cornerY + 1.2f);

		// see if it moves back
		assertSame(KltTrackFault.SUCCESS, tracker.track(feature));

		assertEquals(cornerX, feature.x, 0.2);
		assertEquals(cornerY, feature.y, 0.2);
	}

	/// Test positive examples of tracking when there should be no fault at any point.
	/// Larger offset which will require the pyramid approach
	@Test void track_largeOffset() {
		// set the feature right on the corner
		var feature = new PyramidKltFeature(pyramid.getNumLayers(), featureRadius);
		feature.setPosition(cornerX, cornerY);
		tracker.setImage(pyramid, derivX, derivY, 1);
		tracker.setDescription(feature);

		// now move the corner away from the feature
		feature.setPosition(cornerX - 5.4f, cornerY + 5.3f);

		// see if it moves back
		assertSame(KltTrackFault.SUCCESS, tracker.track(feature));

		assertEquals(cornerX, feature.x, 0.2);
		assertEquals(cornerY, feature.y, 0.2);
	}

	///
	@Test void track_border() {
		float targetX = width - featureRadius;
		float targetY = height - featureRadius - 3;

		// set the feature right on the corner
		var feature = new PyramidKltFeature(pyramid.getNumLayers(), featureRadius);
		feature.setPosition(targetX, targetY);
		tracker.setImage(pyramid, derivX, derivY, 1);
		tracker.setDescription(feature);

		// start it outside the image, but still near its true position
		feature.setPosition(width - featureRadius + 2, height - featureRadius - 1);
		assertSame(KltTrackFault.SUCCESS, tracker.track(feature));

		assertEquals(targetX, feature.x, 0.2);
		assertEquals(targetY, feature.y, 0.2);
	}

	/// See if a track out of bounds error is returned
	@Test void track_OOB() {
		setTargetLocation(5*4 + 1, 22);

		// set the feature right on the corner
		var feature = new PyramidKltFeature(pyramid.getNumLayers(), 4);
		feature.setPosition(21, 22);
		tracker.setImage(pyramid, derivX, derivY, 1);
		tracker.setDescription(feature);

		// put the feature out of bounds
		feature.setPosition(-20, -20);

		assertSame(KltTrackFault.OUT_OF_BOUNDS, tracker.track(feature));
	}

	/// See if a track out of bounds error is returned
	@Test void track_LargeError() {
		setTargetLocation(5*4 + 1, 22);

		// set the feature right on the corner
		var feature = new PyramidKltFeature(pyramid.getNumLayers(), 4);
		feature.setPosition(21, 22);
		tracker.setImage(pyramid, derivX, derivY, 1);
		tracker.setDescription(feature);

		// mess up the description so that it will produce a large error
		feature.desc[0].desc.set(0, 0, 1000);

		assertSame(KltTrackFault.LARGE_ERROR, tracker.track(feature));
	}
}
