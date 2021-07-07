/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.misc.GImageStatistics;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.struct.convolve.Kernel1D;
import boofcv.struct.convolve.Kernel2D;
import boofcv.struct.convolve.KernelBase;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageInterleaved;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static boofcv.core.image.GeneralizedImageOps.get;
import static org.junit.jupiter.api.Assertions.*;

/**
 * To reduce the amount of code reflects are heavily used. If any more functions are added reflections should be
 * used to extract them and the appropriate unit test called. That way its unlikely that anything would be left out.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"SillyAssignment"})
public class TestConvolveImageStandard_IL extends BoofStandardJUnit {

	int width = 4;
	int height = 5;
	int numBands = 2;
	int kernelWidth = 3;
	int kernelRadius = kernelWidth/2;

	int maxKernelValue = 20;
	int maxPixelValue = 10;

	/**
	 * Using reflections get a list of all the functions and test each of them
	 */
	@Test void checkAll() {
		int numExpected = 35;
		Method methods[] = ConvolveImageStandard_IL.class.getMethods();

		// sanity check to make sure the functions are being found
		int numFound = 0;
		for (Method m : methods) {
			if( !isTestMethod(m)) {
//				System.out.println(m.getName());
				continue;
			}

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

		ImageInterleaved input = GeneralizedImageOps.createInterleaved(param[1], width, height, numBands);
		ImageInterleaved output = GeneralizedImageOps.createInterleaved(param[2], width, height, numBands);

		GImageMiscOps.fillUniform(input, rand, 1, maxPixelValue);

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
	public void horizontal(ImageInterleaved img, ImageInterleaved dest) {
		Kernel1D ker = FactoryKernel.createKernelForImage(kernelWidth, kernelRadius, 1, img.getDataType());

		// standard symmetric odd kernel shape
		ker = FactoryKernel.random(ker.getClass(),kernelWidth,kernelRadius,0,maxKernelValue,rand);
		checkHorizontal(ker, img, dest);

		// odd non-symmetric kernel shape
		ker = FactoryKernel.random(ker.getClass(),kernelWidth,0,0,maxKernelValue,rand);
		checkHorizontal(ker,img,dest);

		// even non-symmetric kernel shape
		ker = FactoryKernel.random(ker.getClass(),2,1,0,maxKernelValue,rand);
		checkHorizontal(ker, img, dest);
	}

	private void checkHorizontal(Kernel1D ker , ImageInterleaved img, ImageInterleaved dest ) {
		GImageMiscOps.fill(dest, 0);
		invokeMethod("horizontal", ker, img, dest);
		assertTrue(GImageStatistics.sum(dest) != 0); // making sure it's a good test and not trivial
		int y = 1;

		double tol = toleranceByImageType(img);

		int x0 = ker.offset;
		int x1 = width-(ker.width-ker.offset-1);

		for (int x = 0; x < x0; x++) {
			for (int band = 0; band < numBands; band++) {
				assertEquals(0, get(dest,x, y, band), tol);
			}
		}
		for (int x = x0; x < x1; x++) {
			for (int band = 0; band < numBands; band++) {
				double expected = horizontal(x, y, band, img, ker, ker.width, ker.offset);
				assertEquals(expected, get(dest, x, y, band), tol);
			}
		}
		for (int x = x1; x < width; x++) {
			for (int band = 0; band < numBands; band++) {
				assertEquals(0, get(dest, x, y, band), tol);
			}
		}
	}

	public double horizontal(int x, int y, int band, ImageInterleaved img, Kernel1D ker, int width, int offset) {

		double total = 0;

		for( int i = 0; i < width; i++ ) {
			if( img.isInBounds(x-offset+i,y)) {
				double valI = get(img, x - offset + i, y, band);

				total += valI * ker.getDouble(i);
			}
		}

		return total;
	}

	/**
	 * Unit test for horizontal convolution with division.
	 */
	public void horizontalDiv(ImageInterleaved img, ImageInterleaved dest) {
		Kernel1D ker = FactoryKernel.createKernelForImage(kernelWidth, kernelRadius, 1, img.getDataType());

		// standard symmetric odd kernel shape
		ker = FactoryKernel.random(ker.getClass(),kernelWidth,kernelRadius,0,maxKernelValue,rand);
		checkHorizontalDiv(ker, img, dest);

		// odd non-symmetric kernel shape
		ker = FactoryKernel.random(ker.getClass(),kernelWidth,0,0,maxKernelValue,rand);
		checkHorizontalDiv(ker, img, dest);

		// even non-symmetric kernel shape
		ker = FactoryKernel.random(ker.getClass(),2,1,0,maxKernelValue,rand);
		checkHorizontalDiv(ker, img, dest);
	}

	private void checkHorizontalDiv(Kernel1D ker , ImageInterleaved img, ImageInterleaved dest ) {
		int divisor = kernelWidth;
		GImageMiscOps.fill(dest, 0);
		invokeMethod("horizontal", ker, img, dest, divisor);
		assertTrue(GImageStatistics.sum(dest) != 0); // making sure it's a good test and not trivial
		int y = 1;

		double tol = toleranceByImageType(img);

		int x0 = ker.offset;
		int x1 = width-(ker.width-ker.offset-1);

		for (int x = 0; x < x0; x++) {
			for (int band = 0; band < numBands; band++) {
				assertEquals(0, get(dest,x, y, band), tol);
			}
		}
		for (int x = x0; x < x1; x++) {
			for (int band = 0; band < numBands; band++) {
				double expected = horizontal(x, y, band, img, ker, ker.width, ker.offset, divisor);
				assertEquals(expected, get(dest, x, y, band), tol);
			}
		}
		for (int x = x1; x < width; x++) {
			for (int band = 0; band < numBands; band++) {
				assertEquals(0, get(dest, x, y, band), tol);
			}
		}
	}

	public double horizontal(int x , int y , int band, ImageInterleaved img , Kernel1D ker ,
							 int width , int offset , int divisor ) {

		double total = 0;

		for( int i = 0; i < width; i++ ) {
			if( img.isInBounds(x-offset+i,y)) {
				double valI = get(img, x - offset + i, y, band);

				total += valI * ker.getDouble(i);
			}
		}

		return (total+(divisor/2))/divisor;
	}

	/**
	 * Unit test for vertical convolution.
	 */
	public void vertical(ImageInterleaved img, ImageInterleaved dest) {
		Kernel1D ker = FactoryKernel.createKernelForImage(kernelWidth, kernelRadius, 1, img.getDataType());

		// standard symmetric odd kernel shape
		ker = FactoryKernel.random(ker.getClass(), kernelWidth, kernelRadius, 0, maxKernelValue, rand);
		checkVertical(ker, img, dest);

		// odd non-symmetric kernel shape
		ker = FactoryKernel.random(ker.getClass(),kernelWidth,0,0,maxKernelValue,rand);
		checkVertical(ker, img, dest);

		// even non-symmetric kernel shape
		ker = FactoryKernel.random(ker.getClass(),2,1,0,maxKernelValue,rand);
		checkVertical(ker, img, dest);
	}

	private void checkVertical(Kernel1D ker , ImageInterleaved img, ImageInterleaved dest ) {
		GImageMiscOps.fill(dest, 0);
		invokeMethod("vertical", ker, img, dest);
		assertTrue(GImageStatistics.sum(dest) != 0); // making sure it's a good test and not trivial
		int x = 1;

		double tol = toleranceByImageType(img);

		int y0 = ker.offset;
		int y1 = height-(ker.width-ker.offset-1);

		for (int y = 0; y < y0; y++) {
			for (int band = 0; band < numBands; band++) {
				assertEquals(0, get(dest,x, y, band), tol);
			}
		}
		for (int y = y0; y < y1; y++) {
			for (int band = 0; band < numBands; band++) {
				double expected = vertical(x, y, band, img, ker, ker.width, ker.offset);
				assertEquals(expected, get(dest, x, y, band), tol);
			}
		}
		for (int y = y1; y < width; y++) {
			for (int band = 0; band < numBands; band++) {
				assertEquals(0, get(dest, x, y, band), tol);
			}
		}
	}

	public double vertical(int x , int y , int band, ImageInterleaved img , Kernel1D ker , int width , int offset ) {
		double total = 0;

		for( int i = 0; i < width; i++ ) {
			if( img.isInBounds(x, y - offset + i)) {
				double valI = get(img, x, y - offset + i, band );

				total += valI * ker.getDouble(i);
			}
		}

		return total;
	}

	/**
	 * Unit test for vertical convolution with division.
	 */
	public void verticalDiv(ImageInterleaved img, ImageInterleaved dest) {
		Kernel1D ker = FactoryKernel.createKernelForImage(kernelWidth, kernelRadius, 1, img.getDataType());

		// standard symmetric odd kernel shape
		ker = FactoryKernel.random(ker.getClass(), kernelWidth, kernelRadius, 0, maxKernelValue, rand);
		checkVerticalDiv(ker, img, dest);

		// odd non-symmetric kernel shape
		ker = FactoryKernel.random(ker.getClass(),kernelWidth,0,0,maxKernelValue,rand);
		checkVerticalDiv(ker, img, dest);

		// even non-symmetric kernel shape
		ker = FactoryKernel.random(ker.getClass(),2,1,0,maxKernelValue,rand);
		checkVerticalDiv(ker, img, dest);
	}

	private void checkVerticalDiv(Kernel1D ker , ImageInterleaved img, ImageInterleaved dest ) {
		int divisor = kernelWidth;

		GImageMiscOps.fill(dest, 0);
		invokeMethod("vertical", ker, img, dest, divisor);
		assertTrue(GImageStatistics.sum(dest) != 0); // making sure it's a good test and not trivial
		int x = 1;

		double tol = toleranceByImageType(img);

		int y0 = ker.offset;
		int y1 = height-(ker.width-ker.offset-1);

		for (int y = 0; y < y0; y++) {
			for (int band = 0; band < numBands; band++) {
				assertEquals(0, get(dest,x, y, band), tol);
			}
		}
		for (int y = y0; y < y1; y++) {
			for (int band = 0; band < numBands; band++) {
				double expected = vertical(x, y, band, img, ker, ker.width, ker.offset, divisor);
				assertEquals(expected, get(dest, x, y, band), tol);
			}
		}
		for (int y = y1; y < width; y++) {
			for (int band = 0; band < numBands; band++) {
				assertEquals(0, get(dest, x, y, band), tol);
			}
		}
	}

	public double vertical(int x , int y , int band, ImageInterleaved img , Kernel1D ker , int width , int offset , int divisor ) {
		double total = 0;

		for( int i = 0; i < width; i++ ) {
			if( img.isInBounds(x,y-offset+i)) {
				double valI = get(img, x, y - offset + i, band);

				total += valI * ker.getDouble(i);
			}
		}

		return (total+(divisor/2))/divisor;
	}

	/**
	 * Unit test for 2D convolution.
	 */
	public void convolve(ImageInterleaved img, ImageInterleaved dest) {
		Kernel2D ker = FactoryKernel.createKernelForImage(kernelWidth, kernelRadius, 2, img.getDataType());

		// standard symmetric odd kernel shape
		ker = FactoryKernel.random(ker.getClass(), kernelWidth, kernelRadius, 0, maxKernelValue, rand);
		convolve(ker, img, dest);

		// odd non-symmetric kernel shape
		ker = FactoryKernel.random(ker.getClass(),kernelWidth,0,0,maxKernelValue,rand);
		convolve(ker, img, dest);

		// even non-symmetric kernel shape
		ker = FactoryKernel.random(ker.getClass(),2,1,0,maxKernelValue,rand);
		convolve(ker, img, dest);
	}

	private void convolve(Kernel2D kernel , ImageInterleaved img, ImageInterleaved dest) {
		// manually perform a convolution
		GImageMiscOps.fill(dest,0);

		invokeMethod("convolve", kernel, img, dest);
		assertTrue(GImageStatistics.sum(dest) != 0); // making sure it's a good test and not trivial

		// the border should be zero and everything else should be the expected value
		double tol = toleranceByImageType(img);
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				for (int band = 0; band < numBands; band++) {
					if (i < kernel.offset || j < kernel.offset
							|| i >= height-(kernel.width-kernel.offset-1)
							|| j >= width-(kernel.width-kernel.offset-1))
						assertEquals(0f, get(dest, j, i, band), tol);
					else {
						double expected = convolve(img, j, i, band, kernel);
						assertEquals(expected, get(dest, j, i, band), tol);
					}
				}
			}
		}
	}

	private double convolve(ImageInterleaved img, int cx, int cy, int band,  Kernel2D kernel) {
		double expected = 0;
		for (int i = 0; i < kernel.width; i++) {
			int y = cy - kernel.offset + i;
			for (int j = 0; j < kernel.width; j++) {
				int x = cx - kernel.offset + j;
				expected += kernel.getDouble(j, i) * get(img,x,y, band);
			}
		}
		return expected;
	}

	/**
	 * Unit test for 2D convolution with division.
	 */
	public void convolveDiv(ImageInterleaved img, ImageInterleaved dest) {
		Kernel2D ker = FactoryKernel.createKernelForImage(kernelWidth, kernelRadius, 2, img.getDataType());

		// standard symmetric odd kernel shape
		ker = FactoryKernel.random(ker.getClass(), kernelWidth, kernelRadius, 0, maxKernelValue, rand);
		convolveDiv(ker, img, dest);

		// odd non-symmetric kernel shape
		ker = FactoryKernel.random(ker.getClass(),kernelWidth,0,0,maxKernelValue,rand);
		convolveDiv(ker, img, dest);

		// even non-symmetric kernel shape
		ker = FactoryKernel.random(ker.getClass(),2,1,0,maxKernelValue,rand);
		convolveDiv(ker, img, dest);
	}

	private void convolveDiv(Kernel2D kernel , ImageInterleaved img, ImageInterleaved dest) {
		// manually perform a convolution
		GImageMiscOps.fill(dest,0);
		int divisor = kernelWidth*kernelWidth;

		invokeMethod("convolve", kernel, img, dest,divisor);

		assertTrue(GImageStatistics.sum(dest) != 0); // making sure it's a good test and not trivial

		// the border should be zero and everything else should be the expected value

		double tol = toleranceByImageType(img);
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				for (int band = 0; band < numBands; band++) {
					if (i < kernel.offset || j < kernel.offset
							|| i >= height-(kernel.width-kernel.offset-1)
							|| j >= width-(kernel.width-kernel.offset-1))
						assertEquals(0f, get(dest, j, i, band), tol);
					else {
						double expected = convolveDiv(img, j, i, band, kernel, divisor);
						assertEquals(expected, get(dest, j, i, band), tol);
					}
				}
			}
		}
	}

	private double convolveDiv(ImageInterleaved img, int cx, int cy, int band, Kernel2D kernel, int divisor ) {
		double total = 0;
		for (int i = 0; i < kernel.width; i++) {
			int y = cy - kernel.offset + i;
			for (int j = 0; j < kernel.width; j++) {
				int x = cx - kernel.offset + j;
				total += kernel.getDouble(j, i) * get(img,x,y, band);
			}
		}
		return (total+(divisor/2))/divisor;
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
			Method m = ConvolveImageStandard_IL.class.getMethod(name, inputTypes);
			m.invoke(null, inputs);
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private double toleranceByImageType(ImageBase img) {
		double tol;
		if( img.getImageType().getDataType().isInteger() )  {
			tol = 0.99;
		} else if( img.getImageType().getDataType().getNumBits() == 32 ) {
			tol = 1e-3;
		} else {
			tol = 1e-7;
		}
		return tol;
	}


}
