/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.tracker.pklt;

import boofcv.abst.feature.detect.extract.FeatureExtractor;
import boofcv.abst.feature.detect.extract.WrapperNonMax;
import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.abst.feature.detect.intensity.WrapperGradientCornerIntensity;
import boofcv.alg.feature.detect.extract.FastNonMaxExtractor;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.factory.feature.detect.intensity.FactoryPointIntensityAlg;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_I16;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestGenericPkltFeatSelector extends PyramidKltTestBase {

	int maxFeatures = 100;

	@Before
	public void setup() {
		super.setup();
	}

	/**
	 * Sees if it correctly handles the situation where the bottom most layer in the pyramid
	 * is not one.
	 */
	@Test
	public void handlesBottomLayerNotOne() {
		setup(2,4);// override initial scaling

		// set the first layer to not be one
		GeneralFeatureDetector<ImageFloat32,ImageFloat32> detector =
				new GeneralFeatureDetector<ImageFloat32,ImageFloat32>(new DummyIntensity(), new DummyExtractor(),100);
		GenericPkltFeatSelector<ImageFloat32, ImageFloat32> selector =
				new GenericPkltFeatSelector<ImageFloat32,ImageFloat32>(detector,tracker);

		List<PyramidKltFeature> active = new ArrayList<PyramidKltFeature>();
		List<PyramidKltFeature> available = new ArrayList<PyramidKltFeature>();
		for( int i = 0; i < maxFeatures-5; i++ ) {
			available.add( new PyramidKltFeature(pyramid.getNumLayers(),featureReadius));
		}
		for( int i = 0; i < 5; i++ ) {
			active.add( new PyramidKltFeature(pyramid.getNumLayers(),featureReadius));
		}

		// set some features along the border
		active.get(0).setPosition(width-1,height-1);
		active.get(1).setPosition(width-1,2);
		active.get(2).setPosition(2,height-1);

		tracker.setImage(pyramid,derivX,derivY);
		selector.setInputs(pyramid,derivX,derivY);
		selector.compute(active,available);

		// check to see that feature descriptions were removed from the available list
		// and added to the active list
		assertTrue(active.size()>0);
		assertTrue((available.size()+active.size())==maxFeatures);

		// see how many features are in the outside quadrant
		int numOutside = 0;
		for( PyramidKltFeature f : active ) {
			if( f.x > width/2 )
				numOutside++;
			else if( f.y > height/2 )
				numOutside++;
		}

		// the three known before and the one added by the extractor
		assertTrue(numOutside==4);
	}

	/**
	 * See if it detects features, that it correctly removes features from the available list,
	 * and that the description has been modified.
	 */
	@Test
	public void testPositive() {

		GenericPkltFeatSelector<ImageFloat32, ImageFloat32> selector = createSelector();

		List<PyramidKltFeature> active = new ArrayList<PyramidKltFeature>();
		List<PyramidKltFeature> available = new ArrayList<PyramidKltFeature>();
		for( int i = 0; i < maxFeatures; i++ ) {
			available.add( new PyramidKltFeature(pyramid.getNumLayers(),featureReadius));
		}
		tracker.setImage(pyramid,derivX,derivY);
		selector.setInputs(pyramid,derivX,derivY);
		selector.compute(active,available);

		// check to see that feature descriptions were removed from the available list
		// and added to the active list
		assertTrue(active.size()>0);
		assertTrue((available.size()+active.size())==maxFeatures);

		// see if the description has been modified
		// only the bottom layer is checked because the upper ones might not have changed.
		for( PyramidKltFeature f : active ) {
			assertTrue(f.x!=0);
			assertTrue(f.y!=0);
			assertTrue(f.maxLayer>=0);
		}
	}

	/**
	 * Checks to see that previously found features are returned again
	 * and that duplicates are not returned.
	 */
	@Test
	public void excludeAlreadyFound() {
		GenericPkltFeatSelector<ImageFloat32, ImageFloat32> selector = createSelector();

		List<PyramidKltFeature> active = new ArrayList<PyramidKltFeature>();
		List<PyramidKltFeature> available = new ArrayList<PyramidKltFeature>();
		for( int i = 0; i < maxFeatures; i++ ) {
			available.add( new PyramidKltFeature(pyramid.getNumLayers(),featureReadius));
		}
		tracker.setImage(pyramid,derivX,derivY);
		selector.setInputs(pyramid,derivX,derivY);
		selector.compute(active,available);
		int numBefore = active.size();

		// swap the order so it can see if it just flushed the list or not
		PyramidKltFeature a = active.get(4);
		PyramidKltFeature b = active.get(5);
		active.set(5,a);
		active.set(4,b);

		selector.compute(active,available);

		// should find the same number of features
		assertEquals(numBefore,active.size());
		assertTrue(a == active.get(5));
		assertTrue(b == active.get(4));
	}

	private GenericPkltFeatSelector<ImageFloat32, ImageFloat32> createSelector() {
		GeneralFeatureIntensity<ImageFloat32,ImageFloat32> intensity =
				new WrapperGradientCornerIntensity<ImageFloat32,ImageFloat32>(
						FactoryPointIntensityAlg.createKlt( ImageFloat32.class,3));

		FeatureExtractor extractor = new WrapperNonMax(
				new FastNonMaxExtractor(3, 3, 0.001f));

		GeneralFeatureDetector<ImageFloat32,ImageFloat32> detector =
				new GeneralFeatureDetector<ImageFloat32,ImageFloat32>(intensity,extractor,maxFeatures);

		return new GenericPkltFeatSelector<ImageFloat32,ImageFloat32>(detector,tracker);
	}

	private static class DummyIntensity implements GeneralFeatureIntensity<ImageFloat32,ImageFloat32> {
		ImageFloat32 intensity;

		@Override
		public void process(ImageFloat32 image, ImageFloat32 derivX, ImageFloat32 derivY, ImageFloat32 derivXX, ImageFloat32 derivYY, ImageFloat32 derivXY) {
			intensity = new ImageFloat32(image.width,image.height);
		}

		@Override
		public ImageFloat32 getIntensity() {
			return intensity;
		}

		@Override
		public QueueCorner getCandidates() {
			return null;
		}

		@Override
		public boolean getRequiresGradient() {
			return true;
		}

		@Override
		public boolean getRequiresHessian() {
			return false;
		}

		@Override
		public boolean hasCandidates() {
			return false;
		}
	}

	private class DummyExtractor implements FeatureExtractor
	{

		@Override
		public void process(ImageFloat32 intensity, QueueCorner candidate, int requestedNumber, QueueCorner excludeCorners, QueueCorner foundFeature) {
			// if the feature's position isn't scaled then this test will fail
			for( int i = 0; i < excludeCorners.num; i++ ) {
				Point2D_I16 p = excludeCorners.get(i);
				assertTrue(intensity.isInBounds(p.x,p.y));
			}

			// add a corner on the image boundary for scaled down
			foundFeature.add(width/2-2,height/2-2);
		}

		@Override
		public boolean getUsesCandidates() {
			return false;
		}

		@Override
		public boolean getCanExclude() {
			return true;
		}

		@Override
		public boolean getAcceptRequest() {
			return false;
		}

		@Override
		public float getThreshold() {
			return 0;
		}

		@Override
		public void setThreshold(float threshold) {
		}

		@Override
		public void setIgnoreBorder(int border) {
		}
	}
}
