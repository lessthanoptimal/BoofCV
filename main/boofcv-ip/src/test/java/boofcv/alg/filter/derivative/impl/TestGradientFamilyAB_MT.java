/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.derivative.impl;

import boofcv.BoofTesting;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.GImageStatistics;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageBase;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"rawtypes", "unchecked"})
class TestGradientFamilyAB_MT extends BoofStandardJUnit {
	protected int width = 100, height = 90;

	Class targetClass = GradientFamilyAB_MT.class;
	Class testClass = GradientFamilyAB.class;

	int a = 3;
	int b = 10;

	@Test void compareToSingle() {
		Random random = new Random(234);
		int count = 0;
		Method[] methods = targetClass.getMethods();
		for (Method m : methods) {
			String name = m.getName();
			if (!isTestMethod(m))
				continue;

			// look up the test method
			Class[] params = m.getParameterTypes();
			Method testM = BoofTesting.findMethod(testClass, name, params);

			ImageBase input = GeneralizedImageOps.createImage(params[0], width, height, 2);
			ImageBase expectedX = GeneralizedImageOps.createImage(params[3], width, height, 2);
			ImageBase foundX = GeneralizedImageOps.createImage(params[3], width, height, 2);
			ImageBase expectedY = GeneralizedImageOps.createImage(params[4], width, height, 2);
			ImageBase foundY = GeneralizedImageOps.createImage(params[4], width, height, 2);

//			System.out.println("Method " + name + " " + input.getImageType());

			GImageMiscOps.fillUniform(input, random, 0, 200);

			try {
				Object oe, of;
				if (input.getImageType().getDataType().isInteger()) {
					oe = testM.invoke(null, input, a, b, expectedX, expectedY);
					of = m.invoke(null, input, a, b, foundX, foundY);
				} else {
					oe = testM.invoke(null, input, (float)a, (float)b, expectedX, expectedY);
					of = m.invoke(null, input, (float)a, (float)b, foundX, foundY);
				}
				assertEquals(oe, of);
			} catch (Exception e) {
				e.printStackTrace();
				fail("Exception");
			}

			assertNotEquals(0.0, GImageStatistics.sum(expectedX), 0.1);
			BoofTesting.assertEquals(expectedX, foundX, 1);
			BoofTesting.assertEquals(expectedY, foundY, 1);
			count++;
		}

		assertEquals(3, count);
	}

	private boolean isTestMethod( Method m ) {
		String name = m.getName();
		return name.startsWith("process");
	}
}

