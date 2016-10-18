/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.flow;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.tracker.klt.PkltConfig;
import boofcv.alg.tracker.klt.PyramidKltTracker;
import boofcv.alg.transform.pyramid.PyramidOps;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.tracker.FactoryTrackerAlg;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.GrayF32;
import boofcv.struct.pyramid.ImagePyramid;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDenseOpticalFlowKlt {

	GrayF32 image0 = new GrayF32(30,40);
	GrayF32 image1 = new GrayF32(30,40);

	ImagePyramid<GrayF32> prev;
	GrayF32[] prevDerivX;
	GrayF32[] prevDerivY;
	ImagePyramid<GrayF32> curr;

	ImageGradient<GrayF32, GrayF32> gradient = FactoryDerivative.sobel(GrayF32.class, GrayF32.class);

	PkltConfig config = new PkltConfig();

	@Before
	public void setup() {
		config.pyramidScaling = new int[]{1,2};
		config.config.maxPerPixelError = 15;

		prev = FactoryPyramid.discreteGaussian(config.pyramidScaling, -1, 2, true, GrayF32.class);
		curr = FactoryPyramid.discreteGaussian(config.pyramidScaling, -1, 2, true, GrayF32.class);

		prev.process(image0);
		curr.process(image0);

		prevDerivX = PyramidOps.declareOutput(prev,GrayF32.class);
		prevDerivY = PyramidOps.declareOutput(prev,GrayF32.class);
	}

	private void processInputImage() {
		prev.process(image0);
		curr.process(image1);

		PyramidOps.gradient(prev, gradient, prevDerivX,prevDerivY);
	}

	protected DenseOpticalFlowKlt<GrayF32,GrayF32> createAlg() {
		PyramidKltTracker<GrayF32, GrayF32> tracker =
				FactoryTrackerAlg.kltPyramid(config.config, GrayF32.class, GrayF32.class);
		return new DenseOpticalFlowKlt<>(tracker, config.pyramidScaling.length, 3);
	}

	/**
	 * Very simple positive case
	 */
	@Test
	public void positive() {

		ImageMiscOps.fillRectangle(image0,50,10,12,2,2);
		ImageMiscOps.fillRectangle(image1,50,11,13,2,2);

		processInputImage();

		DenseOpticalFlowKlt<GrayF32,GrayF32> alg = createAlg();

		ImageFlow flow = new ImageFlow(image0.width,image0.height);
		flow.invalidateAll();

		alg.process(prev,prevDerivX,prevDerivY,curr,flow);

		// no texture in the image so KLT can't do anything
		check(flow.get(0,0),false,0,0);
		check(flow.get(29,39),false,0,0);
		// there is texture at the target
		check(flow.get(10,12),true,1,1);
		check(flow.get(11,12),true,1,1);
		check(flow.get(10,13),true,1,1);
		check(flow.get(11,13),true,1,1);
	}

	private void check( ImageFlow.D flow , boolean valid , float x , float y ) {
		assertEquals(valid,flow.isValid());
		if( valid ) {
			assertEquals(x,flow.x,0.05f);
			assertEquals(y,flow.y,0.05f);
		}
	}

	/**
	 * Very simple negative case. The second image is blank so it should fail at tracking
	 */
	@Test
	public void negative() {

		ImageMiscOps.fillRectangle(image0, 200, 7, 9, 5, 5);

		processInputImage();

		DenseOpticalFlowKlt<GrayF32,GrayF32> alg = createAlg();

		ImageFlow flow = new ImageFlow(image0.width,image0.height);

		alg.process(prev,prevDerivX,prevDerivY,curr,flow);

		int totalFail = 0;
		for (int i = 0; i < flow.data.length; i++) {
			if( !flow.data[i].isValid() ) {
				totalFail++;
			}
		}

		assertTrue( totalFail/(double)flow.data.length >= 0.90 );
	}

}
