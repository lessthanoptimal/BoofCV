/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.tracker.hybrid;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.tracker.klt.ConfigKlt;
import boofcv.alg.tracker.klt.PyramidKltFeature;
import boofcv.alg.transform.pyramid.PyramidOps;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.ConfigDiscreteLevels;
import boofcv.struct.pyramid.PyramidDiscrete;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestPyramidKltForCombined extends BoofStandardJUnit {

	int width = 80;
	int height = 100;

	ConfigDiscreteLevels configLevels = ConfigDiscreteLevels.levels(3);

	PyramidDiscrete<GrayF32> pyramid;
	GrayF32[] derivX;
	GrayF32[] derivY;


	public PyramidKltForHybrid<GrayF32,GrayF32> createAlg()
	{
		ConfigKlt config = new ConfigKlt();

		return new PyramidKltForHybrid<>(config, 5, GrayF32.class, GrayF32.class);
	}

	@BeforeEach
	void init() {

		pyramid = FactoryPyramid.discreteGaussian(configLevels,-1,2,false, ImageType.single(GrayF32.class));

		GrayF32 input = new GrayF32(width,height);
		ImageMiscOps.fillUniform(input,rand,0,100);

		// do a real update so that it can track a feature
		ImageGradient<GrayF32,GrayF32> gradient =
				FactoryDerivative.sobel(GrayF32.class, GrayF32.class);

		pyramid.process(input);
		derivX = PyramidOps.declareOutput(pyramid,ImageType.SB_F32);
		derivY = PyramidOps.declareOutput(pyramid,ImageType.SB_F32);
		PyramidOps.gradient(pyramid, gradient, derivX, derivY);
	}

	@Test
	void setDescription() {
		PyramidKltForHybrid<GrayF32,GrayF32> alg = createAlg();

		alg.setInputs(pyramid,derivX,derivY);

		PyramidKltFeature t = alg.createNewTrack();

		alg.setDescription(30.1f,25,t);

		assertTrue(30.1f == t.x);
		assertTrue(25f == t.y);

		for( int i = 0; i < t.desc.length; i++ ) {
			double v = ImageStatistics.sum(t.desc[i].desc);
			double dx = ImageStatistics.sum(t.desc[i].derivX);
			double dy = ImageStatistics.sum(t.desc[i].derivY);

			assertTrue(v!=0);
			assertTrue(dx!=0);
			assertTrue(dy!=0);
		}
	}

	@Test
	void performTracking() {
		PyramidKltForHybrid<GrayF32,GrayF32> alg = createAlg();

		alg.setInputs(pyramid,derivX,derivY);

		PyramidKltFeature t = alg.createNewTrack();

		alg.setDescription(30.1f, 25, t);

		// offset it from the original pose
		t.x = 33.5f;
		t.y = 18f;

		// see if it moves it back close to the original pose
		assertTrue(alg.performTracking(t));

		assertTrue(Math.abs(t.x-30.1)<0.1);
		assertTrue(Math.abs(t.y-25)<0.1);
	}
}
