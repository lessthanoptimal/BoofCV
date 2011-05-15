/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.tracker.pklt;

import gecv.alg.drawing.impl.ImageInitialization_F32;
import gecv.alg.filter.convolve.KernelFactory;
import gecv.alg.filter.derivative.GradientSobel;
import gecv.alg.pyramid.ConvolutionPyramid_F32;
import gecv.alg.pyramid.PyramidUpdater;
import gecv.alg.tracker.klt.KltTrackFault;
import gecv.alg.tracker.klt.KltTracker;
import gecv.alg.tracker.klt.TestKltTracker;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.pyramid.ImagePyramid;
import gecv.struct.pyramid.ImagePyramid_F32;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestPyramidKltTracker {

	Random rand = new Random(234);

	int width = 50;
	int height = 60;

	int featureReadius = 2;

	ImageFloat32 image = new ImageFloat32(width,height);
	PyramidUpdater<ImageFloat32> updater = createPyramidUpdater();
	ImagePyramid<ImageFloat32> pyramid = createPyramid();
	ImageFloat32[] derivX;
	ImageFloat32[] derivY;
	PyramidKltTracker<ImageFloat32,ImageFloat32> tracker = createDefaultTracker();

	int cornerX = 20;
	int cornerY = 22;

	@Before
	public void setup() {
		ImageInitialization_F32.randomize(image,rand,0,1);
		ImageInitialization_F32.fillRectangle(image,100,cornerX,cornerY,20,20);
		updater.setPyramid(pyramid);
		updater.update(image);

		derivX = new ImageFloat32[pyramid.getNumLayers()];
		derivY = new ImageFloat32[pyramid.getNumLayers()];
		for( int i = 0; i < derivX.length; i++ ) {
			int w = pyramid.getWidth(i);
			int h = pyramid.getHeight(i);
			derivX[i] = new ImageFloat32(w,h);
			derivY[i] = new ImageFloat32(w,h);

			GradientSobel.process(pyramid.getLayer(i),derivX[i],derivY[i],true);
		}
	}

	private void setTargetLocation( int x , int y ) {
		ImageInitialization_F32.randomize(image,rand,0,1);
		ImageInitialization_F32.fillRectangle(image,100,cornerX,cornerY,20,20);
		updater.update(image);

		for( int i = 0; i < derivX.length; i++ ) {
			GradientSobel.process(pyramid.getLayer(i),derivX[i],derivY[i],true);
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

		// see if it updated the tracks description to allow more layers
		assertTrue(feature.desc[2].Gxx != 0);
	}

	/**
	 * The feature being tracked will have a fault.
	 */
	@Test
	public void track_fault() {
		fail("implement tests");
	}

	private ImagePyramid<ImageFloat32> createPyramid() {

		ImagePyramid<ImageFloat32> pyramid = new ImagePyramid_F32(width,height,false);
		pyramid.setScaling(1,2,2);

		return pyramid;
	}

	private PyramidUpdater<ImageFloat32> createPyramidUpdater() {

		Kernel1D_F32 kernel = KernelFactory.gaussian1D_F32(2,true);
		return new ConvolutionPyramid_F32(kernel);
	}

	private PyramidKltTracker<ImageFloat32,ImageFloat32> createDefaultTracker() {
		KltTracker<ImageFloat32, ImageFloat32> klt = TestKltTracker.createDefaultTracker();

		return new PyramidKltTracker<ImageFloat32,ImageFloat32>(klt);
	}
}
