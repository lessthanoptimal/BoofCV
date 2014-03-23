/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel1D;
import boofcv.struct.convolve.KernelBase;
import boofcv.struct.image.ImageSingleBand;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static boofcv.core.image.GeneralizedImageOps.get;
import static org.junit.Assert.*;

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

	/**
	 * Using reflections get a list of all the functions and test each of them
	 */
	@Test
	public void checkAll() {
		int numExpected = 27;
		Method methods[] = ConvolveImageStandard.class.getMethods();

		// sanity check to make sure the functions are being found
		int numFound = 0;
		for (Method m : methods) {
			if( !isTestMethod(m))
				continue;

			System.out.println("Testing "+m.getName());
			testMethod(m);
			numFound++;
		}

		// update this as needed when new functions are added
		if(numExpected != numFound)
			throw new RuntimeException("Unexpected number of methods: Found "+numFound+"  expected "+numExpected);
	}

	private boolean isTestMethod(Method m ) {

		Class<?> param[] = m.getParameterTypes();

		if( param.length < 3 )
			return false;

		return KernelBase.class.isAssignableFrom(param[0]);
	}

	/**
	 * Using the method's name and the number of parameters invoke the appropriate test function
	 */
	private void testMethod( Method m ) {
		Class param[] = m.getParameterTypes();

		ImageSingleBand input = GeneralizedImageOps.createSingleBand(param[1], width, height);
		ImageSingleBand output = GeneralizedImageOps.createSingleBand(param[2], width, height);

		GImageMiscOps.fillUniform(input, rand, 1, 10);

		if( m.getName().contentEquals("horizontal")) {
			if( param.length == 3 ) {
				BoofTesting.checkSubImage(this, "horizontal", true, input, output);
			} else {
				BoofTesting.checkSubImage(this, "horizontalDiv", true, input, output);
			}
		} else if( m.getName().contentEquals("vertical")) {
			if( param.length == 3 ) {
				BoofTesting.checkSubImage(this, "vertical", true, input, output);
			} else {
				BoofTesting.checkSubImage(this, "verticalDiv", true, input, output);
			}
		} else if( m.getName().contentEquals("convolve")) {
			if( param.length == 3 ) {
				BoofTesting.checkSubImage(this, "convolve", true, input, output);
			} else {
				BoofTesting.checkSubImage(this, "convolveDiv", true, input, output);
			}
		} else {
			fail("Unknown method name: "+m.getName());
		}
	}


	/**
	 * Unit test for horizontal convolution.
	 */
	public void horizontal(ImageSingleBand img, ImageSingleBand dest) {
		Object ker = FactoryKernelGaussian.gaussian1D(img.getClass(),-1,kernelRadius);

		// standard symmetric odd kernel
		GImageMiscOps.fill(dest,0);
		invokeMethod("horizontal", ker, img, dest);
		double expected = horizontal(1,1,img,ker,kernelRadius,2*kernelRadius+1);
		assertEquals(expected, get(dest, 1, 1), 1e-6);
		// horizontal border check
		assertEquals(0, get(dest, 0, 3), 1e-6);
		assertEquals(0, get(dest, width-1, 3), 1e-6);

		// non-symmetric kernel
		GImageMiscOps.fill(dest,0);
		((Kernel1D)ker).offset=0;
		invokeMethod("horizontal", ker, img, dest);
		expected = horizontal(1,1,img,ker,0,2*kernelRadius+1);
		assertEquals(expected, get(dest, 1, 1), 1e-6);
		// horizontal border check
		assertTrue(0 != get(dest, 0, 3));
		assertEquals(0, get(dest, width-2, 3), 1e-6);
		assertEquals(0, get(dest, width-1, 3), 1e-6);
	}


	public double horizontal( int x , int y , ImageSingleBand img , Object ker , int offset , int width ) {

		double total = 0;

		for( int i = 0; i < width; i++ ) {
			double valI = get(img,x-offset+i,y);
			double valK = getKernel(ker,i);

			total += valI*valK;
		}

		return total;
	}

	/**
	 * Unit test for horizontal convolution with division.
	 */
	public void horizontalDiv(ImageSingleBand img, ImageSingleBand dest) {
		int divisor = 11;
		Object ker = FactoryKernelGaussian.gaussian1D(img.getClass(),-1,kernelRadius);

		// standard symmetric odd kernel
		GImageMiscOps.fill(dest,0);
		invokeMethod("horizontal", ker, img, dest, divisor);
		double expected = horizontal(1,1,img,ker,kernelRadius,2*kernelRadius+1,divisor);
		if( dest.getDataType().isInteger())
			expected = (int)expected;
		assertEquals(expected, get(dest, 1, 1), 1e-6);
		// horizontal border check
		assertEquals(0, get(dest, 0, 3), 1e-6);
		assertEquals(0, get(dest, width-1, 3), 1e-6);

		// non-symmetric kernel
		GImageMiscOps.fill(dest,0);
		((Kernel1D)ker).offset=0;
		invokeMethod("horizontal", ker, img, dest, divisor);
		expected = horizontal(1,1,img,ker,0,2*kernelRadius+1,divisor);
		if( dest.getDataType().isInteger())
			expected = (int)expected;
		assertEquals(expected, get(dest, 1, 1), 1e-6);
		// border check
		assertTrue(0 != get(dest, 0, 3));
		assertEquals(0, get(dest, width-2, 3), 1e-6);
		assertEquals(0, get(dest, width-1, 3), 1e-6);
	}

	public double horizontal( int x , int y , ImageSingleBand img , Object ker , int offset , int width , int divisor ) {

		double total = 0;

		for( int i = 0; i < width; i++ ) {
			double valI = get(img,x-offset+i,y);
			double valK = getKernel(ker,i);

			total += valI*valK;
		}

		return (total+(divisor/2))/divisor;
	}

	/**
	 * Unit test for vertical convolution.
	 */
	public void vertical(ImageSingleBand img, ImageSingleBand dest) {
		Object ker = FactoryKernelGaussian.gaussian1D(img.getClass(),-1,kernelRadius);

		// standard symmetric odd kernel
		GImageMiscOps.fill(dest,0);
		invokeMethod("vertical", ker, img, dest);
		double expected = vertical(1, 1, img, ker, kernelRadius, 2 * kernelRadius + 1);
		assertEquals(expected, get(dest, 1, 1), 1e-6);
		// horizontal border check
		assertEquals(0, get(dest, 3, 0), 1e-6);
		assertEquals(0, get(dest, 3, height-1), 1e-6);

		// non-symmetric kernel
		GImageMiscOps.fill(dest,0);
		((Kernel1D)ker).offset=0;
		invokeMethod("vertical", ker, img, dest);
		expected = vertical(1, 1, img, ker, 0, 2 * kernelRadius + 1);
		assertEquals(expected, get(dest, 1, 1), 1e-6);
		// horizontal border check
		assertTrue(0 != get(dest, 3, 0));
		assertEquals(0, get(dest, 3, height-2), 1e-6);
		assertEquals(0, get(dest, 3, height-1), 1e-6);
	}

	public double vertical( int x , int y , ImageSingleBand img , Object ker , int offset , int width ) {
		double total = 0;

		for( int i = 0; i < width; i++ ) {
			double valI = get(img,x,y-offset+i);
			double valK = getKernel(ker,i);

			total += valI*valK;
		}

		return total;
	}

	/**
	 * Unit test for vertical convolution with division.
	 */
	public void verticalDiv(ImageSingleBand img, ImageSingleBand dest) {
		Object ker = FactoryKernelGaussian.gaussian1D(img.getClass(),-1,kernelRadius);

		int divisor = 11;

		// standard symmetric odd kernel
		GImageMiscOps.fill(dest,0);
		invokeMethod("vertical", ker, img, dest, divisor);
		double expected = vertical(1,1,img,ker,kernelRadius,2*kernelRadius+1,divisor);
		if (dest.getDataType().isInteger())
			expected = (int) expected;
		assertEquals(expected, get(dest, 1, 1), 1e-6);
		// horizontal border check
		assertEquals(0, get(dest, 3, 0), 1e-6);
		assertEquals(0, get(dest, 3, height-1), 1e-6);

		// non-symmetric kernel
		GImageMiscOps.fill(dest,0);
		((Kernel1D)ker).offset=0;
		invokeMethod("vertical", ker, img, dest, divisor);
		expected = vertical(1,1,img,ker,0,2*kernelRadius+1,divisor);
		if (dest.getDataType().isInteger())
			expected = (int) expected;
		assertEquals(expected, get(dest, 1, 1), 1e-6);
		// horizontal border check
		assertTrue(0 != get(dest, 3, 0));
		assertEquals(0, get(dest, 3, height-2), 1e-6);
		assertEquals(0, get(dest, 3, height-1), 1e-6);
	}

	public double vertical( int x , int y , ImageSingleBand img , Object ker , int offset , int width , int divisor ) {
		double total = 0;

		for( int i = 0; i < width; i++ ) {
			double valI = get(img,x,y-offset+i);
			double valK = getKernel(ker,i);

			total += valI*valK;
		}

		return (total+(divisor/2))/divisor;
	}

	/**
	 * Unit test for 2D convolution.
	 */
	public void convolve(ImageSingleBand img, ImageSingleBand dest) {
		Object ker;
		if (!img.getDataType().isInteger()) {
			if( img.getDataType().getNumBits() == 32 )
				ker = FactoryKernel.random2D_F32(kernelRadius, 0f, 1f, new Random(234));
			else
				ker = FactoryKernel.random2D_F64(kernelRadius, 0f, 1f, new Random(234));
		} else
			ker = FactoryKernel.random2D_I32(kernelRadius, 0, 10, new Random(234));

		int testX = 1;
		int testY = 2;
		double expected = 0;

		// manually perform a convolution
		for (int i = -kernelRadius; i <= kernelRadius; i++) {
			for (int j = -kernelRadius; j <= kernelRadius; j++) {
				expected += getKernel(ker, kernelRadius + i, kernelRadius + j) * get(img, testX + i, testY + j);
			}
		}

		invokeMethod("convolve",ker, img, dest);

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
	public void convolveDiv(ImageSingleBand img, ImageSingleBand dest) {
		Object ker;
		if (!img.getDataType().isInteger())
			ker = FactoryKernel.random2D_F32(kernelRadius, 0f, 1f, new Random(234));
		else
			ker = FactoryKernel.random2D_I32(kernelRadius, 0, 4, new Random(234));

		int divisor = 11;
		int halfDivisor = divisor/2;
		int testX = 1;
		int testY = 2;
		double expected = 0;

		// manually perform a convolution
		for (int i = -kernelRadius; i <= kernelRadius; i++) {
			for (int j = -kernelRadius; j <= kernelRadius; j++) {
				expected += getKernel(ker, kernelRadius + i, kernelRadius + j) * get(img, testX + i, testY + j);
			}
		}
		expected = (expected + halfDivisor)/divisor;
		if (dest.getDataType().isInteger())
			expected = (int) expected;

		invokeMethod("convolve",ker, img, dest, divisor);

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

	private void invokeMethod(String name, Object ...inputs ) {

		Class<?> inputTypes[] = new Class<?>[ inputs.length ];
		for( int i = 0; i < inputs.length; i++ ) {
			if( inputs[i].getClass() == Boolean.class ) {
				inputTypes[i] = boolean.class;
			} else if( inputs[i].getClass() == Integer.class ) {
				inputTypes[i] = int.class;
			} else {
				inputTypes[i] = inputs[i].getClass();
			}
		}

		inputTypes[2] = BoofTesting.convertToGenericType(inputTypes[2]);

		try {
			Method m = ConvolveImageStandard.class.getMethod(name, inputTypes);
			m.invoke(null, inputs);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

}
