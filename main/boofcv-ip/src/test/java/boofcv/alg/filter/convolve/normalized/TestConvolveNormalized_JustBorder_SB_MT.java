/*
 * Copyright (c) 2025, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.convolve.normalized;

import boofcv.BoofTesting;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.struct.convolve.KernelBase;
import boofcv.struct.image.ImageBase;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;
import pabeles.concurrency.GrowArray;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class TestConvolveNormalized_JustBorder_SB_MT extends BoofStandardJUnit {
	int width = 100, height = 90;

	/// Compares results to single threaded
	@Test void compareToSingle() {
		int count = 0;
		Method[] methods = ConvolveNormalized_JustBorder_SB_MT.class.getMethods();
		for (Method m : methods) {
			String name = m.getName();
			if (!(name.startsWith("horizontal") || name.startsWith("vertical") || name.startsWith("convolve")))
				continue;

			// look up the test method
			Class[] params = m.getParameterTypes();
			Method testM = BoofTesting.findMethod(ConvolveNormalized_JustBorder_SB.class, name, params);

			boolean hasWorkspace = params[params.length - 1].isAssignableFrom(GrowArray.class);

			try {
				KernelBase kernel1, kernel2 = null;
				ImageBase input, expected, found;
				if (params.length == 3 || hasWorkspace) {
					kernel1 = FactoryKernel.random((Class)params[0], 2, 1, 10, rand);
					input = GeneralizedImageOps.createImage(params[1], width, height, 2);
					expected = GeneralizedImageOps.createImage(params[2], width, height, 2);
					found = GeneralizedImageOps.createImage(params[2], width, height, 2);
				} else {
					kernel1 = FactoryKernel.random((Class)params[0], 2, 1, 10, rand);
					kernel2 = FactoryKernel.random((Class)params[1], 2, 1, 10, rand);
					input = GeneralizedImageOps.createImage(params[2], width, height, 2);
					expected = GeneralizedImageOps.createImage(params[3], width, height, 2);
					found = GeneralizedImageOps.createImage(params[3], width, height, 2);
				}

				GImageMiscOps.fillUniform(input, rand, 0, 200);

				if (params.length == 3) {
					testM.invoke(null, kernel1, input, expected);
					m.invoke(null, kernel1, input, found);
				} else if (hasWorkspace) {
					testM.invoke(null, kernel1, input, expected, null);
					m.invoke(null, kernel1, input, found, null);
				} else {
					testM.invoke(null, kernel1, kernel2, input, expected);
					m.invoke(null, kernel1, kernel2, input, found);
				}

				BoofTesting.assertEquals(expected, found, 1);
				count++;
			} catch (Exception e) {
				e.printStackTrace();
				fail("Exception: method=" + m);
			}
		}
		assertEquals(20, count);
	}
}

