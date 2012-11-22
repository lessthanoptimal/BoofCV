/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.filter.FilterImageInterface;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.misc.ImageStatistics;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.pyramid.PyramidDiscrete;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestPyramidOps {

	Random rand = new Random(2342);
	int width = 60;
	int height = 50;

	int scales[] = new int[]{1,2,4};

	@Test
	public void randomize() {
		PyramidDiscrete<ImageFloat32> in = new PyramidDiscrete<ImageFloat32>(ImageFloat32.class,false,scales);
		in.initialize(width,height);
		PyramidOps.randomize(in,rand,0,100);

		for( int i = 0; i < scales.length; i++ ) {
			ImageFloat32 input = in.getLayer(i);
			assertTrue(ImageStatistics.sum(input)>0);
		}
	}

	@Test
	public void filter() {
		FilterImageInterface<ImageFloat32,ImageFloat32> filter = FactoryBlurFilter.gaussian(ImageFloat32.class,-1,1);

		PyramidDiscrete<ImageFloat32> in = new PyramidDiscrete<ImageFloat32>(ImageFloat32.class,false,scales);
		PyramidDiscrete<ImageFloat32> out = new PyramidDiscrete<ImageFloat32>(ImageFloat32.class,false,scales);

		in.initialize(width,height);

		PyramidOps.randomize(in,rand,0,100);
		PyramidOps.filter(in, filter, out);

		for( int i = 0; i < scales.length; i++ ) {
			ImageFloat32 input = in.getLayer(i);
			ImageFloat32 expected = new ImageFloat32(input.width,input.height);

			filter.process(input,expected);
			BoofTesting.assertEquals(expected,out.getLayer(i),0,1e-4);
		}
	}

	@Test
	public void gradient() {
		ImageGradient<ImageFloat32,ImageFloat32> gradient = FactoryDerivative.sobel_F32();

		PyramidDiscrete<ImageFloat32> in = new PyramidDiscrete<ImageFloat32>(ImageFloat32.class,false,scales);
		PyramidDiscrete<ImageFloat32> outX = new PyramidDiscrete<ImageFloat32>(ImageFloat32.class,false,scales);
		PyramidDiscrete<ImageFloat32> outY = new PyramidDiscrete<ImageFloat32>(ImageFloat32.class,false,scales);

		in.initialize(width,height);

		PyramidOps.randomize(in,rand,0,100);
		PyramidOps.gradient(in, gradient, outX,outY);

		for( int i = 0; i < scales.length; i++ ) {
			ImageFloat32 input = in.getLayer(i);
			ImageFloat32 x = new ImageFloat32(input.width,input.height);
			ImageFloat32 y = new ImageFloat32(input.width,input.height);

			gradient.process(input,x,y);
			BoofTesting.assertEquals(x,outX.getLayer(i),0,1e-4);
			BoofTesting.assertEquals(y,outY.getLayer(i),0,1e-4);
		}
	}
}
