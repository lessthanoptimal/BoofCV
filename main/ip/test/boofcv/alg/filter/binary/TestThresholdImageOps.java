/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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
	public void localSquare() {
		int total = 0;
		Method[] list = ThresholdImageOps.class.getMethods();

		for( Method m : list ) {
			if( !m.getName().equals("localSquare"))
				continue;

			Class param[] = m.getParameterTypes();

			ImageSingleBand input = GeneralizedImageOps.createSingleBand(param[0], width, height);
			ImageUInt8 output = new ImageUInt8(width,height);

			GImageMiscOps.fillUniform(input, rand, 0, 200);

			BoofTesting.checkSubImage(this,"performLocalSquare",true,m,input,output);
			total++;
		}

		assertEquals(2, total);
	}

	public void performLocalSquare( Method m , ImageSingleBand input , ImageUInt8 output )
			throws InvocationTargetException, IllegalAccessException
	{
		ImageUInt8 expected = new ImageUInt8(output.width,output.height);

		for( int radius = 1; radius <= 5; radius++ ) {
			for( int indexScale = 0; indexScale < 4; indexScale++ ) {
				float scale = (float)(0.8+0.4*(indexScale/3.0));
				ImageMiscOps.fillUniform(output,rand,0,200);
				ImageMiscOps.fillUniform(expected,rand,0,200);
				m.invoke(null,input,output,radius,scale,true,null,null);
				naiveLocalSquare(input, expected, radius, scale, true);

				BoofTesting.assertEquals(expected,output,0);

				ImageMiscOps.fillUniform(output,rand,0,200);
				ImageMiscOps.fillUniform(expected,rand,0,200);
				m.invoke(null,input,output,radius,scale,false,null,null);
				naiveLocalSquare(input, expected, radius, scale, false);

				BoofTesting.assertEquals(expected,output,0);
			}
		}
	}

	public void naiveLocalSquare(ImageSingleBand input, ImageUInt8 output,
									int radius, double scale, boolean down) {

		ImageSingleBand blur;
		boolean isInt;
		if( input instanceof ImageUInt8 ) {
			isInt = true;
			blur = BlurImageOps.mean((ImageUInt8)input,null,radius,null);
		} else {
			isInt = false;
			blur = BlurImageOps.mean((ImageFloat32)input,null,radius,null);
		}

		float fscale = (float)scale;

		for( int y = 0; y < input.height; y++ ) {
			for( int x = 0; x < input.width; x++ ) {

				double threshold = GeneralizedImageOps.get(blur,x,y);
				double v = GeneralizedImageOps.get(input,x,y);

				boolean one;
				if( down ) {
					if( isInt ) {
						one = (int)v <= ((int)threshold)*fscale;
					} else {
						one = v <= threshold*fscale;
					}
				} else {
					if( isInt ) {
						one = ((int)v)*fscale > (int)threshold;
					} else {
						one = v*fscale > threshold;
					}
				}

				if( one ) {
					output.set(x,y,1);
				} else {
					output.set(x,y,0);
				}
			}
		}
	}

	@Test
	public void localGaussian() {
		int total = 0;
		Method[] list = ThresholdImageOps.class.getMethods();

		for( Method m : list ) {
			if( !m.getName().equals("localGaussian"))
				continue;

			Class param[] = m.getParameterTypes();

			ImageSingleBand input = GeneralizedImageOps.createSingleBand(param[0], width, height);
			ImageUInt8 output = new ImageUInt8(width,height);

			GImageMiscOps.fillUniform(input, rand, 0, 200);

			BoofTesting.checkSubImage(this,"performLocalGaussian",true,m,input,output);
			total++;
		}

		assertEquals(2, total);
	}

	public void performLocalGaussian( Method m , ImageSingleBand input , ImageUInt8 output )
			throws InvocationTargetException, IllegalAccessException
	{
		ImageUInt8 expected = new ImageUInt8(output.width,output.height);

		for( int radius = 1; radius <= 5; radius++ ) {
			for( int indexScale = 0; indexScale < 4; indexScale++ ) {
				float scale = (float)(0.8+0.4*(indexScale/3.0));

				ImageMiscOps.fillUniform(output,rand,0,200);
				ImageMiscOps.fillUniform(expected,rand,0,200);
				m.invoke(null,input,output,radius,scale,true,null,null);
				naiveLocalGaussian(input, expected, radius, scale, true);

				BoofTesting.assertEquals(expected,output,0);

				ImageMiscOps.fillUniform(output, rand, 0, 200);
				ImageMiscOps.fillUniform(expected,rand,0,200);
				m.invoke(null, input, output, radius, scale, false, null, null);
				naiveLocalGaussian(input, expected, radius, scale, false);

				BoofTesting.assertEquals(expected,output,0);
			}
		}
	}

	public void naiveLocalGaussian( ImageSingleBand input , ImageUInt8 output ,
									   int radius , double scale , boolean down ) {

		ImageSingleBand blur;
		boolean isInt;
		if( input instanceof ImageUInt8 ) {
			isInt = true;
			blur = BlurImageOps.gaussian((ImageUInt8) input, null, -1, radius, null);
		} else {
			isInt = false;
			blur = BlurImageOps.gaussian((ImageFloat32) input, null, -1, radius, null);
		}

		float fscale = (float)scale;

		for( int y = 0; y < input.height; y++ ) {
			for( int x = 0; x < input.width; x++ ) {
				double threshold = GeneralizedImageOps.get(blur,x,y);
				double v = GeneralizedImageOps.get(input,x,y);

				boolean one;
				if( down ) {
					if( isInt ) {
						one = (int)v <= ((int)threshold)*fscale;
					} else {
						one = v <= threshold*fscale;
					}
				} else {
					if( isInt ) {
						one = ((int)v)*fscale > (int)threshold;
					} else {
						one = v*fscale > threshold;
					}
				}

				if( one ) {
					output.set(x,y,1);
				} else {
					output.set(x,y,0);
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
		int areaBelow = 8*input.height;
		int areaAbove = input.width*input.height - areaBelow;


		m.invoke(null,input,output,7,true);
		assertEquals(areaBelow, GImageStatistics.sum(output),1e-4);

		m.invoke(null,input,output,7,false);
		assertEquals(areaAbove, GImageStatistics.sum(output),1e-4);
	}

}
