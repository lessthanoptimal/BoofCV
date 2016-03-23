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

import boofcv.alg.filter.convolve.normalized.ConvolveNormalizedStandardSparse;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_I32;
import boofcv.struct.image.ImageGray;
import boofcv.testing.CompareIdenticalFunctions;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestConvolveNormalizedSparse {
	Random rand = new Random(0xFF);

	int width = 10;
	int height = 15;
	int kernelRadius = 1;

	int testX = 0;
	int testY = 3;

	@Test
	public void compareToStandard() {
		CompareToStandard a = new CompareToStandard();
		testX = 0; testY = 3;
		a.performTests(3);
		testX = 3; testY = 0;
		a.performTests(3);
		testX = width-1; testY = 3;
		a.performTests(3);
		testX = 3; testY = height-1;
		a.performTests(3);
		testX = 5; testY = 5;
		a.performTests(3);
	}

	public class CompareToStandard extends CompareIdenticalFunctions
	{
		protected CompareToStandard() {
			super(ConvolveNormalizedSparse.class, ConvolveNormalizedStandardSparse.class);
		}

		@Override
		protected void compareResults(Object targetResult, Object[] targetParam, Object validationResult, Object[] validationParam) {
			Number a = (Number)targetResult;
			Number b = (Number)validationResult;

			assertEquals(a.doubleValue(),b.doubleValue(),1e-4);
		}

		@Override
		protected Object[][] createInputParam(Method candidate, Method validation) {
			Class<?> paramTypes[] = candidate.getParameterTypes();

			Object storage;
			Object kernel;
			kernel = FactoryKernelGaussian.gaussian((Class)paramTypes[0],-1,kernelRadius);
			if (Kernel1D_F32.class == paramTypes[0]) {
				storage = new float[ kernelRadius*2+1];
			} else if (Kernel1D_I32.class == paramTypes[0]) {
				storage = new int[ kernelRadius*2+1];
			} else {
				throw new RuntimeException("Unknown kernel type");
			}

			ImageGray src = ConvolutionTestHelper.createImage(paramTypes[2], width, height);
			GImageMiscOps.fillUniform(src, rand, 0, 5);


			Object[][] ret = new Object[1][paramTypes.length];

			ret[0][0] = kernel;
			ret[0][1] = kernel;
			ret[0][2] = src;
			ret[0][3] = testX;
			ret[0][4] = testY;
			ret[0][5] = storage;

			return ret;
		}
	}
}
