/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.filter.convolve.impl;

import gecv.alg.filter.convolve.KernelFactory;
import gecv.core.image.UtilImageFloat32;
import gecv.core.image.UtilImageInt16;
import gecv.core.image.UtilImageInt8;
import gecv.struct.image.*;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static gecv.core.image.GeneralizedImageOps.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * To reduce the amount of code reflects are heavily used.  If any more functions are added reflections should be
 * used to extract them and the appropriate unit test called.  That way its unlikely that anything would be left out.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"SillyAssignment"})
public class TestConvolveImageStandard {

	Random rand = new Random(0xFF);

	int width = 4;
	int height = 5;
	int kernelRadius = 1;

	@Test
	public void horizontal_F32() {
		ImageFloat32 img = new ImageFloat32(width, height);
		UtilImageFloat32.randomize(img, rand, 0, 1);
		ImageFloat32 dest = new ImageFloat32(width, height);

		GecvTesting.checkSubImage(this, "horizontal", true, img, dest);
	}

	@Test
	public void horizontal_I8() {
		ImageInt8 img = new ImageInt8(width, height);
		UtilImageInt8.randomize(img, rand);
		ImageInt16 dest = new ImageInt16(width, height);

		GecvTesting.checkSubImage(this, "horizontal", true, img, dest);
	}

	@Test
	public void horizontal_I8_div() {
		ImageInt8 img = new ImageInt8(width, height);
		UtilImageInt8.randomize(img, rand);
		ImageInt8 dest = new ImageInt8(width, height);

		GecvTesting.checkSubImage(this, "horizontalDiv", true, img, dest);
	}

	@Test
	public void horizontal_I16() {
		ImageInt16 img = new ImageInt16(width, height);
		UtilImageInt16.randomize(img, rand, (short) 0, (short) 100);
		ImageInt16 dest = new ImageInt16(width, height);

		GecvTesting.checkSubImage(this, "horizontal", true, img, dest);
	}

	@Test
	public void vertical_F32() {
		ImageFloat32 img = new ImageFloat32(width, height);
		UtilImageFloat32.randomize(img, rand, 0, 1);
		ImageFloat32 dest = new ImageFloat32(width, height);

		GecvTesting.checkSubImage(this, "vertical", true, img, dest);
	}

	@Test
	public void vertical_I8() {
		ImageInt8 img = new ImageInt8(width, height);
		UtilImageInt8.randomize(img, rand);
		ImageInt16 dest = new ImageInt16(width, height);

		GecvTesting.checkSubImage(this, "vertical", true, img, dest);
	}

	@Test
	public void vertical_I8_div() {
		ImageInt8 img = new ImageInt8(width, height);
		UtilImageInt8.randomize(img, rand);
		ImageInt8 dest = new ImageInt8(width, height);

		GecvTesting.checkSubImage(this, "verticalDiv", true, img, dest);
	}

	@Test
	public void vertical_I16() {
		ImageInt16 img = new ImageInt16(width, height);
		UtilImageInt16.randomize(img, rand, (short) 0, (short) 100);
		ImageInt16 dest = new ImageInt16(width, height);

		GecvTesting.checkSubImage(this, "vertical", true, img, dest);
	}

	@Test
	public void convolve_F32() {
		ImageFloat32 img = new ImageFloat32(width, height);
		UtilImageFloat32.randomize(img, rand, 0, 1);
		ImageFloat32 dest = new ImageFloat32(width, height);
		GecvTesting.checkSubImage(this, "convolve", true, img, dest);
	}

	@Test
	public void convolve_I8() {
		ImageInt8 img = new ImageInt8(width, height);
		UtilImageInt8.randomize(img, rand);
		ImageInt16 dest = new ImageInt16(width, height);
		GecvTesting.checkSubImage(this, "convolve", true, img, dest);
	}

	@Test
	public void convolve_I8_I32() {
		ImageInt8 img = new ImageInt8(width, height);
		UtilImageInt8.randomize(img, rand);
		ImageInt32 dest = new ImageInt32(width, height);
		GecvTesting.checkSubImage(this, "convolve", true, img, dest);
	}

	@Test
	public void convolve_I8_div() {
		ImageInt8 img = new ImageInt8(width, height);
		UtilImageInt8.randomize(img, rand, 0, 50);
		ImageInt8 dest = new ImageInt8(width, height);
		GecvTesting.checkSubImage(this, "convolveDiv", true, img, dest);
	}

	@Test
	public void convolve_I16() {
		ImageInt16 img = new ImageInt16(width, height);
		UtilImageInt16.randomize(img, rand, (short) 0, (short) 100);
		ImageInt16 dest = new ImageInt16(width, height);
		GecvTesting.checkSubImage(this, "convolve", true, img, dest);
	}

	/**
	 * Unit test for horizontal convolution.
	 */
	public void horizontal(ImageBase img, ImageBase dest) {
		Object ker;
		if (isFloatingPoint(img))
			ker = KernelFactory.gaussian1D_F32(kernelRadius, true);
		else
			ker = KernelFactory.gaussian1D_I32(kernelRadius);

		convolve1D("horizontal", ker, img, dest, false);
		// the top border should not be convolved yet
		assertEquals(0, get(dest, 1, 0), 1e-6);

		//  see if some point was convolved correctly
		double val = get(img, 0, 1) * getKernel(ker, 0) + get(img, 1, 1) * getKernel(ker, 1) + get(img, 2, 1) * getKernel(ker, 2);

		assertEquals(val, get(dest, 1, 1), 1e-6);

		// now let it process the vertical border
		convolve1D("horizontal", ker, img, dest, true);
		assertEquals(val, get(dest, 1, 1), 1e-6);
		assertTrue(0 != get(dest, 1, 0));
	}

	/**
	 * Unit test for horizontal convolution with division.
	 */
	public void horizontalDiv(ImageBase img, ImageBase dest) {
		int divisor = 11;
		Object ker;
		if (isFloatingPoint(img))
			ker = KernelFactory.gaussian1D_F32(kernelRadius, true);
		else
			ker = KernelFactory.gaussian1D_I32(kernelRadius);

		convolve1D("horizontal", ker, img, dest, divisor, false);
		// the top border should not be convolved yet
		assertEquals(0, get(dest, 1, 0), 1e-6);

		//  see if some point was convolved correctly
		double val = (get(img, 0, 1) * getKernel(ker, 0) + get(img, 1, 1) * getKernel(ker, 1) + get(img, 2, 1) * getKernel(ker, 2)) / divisor;
		if (!isFloatingPoint(dest))
			val = (int) val;

		assertEquals(val, get(dest, 1, 1), 1e-6);

		// now let it process the vertical border
		convolve1D("horizontal", ker, img, dest, divisor, true);
		assertEquals(val, get(dest, 1, 1), 1e-6);
		assertTrue(0 != get(dest, 1, 0));
	}

	/**
	 * Unit test for vertical convolution.
	 */
	public void vertical(ImageBase img, ImageBase dest) {
		Object ker;
		if (isFloatingPoint(img))
			ker = KernelFactory.gaussian1D_F32(kernelRadius, true);
		else
			ker = KernelFactory.gaussian1D_I32(kernelRadius);

		convolve1D("vertical", ker, img, dest, false);

		// the left border should not be convolved yet
		assertEquals(0, get(dest, 0, 1), 1e-6);

		double val = get(img, 1, 0) * getKernel(ker, 0) + get(img, 1, 1) * getKernel(ker, 1) + get(img, 1, 2) * getKernel(ker, 2);

		assertEquals(val, get(dest, 1, 1), 1e-6);

		// now let it process the vertical border
		convolve1D("vertical", ker, img, dest, true);
		assertEquals(val, get(dest, 1, 1), 1e-6);
		assertTrue(0 != get(dest, 0, 1));
	}

	/**
	 * Unit test for vertical convolution with division.
	 */
	public void verticalDiv(ImageBase img, ImageBase dest) {
		Object ker;
		if (isFloatingPoint(img))
			ker = KernelFactory.gaussian1D_F32(kernelRadius, true);
		else
			ker = KernelFactory.gaussian1D_I32(kernelRadius);

		int divisor = 11;
		convolve1D("vertical", ker, img, dest, divisor, false);

		// the left border should not be convolved yet
		assertEquals(0, get(dest, 0, 1), 1e-6);

		double val = (get(img, 1, 0) * getKernel(ker, 0) + get(img, 1, 1) * getKernel(ker, 1) + get(img, 1, 2) * getKernel(ker, 2)) / divisor;
		if (!isFloatingPoint(dest))
			val = (int) val;

		assertEquals(val, get(dest, 1, 1), 1e-6);

		// now let it process the vertical border
		convolve1D("vertical", ker, img, dest, divisor, true);
		assertEquals(val, get(dest, 1, 1), 1e-6);
		assertTrue(0 != get(dest, 0, 1));
	}

	/**
	 * Unit test for 2D convolution.
	 */
	public void convolve(ImageBase img, ImageBase dest) {
		Object ker;
		if (isFloatingPoint(img))
			ker = KernelFactory.random2D_F32(kernelRadius, 0f, 1f, new Random(234));
		else
			ker = KernelFactory.random2D_I32(kernelRadius, 0, 10, new Random(234));

		int testX = 1;
		int testY = 2;
		double expected = 0;

		// manually perform a convolution
		for (int i = -kernelRadius; i <= kernelRadius; i++) {
			for (int j = -kernelRadius; j <= kernelRadius; j++) {
				expected += getKernel(ker, kernelRadius + i, kernelRadius + j) * get(img, testX + i, testY + j);
			}
		}

		convolve2D(ker, img, dest);

		// is the test point the same as the expected?
		assertEquals(expected, get(dest, testX, testY), 1e-5);

		// the border should be zero
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				if (i < kernelRadius || j < kernelRadius)
					assertEquals(0f, get(dest, j, i), 1e-6);
			}
		}
	}

	/**
	 * Unit test for 2D convolution with division.
	 */
	public void convolveDiv(ImageBase img, ImageBase dest) {
		Object ker;
		if (isFloatingPoint(img))
			ker = KernelFactory.random2D_F32(kernelRadius, 0f, 1f, new Random(234));
		else
			ker = KernelFactory.random2D_I32(kernelRadius, 0, 4, new Random(234));

		int divisor = 11;
		int testX = 1;
		int testY = 2;
		double expected = 0;

		// manually perform a convolution
		for (int i = -kernelRadius; i <= kernelRadius; i++) {
			for (int j = -kernelRadius; j <= kernelRadius; j++) {
				expected += getKernel(ker, kernelRadius + i, kernelRadius + j) * get(img, testX + i, testY + j);
			}
		}
		expected /= divisor;
		if (!isFloatingPoint(dest))
			expected = (int) expected;

		convolve2D(ker, img, dest, divisor);

		// is the test point the same as the expected?
		assertEquals(expected, get(dest, testX, testY), 1e-5);

		// the border should be zero
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				if (i < kernelRadius || j < kernelRadius)
					assertEquals(0f, get(dest, j, i), 1e-6);
			}
		}
	}

	private double getKernel(Object ker, int index) {
		try {
			Field f = ker.getClass().getField("data");
			Object o = f.get(ker);
			Number n = (Number) Array.get(o, index);
			return n.doubleValue();
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private double getKernel(Object ker, int x, int y) {
		try {
			Field fw = ker.getClass().getField("width");
			int width = (Integer) fw.get(ker);
			Field f = ker.getClass().getField("data");
			Object o = f.get(ker);
			Number n = (Number) Array.get(o, y * width + x);
			return n.doubleValue();
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private void convolve1D(String name, Object kernel, ImageBase input, ImageBase output, boolean border) {
		try {
			Method m = ConvolveImageStandard.class.getMethod("" +
					name, kernel.getClass(), input.getClass(), output.getClass(), boolean.class);
			m.invoke(null, kernel, input, output, border);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private void convolve1D(String name, Object kernel, ImageBase input, ImageBase output, int divisor, boolean border) {
		try {
			Method m = ConvolveImageStandard.class.getMethod("" +
					name, kernel.getClass(), input.getClass(), output.getClass(), int.class, boolean.class);
			m.invoke(null, kernel, input, output, divisor, border);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private void convolve2D(Object kernel, ImageBase input, ImageBase output) {
		try {
			Method m = ConvolveImageStandard.class.getMethod("convolve", kernel.getClass(), input.getClass(), output.getClass());
			m.invoke(null, kernel, input, output);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private void convolve2D(Object kernel, ImageBase input, ImageBase output, int divisor) {
		try {
			Method m = ConvolveImageStandard.class.getMethod("convolve", kernel.getClass(), input.getClass(), output.getClass(), int.class);
			m.invoke(null, kernel, input, output, divisor);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

}
