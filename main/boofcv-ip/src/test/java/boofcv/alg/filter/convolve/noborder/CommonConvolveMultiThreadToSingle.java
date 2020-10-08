/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.convolve.noborder;

import boofcv.BoofTesting;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.GImageStatistics;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.struct.convolve.KernelBase;
import boofcv.struct.image.ImageBase;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public abstract class CommonConvolveMultiThreadToSingle extends BoofStandardJUnit {
	protected int width = 100, height = 90;
	Class targetClass, testClass;
	int totalExpected;
	protected int[] radiuses = new int[]{1, 3};

	protected CommonConvolveMultiThreadToSingle( Class targetClass, Class testClass, int totalExpected ) {
		this.targetClass = targetClass;
		this.testClass = testClass;
		this.totalExpected = totalExpected;
	}

	/**
	 * Compares results to single threaded
	 */
	@Test
	void compareToSingle() {
		Random random = new Random(234);
		for (int i = 0; i < radiuses.length; i++) {
			int count = 0;
			int kernelRadius = radiuses[i];
			int kernelWidth = kernelRadius*2 + 1;
			Method[] methods = targetClass.getMethods();
			for (Method m : methods) {
				String name = m.getName();
				if (!isTestMethod(m))
					continue;

				// look up the test method
				Class[] params = m.getParameterTypes();
				Method testM = BoofTesting.findMethod(testClass, name, params);

				ImageBase input = GeneralizedImageOps.createImage(params[1], width, height, 2);
				ImageBase expected = GeneralizedImageOps.createImage(params[2], width, height, 2);
				ImageBase found = GeneralizedImageOps.createImage(params[2], width, height, 2);

//			System.out.println("Method "+name+" "+input.getImageType()+" radius "+kernelRadius);

				GImageMiscOps.fillUniform(input, random, 0, 200);

				KernelBase ker;
				if (name.equals("convolve")) {
					ker = FactoryKernel.createKernelForImage(kernelWidth, kernelRadius, 2, input.getImageType().getDataType());
					ker = FactoryKernel.random(ker.getClass(), kernelWidth, kernelRadius, 0, 10, random);
				} else {
					ker = FactoryKernel.createKernelForImage(kernelWidth, kernelRadius, 1, input.getImageType().getDataType());
					ker = FactoryKernel.random(ker.getClass(), kernelWidth, kernelRadius, 0, 10, random);
				}

				try {
					Object oe, of;
					if (params.length == 5) {
						oe = testM.invoke(null, ker, input, expected, 5, null);
						of = m.invoke(null, ker, input, found, 5, null);
					} else if (params.length == 4) {
						oe = testM.invoke(null, ker, input, expected, 5);
						of = m.invoke(null, ker, input, found, 5);
					} else {
						oe = testM.invoke(null, ker, input, expected);
						of = m.invoke(null, ker, input, found);
					}
					assertEquals(oe, of);

					// if an unrolled class returns false then its not supported. Check the expected result
					// and skip the check
					if (oe instanceof Boolean) {
						if (!((Boolean)oe)) {
							continue;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					fail("Exception");
				}

				assertNotEquals(0.0, GImageStatistics.sum(expected), 0.1);
				try {
					BoofTesting.assertEquals(expected, found, 1);
				} catch (RuntimeException e) {
					e.printStackTrace();
					System.out.println("Method " + name + " " + input.getImageType() + " radius " + kernelRadius);
					fail("images not identical");
				}
				count++;
			}
			assertEquals(totalExpected, count);
		}
	}

	private boolean isTestMethod( Method m ) {
		String name = m.getName();
		return name.equals("horizontal") || name.equals("vertical") || name.equals("convolve");
	}
}
