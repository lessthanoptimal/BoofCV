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
import boofcv.alg.filter.convolve.ConvolveImageNormalized;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayF64;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestImplConvolveMean extends BoofStandardJUnit {
	/// width, height, offset, length. Includes kernels as large as the image, images with several VERTICAL_RESTART
	/// blocks, and a kernel too long for U8's multiply and shift division
	static int[][] cases = {
			{10, 12, 2, 5}, {10, 12, 0, 5}, {10, 12, 4, 5}, {10, 12, 1, 4}, {31, 9, 3, 7}, {9, 31, 3, 7},
			{5, 6, 3, 7}, {40, 30, 4, 9}, {20, 200, 2, 5}, {1010, 4, 500, 1001}, {4, 1010, 500, 1001}};

	/// The whole image against ConvolveImageNormalized, with and without an ImageBorder
	@Test void compareToStandard() throws Exception {
		int count = 0;
		for (Method m : ImplConvolveMean.class.getMethods()) {
			if (!isWholeImage(m))
				continue;
			Class[] params = m.getParameterTypes();
			for (int[] c : cases) {
				// the long kernel is for integer division. Float sums of 1000 pixels differ by more than the tolerance
				if (c[3] > 100 && (params[0] == GrayF32.class || params[0] == GrayF64.class))
					continue;
				ImageGray input = GeneralizedImageOps.createSingleBand(params[0], c[0], c[1]);
				// make sure the bitwise operators do something
				double maxValue = input.getDataType().isInteger() ? input.getDataType().getMaxValue() : 100;
				GImageMiscOps.fillUniform(input, rand, 0, maxValue);
				for (boolean useBorder : new boolean[]{false, true}) {
					ImageBorder border = useBorder ? FactoryImageBorder.generic(BorderType.REFLECT, input.getImageType()) : null;
					if (border != null)
						border.setImage(input);

					ImageGray expected = GeneralizedImageOps.createSingleBand(params[1], c[0], c[1]);
					ImageGray found = GeneralizedImageOps.createSingleBand(params[1], c[0], c[1]);
					Object kernel = TestImplConvolveBox.tableKernel(input, c[2], c[3], true);
					if (border == null)
						findMethod(ConvolveImageNormalized.class, m.getName(), kernel, input, expected).
								invoke(null, kernel, input, expected);
					else
						findMethod(ConvolveImageNormalized.class, m.getName(), kernel, input, expected, border).
								invoke(null, kernel, input, expected, border);

					m.invoke(null, arguments(m, input, found, c[2], c[3], border));
					BoofTesting.assertEquals(expected, found, input.getDataType().isInteger() ? 0 : 1e-4);
				}
			}
			count++;
		}
		assertEquals(10, count);
	}

	/// horizontalInner() and verticalInner() match horizontal() and vertical() inside and leave the border unchanged
	@Test void inner() throws Exception {
		int count = 0;
		for (Method m : ImplConvolveMean.class.getMethods()) {
			boolean horizontal = m.getName().equals("horizontalInner") && m.getParameterCount() == 4;
			if (!horizontal && !(m.getName().equals("verticalInner") && m.getParameterCount() == 5))
				continue;
			Class[] params = m.getParameterTypes();
			Method whole = null;
			for (Method w : ImplConvolveMean.class.getMethods()) {
				if (isWholeImage(w) && w.getName().equals(horizontal ? "horizontal" : "vertical") &&
						w.getParameterTypes()[0] == params[0] && w.getParameterTypes()[1] == params[1])
					whole = w;
			}
			for (int[] c : cases) {
				int width = c[0], height = c[1], offset = c[2], length = c[3];
				ImageGray input = GeneralizedImageOps.createSingleBand(params[0], width, height);
				GImageMiscOps.fillUniform(input, rand, 0, 100);
				ImageGray expected = GeneralizedImageOps.createSingleBand(params[1], width, height);
				ImageGray found = GeneralizedImageOps.createSingleBand(params[1], width, height);
				GImageMiscOps.fill(found, 3);

				whole.invoke(null, arguments(whole, input, expected, offset, length, null));
				if (horizontal)
					m.invoke(null, input, found, offset, length);
				else
					m.invoke(null, input, found, offset, length, createRows(m.getGenericParameterTypes()[4]));

				int offsetR = length - offset - 1;
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						int i = horizontal ? x : y, size = horizontal ? width : height;
						boolean inside = length <= size && i >= offset && i < size - offsetR;
						double value = inside ? GeneralizedImageOps.get(expected, x, y) : 3;
						assertEquals(value, GeneralizedImageOps.get(found, x, y), 0.0);
					}
				}
			}
			count++;
		}
		assertEquals(10, count);
	}

	/// Floating point vertical() restarts its running sums every VERTICAL_RESTART rows, so processing an image in two
	/// blocks split at a restart row gives the same image as processing it at once
	@Test void floatRestartContract() throws Exception {
		int count = 0;
		for (Method m : ImplConvolveMean.class.getMethods()) {
			Class[] params = m.getParameterTypes();
			if (!m.getName().equals("vertical") || params.length != 8 || !(params[0] == GrayF32.class || params[0] == GrayF64.class))
				continue;
			int width = 20, height = 200, offset = 2, length = 5;
			ImageGray input = GeneralizedImageOps.createSingleBand(params[0], width, height);
			ImageGray expected = GeneralizedImageOps.createSingleBand(params[1], width, height);
			ImageGray found = GeneralizedImageOps.createSingleBand(params[1], width, height);
			GImageMiscOps.fillUniform(input, rand, 0, 100);

			int blocks = ImplConvolveBox.restartBlocks(height, length);
			assertTrue(blocks > 2);
			int middle = ImplConvolveBox.restartRow(2, blocks, height, offset);

			Object rows = createRows(m.getGenericParameterTypes()[7]);
			m.invoke(null, input, expected, offset, length, null, 0, height, rows);
			m.invoke(null, input, found, offset, length, null, 0, middle, rows);
			m.invoke(null, input, found, offset, length, null, middle, height, rows);
			BoofTesting.assertEquals(expected, found, 0);
			count++;
		}
		assertEquals(2, count);
	}

	/// horizontal() or vertical() of the whole image
	static boolean isWholeImage( Method m ) {
		int length = m.getParameterCount();
		return (m.getName().equals("horizontal") && length == 5) || (m.getName().equals("vertical") && length == 6);
	}

	/// Arguments of a whole image function, with new row buffers for vertical()
	static Object[] arguments( Method m, ImageGray input, ImageGray output, int offset, int length,
							   @Nullable ImageBorder border ) {
		if (m.getParameterCount() == 5)
			return new Object[]{input, output, offset, length, border};
		return new Object[]{input, output, offset, length, border, createRows(m.getGenericParameterTypes()[5])};
	}

	/// DogArray<DogArray_X> for the parameter type
	static DogArray<?> createRows( Type type ) {
		Class<?> element = (Class<?>)((ParameterizedType)type).getActualTypeArguments()[0];
		return new DogArray<>(() -> {
			try {
				return element.getConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		});
	}

	/// Static method with this name whose parameters accept the arguments
	static Method findMethod( Class<?> owner, String name, Object... args ) {
		escape:
		for (Method m : owner.getMethods()) {
			Class<?>[] p = m.getParameterTypes();
			if (!m.getName().equals(name) || p.length != args.length)
				continue;
			for (int i = 0; i < p.length; i++) {
				if (!p[i].isAssignableFrom(args[i].getClass()))
					continue escape;
			}
			return m;
		}
		throw new RuntimeException("No " + name + " in " + owner.getSimpleName());
	}
}
