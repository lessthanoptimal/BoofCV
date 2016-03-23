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

package boofcv.alg.filter.convolve;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.*;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.image.*;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestConvolveWithBorderSparse {

	Random rand = new Random(234);
	int width = 20;
	int height = 30;
	int numExpected = 2;

	@Test
	public void testHorizontal() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
		int numFound = 0;

		Method methods[] = ConvolveWithBorderSparse.class.getMethods();

		for( Method m : methods ) {
			if( !m.getName().equals("horizontal"))
				continue;

			testMethod(m);
			numFound++;
		}

		assertEquals(numExpected,numFound);
	}

	@Test
	public void testVertical() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
		int numFound = 0;

		Method methods[] = ConvolveWithBorderSparse.class.getMethods();

		for( Method m : methods ) {
			if( !m.getName().equals("vertical"))
				continue;

			testMethod(m);
			numFound++;
		}

		assertEquals(numExpected,numFound);
	}

	@Test
	public void testConvolve() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
		int numFound = 0;

		Method methods[] = ConvolveWithBorderSparse.class.getMethods();

		for( Method m : methods ) {
			if( !m.getName().equals("convolve"))
				continue;

			testMethod(m);
			numFound++;
		}

		assertEquals(numExpected,numFound);
	}

	public void testMethod( Method m ) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

		Class<?> kernelType = m.getParameterTypes()[0];
		Class<?> borderType = m.getParameterTypes()[1];
		Class inputType = borderToInputType(borderType);
		Class outputType = borderToOutputType(borderType);

		ImageGray input = GeneralizedImageOps.createSingleBand(inputType, width, height);
		GImageMiscOps.fillUniform(input, rand, 0, 20);

		ImageGray expected = GeneralizedImageOps.createSingleBand(outputType, width, height);
		Object kernel = createKernel(kernelType,2);
		ImageBorder border = createBorder(borderType);

		Method checkM = BoofTesting.findMethod(ConvolveWithBorder.class,m.getName(),kernelType,inputType,outputType,borderType);
		checkM.invoke(null,kernel,input,expected,border);

		Number v = (Number)m.invoke(null,kernel,border,3,6);

		double expectedValue = GeneralizedImageOps.get(expected,3,6);

		assertEquals(expectedValue,v.doubleValue(),1e-3);
	}

	protected static Object createKernel(Class kernelType , int kernelRadius ) {
		return FactoryKernelGaussian.gaussian(kernelType,-1,kernelRadius);
	}

	protected static ImageBorder createBorder(Class<?> borderType ) {
		ImageBorder ret;
		if (ImageBorder_F32.class == borderType) {
			ret = FactoryImageBorder.single(GrayF32.class, BorderType.REFLECT);
		} else if (ImageBorder_F64.class == borderType) {
			ret = FactoryImageBorder.single(GrayF64.class, BorderType.REFLECT);
		} else if (ImageBorder_S32.class == borderType) {
			ret =FactoryImageBorder.single(GrayS32.class, BorderType.REFLECT);
		} else if (ImageBorder_S64.class == borderType) {
			ret = FactoryImageBorder.single(GrayS64.class, BorderType.REFLECT);
		} else {
			throw new RuntimeException("Unknown kernel type");
		}
		return ret;
	}

	protected static Class borderToInputType(Class<?> borderType ) {
		if (ImageBorder_F32.class == borderType) {
			return GrayF32.class;
		} else if (ImageBorder_F64.class == borderType) {
			return GrayF64.class;
		} else if (ImageBorder_S32.class == borderType) {
			return GrayU8.class;
		} else if (ImageBorder_S64.class == borderType) {
			return GrayS64.class;
		} else {
			throw new RuntimeException("Unknown border type");
		}
	}

	protected static Class<?> borderToOutputType(Class<?> borderType ) {
		if (ImageBorder_F32.class == borderType) {
			return GrayF32.class;
		} else if (ImageBorder_F64.class == borderType) {
			return GrayF64.class;
		} else if (ImageBorder_S32.class == borderType) {
			return GrayS16.class;
		} else if (ImageBorder_S64.class == borderType) {
			return GrayS64.class;
		} else {
			throw new RuntimeException("Unknown border type");
		}
	}
}
