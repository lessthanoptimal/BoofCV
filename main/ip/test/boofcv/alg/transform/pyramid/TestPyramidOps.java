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

package boofcv.alg.transform.pyramid;

import boofcv.abst.filter.FilterImageInterface;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.abst.filter.derivative.ImageHessian;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.pyramid.ImagePyramid;
import boofcv.struct.pyramid.PyramidDiscrete;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestPyramidOps {

	Random rand = new Random(2342);
	int width = 60;
	int height = 50;

	int scales[] = new int[]{1,2,4};

	@Test
	public void declareOutput() {
		DummyDiscrete<ImageFloat32> in = new DummyDiscrete<ImageFloat32>(ImageFloat32.class,false,scales);
		in.initialize(width,height);
		ImageFloat32[] out = PyramidOps.declareOutput(in,ImageFloat32.class);

		assertEquals(out.length,in.getNumLayers());
		for( int i = 0; i < in.getNumLayers(); i++ ) {
			ImageFloat32 o = out[i];
			assertEquals(o.width,in.getWidth(i));
			assertEquals(o.height,in.getHeight(i));
		}
	}

	@Test
	public void filter() {
		FilterImageInterface<ImageFloat32,ImageFloat32> filter = FactoryBlurFilter.gaussian(ImageFloat32.class,-1,1);

		DummyDiscrete<ImageFloat32> in = new DummyDiscrete<ImageFloat32>(ImageFloat32.class,false,scales);

		in.initialize(width,height);
		ImageFloat32[] out = PyramidOps.declareOutput(in,ImageFloat32.class);

		randomize(in, rand, 0, 100);
		PyramidOps.filter(in, filter, out);

		for( int i = 0; i < scales.length; i++ ) {
			ImageFloat32 input = in.getLayer(i);
			ImageFloat32 expected = new ImageFloat32(input.width,input.height);

			filter.process(input,expected);
			BoofTesting.assertEquals(expected,out[i],1e-4);
		}
	}

	@Test
	public void gradient() {
		ImageGradient<ImageFloat32,ImageFloat32> gradient = FactoryDerivative.sobel_F32();

		DummyDiscrete<ImageFloat32> in = new DummyDiscrete<ImageFloat32>(ImageFloat32.class,false,scales);
		in.initialize(width, height);

		ImageFloat32[] outX = PyramidOps.declareOutput(in,ImageFloat32.class);
		ImageFloat32[] outY = PyramidOps.declareOutput(in,ImageFloat32.class);

		randomize(in, rand, 0, 100);
		PyramidOps.gradient(in, gradient, outX,outY);

		for( int i = 0; i < scales.length; i++ ) {
			ImageFloat32 input = in.getLayer(i);
			ImageFloat32 x = new ImageFloat32(input.width,input.height);
			ImageFloat32 y = new ImageFloat32(input.width,input.height);

			gradient.process(input,x,y);
			BoofTesting.assertEquals(x,outX[i],1e-4);
			BoofTesting.assertEquals(y,outY[i],1e-4);
		}
	}

	@Test
	public void hessian() {
		ImageHessian<ImageFloat32> gradient = FactoryDerivative.hessianThree(ImageFloat32.class);

		ImageFloat32[] derivX = new ImageFloat32[5];
		ImageFloat32[] derivY = new ImageFloat32[5];
		ImageFloat32[] derivXX = new ImageFloat32[5];
		ImageFloat32[] derivYY = new ImageFloat32[5];
		ImageFloat32[] derivXY = new ImageFloat32[5];

		for( int i = 0; i < 5; i++ ) {
			derivX[i] = new ImageFloat32(20,30-i);
			derivY[i] = new ImageFloat32(20,30-i);
			derivXX[i] = new ImageFloat32(20,30-i);
			derivYY[i] = new ImageFloat32(20,30-i);
			derivXY[i] = new ImageFloat32(20,30-i);

			GImageMiscOps.fillUniform(derivX[i], rand, 0, 100);
			GImageMiscOps.fillUniform(derivY[i], rand, 0, 100);
		}

		PyramidOps.hessian(derivX,derivY, gradient,derivXX,derivYY,derivXY);

		for( int i = 0; i < derivX.length; i++ ) {
			ImageFloat32 dx = derivX[i];
			ImageFloat32 dy = derivY[i];

			ImageFloat32 foundXX = new ImageFloat32(dx.width,dy.height);
			ImageFloat32 foundYY = new ImageFloat32(dx.width,dy.height);
			ImageFloat32 foundXY = new ImageFloat32(dx.width,dy.height);

			gradient.process(dx,dy,foundXX,foundYY,foundXY);

			BoofTesting.assertEquals(foundXX,derivXX[i],1e-4);
			BoofTesting.assertEquals(foundYY,derivYY[i],1e-4);
			BoofTesting.assertEquals(foundXY,derivXY[i],1e-4);
		}

	}

	public static <I extends ImageSingleBand>
	void randomize( ImagePyramid<I> pyramid , Random rand , int min , int max ) {

		for( int i = 0; i < pyramid.getNumLayers(); i++ ) {
			I imageIn = pyramid.getLayer(i);
			GImageMiscOps.fillUniform(imageIn, rand, min, max);
		}
	}

	private static class DummyDiscrete<T extends ImageSingleBand> extends PyramidDiscrete<T> {

		public DummyDiscrete(Class<T> imageType, boolean saveOriginalReference, int scales[] ) {
			super(imageType, saveOriginalReference,scales);
		}

		@Override
		public void initialize(int width, int height) {
			super.initialize(width, height);
		}

		@Override
		public void process(T input) {}

		@Override
		public double getSampleOffset(int layer) {return 0;}

		@Override
		public double getSigma(int layer) {return 0;}
	}
}
