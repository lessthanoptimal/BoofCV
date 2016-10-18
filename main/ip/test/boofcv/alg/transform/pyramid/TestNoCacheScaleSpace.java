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

package boofcv.alg.transform.pyramid;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofTesting;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;


/**
 * @author Peter Abeles
 */
public class TestNoCacheScaleSpace {

	Random rand = new Random(234);
	int width = 20;
	int height = 30;

	GrayF32 original = new GrayF32(width,height);

	@Before
	public void setup() {
		GImageMiscOps.fillUniform(original, rand, 0, 40);
	}

	@Test
	public void getScaledImage() {
		NoCacheScaleSpace<GrayF32,GrayF32> alg =
				new NoCacheScaleSpace<>(GrayF32.class, GrayF32.class);

		int radius = FactoryKernelGaussian.radiusForSigma(1.2,0);
		GrayF32 expected = BlurImageOps.gaussian(original,null,1.2,radius,null);

		alg.setScales(1.2,2.3,3.5);
		alg.setImage(original);
		alg.setActiveScale(0);
		GrayF32 found = alg.getScaledImage();

		BoofTesting.assertEquals(expected,found, 1e-4);
	}

	@Test
	public void getDerivative() {
		NoCacheScaleSpace<GrayF32,GrayF32> alg =
				new NoCacheScaleSpace<>(GrayF32.class, GrayF32.class);

		double target = 2.3;

		ImageGradient<GrayF32,GrayF32> g =  FactoryDerivative.three(GrayF32.class,null);

		GrayF32 derivX = new GrayF32(width,height);
		GrayF32 derivY = new GrayF32(width,height);
		GrayF32 derivXX = new GrayF32(width,height);
		GrayF32 derivYY = new GrayF32(width,height);
		GrayF32 derivXY = new GrayF32(width,height);
		GrayF32 derivYX = new GrayF32(width,height);
		GrayF32 derivYYX = new GrayF32(width,height);
		GrayF32 derivYYY = new GrayF32(width,height);

		alg.setScales(1.2,target,3.5);
		alg.setImage(original);
		alg.setActiveScale(1);

		g.process(alg.getScaledImage(),derivX,derivY);
		g.process(derivX,derivXX,derivXY);
		g.process(derivY,derivYX,derivYY);
		g.process(derivYY,derivYYX,derivYYY);


		// do one out of order which will force it to meet all the dependencies
		BoofTesting.assertEquals(derivYYY,alg.getDerivative(false,false,false), 1e-4);
		BoofTesting.assertEquals(derivX,alg.getDerivative(true), 1e-4);
		BoofTesting.assertEquals(derivY,alg.getDerivative(false), 1e-4);
		BoofTesting.assertEquals(derivXX,alg.getDerivative(true,true), 1e-4);
		BoofTesting.assertEquals(derivXY,alg.getDerivative(true,false), 1e-4);
		BoofTesting.assertEquals(derivYY,alg.getDerivative(false,false), 1e-4);
		BoofTesting.assertEquals(derivYYX,alg.getDerivative(false,false,true), 1e-4);
	}
}
