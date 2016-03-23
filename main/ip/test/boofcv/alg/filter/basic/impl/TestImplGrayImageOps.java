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

package boofcv.alg.filter.basic.impl;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GImageGray;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestImplGrayImageOps {
	int width = 10;
	int height = 15;
	Random rand = new Random(234);

	int numExpected = 5;

	@Test
	public void invert() {
		int numFound = BoofTesting.findMethodThenCall(this,"invert",ImplGrayImageOps.class,"invert");
		assertEquals(numExpected,numFound);
	}

	public void invert( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class param[] = m.getParameterTypes();
		ImageGray input = GeneralizedImageOps.createSingleBand(param[0], width, height);
		GImageMiscOps.fillUniform(input, rand, 0, 100);
		ImageGray output = GeneralizedImageOps.createSingleBand(param[0], width, height);

		m.invoke(null,input, 255, output);

		GImageGray a = FactoryGImageGray.wrap(input);
		GImageGray b = FactoryGImageGray.wrap(output);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				assertEquals(255 - a.get(x, y).doubleValue(), b.get(x, y).doubleValue(),1e-4f);
			}
		}
	}

	@Test
	public void brighten() {
		int numFound = BoofTesting.findMethodThenCall(this,"brighten",ImplGrayImageOps.class,"brighten");
		assertEquals(numExpected,numFound);
	}

	public void brighten(Method m) throws InvocationTargetException, IllegalAccessException {
		Class param[] = m.getParameterTypes();
		ImageGray input = GeneralizedImageOps.createSingleBand(param[0], width, height);
		ImageGray output = GeneralizedImageOps.createSingleBand(param[0], width, height);
		GImageMiscOps.fill(input, 23);

		m.invoke(null,input, 10,255, output);

		GImageGray b = FactoryGImageGray.wrap(output);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				assertEquals(33, b.get(x, y).doubleValue(),1e-4);
			}
		}

		// check to see how well it sets the ceiling
		m.invoke(null,input, 240,255, output);
		assertEquals(255, b.get(5, 6).doubleValue(),1e-4);

		// check it flooring to zero
		m.invoke(null,input, -50,255, output);
		assertEquals(0, b.get(5, 6).doubleValue(),1e-4);
	}

	@Test
	public void stretch() {
		int numFound = BoofTesting.findMethodThenCall(this,"stretch",ImplGrayImageOps.class,"stretch");
		assertEquals(numExpected,numFound);
	}

	public void stretch(Method m) throws InvocationTargetException, IllegalAccessException {
		Class param[] = m.getParameterTypes();
		ImageGray input = GeneralizedImageOps.createSingleBand(param[0], width, height);
		ImageGray output = GeneralizedImageOps.createSingleBand(param[0], width, height);
		GImageMiscOps.fill(input, 23);

		m.invoke(null,input, 2.5,10,255, output);

		GImageGray b = FactoryGImageGray.wrap(output);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if( output.getDataType().isInteger() )
					assertEquals(67, b.get(x, y).doubleValue(),1e-4);
				else
					assertEquals(67.5, b.get(x, y).doubleValue(),1e-4);
			}
		}

		// check to see how well it sets the ceiling
		m.invoke(null,input, 10,28,255, output);
		assertEquals(255, b.get(5, 6).doubleValue(),1e-4);

		// check it flooring to zero
		m.invoke(null,input, -1,2,255, output);
		assertEquals(0, b.get(5, 6).doubleValue(),1e-4);
	}
}
