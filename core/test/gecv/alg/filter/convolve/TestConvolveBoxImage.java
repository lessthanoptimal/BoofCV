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

import gecv.core.image.GeneralizedImageOps;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.image.ImageBase;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestConvolveBoxImage {

	Random rand = new Random(0xFF);

	static int width = 10;
	static int height = 12;
	static int kernelRadius = 2;

	/**
	 * Automatically compares all the box filters against a generalize convolution
	 */
	@Test
	public void compareToGeneral() {
		Method methods[] = ConvolveBoxImage.class.getMethods();

		// sanity check to make sure the functions are being found
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
		assertEquals(6, numFound);
	}

	private Method findEquivalent(Method m) {
		try {
			Class<?> inputType = m.getParameterTypes()[0];
			Class<?> outputType = m.getParameterTypes()[1];

			Class<?> kernelType = ConvolutionTestHelper.kernelTypeByInputType(inputType);

			return ConvolveImageNoBorder.class.getMethod(m.getName(), kernelType,inputType,outputType,boolean.class);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	protected static void checkMethod(Method target, Method check,
									  int width, int height, int kernelRadius, Random rand) {
		Class<?> paramTypes[] = target.getParameterTypes();

		Object kernel = createTableKernel(check.getParameterTypes()[0], kernelRadius, rand);

		ImageBase src = ConvolutionTestHelper.createImage(paramTypes[0], width, height);
		GeneralizedImageOps.randomize(src, 0, 5, rand);
		ImageBase dst = ConvolutionTestHelper.createImage(paramTypes[1], width, height);

		if (target.getName().compareTo("horizontal") == 0 || target.getName().compareTo("vertical") == 0) {
			performTest(target, check, kernel, src, dst, true);
			performTest(target, check, kernel, src, dst, false);
		} else {
			throw new RuntimeException("Unknown function");
		}
	}

	private static Object createTableKernel(Class<?> aClass, int kernelRadius, Random rand) {
		Object kernel;
		if (Kernel1D_F32.class == aClass) {
			kernel = KernelFactory.table1D_F32(kernelRadius,false);
		} else if (Kernel1D_I32.class == aClass) {
			kernel = KernelFactory.table1D_I32(kernelRadius);
		} else {
			throw new RuntimeException("Unknown kernel type");
		}
		return kernel;
	}

	/**
	 * Sees if the two methods produce the same output image
	 */
	protected static void performTest(Method target, Method check, Object kernel, ImageBase input , ImageBase output , boolean includeBorder) {
		Object[] foundInput = new Object[]{input,output,kernelRadius,includeBorder};
		Object[] foundOutput;
		if( GeneralizedImageOps.isFloatingPoint(input) ) {
			foundOutput = new Object[]{kernel,input,output,includeBorder};
		} else {
			foundOutput = new Object[]{kernel,input,output,includeBorder};
		}
		Object[] found = ConvolutionTestHelper.copyImgs(foundInput);
		Object[] expected = ConvolutionTestHelper.copyImgs(foundOutput);

		try {
			target.invoke(null, found);
			check.invoke(null, expected);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}

		// see if the output image is the same
		GecvTesting.assertEqualsGeneric((ImageBase) found[1], (ImageBase) expected[2], 0, 1e-5);

	}

}
