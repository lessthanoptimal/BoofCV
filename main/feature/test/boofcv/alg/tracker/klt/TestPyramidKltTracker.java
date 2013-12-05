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

import static org.junit.Assert.*;

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
		assertTrue(tracker.setDescription(feature));

		// all the layers should have been set
		for( int i = 0; i < pyramid.getNumLayers(); i++ ) {
			assertTrue( feature.desc[i].Gxx != 0 );
		}
	}

	/**
	 * Test set description when a feature partially inside and outside of the image at all levels
	 */
	@Test
	public void setDescription_border() {
		// now tell it to set a description near the edge
		// only the first layer should be set
		PyramidKltFeature feature = new PyramidKltFeature(pyramid.getNumLayers(),featureReadius);
		feature.setPosition(featureReadius-1,featureReadius-1);
		tracker.setImage(pyramid,derivX,derivY);
		assertTrue(tracker.setDescription(feature));
		for( int i = 0; i < pyramid.getNumLayers(); i++ ) {
			assertTrue( feature.desc[i].x != 0 );
			assertTrue( feature.desc[i].y != 0 );
			assertTrue( feature.desc[i].Gxx != 0.0f );
		}
	}

	/**
	 * Test set description when a feature is completely outside the image
	 */
	@Test
	public void setDescription_outside() {
		// now tell it to set a description near the edge
		// only the first layer should be set
		PyramidKltFeature feature = new PyramidKltFeature(pyramid.getNumLayers(),featureReadius);
		feature.setPosition(-featureReadius-1,-featureReadius-1);
		tracker.setImage(pyramid,derivX,derivY);
		assertFalse(tracker.setDescription(feature));
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
	 *
	 */
	@Test
	public void track_border() {
		float targetX = width-featureReadius;
		float targetY = height-featureReadius-3;

		// set the feature right on the corner
		PyramidKltFeature feature = new PyramidKltFeature(pyramid.getNumLayers(),featureReadius);
		feature.setPosition(targetX,targetY);
		tracker.setImage(pyramid,derivX,derivY);
		tracker.setDescription(feature);

		// start it outside the image, but still near its true position
		feature.setPosition(width-featureReadius+2,height-featureReadius-1);
		assertTrue( tracker.track(feature) == KltTrackFault.SUCCESS);

		assertEquals(targetX,feature.x,0.2);
		assertEquals(targetY,feature.y,0.2);
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
		feature.setPosition(-20,-20);

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
