/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

import boofcv.core.image.FactoryGeneralizedSingleBand;
import boofcv.core.image.GImageSingleBand;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestThresholdImageOps {

	int[] patternDo = new int[]{0,0,0,0,0,0,0,0,0,0,
								0,0,0,1,1,0,1,0,0,0,
								0,0,1,1,1,1,0,0,0,0,
								0,0,1,1,5,0,0,0,1,0,
								0,0,0,1,1,0,0,1,1,0,
								0,0,0,0,0,0,0,0,1,0};

	int[] patternUp = new int[]{6,6,6,6,6,6,6,6,6,6,
								6,6,6,5,5,6,5,6,6,6,
							 	6,6,5,5,5,5,6,6,6,6,
							 	6,6,5,5,2,6,6,6,5,6,
								6,6,6,5,5,6,6,5,5,6,
								6,6,6,6,6,6,6,6,5,6};


	int width = 20;
	int height = 30;

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

			GImageSingleBand a = FactoryGeneralizedSingleBand.wrap(input);
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
		assertEquals(240, GeneralizedImageOps.sum(output),1e-4);

		m.invoke(null,input,output,7,false);
		assertEquals(390, GeneralizedImageOps.sum(output),1e-4);
	}

	@Test
	public void thresholdBlobs() {
		int total = 0;
		Method[] list = ThresholdImageOps.class.getMethods();

		for( Method m : list ) {
			if( !m.getName().equals("thresholdBlobs"))
				continue;

			Class param[] = m.getParameterTypes();

			ImageSingleBand input = GeneralizedImageOps.createSingleBand(param[0], width, height);
			ImageSInt32 labeled = new ImageSInt32(width,height);

			GImageSingleBand a = FactoryGeneralizedSingleBand.wrap(input);
			for( int y = 0; y < input.height; y++ ) {
				for( int x = 0; x < input.width; x++ ) {
					a.set(x,y,x);
					labeled.set(x,y,x);
				}
			}

			BoofTesting.checkSubImage(this,"performThresholdBlobs",true,m,input,labeled);
			total++;
		}

		assertEquals(6,total);
	}

	public void performThresholdBlobs( Method m , ImageSingleBand input , ImageSInt32 labeled )
			throws InvocationTargetException, IllegalAccessException
	{
		int results[] = new int[width];
		m.invoke(null,input,labeled,results,width,7,true);
		for( int i = 0; i <= 7; i++ ) {
			assertEquals(i,results[i]);
		}
		for( int i = 8; i < width; i++ ) {
			assertEquals(0,results[i]);
		}

		m.invoke(null,input,labeled,results,width,7,false);
		for( int i = 0; i < 7; i++ ) {
			assertEquals(0,results[i]);
		}
		for( int i = 7; i < width; i++ ) {
			assertEquals(i,results[i]);
		}
	}

		@Test
	public void hysteresisLabel4() {
		int total = 0;
		Method[] list = ThresholdImageOps.class.getMethods();

			int width = 10;
			int height = 6;

		for( Method m : list ) {
			if( !m.getName().equals("hysteresisLabel4"))
				continue;

			Class param[] = m.getParameterTypes();

			ImageSingleBand inputDown = GeneralizedImageOps.createSingleBand(param[0], width, height);
			ImageSingleBand inputUp = GeneralizedImageOps.createSingleBand(param[0], width, height);
			ImageSInt32 labeled = new ImageSInt32(width,height);

			GImageSingleBand a = FactoryGeneralizedSingleBand.wrap(inputDown);
			for( int y = 0; y < height; y++ ) {
				for( int x = 0; x < width; x++ ) {
					a.set(x,y,patternDo[y*width+x]);
				}
			}

			a = FactoryGeneralizedSingleBand.wrap(inputUp);
			for( int y = 0; y < height; y++ ) {
				for( int x = 0; x < width; x++ ) {
					a.set(x,y,patternUp[y*width+x]);
				}
			}

			BoofTesting.checkSubImage(this,"performHysteresisLabel",true,m,11,inputDown,inputUp,labeled);
			total++;
		}

		assertEquals(6,total);
	}

	@Test
	public void hysteresisLabel8() {
		int total = 0;
		Method[] list = ThresholdImageOps.class.getMethods();

		int width = 10;
		int height = 6;

		for( Method m : list ) {
			if( !m.getName().equals("hysteresisLabel8"))
				continue;

			Class param[] = m.getParameterTypes();

			ImageSingleBand inputDown = GeneralizedImageOps.createSingleBand(param[0], width, height);
			ImageSingleBand inputUp = GeneralizedImageOps.createSingleBand(param[0], width, height);
			ImageSInt32 labeled = new ImageSInt32(width,height);

			GImageSingleBand a = FactoryGeneralizedSingleBand.wrap(inputDown);
			for( int y = 0; y < height; y++ ) {
				for( int x = 0; x < width; x++ ) {
					a.set(x,y,patternDo[y*width+x]);
				}
			}

			a = FactoryGeneralizedSingleBand.wrap(inputUp);
			for( int y = 0; y < height; y++ ) {
				for( int x = 0; x < width; x++ ) {
					a.set(x,y,patternUp[y*width+x]);
				}
			}

			BoofTesting.checkSubImage(this,"performHysteresisLabel",true,m,12,inputDown,inputUp,labeled);
			total++;
		}

		assertEquals(6,total);
	}

	public void performHysteresisLabel( Method m , Integer expected , ImageSingleBand inputDown , ImageSingleBand inputUp , ImageSInt32 labeled )
			throws InvocationTargetException, IllegalAccessException
	{
		int numFound = (Integer)m.invoke(null,inputUp,labeled,3,5,true,null);
		assertEquals(1,numFound);
		assertEquals(expected, countNotZero(labeled),1e-4);

		numFound = (Integer)m.invoke(null,inputDown,labeled,1,4,false,null);
		assertEquals(1,numFound);
		assertEquals(expected, countNotZero(labeled),1e-4);
	}

	private int countNotZero( ImageSingleBand image ) {
		GImageSingleBand a = FactoryGeneralizedSingleBand.wrap(image);

		int ret = 0;

		for( int i = 0; i < a.getHeight(); i++ ) {
			for( int j = 0; j < a.getWidth(); j++ ) {
				if( a.get(j,i).intValue() != 0 )
					ret++;
			}
		}

		return ret;
	}
}
