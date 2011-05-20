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

import gecv.abst.detect.corner.GeneralCornerDetector;
import gecv.abst.detect.corner.GeneralCornerIntensity;
import gecv.abst.detect.corner.WrapperGradientCornerIntensity;
import gecv.abst.detect.extract.CornerExtractor;
import gecv.abst.detect.extract.WrapperNonMax;
import gecv.alg.detect.corner.FactoryCornerIntensity;
import gecv.alg.detect.extract.FastNonMaxCornerExtractor;
import gecv.struct.image.ImageFloat32;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
		GeneralCornerIntensity<ImageFloat32,ImageFloat32> intensity =
				new WrapperGradientCornerIntensity<ImageFloat32,ImageFloat32>(
						FactoryCornerIntensity.createKlt_F32(image.width, image.height, 3));

		CornerExtractor extractor = new WrapperNonMax(
				new FastNonMaxCornerExtractor(3, 3, 0.001f));

		GeneralCornerDetector<ImageFloat32,ImageFloat32> detector =
				new GeneralCornerDetector<ImageFloat32,ImageFloat32>(intensity,extractor,maxFeatures);

		return new GenericPkltFeatSelector<ImageFloat32,ImageFloat32>(detector,tracker);
	}
}
