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

package boofcv.alg.filter.misc.impl;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayF64;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SuppressWarnings({"rawtypes", "unchecked"})
public class TestImplAverageDownSample extends BoofStandardJUnit {

	public static List<Method> find( String name ) {
		var ret = new ArrayList<Method>();

		Method[] methods = ImplAverageDownSample.class.getMethods();

		for (Method m : methods) {
			if (m.getName().equals(name)) {
				ret.add(m);
			}
		}

		// Ensure the order is constant. Debug and run modes were returning different results
		Collections.sort(ret, Comparator.comparing(Method::toString));

		return ret;
	}

	// Compares to the naive implementation
	@Test void horizontal_naive() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
		List<Method> methods = find("horizontal");
		assertFalse(methods.isEmpty());

		for (Method m : methods) {
			Class typeSrc = m.getParameterTypes()[0];
			Class typeDst = m.getParameterTypes()[2];

			Method mTest = ImplAverageDownSample.class.getDeclaredMethod("naivePixelHorizontal", typeSrc,
					float.class, float.class, int.class, int.class);

			for (int i = 0; i < 25; i++) {
				// Randomize the width of the source image
				ImageGray src = GeneralizedImageOps.createSingleBand(typeSrc, 20 + rand.nextInt(3), 5);

				// randomly scale the width for output image
				int width = (int)(src.width/(1.2 + rand.nextDouble()) - 0.5);
				ImageGray dst = GeneralizedImageOps.createSingleBand(typeDst, width, src.height);

				// Fill source image with random values
				GImageMiscOps.fillUniform(src, rand, 0, 200);

				for (boolean centered : new boolean[]{true, false}) {
					m.invoke(null, src, centered, dst);

					float scale = src.width/(float)dst.width;
					float offset = centered ? -(scale - 1.0f)/2.0f : 0.0f;

					for (int y = 0; y < dst.height; y++) {
						for (int x = 0; x < dst.width; x++) {
							var expected = (Number)mTest.invoke(null, src, offset, scale, x, y);
							var found = GeneralizedImageOps.get(dst, x, y);
							assertEquals(expected.doubleValue(), found, 1e-3, "pixel: " + x + " " + y);
						}
					}
				}
			}
		}

		assertEquals(4, methods.size());
	}

	// Compares to the naive implementation
	@Test void vertical_naive() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
		List<Method> methods = find("vertical");
		assertFalse(methods.isEmpty());

		for (Method m : methods) {
			// Skip over ones where the workspace is provided
			if (m.getParameterTypes().length != 3)
				continue;
			Class typeSrc = m.getParameterTypes()[0];
			Class typeDst = m.getParameterTypes()[2];

			Method mTest = ImplAverageDownSample.class.getDeclaredMethod("naivePixelVertical", typeSrc,
					float.class, float.class, int.class, int.class);

			for (int i = 0; i < 25; i++) {
				// Randomize the width of the source image
				ImageGray src = GeneralizedImageOps.createSingleBand(typeSrc, 5, 20 + rand.nextInt(3));

				// randomly scale the width for output image
				int height = (int)(src.height/(1.2 + rand.nextDouble()) - 0.5);
				ImageGray dst = GeneralizedImageOps.createSingleBand(typeDst, src.width, height);

				// Fill source image with random values
				GImageMiscOps.fillUniform(src, rand, 0, 200);

				for (boolean centered : new boolean[]{true, false}) {
					m.invoke(null, src, centered, dst);

					float scale = src.height/(float)dst.height;
					float offset = centered ? -(scale - 1.0f)/2.0f : 0.0f;

					for (int y = 0; y < dst.height; y++) {
						for (int x = 0; x < dst.width; x++) {
							var expected = (Number)mTest.invoke(null, src, offset, scale, x, y);
							var found = GeneralizedImageOps.get(dst, x, y);
							if (dst.getDataType().isInteger()) {
								assertEquals(Math.round(expected.doubleValue()), found, 1e-3, "pixel: " + x + " " + y);
							} else {
								assertEquals(expected.doubleValue(), found, 1e-3, "pixel: " + x + " " + y);
							}
						}
					}
				}
			}
		}
	}

	@Test void naivePixelHorizontal() throws InvocationTargetException, IllegalAccessException {
		List<Method> methods = find("naivePixelHorizontal");
		assertFalse(methods.isEmpty());

		for (Method m : methods) {
			Class typeSrc = m.getParameterTypes()[0];

			// skip double because it has a different header
			if (typeSrc.isAssignableFrom(GrayF64.class))
				continue;

			ImageGray src = GeneralizedImageOps.createSingleBand(typeSrc, 4, 5);

			for (int row = 0; row < src.height; row++) {
				for (int col = 0; col < src.width; col++) {
					GeneralizedImageOps.set(src, col, row, col + row);
				}
			}

			assertEquals(0.4737f, (float)m.invoke(null, src, -0.1f, 2.0f, 0, 0), 1e-3f);
			assertEquals(4.4000f, (float)m.invoke(null, src, -0.1f, 2.0f, 1, 2), 1e-3f);
			assertEquals(5.0f, (float)m.invoke(null, src, -0.1f, 2.0f, 2, 2), 1e-3f);
			assertEquals(0.5f, (float)m.invoke(null, src, 0.0f, 2.0f, 0, 0), 1e-3f);
			assertEquals(4.5f, (float)m.invoke(null, src, 0.0f, 2.0f, 1, 2), 1e-3f);
		}
	}

	@Test void naivePixelVertical() throws InvocationTargetException, IllegalAccessException {
		List<Method> methods = find("naivePixelVertical");
		assertFalse(methods.isEmpty());

		for (Method m : methods) {
			Class typeSrc = m.getParameterTypes()[0];
			ImageGray src = GeneralizedImageOps.createSingleBand(typeSrc, 4, 5);

			// skip double because it has a different header
			if (typeSrc.isAssignableFrom(GrayF64.class))
				continue;

			for (int row = 0; row < src.height; row++) {
				for (int col = 0; col < src.width; col++) {
					GeneralizedImageOps.set(src, col, row, col + row);
				}
			}

			assertEquals(0.4737f, (float)m.invoke(null, src, -0.1f, 2.0f, 0, 0), 1e-3f);
			assertEquals(3.4f, (float)m.invoke(null, src, -0.1f, 2.0f, 1, 1), 1e-3f);
			assertEquals(4.4f, (float)m.invoke(null, src, -0.1f, 2.0f, 2, 1), 1e-3f);
			assertEquals(0.5f, (float)m.invoke(null, src, 0.0f, 2.0f, 0, 0), 1e-3f);
			assertEquals(4.5f, (float)m.invoke(null, src, 0.0f, 2.0f, 2, 1), 1e-3f);
		}
	}
}
