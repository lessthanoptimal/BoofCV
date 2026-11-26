/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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
import boofcv.concurrency.BoofConcurrency;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import org.ddogleg.struct.DogArray;
import pabeles.concurrency.GrowArray;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestImplConvolveBox_MT extends BoofStandardJUnit {
	// several VERTICAL_RESTART blocks
	int width = 100, height = 300;

	/// Results must not depend on the number of threads
	@Test void compareToSingle_threads() throws Exception {
		int original = BoofConcurrency.getMaxThreads();
		try {
			for (int threads : new int[]{1, 3, 7}) {
				BoofConcurrency.setMaxThreads(threads);
				compareToSingle();
			}
		} finally {
			BoofConcurrency.setMaxThreads(original);
		}
	}

	/// Compares results to single threaded
	@Test void compareToSingle() throws Exception {
		int count = 0;
		for (Method m : ImplConvolveBox_MT.class.getMethods()) {
			String name = m.getName();
			if (!name.equals("horizontal") && !name.equals("vertical"))
				continue;
			Class[] params = m.getParameterTypes();
			Method single = ImplConvolveBox.class.getMethod(name, name.equals("horizontal") ? params :
					new Class[]{params[0], params[1], int.class, DogArray.class});

			ImageGray input = GeneralizedImageOps.createSingleBand(params[0], width, height);
			ImageGray expected = GeneralizedImageOps.createSingleBand(params[1], width, height);
			ImageGray found = GeneralizedImageOps.createSingleBand(params[1], width, height);
			GImageMiscOps.fillUniform(input, rand, 0, 200);

			single.invoke(null, TestImplConvolveBox.arguments(single, input, expected, 8));
			if (name.equals("horizontal")) {
				m.invoke(null, input, found, 8);
			} else {
				Type rowsType = ((ParameterizedType)m.getGenericParameterTypes()[3]).getActualTypeArguments()[0];
				m.invoke(null, input, found, 8, new GrowArray<>(() -> TestImplConvolveBox.createRows(rowsType)));
			}

			BoofTesting.assertEquals(expected, found, 0);
			count++;
		}
		assertEquals(14, count);
	}
}