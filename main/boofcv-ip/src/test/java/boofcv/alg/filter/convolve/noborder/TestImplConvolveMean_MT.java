/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray;
import org.junit.jupiter.api.Test;
import pabeles.concurrency.GrowArray;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestImplConvolveMean_MT extends BoofStandardJUnit {
	// several VERTICAL_RESTART blocks
	int width = 100, height = 300;

	/// Results must not depend on the number of threads
	@Test void compareToSingle_threads() throws Exception {
		int original = BoofConcurrency.getMaxThreads();
		try {
			for (int threads : new int[]{1, 3, 7}) {
				BoofConcurrency.setMaxThreads(threads);
				compareToSingle();
				compareToSingleInner();
			}
		} finally {
			BoofConcurrency.setMaxThreads(original);
		}
	}

	/// Compares results to single threaded, with and without an ImageBorder
	private void compareToSingle() throws Exception {
		int count = 0;
		for (Method m : ImplConvolveMean_MT.class.getMethods()) {
			String name = m.getName();
			if (!name.equals("horizontal") && !name.equals("vertical"))
				continue;
			Class[] params = m.getParameterTypes();
			Method single = ImplConvolveMean.class.getMethod(name, name.equals("horizontal") ? params :
					new Class[]{params[0], params[1], int.class, int.class, params[4], DogArray.class});

			ImageGray input = GeneralizedImageOps.createSingleBand(params[0], width, height);
			double maxValue = input.getDataType().isInteger() ? input.getDataType().getMaxValue() : 200;
			GImageMiscOps.fillUniform(input, rand, 0, maxValue);

			for (boolean useBorder : new boolean[]{false, true}) {
				ImageBorder border = useBorder ? FactoryImageBorder.generic(BorderType.REFLECT, input.getImageType()) : null;
				if (border != null)
					border.setImage(input);
				ImageGray expected = GeneralizedImageOps.createSingleBand(params[1], width, height);
				ImageGray found = GeneralizedImageOps.createSingleBand(params[1], width, height);

				single.invoke(null, TestImplConvolveMean.arguments(single, input, expected, 4, 9, border));
				if (name.equals("horizontal")) {
					m.invoke(null, input, found, 4, 9, border);
				} else {
					Type rowsType = ((ParameterizedType)m.getGenericParameterTypes()[5]).getActualTypeArguments()[0];
					m.invoke(null, input, found, 4, 9, border, new GrowArray<>(() -> TestImplConvolveMean.createRows(rowsType)));
				}
				BoofTesting.assertEquals(expected, found, 0);
			}
			count++;
		}
		assertEquals(10, count);
	}

	/// horizontalInner() and verticalInner() compared to single threaded
	private void compareToSingleInner() throws Exception {
		int count = 0;
		for (Method m : ImplConvolveMean_MT.class.getMethods()) {
			String name = m.getName();
			if (!name.equals("horizontalInner") && !name.equals("verticalInner"))
				continue;
			boolean horizontal = name.equals("horizontalInner");
			Class[] params = m.getParameterTypes();
			Method single = ImplConvolveMean.class.getMethod(name, horizontal ? params :
					new Class[]{params[0], params[1], int.class, int.class, DogArray.class});

			ImageGray input = GeneralizedImageOps.createSingleBand(params[0], width, height);
			GImageMiscOps.fillUniform(input, rand, 0, 200);
			ImageGray expected = GeneralizedImageOps.createSingleBand(params[1], width, height);
			ImageGray found = GeneralizedImageOps.createSingleBand(params[1], width, height);

			if (horizontal) {
				single.invoke(null, input, expected, 4, 9);
				m.invoke(null, input, found, 4, 9);
			} else {
				single.invoke(null, input, expected, 4, 9, TestImplConvolveMean.createRows(single.getGenericParameterTypes()[4]));
				Type rowsType = ((ParameterizedType)m.getGenericParameterTypes()[4]).getActualTypeArguments()[0];
				m.invoke(null, input, found, 4, 9, new GrowArray<>(() -> TestImplConvolveMean.createRows(rowsType)));
			}
			BoofTesting.assertEquals(expected, found, 0);
			count++;
		}
		assertEquals(10, count);
	}
}
