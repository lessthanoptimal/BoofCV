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

import boofcv.alg.misc.GImageStatistics;
import boofcv.core.image.FactoryGImageSingleBand;
import boofcv.core.image.GImageSingleBand;
import boofcv.core.image.GeneralizedImageOps;
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

	private int countNotZero( ImageSingleBand image ) {
		GImageSingleBand a = FactoryGImageSingleBand.wrap(image);

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
