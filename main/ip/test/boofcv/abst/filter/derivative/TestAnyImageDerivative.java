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

package boofcv.abst.filter.derivative;

import boofcv.alg.filter.derivative.GradientThree;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestAnyImageDerivative {

	Random rand = new Random(234);
	int width = 20;
	int height = 30;

	GrayF32 original = new GrayF32(width,height);

	/**
	 * See if changing the input image size causes an exception to be thrown.
	 */
	@Test
	public void changeInputImageSize() {
		Kernel1D_F32 kernelX = (Kernel1D_F32)GradientThree.getKernelX(false);
		AnyImageDerivative<GrayF32,GrayF32> alg = new AnyImageDerivative<>(kernelX, GrayF32.class, GrayF32.class);
		alg.setInput(original);
		alg.getDerivative(true);

		GrayF32 smaller = new GrayF32(width-5,height-5);
		GImageMiscOps.fillUniform(smaller, rand, 0, 40);

		// assume that if it can't handle variable sized inputs then an exception is thrown
		alg.setInput(smaller);
		alg.getDerivative(true);
	}

	@Test
	public void test() {
		ImageGradient<GrayF32,GrayF32> g =  FactoryDerivative.three(GrayF32.class,null);

		GrayF32 derivX = new GrayF32(width,height);
		GrayF32 derivY = new GrayF32(width,height);
		GrayF32 derivXX = new GrayF32(width,height);
		GrayF32 derivYY = new GrayF32(width,height);
		GrayF32 derivXY = new GrayF32(width,height);
		GrayF32 derivYX = new GrayF32(width,height);
		GrayF32 derivYYX = new GrayF32(width,height);
		GrayF32 derivYYY = new GrayF32(width,height);

		GImageMiscOps.fillUniform(original, rand, 0, 40);
		g.process(original,derivX,derivY);
		g.process(derivX,derivXX,derivXY);
		g.process(derivY,derivYX,derivYY);
		g.process(derivYY,derivYYX,derivYYY);

		Kernel1D_F32 kernelX = (Kernel1D_F32)GradientThree.getKernelX(false);

		AnyImageDerivative<GrayF32,GrayF32> alg = new AnyImageDerivative<>(kernelX, GrayF32.class, GrayF32.class);
		alg.setInput(original);

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
