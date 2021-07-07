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
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_F64;
import boofcv.struct.convolve.Kernel1D_S32;
import boofcv.struct.image.ImageGray;
import boofcv.testing.CompareEquivalentFunctions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

@SuppressWarnings({"rawtypes", "unchecked"})
public class TestImplConvolveMean extends CompareEquivalentFunctions {

	static int width = 10;
	static int height = 12;
	static int kernelOffset = 2;
	static int kernelLength = 5;

	public TestImplConvolveMean() {
		super(ImplConvolveMean.class, ConvolveImageStandard_SB.class);
	}

	@Test void compareToStandard() {
		performTests(10);
	}

	@Override
	protected boolean isTestMethod( Method m ) {
		Class<?>[] params = m.getParameterTypes();

		if (params.length != 4 && params.length != 5)
			return false;

		return ImageGray.class.isAssignableFrom(params[0]);
	}

	@Override
	protected boolean isEquivalent( Method candidate, Method validation ) {
		Class<?>[] c = candidate.getParameterTypes();
		Class<?>[] v = validation.getParameterTypes();

		if (c.length < 3)
			return false;

		if (!GeneralizedImageOps.isFloatingPoint(v[0])) {
			if (c.length != 4 && c.length != 5)
				return false;
		} else {
			if (c.length != 3)
				return false;
		}

		if (!candidate.getName().equals(validation.getName()))
			return false;

		return v[0] == c[1] && v[1] == c[2];
	}

	@Override
	protected Object[][] createInputParam( Method candidate, Method validation ) {

		Class[] candidateParam = candidate.getParameterTypes();

		ImageGray input = GeneralizedImageOps.createSingleBand(candidateParam[0], width, height);
		ImageGray output = GeneralizedImageOps.createSingleBand(candidateParam[1], width, height);

		GImageMiscOps.fillUniform(input, rand, 0, 50);

		Object[][] ret = new Object[3][];
		if (candidateParam.length == 4) {
			ret[0] = new Object[]{input, output, kernelOffset, kernelLength};
			ret[1] = new Object[]{input, output, kernelOffset + 1, kernelLength};
			ret[2] = new Object[]{input, output, kernelOffset, kernelLength - 1};
		} else {
			// vertical has one more argument
			ret[0] = new Object[]{input, output, kernelOffset, kernelLength, null};
			ret[1] = new Object[]{input, output, kernelOffset + 1, kernelLength, null};
			ret[2] = new Object[]{input, output, kernelOffset, kernelLength - 1, null};
		}

		return ret;
	}

	@Override
	protected Object[] reformatForValidation( Method m, Object[] targetParam ) {
		Class<?>[] params = m.getParameterTypes();
		Object kernel = createTableKernel(params[0], (Integer)targetParam[2], (Integer)targetParam[3]);

		ImageGray output = (ImageGray)((ImageGray)targetParam[1]).clone();

		if (output.getDataType().isInteger())
			if (m.getName().equals("vertical"))
				return new Object[]{kernel, targetParam[0], output, targetParam[3], null};
			else
				return new Object[]{kernel, targetParam[0], output, targetParam[3]};
		else
			return new Object[]{kernel, targetParam[0], output};
	}

	@Override
	protected void compareResults( Object targetResult, Object[] targetParam, Object validationResult, Object[] validationParam ) {
		ImageGray expected = (ImageGray)validationParam[2];
		ImageGray found = (ImageGray)targetParam[1];

		BoofTesting.assertEquals(expected, found, 1e-4);
	}

	public static Object createTableKernel( Class<?> kernelType, int offset, int length ) {
		Object kernel;
		if (Kernel1D_F32.class == kernelType) {
			kernel = FactoryKernel.table1D_F32(offset, length, true);
		} else if (Kernel1D_F64.class == kernelType) {
			kernel = FactoryKernel.table1D_F64(offset, length, true);
		} else if (Kernel1D_S32.class == kernelType) {
			kernel = FactoryKernel.table1D_S32(offset, length);
		} else {
			throw new RuntimeException("Unknown kernel type");
		}
		return kernel;
	}
}
