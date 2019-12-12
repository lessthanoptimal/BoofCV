/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_F64;
import boofcv.struct.convolve.Kernel1D_S32;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofTesting;
import boofcv.testing.CompareEquivalentFunctions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestConvolveImageMean extends CompareEquivalentFunctions {

	Random rand = new Random(0xFF);

	static int width = 10;
	static int height = 12;
	static int kernelRadius = 2;
	static int kernelRadius2 = 6; // kernel will be larger than the image

	public TestConvolveImageMean() {
		super(ConvolveImageMean.class, ConvolveImageNormalized.class);
	}

	@Test
	public void compareToStandard() {
		performTests(20);
	}

	@Override
	protected boolean isTestMethod(Method m) {
		Class<?> params[] = m.getParameterTypes();

		if( params.length < 3 || params.length > 5)
			return false;

		return ImageGray.class.isAssignableFrom(params[0]);
	}

	@Override
	protected boolean isEquivalent(Method validation, Method target) {

		Class<?>[] v = validation.getParameterTypes();
		Class<?>[] c = target.getParameterTypes();

		if( !target.getName().equals(validation.getName()))
			return false;

		if( c[0] != v[1] || c[1] != v[2])
			return false;

		if( target.getName().equals("vertical")) {
			if( ImageBorder.class.isAssignableFrom(c[3]) ) {
				return v.length >= 4 && ImageBorder.class.isAssignableFrom(v[3]);
			} else if( v.length != 3 ){
				return false;
			}
		} else if( (c.length == 3) ^ (v.length == 3)) {
			return false;
		}

		return true;
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {

		Class[] c = candidate.getParameterTypes();

		ImageGray input = GeneralizedImageOps.createSingleBand(c[0], width, height);
		ImageGray output = GeneralizedImageOps.createSingleBand(c[1], width, height);

		GImageMiscOps.fillUniform(input, rand, 0, 100);

		ImageBorder border = FactoryImageBorder.generic(BorderType.REFLECT,input.getImageType());

		Object[][] ret = new Object[2][];
		if( c.length == 3 ) {
			ret[0] = new Object[]{input, output, kernelRadius};
			ret[1] = new Object[]{input, output, kernelRadius2};
		} else if( c.length == 4 ) {
			ret[0] = new Object[]{input, output, kernelRadius, null};
			ret[1] = new Object[]{input, output, kernelRadius2, null};
			if( ImageBorder.class.isAssignableFrom(c[3]) ) {
				ret[0][3] = border;
				ret[1][3] = border;
			}
		} else {
			ret[0] = new Object[]{input, output, kernelRadius, border, null};
			ret[1] = new Object[]{input, output, kernelRadius2, border, null};
		}

		return ret;
	}

	@Override
	protected Object[] reformatForValidation(Method m, Object[] targetParam) {
		Class<?>[] params = m.getParameterTypes();
		int radius = (Integer)targetParam[2];
		Object kernel = createTableKernel(params[0],radius);

		ImageGray output = (ImageGray)((ImageGray)targetParam[1]).clone();

		if( ImageBorder.class.isAssignableFrom(params[params.length-1])) {
			return new Object[]{kernel, targetParam[0], output, targetParam[3]};
		} else {
			return new Object[]{kernel, targetParam[0], output};
		}
	}

	@Override
	protected void compareResults(Object targetResult, Object[] targetParam, Object validationResult, Object[] validationParam) {

		if (validationParam.length == 3) {
			ImageGray expected = (ImageGray) validationParam[2];
			ImageGray found = (ImageGray) targetParam[1];

			BoofTesting.assertEquals(expected, found, 1e-4);
		} else {
			ImageGray expected = (ImageGray) validationParam[2];
			ImageGray found = (ImageGray) targetParam[1];

			BoofTesting.assertEquals(expected, found, 1e-4);
		}
	}

	public static Object createTableKernel(Class<?> kernelType, int kernelRadius) {
		Object kernel;
		if (Kernel1D_F32.class == kernelType) {
			kernel = FactoryKernel.table1D_F32(kernelRadius, true);
		} else if (Kernel1D_F64.class == kernelType) {
			kernel = FactoryKernel.table1D_F64(kernelRadius,true);
		} else if (Kernel1D_S32.class == kernelType) {
			kernel = FactoryKernel.table1D_S32(kernelRadius);
		} else {
			throw new RuntimeException("Unknown kernel type");
		}
		return kernel;
	}
}
