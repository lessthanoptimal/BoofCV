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
import boofcv.alg.filter.convolve.border.ConvolveJustBorder_General_SB;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.ImageBorderValue;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayF64;
import boofcv.struct.image.ImageGray;
import boofcv.testing.CompareEquivalentFunctions;
import org.ddogleg.struct.DogArray;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"rawtypes", "unchecked"})
public class TestImplConvolveBox extends CompareEquivalentFunctions {

	public TestImplConvolveBox() {
		super(ImplConvolveBox.class, ConvolveImageStandard_SB.class);
	}

	/// The whole image, border included, against ConvolveJustBorder_General_SB with a zero border for the border and
	/// ConvolveImageStandard_SB for the inner image, like ConvolveImage
	@Test void compareToStandard() {
		performTests(14);
	}

	/// Image shapes and radii, including borders that overlap and images with several VERTICAL_RESTART blocks
	static int[][] borderCases = {{10, 12, 2}, {31, 9, 1}, {9, 31, 4}, {5, 5, 2}, {40, 30, 4}, {20, 200, 2}};

	/// Floating point vertical() restarts its running sums every VERTICAL_RESTART rows, so processing an image in two
	/// blocks split at a restart row gives the same image as processing it at once
	@Test void floatRestartContract() throws Exception {
		int count = 0;
		for (Method m : ImplConvolveBox.class.getMethods()) {
			Class[] params = m.getParameterTypes();
			if (!m.getName().equals("vertical") || params.length != 6 || !(params[0] == GrayF32.class || params[0] == GrayF64.class))
				continue;
			int width = 20, height = 200, radius = 2;
			ImageGray input = GeneralizedImageOps.createSingleBand(params[0], width, height);
			ImageGray expected = GeneralizedImageOps.createSingleBand(params[1], width, height);
			ImageGray found = GeneralizedImageOps.createSingleBand(params[1], width, height);
			GImageMiscOps.fillUniform(input, rand, 0, 100);

			int blocks = ImplConvolveBox.restartBlocks(height, 2*radius + 1);
			assertTrue(blocks > 2);
			int middle = ImplConvolveBox.restartRow(2, blocks, height, radius);

			Object rows = createRows(m.getGenericParameterTypes()[5]);
			m.invoke(null, input, expected, radius, 0, height, rows);
			m.invoke(null, input, found, radius, 0, middle, rows);
			m.invoke(null, input, found, radius, middle, height, rows);
			BoofTesting.assertEquals(expected, found, 0);
			count++;
		}
		assertEquals(2, count);
	}

	@Test void restartBlocks() {
		// length - 1 rows are border, then blocks of VERTICAL_RESTART inner rows
		int R = ImplConvolveBox.VERTICAL_RESTART;
		assertEquals(1, ImplConvolveBox.restartBlocks(10, 5));
		assertEquals(1, ImplConvolveBox.restartBlocks(4 + R, 5));
		assertEquals(2, ImplConvolveBox.restartBlocks(5 + R, 5));
		// image smaller than the kernel
		assertEquals(1, ImplConvolveBox.restartBlocks(3, 5));
	}

	@Test void restartRow() {
		int R = ImplConvolveBox.VERTICAL_RESTART;
		int height = 4 + 3*R;
		assertEquals(0, ImplConvolveBox.restartRow(0, 3, height, 2));
		assertEquals(2 + R, ImplConvolveBox.restartRow(1, 3, height, 2));
		assertEquals(2 + 2*R, ImplConvolveBox.restartRow(2, 3, height, 2));
		assertEquals(height, ImplConvolveBox.restartRow(3, 3, height, 2));
	}

	/// horizontal() or vertical() of the whole image
	static boolean isWholeImage( Method m ) {
		int length = m.getParameterCount();
		return (m.getName().equals("horizontal") && length == 3) || (m.getName().equals("vertical") && length == 4);
	}

	/// Arguments of a whole image function, with new row buffers for vertical()
	static Object[] arguments( Method m, ImageGray input, ImageGray output, int radius ) {
		if (m.getParameterCount() == 3)
			return new Object[]{input, output, radius};
		return new Object[]{input, output, radius, createRows(m.getGenericParameterTypes()[3])};
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

	@Override
	protected boolean isTestMethod( Method m ) {
		return isWholeImage(m);
	}

	@Override
	protected boolean isEquivalent( Method candidate, Method validation ) {

		Class<?>[] v = candidate.getParameterTypes();
		Class<?>[] c = validation.getParameterTypes();

		if (v.length != 3)
			return false;

		if (!candidate.getName().equals(validation.getName()))
			return false;

		return c[0] == v[1] && c[1] == v[2];
	}

	@Override
	protected Object[][] createInputParam( Method candidate, Method validation ) {
		Class[] c = candidate.getParameterTypes();
		Object[][] ret = new Object[borderCases.length][];
		for (int i = 0; i < borderCases.length; i++) {
			int[] shape = borderCases[i];
			ImageGray input = GeneralizedImageOps.createSingleBand(c[0], shape[0], shape[1]);
			ImageGray output = GeneralizedImageOps.createSingleBand(c[1], shape[0], shape[1]);
			GImageMiscOps.fillUniform(input, rand, 0, 20);
			ret[i] = arguments(candidate, input, output, shape[2]);
		}
		return ret;
	}

	@Override
	protected Object[] reformatForValidation( Method m, Object[] targetParam ) {
		ImageGray input = (ImageGray)targetParam[0];
		ImageGray expected = (ImageGray)((ImageGray)targetParam[1]).createSameShape();
		int radius = (Integer)targetParam[2];

		Object kernel = tableKernel(input, radius, 2*radius + 1, false);

		// The border. ConvolveImageStandard_SB fills in the inner image
		ImageBorder border = ImageBorderValue.wrap(input, 0);
		try {
			findMethod(ConvolveJustBorder_General_SB.class, m.getName(), kernel, border, expected)
					.invoke(null, kernel, border, expected);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
		return new Object[]{kernel, input, expected};
	}

	@Override
	protected void compareResults( Object targetResult, Object[] targetParam, Object validationResult, Object[] validationParam ) {
		BoofTesting.assertEquals((ImageGray)validationParam[2], (ImageGray)targetParam[1], 1e-4);
	}

	/// Kernel of ones, or of 1/length for floating point if normalized, for ConvolveImage functions with this input
	public static Object tableKernel( ImageGray<?> input, int offset, int length, boolean normalized ) {
		if (input instanceof GrayF32)
			return FactoryKernel.table1D_F32(offset, length, normalized);
		if (input instanceof GrayF64)
			return FactoryKernel.table1D_F64(offset, length, normalized);
		return FactoryKernel.table1D_S32(offset, length);
	}
}
