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

import static boofcv.alg.filter.misc.TestImplAverageDownSampleN.naive;

/**
 * @author Peter Abeles
 */
public class TestImplAverageDownSample2 {

	int numMethods = 7;

	Random rand = new Random(234);

	int width = 20;
	int height = 15;

	@Test
	public void compareToNaive() {
		Method methods[] = ImplAverageDownSample2.class.getMethods();

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
		Class outputType = m.getParameterTypes()[1];

		ImageGray input = GeneralizedImageOps.createSingleBand(inputType,width,height);

		GImageMiscOps.fillUniform(input, rand, 0, 100);

			int downWidth = AverageDownSampleOps.downSampleSize(width,2);
			int downHeight = AverageDownSampleOps.downSampleSize(height,2);

			ImageGray found = GeneralizedImageOps.createSingleBand(outputType,downWidth,downHeight);
			ImageGray expected = GeneralizedImageOps.createSingleBand(outputType,downWidth,downHeight);

			try {
				m.invoke(null, input, found);
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		naive(input,2,expected);

			BoofTesting.assertEquals(found,expected,1e-4);
	}
}
