/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.filter.convolve.ConvolveImageNoBorder;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.ConvertImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_I32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestConvolveImageStandardSparse {

	Random rand = new Random(0xFF);

	static int width = 10;
	static int height = 12;
	static int kernelRadius = 2;
	static int targetX = 5;
	static int targetY = 6;

	static Kernel1D_F32 kernelF32;
	static Kernel1D_I32 kernelI32;
	static int sumKernel;
	static float expectedOutput;

	/**
	 * Automatically compares all the box filters against a generalize convolution
	 */
	@Test
	public void compareToGeneral() {
		Method methods[] = ConvolveImageStandardSparse.class.getMethods();

		// sanity check to make sure the functions are being found
		int numFound = 0;
		for (Method m : methods) {
			// search for methods which have equivalent in the other class
			Class<?> paramTypes[] = m.getParameterTypes();
			if (paramTypes.length < 3)
				continue;

			checkMethod(m, width, height, kernelRadius,kernelRadius*2+1, rand);
			checkMethod(m, width, height, 0,kernelRadius*2+1, rand);

			numFound++;
		}

		// update this as needed when new functions are added
		assertEquals(5, numFound);
	}

	private void checkMethod(Method method, int width, int height, int kernelOffset , int kernelWidth, Random rand) {
		GrayU8 seedImage = new GrayU8(width,height);
		ImageMiscOps.fillUniform(seedImage,rand,0,255);

		// creates a floating point image with integer elements
		GrayF32 floatImage = new GrayF32(width,height);
		ConvertImage.convert(seedImage,floatImage);

		sumKernel = 0;
		kernelI32 = FactoryKernelGaussian.gaussian(Kernel1D_I32.class,-1,kernelWidth/2);
		kernelF32 = new Kernel1D_F32(kernelI32.width);
		for( int i = 0; i < kernelI32.width; i++ ) {
			sumKernel += kernelF32.data[i] = kernelI32.data[i];
		}

		kernelI32.offset = kernelOffset;
		kernelF32.offset = kernelOffset;


		boolean isFloatingKernel = method.getParameterTypes()[0] == Kernel1D_F32.class;
		boolean isDivisor = method.getParameterTypes().length != 6;

		expectedOutput = computeExpected(seedImage,!isFloatingKernel,isDivisor);

		ImageGray inputImage = GeneralizedImageOps.convert(floatImage,null,(Class)method.getParameterTypes()[2]);
		Object inputKernel = isFloatingKernel ? kernelF32 : kernelI32;
		Object inputStorage = isFloatingKernel ? new float[kernelI32.width] : new int[ kernelI32.width];

		checkResults(method,inputKernel,inputImage,inputStorage);
	}

	private void checkResults(Method method, Object inputKernel, ImageGray<?> inputImage, Object inputStorage) {
		try {
			Number result;
			if( method.getParameterTypes().length == 6 )
				result = (Number)method.invoke(null,inputKernel,inputKernel,inputImage,targetX,targetY,inputStorage);
			else
				result = (Number)method.invoke(null,inputKernel,inputKernel,inputImage,targetX,targetY,inputStorage,sumKernel,sumKernel);

			String description = method.getName()+" "+inputImage.getClass().getSimpleName()+" "+method.getParameterTypes().length;

			assertEquals(description,(int)expectedOutput,result.intValue());
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private float computeExpected(GrayU8 image , boolean isInteger , boolean isDivisor ) {

		if( isInteger && isDivisor  ) {
			GrayU8 temp = new GrayU8(image.width,image.height);
			GrayU8 temp2 = new GrayU8(image.width,image.height);

			ConvolveImageNoBorder.horizontal(kernelI32,image,temp,sumKernel);
			ConvolveImageNoBorder.vertical(kernelI32,temp,temp2,sumKernel);

			return temp2.get(targetX,targetY);
		} else {
			GrayF32 imageF = new GrayF32(image.width,image.height);
			GrayF32 temp = new GrayF32(image.width,image.height);
			GrayF32 temp2 = new GrayF32(image.width,image.height);

			ConvertImage.convert(image,imageF);

			ConvolveImageNoBorder.horizontal(kernelF32,imageF,temp);
			ConvolveImageNoBorder.vertical(kernelF32,temp,temp2);

			return temp2.get(targetX,targetY);
		}

	}
}
