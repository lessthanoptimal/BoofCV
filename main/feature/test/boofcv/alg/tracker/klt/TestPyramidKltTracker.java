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
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.BorderIndex1D_Extend;
import boofcv.core.image.border.ImageBorder1D_F32;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestPyramidKltTracker extends PyramidKltTestBase {

	@Before
	public void setup() {
		super.setup();
	}

	private void setTargetLocation( int x , int y ) {
		ImageMiscOps.fillUniform(image,rand,0,1);
		ImageMiscOps.fillRectangle(image,100,cornerX,cornerY,20,20);
		pyramid.process(image);

		for( int i = 0; i < pyramid.getNumLayers(); i++ ) {
			GradientSobel.process(pyramid.getLayer(i),derivX[i],derivY[i],new ImageBorder1D_F32(BorderIndex1D_Extend.class));
		}
	}

	/**
	 * Test set description when the image is fully inside the image for all the pyramid layers
	 */
	@Test
	public void setDescription() {
		// tell it to generate a feature inside directly on a pixel
		PyramidKltFeature feature = new PyramidKltFeature(pyramid.getNumLayers(),featureReadius);
		feature.setPosition(25,20);
		tracker.setImage(pyramid,derivX,derivY);
		tracker.setDescription(feature);

		// all the layers should have been set
		for( int i = 0; i < pyramid.getNumLayers(); i++ ) {
			assertTrue( feature.desc[i].Gxx != 0 );
		}
	}

	/**
	 * Test set description when a feature is inside the allowed region for only part of the pyramid.
	 */
	@Test
	public void setDescription_partial() {
		// now tell it to set a description near the edge
		// only the first layer should be set
		PyramidKltFeature feature = new PyramidKltFeature(pyramid.getNumLayers(),featureReadius);
		feature.setPosition(featureReadius,featureReadius);
		tracker.setImage(pyramid,derivX,derivY);
		tracker.setDescription(feature);
		assertTrue( feature.desc[0].x != 0 );
		for( int i = 1; i < pyramid.getNumLayers(); i++ ) {
			assertTrue( feature.desc[i].Gxx == 0.0f );
		}
	}

	/**
	 * Test positive examples of tracking when there should be no fault at any point.
	 *
	 * Only a small offset easily done with a single layer tracker
	 */
	@Test
	public void track_smallOffset() {
		// set the feature right on the corner
		PyramidKltFeature feature = new PyramidKltFeature(pyramid.getNumLayers(),featureReadius);
		feature.setPosition(cornerX,cornerY);
		tracker.setImage(pyramid,derivX,derivY);
		tracker.setDescription(feature);

		// now move the corner away from the feature
		feature.setPosition(cornerX-1.3f,cornerY+1.2f);

		// see if it moves back
		assertTrue( tracker.track(feature) == KltTrackFault.SUCCESS);

		assertEquals(cornerX,feature.x,0.2);
		assertEquals(cornerY,feature.y,0.2);
	}

	/**
	 * Test positive examples of tracking when there should be no fault at any point.
	 *
	 * Larger offset which will require the pyramid approach
	 */
	@Test
	public void track_largeOffset() {
		// set the feature right on the corner
		PyramidKltFeature feature = new PyramidKltFeature(pyramid.getNumLayers(),featureReadius);
		feature.setPosition(cornerX,cornerY);
		tracker.setImage(pyramid,derivX,derivY);
		tracker.setDescription(feature);

		// now move the corner away from the feature
		feature.setPosition(cornerX-5.4f,cornerY+5.3f);

		// see if it moves back
		assertTrue( tracker.track(feature) == KltTrackFault.SUCCESS);

		assertEquals(cornerX,feature.x,0.2);
		assertEquals(cornerY,feature.y,0.2);
	}

	/**
	 * Test tracking when a feature is out of bounds in the middle of the pyramid
	 */
	@Test
	public void track_outside_middle() {
		setTargetLocation(5*4+1,22);

		// set the feature right on the corner
		PyramidKltFeature feature = new PyramidKltFeature(pyramid.getNumLayers(),4);
		feature.setPosition(21,22);
		tracker.setImage(pyramid,derivX,derivY);
		tracker.setDescription(feature);

		// move it towards the image border so that it won't be on the pyramids last layer
		feature.setPosition(19,22);
		feature.desc[2].Gxx = feature.desc[2].Gyy = feature.desc[2].Gxy = 0;

		// see if it tracked the target
		assertTrue( tracker.track(feature) == KltTrackFault.SUCCESS);

		assertEquals(21,feature.x,0.2);
		assertEquals(22,feature.y,0.2);

		// outside layers should not be updated automatically
		assertTrue(feature.desc[2].Gxx == 0);
	}

	/**
	 * See if a track out of bounds error is returned
	 */
	@Test
	public void track_OOB() {
		setTargetLocation(5*4+1,22);

		// set the feature right on the corner
		PyramidKltFeature feature = new PyramidKltFeature(pyramid.getNumLayers(),4);
		feature.setPosition(21,22);
		tracker.setImage(pyramid,derivX,derivY);
		tracker.setDescription(feature);

		// put the feature out of bounds
		feature.setPosition(5,0);

		assertTrue( tracker.track(feature) == KltTrackFault.OUT_OF_BOUNDS);
	}

	/**
	 * See if a track out of bounds error is returned
	 */
	@Test
	public void track_LargeError() {
		setTargetLocation(5*4+1,22);

		// set the feature right on the corner
		PyramidKltFeature feature = new PyramidKltFeature(pyramid.getNumLayers(),4);
		feature.setPosition(21,22);
		tracker.setImage(pyramid,derivX,derivY);
		tracker.setDescription(feature);

		// mess up the description so that it will produce a large error
		feature.desc[0].desc.set(0,0,1000);

		assertTrue( tracker.track(feature) == KltTrackFault.LARGE_ERROR);
	}
}
