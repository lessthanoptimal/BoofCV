/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.filter.convolve.noborder.ConvolveImageStandard;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_I32;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.convolve.Kernel2D_I32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.testing.CompareIdenticalFunctions;

import java.lang.reflect.Method;
import java.util.Random;

/**
 * Compares the target class to functions in the standard convolution class.
 *
 * @author Peter Abeles
 */
public class CompareToStandardConvolution extends CompareIdenticalFunctions
{
	protected Random rand = new Random(0xFF);

	protected int width = 20;
	protected int height = 30;
	protected int kernelRadius = 1;

	public CompareToStandardConvolution( Class<?> targetClass ) {
		super(targetClass, ConvolveImageStandard.class);
	}

	public void compareMethod( Method target , String validationName , int radius ) {
		this.kernelRadius = radius;
		super.compareMethod(target,validationName);
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {
		Class<?> paramTypes[] = candidate.getParameterTypes();

		Object kernel;
		if (Kernel1D_F32.class == paramTypes[0]) {
			kernel = FactoryKernel.random1D_F32(kernelRadius, -1, 1, rand);
		} else if (Kernel1D_I32.class == paramTypes[0]) {
			kernel = FactoryKernel.random1D_I32(kernelRadius, 0, 5, rand);
		} else if (Kernel2D_I32.class == paramTypes[0]) {
			kernel = FactoryKernel.random2D_I32(kernelRadius, -1, 1, rand);
		} else if (Kernel2D_F32.class == paramTypes[0]) {
			kernel = FactoryKernel.random2D_F32(kernelRadius, 0, 5, rand);
		} else {
			throw new RuntimeException("Unknown kernel type");
		}

		ImageSingleBand src = ConvolutionTestHelper.createImage(paramTypes[1], width, height);
		GImageMiscOps.fillUniform(src, rand, 0, 130);
		ImageSingleBand dst = ConvolutionTestHelper.createImage(paramTypes[2], width, height);

		if( candidate.getName().compareTo("convolve") != 0 ) {
			Object[][] ret = new Object[2][paramTypes.length];
			int i = 0;
			ret[0][i] = ret[1][i] = kernel; i++;
			ret[0][i] = ret[1][i] = src;    i++;
			ret[0][i] = ret[1][i] = dst;    i++;
			if( paramTypes.length == 5) {
				ret[0][i] = ret[1][i] = 11;
				i++;
			}
			ret[0][i] = true;
			ret[1][i] = false;

			return ret;
		} else {
			Object[][] ret = new Object[1][paramTypes.length];

			ret[0][0] = kernel;
			ret[0][1] = src;
			ret[0][2] = dst;

			if( paramTypes.length == 4 ) {
				ret[0][3] = 11;
			}

			return ret;
		}
	}
}
