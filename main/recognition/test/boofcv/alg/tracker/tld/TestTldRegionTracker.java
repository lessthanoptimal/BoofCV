/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.tracker.tld;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.tracker.klt.KltConfig;
import boofcv.alg.tracker.klt.PyramidKltTracker;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.tracker.FactoryTrackerAlg;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.pyramid.PyramidDiscrete;
import georegression.struct.shapes.Rectangle2D_F64;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * Basic sanity tests for tracking.  Checks for the object's motion are handled by higher level unit tests.
 *
 * @author Peter Abeles
 */
public class TestTldRegionTracker {

	int width = 120;
	int height = 150;

	Random rand = new Random(234);

	ImageUInt8 input = new ImageUInt8(width,height);
	PyramidDiscrete<ImageUInt8> pyramid;

	public TestTldRegionTracker() {
		GImageMiscOps.fillUniform(input, rand, 0, 200);

		pyramid = FactoryPyramid.discreteGaussian(new int[]{1,2,4},-1,1,true,ImageUInt8.class);
		pyramid.process(input);
	}

	/**
	 * Very basic test.  Feeds it the same image twice and sees if it does nothing without blowing up.
	 */
	@Test
	public void process() {
		TldRegionTracker alg = createAlg();

		Rectangle2D_F64 rect = new Rectangle2D_F64(10,20,115,125);

		alg.initialize(pyramid);
		assertTrue(alg.process(pyramid, rect));
		assertEquals(alg.getPairs().size,10*10);
		assertTrue(alg.process(pyramid, rect));
		assertEquals(alg.getPairs().size,10*10);
	}

	/**
	 * See if the expected number of points are spawned
	 */
	@Test
	public void spawnGrid() {
		TldRegionTracker alg = createAlg();

		alg.initialize(pyramid);
		alg.spawnGrid(new Rectangle2D_F64(10,20,80,100));

		TldRegionTracker.Track[] tracks = alg.getTracks();

		assertEquals(10 * 10, tracks.length);

		for( int i = 0; i < tracks.length; i++ ) {
			float x = tracks[i].klt.x;
			float y = tracks[i].klt.y;

			assertTrue(x >= 10 && x <= 80);
			assertTrue(y >= 20 && y <= 100);

			assertTrue(tracks[i].active);
		}
	}

	/**
	 * Empty image with no texture.  All spawn points should fail
	 */
	@Test
	public void spawnGrid_fail() {
		PyramidDiscrete<ImageUInt8> pyramid = FactoryPyramid.discreteGaussian(new int[]{1,2,4},-1,1,true,ImageUInt8.class);
		pyramid.process(new ImageUInt8(width,height));

		TldRegionTracker alg = createAlg();

		alg.initialize(pyramid);
		alg.updateCurrent(pyramid);
		alg.spawnGrid(new Rectangle2D_F64(10,20,80,100));

		TldRegionTracker.Track[] tracks = alg.getTracks();

		for( int i = 0; i < tracks.length; i++ ) {
			assertFalse(tracks[i].active);
		}
	}

	private TldRegionTracker<ImageUInt8,ImageSInt16> createAlg() {

		ImageGradient<ImageUInt8,ImageSInt16> gradient = FactoryDerivative.sobel(ImageUInt8.class,ImageSInt16.class);
		PyramidKltTracker<ImageUInt8,ImageSInt16> tracker =
				FactoryTrackerAlg.kltPyramid(new KltConfig(), ImageUInt8.class, ImageSInt16.class);

		return new TldRegionTracker<ImageUInt8,ImageSInt16>(10,5,100,gradient,tracker,ImageUInt8.class,ImageSInt16.class);
	}
}
