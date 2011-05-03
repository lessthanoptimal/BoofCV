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

package gecv.alg.filter.convolve.edge.impl;

import gecv.alg.filter.convolve.KernelFactory;
import gecv.alg.filter.convolve.edge.ConvolveNormalized;
import gecv.core.image.ConvertImage;
import gecv.core.image.UtilImageInt8;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt16;
import gecv.struct.image.ImageInt8;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestConvolveNormalizedStandardSparse {

	Random rand = new Random(0xFF);

	static int width = 10;
	static int height = 12;
	static int kernelRadius = 2;
	static int targetX = 0;
	static int targetY = 5;

	static Kernel1D_F32 kernelF32;
	static Kernel1D_I32 kernelI32;
	static float expectedOutput;

	/**
	 * Automatically compares all the box filters against a generalize convolution
	 */
	@Test
	public void compareToGeneral() {
		// try different edges in the image as test points
		for( int i = 0; i < 2; i++ ) {
//			targetX = i; targetY = 5;
//			performComparision();
//			targetX = 5; targetY = i;
//			performComparision();
//			targetX = width-1-i; targetY = 5;
//			performComparision();
//			targetX = 5; targetY = height-1-i;
//			performComparision();
			targetX = 5; targetY = 5;
			performComparision();
		}
	}

	private void performComparision() {
		Method methods[] = ConvolveNormalizedStandardSparse.class.getMethods();

		// sanity check to make sure the functions are being found
		int numFound = 0;
		for (Method m : methods) {
			// search for methods which have equivalent in the other class
			Class<?> paramTypes[] = m.getParameterTypes();
			if (paramTypes.length < 3) {
				continue;
			}
			System.out.println("Checking "+m.getName()+"  type "+paramTypes[2].getSimpleName());

			checkMethod(m, width, height, kernelRadius, rand);
			numFound++;
		}

		// update this as needed when new functions are added
		assertEquals(3, numFound);
	}

	private void checkMethod(Method method, int width, int height, int kernelRadius, Random rand) {
		ImageInt8 seedImage = new ImageInt8(width,height);
		UtilImageInt8.randomize(seedImage,rand,0,255);

		// creates a floating point image with integer elements
		ImageFloat32 floatImage = new ImageFloat32(width,height);
		ConvertImage.convert(seedImage,floatImage,false);
		ImageInt16 shortImage = new ImageInt16(width,height);
		ConvertImage.convert(seedImage,shortImage,false);

		kernelI32 = KernelFactory.gaussian1D_I32(kernelRadius);
		kernelF32 = KernelFactory.gaussian1D_F32(kernelRadius,true);

		boolean isFloatingKernel = method.getParameterTypes()[0] == Kernel1D_F32.class;

		Class<?> imageType = method.getParameterTypes()[2];
		ImageBase<?> inputImage;

		if( imageType == ImageFloat32.class) {
			inputImage = floatImage;
			expectedOutput = computeExpected(floatImage);
		} else if( imageType == ImageInt8.class ){
			inputImage = seedImage;
			expectedOutput = computeExpected(seedImage);
		} else {
			inputImage = shortImage;
			expectedOutput = computeExpected(shortImage);
		}

		Object inputKernel = isFloatingKernel ? kernelF32 : kernelI32;
		Object inputStorage = isFloatingKernel ? new float[kernelI32.width] : new int[ kernelI32.width];

		checkResults(method,inputKernel,inputImage,inputStorage);
	}

	private void checkResults(Method method, Object inputKernel, ImageBase<?> inputImage, Object inputStorage) {
		try {
			Number result = (Number)method.invoke(null,inputKernel,inputKernel,inputImage,targetX,targetY,inputStorage);
			assertEquals(expectedOutput,result.floatValue(),1e-4);
		} catch (IllegalAccessException e) {                                                         
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private float computeExpected( ImageFloat32 image ) {
		ImageFloat32 temp = new ImageFloat32(image.width,image.height);
		ImageFloat32 temp2 = new ImageFloat32(image.width,image.height);

		ConvolveNormalized.horizontal(kernelF32,image,temp);
		ConvolveNormalized.vertical(kernelF32,temp,temp2);

		return temp2.get(targetX,targetY);
	}

	private float computeExpected( ImageInt8 image ) {
		ImageInt8 temp = new ImageInt8(image.width,image.height);
		ImageInt8 temp2 = new ImageInt8(image.width,image.height);

		ConvolveNormalized.horizontal(kernelI32,image,temp);
		ConvolveNormalized.vertical(kernelI32,temp,temp2);

		return temp2.getU(targetX,targetY);
	}

	private float computeExpected( ImageInt16 image ) {
		ImageInt16 temp = new ImageInt16(image.width,image.height);
		ImageInt16 temp2 = new ImageInt16(image.width,image.height);

		ConvolveNormalized.horizontal(kernelI32,image,temp);
		ConvolveNormalized.vertical(kernelI32,temp,temp2);

		return temp2.get(targetX,targetY);
	}
}
