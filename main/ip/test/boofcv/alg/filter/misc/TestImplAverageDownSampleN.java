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

package boofcv.alg.filter.misc;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestImplAverageDownSampleN {

	int numMethods = 7;

	Random rand = new Random(234);

	int width = 20;
	int height = 15;

	@Test
	public void compareToNaive() {
		Method methods[] = ImplAverageDownSampleN.class.getMethods();

		// sanity check to make sure the functions are being found
		int numFound = 0;
		for (Method m : methods) {
			if( m.getName().compareTo("down") != 0 )
				continue;

			compareToNaive(m);

			numFound++;
		}

		// update this as needed when new functions are added
		if(numMethods != numFound)
			throw new RuntimeException("Unexpected number of methods: Found "+numFound+"  expected "+numMethods);
	}

	public void compareToNaive( Method m ) {
		Class inputType = m.getParameterTypes()[0];
		Class outputType = m.getParameterTypes()[2];


		ImageGray input = GeneralizedImageOps.createSingleBand(inputType,width,height);

		GImageMiscOps.fillUniform(input, rand, 0, 100);

		for( int region = 1; region <= 5; region++ ) {
			int downWidth = width%region == 0 ? width/region : width/region+1;
			int downHeight = height%region == 0 ? height/region : height/region+1;

			ImageGray found = GeneralizedImageOps.createSingleBand(outputType,downWidth,downHeight);
			ImageGray expected = GeneralizedImageOps.createSingleBand(outputType,downWidth,downHeight);

			try {
				m.invoke(null, input, region, found);
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
			naive(input,region,expected);

			BoofTesting.assertEquals(found,expected,1e-4);
		}
	}

	public static void naive(ImageGray input , int region , ImageGray output ) {
		for( int y = 0; y < input.height; y += region ) {
			int endY = y+region > input.height ? input.height : y+region;

			for( int x = 0; x < input.width; x += region ) {
				int endX = x+region > input.width ? input.width : x+region;


				double total = 0;
				int count = 0;

				for( int yy = y; yy < endY; yy++ ) {
					for( int xx = x; xx < endX; xx++ ) {
						total += GeneralizedImageOps.get(input, xx, yy);
						count++;
					}
				}

				if( input.getDataType().isInteger())
					GeneralizedImageOps.set( output, x/region, y/region , (int)Math.round(total/count) );
				else
					GeneralizedImageOps.set( output, x/region, y/region , total/count );
			}
		}
	}
}
