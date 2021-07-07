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

package boofcv.alg.filter.convolve;

import boofcv.BoofTesting;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.ImageBorderValue;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_F64;
import boofcv.struct.convolve.Kernel1D_S32;
import boofcv.struct.image.ImageGray;
import boofcv.testing.CompareEquivalentFunctions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestConvolveImageBox extends CompareEquivalentFunctions {

	static int width = 10;
	static int height = 12;
	static int kernelRadius = 2;

	public TestConvolveImageBox() {
		super(ConvolveImageBox.class, ConvolveImage.class);
	}

	@Test void compareToStandard() {
		performTests(8);
	}

	@Override
	protected boolean isTestMethod(Method m) {
		Class<?> params[] = m.getParameterTypes();

		if( params.length != 3 && params.length != 4 )
			return false;

		return ImageGray.class.isAssignableFrom(params[0]);
	}

	@Override
	protected boolean isEquivalent(Method candidate, Method validation) {

		Class<?> v[] = candidate.getParameterTypes();
		Class<?> c[] = validation.getParameterTypes();

		if( v.length != 4 )
			return false;

		if( !candidate.getName().equals(validation.getName()))
			return false;

		return c[0] == v[1] && c[1] == v[2];
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {

		Class c[] = candidate.getParameterTypes();

		ImageGray input = GeneralizedImageOps.createSingleBand(c[0], width, height);
		ImageGray output = GeneralizedImageOps.createSingleBand(c[1], width, height);

		GImageMiscOps.fillUniform(input, rand, 0, 20);

		Object[][] ret = new Object[1][];
		if( c.length == 3 )
			ret[0] = new Object[]{input,output,kernelRadius};
		else
			ret[0] = new Object[]{input,output,kernelRadius,null};
		return ret;
	}

	@Override
	protected Object[] reformatForValidation(Method m, Object[] targetParam) {
		Class<?> params[] = m.getParameterTypes();
		Object kernel = TestConvolveImageBox.createTableKernel(params[0],kernelRadius,rand);

		ImageGray output = (ImageGray)((ImageGray)targetParam[1]).clone();

		ImageBorder border = output.getDataType().isInteger() ? new ImageBorderValue.Value_I(0) : new ImageBorderValue.Value_F32(0);

		return new Object[]{kernel,targetParam[0],output,border };
	}

	@Override
	protected void compareResults(Object targetResult, Object[] targetParam, Object validationResult, Object[] validationParam) {
		ImageGray expected = (ImageGray)validationParam[2];
		ImageGray found = (ImageGray)targetParam[1];

		BoofTesting.assertEquals(expected, found, 1e-4);
	}

	public static Object createTableKernel(Class<?> kernelType, int kernelRadius, Random rand) {
		Object kernel;
		if (Kernel1D_F32.class == kernelType) {
			kernel = FactoryKernel.table1D_F32(kernelRadius, false);
		} else if (Kernel1D_F64.class == kernelType) {
			kernel = FactoryKernel.table1D_F64(kernelRadius, false);
		} else if (Kernel1D_S32.class == kernelType) {
			kernel = FactoryKernel.table1D_S32(kernelRadius);
		} else {
			throw new RuntimeException("Unknown kernel type: "+kernelType.getSimpleName());
		}
		return kernel;
	}

}
