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

package gecv.alg.filter.convolve;

import gecv.alg.filter.convolve.impl.ConvolveImageStandard;
import gecv.core.image.GeneralizedImageOps;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.convolve.Kernel2D_I32;
import gecv.struct.image.ImageBase;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestConvolveImage {

	Random rand = new Random(0xFF);

	int width = 4;
	int height = 5;
	int kernelRadius = 1;

	/**
	 * Compares the output to equivalent functions in {@link gecv.alg.filter.convolve.impl.ConvolveImageStandard}.
	 */
	@Test
	public void compareToStandard() {
		Method methods[] = ConvolveImage.class.getMethods();

		// sanity check to make sure the funcitons are being found
		int numFound = 0;
		for (Method m : methods) {
			// search for methods which have equivalent in the other class
			Class<?> paramTypes[] = m.getParameterTypes();
			if (paramTypes.length < 3)
				continue;

			Method equiv = findEquivalent(m);
			if (equiv == null)
				continue;

			checkMethod(m, equiv, width, height, kernelRadius, rand);
			numFound++;
		}

		// update this as needed when new functions are added
		assertEquals(14, numFound);
	}

	/**
	 * Compare the specified function against the function in ConvolveImageStandard.
	 *
	 * @param target	   The method being tested
	 * @param standardName Name of the function in ConvolveImageStandard
	 * @param width		width of the images being tested
	 * @param height	   height of the images being tested
	 * @param kernelRadius
	 * @param rand		 used to create random images
	 */
	public static void checkAgainstStandard(Method target, String standardName,
											int width, int height, int kernelRadius, Random rand) {
		try {
			Method check = ConvolveImageStandard.class.getMethod(standardName, target.getParameterTypes());
			checkMethod(target, check, width, height, kernelRadius, rand);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	protected static void checkMethod(Method target, Method check,
									  int width, int height, int kernelRadius, Random rand) {
		Class<?> paramTypes[] = target.getParameterTypes();

		Object kernel;
		if (Kernel1D_F32.class == paramTypes[0]) {
			kernel = KernelFactory.random1D_F32(kernelRadius, -1, 1, rand);
		} else if (Kernel1D_I32.class == paramTypes[0]) {
			kernel = KernelFactory.random1D_I32(kernelRadius, 0, 5, rand);
		} else if (Kernel2D_I32.class == paramTypes[0]) {
			kernel = KernelFactory.random2D_I32(kernelRadius, -1, 1, rand);
		} else if (Kernel2D_F32.class == paramTypes[0]) {
			kernel = KernelFactory.random2D_F32(kernelRadius, 0, 5, rand);
		} else {
			throw new RuntimeException("Unknown kernel type");
		}

		ImageBase src = createImage(paramTypes[1], width, height);
		GeneralizedImageOps.randomize(src, 0, 5, rand);
		ImageBase dst = createImage(paramTypes[2], width, height);

		if (target.getName().compareTo("horizontal") == 0 || target.getName().compareTo("vertical") == 0) {
			if (paramTypes[3] == boolean.class) {
				performTest(target, width, height, check, kernel, src, dst, true);
				performTest(target, width, height, check, kernel, src, dst, false);
			} else {
				performTest(target, width, height, check, kernel, src, dst, 11, true);
				performTest(target, width, height, check, kernel, src, dst, 11, false);
			}
		} else if (target.getName().compareTo("convolve") == 0) {
			if (paramTypes.length == 3) {
				performTest(target, width, height, check, kernel, src, dst);
				performTest(target, width, height, check, kernel, src, dst);
			} else {
				performTest(target, width, height, check, kernel, src, dst, 11);
				performTest(target, width, height, check, kernel, src, dst, 11);
			}
		} else {
			throw new RuntimeException("Unknown function");
		}
	}

	/**
	 * Sees if the two methods produce the same output image
	 */
	protected static void performTest(Method target, int width, int height, Method check, Object... inputs) {
		Object[] found = copyImgs(inputs, width, height);
		Object[] expected = copyImgs(inputs, width, height);

		try {
			target.invoke(null, found);
			check.invoke(null, expected);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}

		// see if the output image is the same
		GecvTesting.assertEqualsGeneric((ImageBase) found[2], (ImageBase) expected[2], 0, 1e-5);

	}

	/**
	 * Searches for images and creates copies.  The same instance of all other variables is returned
	 */
	protected static Object[] copyImgs(Object[] input, int width, int height) {
		Object[] output = new Object[input.length];
		for (int i = 0; i < input.length; i++) {
			Object o = input[i];
			if (o instanceof ImageBase) {
				ImageBase img = ((ImageBase) o)._createNew(width, height);
				img.setTo((ImageBase) o);
				output[i] = img;
			} else {
				output[i] = o;
			}
		}

		return output;
	}

	/**
	 * Creates an image of the specified type
	 */
	private static ImageBase createImage(Class<?> imageType, int width, int height) {
		try {
			ImageBase img = (ImageBase) imageType.newInstance();
			return img._createNew(width, height);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private Method findEquivalent(Method m) {
		try {
			return ConvolveImageStandard.class.getMethod(m.getName(), m.getParameterTypes());
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

}
