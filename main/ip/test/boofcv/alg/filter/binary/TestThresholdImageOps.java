/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.binary;

import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.GImageStatistics;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.FactoryGImageSingleBand;
import boofcv.core.image.GImageSingleBand;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestThresholdImageOps {

	int width = 20;
	int height = 30;

	Random rand = new Random(234);

	@Test
	public void adaptiveSquare() {
		int total = 0;
		Method[] list = ThresholdImageOps.class.getMethods();

		for( Method m : list ) {
			if( !m.getName().equals("adaptiveSquare"))
				continue;

			Class param[] = m.getParameterTypes();

			ImageSingleBand input = GeneralizedImageOps.createSingleBand(param[0], width, height);
			ImageUInt8 output = new ImageUInt8(width,height);

			GImageMiscOps.fillUniform(input, rand, 0, 200);

			BoofTesting.checkSubImage(this,"performAdaptiveSquare",true,m,input,output);
			total++;
		}

		assertEquals(2, total);
	}

	public void performAdaptiveSquare( Method m , ImageSingleBand input , ImageUInt8 output )
			throws InvocationTargetException, IllegalAccessException
	{
		ImageUInt8 expected = new ImageUInt8(output.width,output.height);

		for( int radius = 1; radius <= 5; radius++ ) {
			for( int indexBias = 0; indexBias < 4; indexBias++ ) {
				int bias = indexBias*10-20;

				ImageMiscOps.fillUniform(output,rand,0,200);
				ImageMiscOps.fillUniform(expected,rand,0,200);
				m.invoke(null,input,output,radius,bias,true,null,null);
				naiveAdaptiveSquare(input, expected, radius, bias, true);

				BoofTesting.assertEquals(expected,output,0);

				ImageMiscOps.fillUniform(output,rand,0,200);
				ImageMiscOps.fillUniform(expected,rand,0,200);
				m.invoke(null,input,output,radius,bias,false,null,null);
				naiveAdaptiveSquare(input, expected, radius, bias, false);

				BoofTesting.assertEquals(expected,output,0);
			}
		}
	}

	public void naiveAdaptiveSquare(ImageSingleBand input, ImageUInt8 output,
									int radius, double bias, boolean down) {

		int w = radius*2+1;

		ImageSingleBand blur;
		if( input instanceof ImageUInt8 ) {
			blur = BlurImageOps.mean((ImageUInt8)input,null,radius,null);
		} else {
			blur = BlurImageOps.mean((ImageFloat32)input,null,radius,null);
		}

		for( int y = 0; y < input.height; y++ ) {
			for( int x = 0; x < input.width; x++ ) {

				double threshold = GeneralizedImageOps.get(blur,x,y)+bias;
				double v = GeneralizedImageOps.get(input,x,y);

				if( down ) {
					if( v <= threshold ) {
						output.set(x,y,1);
					} else {
						output.set(x,y,0);
					}
				} else {
					if( v >= threshold ) {
						output.set(x,y,1);
					} else {
						output.set(x,y,0);
					}
				}

			}
		}
	}

	@Test
	public void adaptiveGaussian() {
		int total = 0;
		Method[] list = ThresholdImageOps.class.getMethods();

		for( Method m : list ) {
			if( !m.getName().equals("adaptiveGaussian"))
				continue;

			Class param[] = m.getParameterTypes();

			ImageSingleBand input = GeneralizedImageOps.createSingleBand(param[0], width, height);
			ImageUInt8 output = new ImageUInt8(width,height);

			GImageMiscOps.fillUniform(input, rand, 0, 200);

			BoofTesting.checkSubImage(this,"performAdaptiveGaussian",true,m,input,output);
			total++;
		}

		assertEquals(2, total);
	}

	public void performAdaptiveGaussian( Method m , ImageSingleBand input , ImageUInt8 output )
			throws InvocationTargetException, IllegalAccessException
	{
		ImageUInt8 expected = new ImageUInt8(output.width,output.height);

		for( int radius = 1; radius <= 5; radius++ ) {
			for( int indexBias = 0; indexBias < 4; indexBias++ ) {
				int bias = indexBias*10-20;

				ImageMiscOps.fillUniform(output,rand,0,200);
				ImageMiscOps.fillUniform(expected,rand,0,200);
				m.invoke(null,input,output,radius,bias,true,null,null);
				naiveAdaptiveGaussian(input, expected, radius, bias, true);

				BoofTesting.assertEquals(expected,output,0);

				ImageMiscOps.fillUniform(output, rand, 0, 200);
				ImageMiscOps.fillUniform(expected,rand,0,200);
				m.invoke(null, input, output, radius, bias, false, null, null);
				naiveAdaptiveGaussian(input, expected, radius, bias, false);

				BoofTesting.assertEquals(expected,output,0);
			}
		}
	}

	public void naiveAdaptiveGaussian( ImageSingleBand input , ImageUInt8 output ,
									   int radius , double bias , boolean down ) {

		ImageSingleBand blur;
		if( input instanceof ImageUInt8 ) {
			blur = BlurImageOps.gaussian((ImageUInt8) input, null, -1, radius, null);
		} else {
			blur = BlurImageOps.gaussian((ImageFloat32) input, null, -1, radius, null);
		}

		for( int y = 0; y < input.height; y++ ) {
			for( int x = 0; x < input.width; x++ ) {
				double threshold = GeneralizedImageOps.get(blur,x,y)+bias;
				double v = GeneralizedImageOps.get(input,x,y);

				if( down ) {
					if( v <= threshold ) {
						output.set(x,y,1);
					} else {
						output.set(x,y,0);
					}
				} else {
					if( v >= threshold ) {
						output.set(x,y,1);
					} else {
						output.set(x,y,0);
					}
				}

			}
		}
	}

	@Test
	public void threshold() {

		int total = 0;
		Method[] list = ThresholdImageOps.class.getMethods();

		for( Method m : list ) {
			if( !m.getName().equals("threshold"))
				continue;

			Class param[] = m.getParameterTypes();

			ImageSingleBand input = GeneralizedImageOps.createSingleBand(param[0], width, height);
			ImageUInt8 output = new ImageUInt8(width,height);

			GImageSingleBand a = FactoryGImageSingleBand.wrap(input);
			for( int y = 0; y < input.height; y++ ) {
				for( int x = 0; x < input.width; x++ ) {
					a.set(x,y,x);
				}
			}

			BoofTesting.checkSubImage(this,"performThreshold",true,m,input,output);
			total++;
		}

		assertEquals(6,total);
	}

	public void performThreshold( Method m , ImageSingleBand input , ImageUInt8 output )
			throws InvocationTargetException, IllegalAccessException
	{
		m.invoke(null,input,output,7,true);
		assertEquals(240, GImageStatistics.sum(output),1e-4);

		m.invoke(null,input,output,7,false);
		assertEquals(390, GImageStatistics.sum(output),1e-4);
	}
}
