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

package boofcv.alg.filter.convolve;

import boofcv.alg.filter.convolve.noborder.ConvolveImageStandard;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.struct.convolve.*;
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
	protected int kernelRadius = 2;
	protected int offset = kernelRadius;

	public CompareToStandardConvolution( Class<?> targetClass ) {
		super(targetClass, ConvolveImageStandard.class);
	}

	public void compareMethod( Method target , String validationName , int radius ) {
		compareMethod(target,validationName,radius,radius);
	}

	public void compareMethod( Method target , String validationName , int radius , int offset ) {
		this.kernelRadius = radius;
		this.offset = offset;
		super.compareMethod(target,validationName);
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {
		Class<?> paramTypes[] = candidate.getParameterTypes();

		ImageSingleBand src = ConvolutionTestHelper.createImage(paramTypes[1], width, height);
		GImageMiscOps.fillUniform(src, rand, 0, 130);
		ImageSingleBand dst = ConvolutionTestHelper.createImage(paramTypes[2], width, height);

		if( candidate.getName().compareTo("convolve") != 0 ) {
			Object[][] ret = new Object[1][paramTypes.length];
			ret[0][0] = createKernel(paramTypes[0]);
			ret[0][1] = src;
			ret[0][2] = dst;
			if( paramTypes.length == 4) {
				ret[0][3] = 11;
			}


			return ret;
		} else {
			Object[][] ret = new Object[1][paramTypes.length];

			ret[0][0] = createKernel(paramTypes[0]);
			ret[0][1] = src;
			ret[0][2] = dst;

			if( paramTypes.length == 4 ) {
				ret[0][3] = 11;
			}

			return ret;
		}
	}

	private KernelBase createKernel(Class<?> paramType) {
		KernelBase kernel;
		if (Kernel1D_F32.class == paramType) {
			kernel = FactoryKernel.random1D_F32(kernelRadius, -1, 1, rand);
		} else if (Kernel1D_I32.class == paramType) {
			kernel = FactoryKernel.random1D_I32(kernelRadius, 0, 5, rand);
		} else if (Kernel2D_I32.class == paramType) {
			kernel = FactoryKernel.random2D_I32(kernelRadius, -1, 1, rand);
		} else if (Kernel2D_F32.class == paramType) {
			kernel = FactoryKernel.random2D_F32(kernelRadius, 0, 5, rand);
		} else {
			throw new RuntimeException("Unknown kernel type");
		}
		kernel.offset = offset;
		return kernel;
	}

	public void setKernelRadius(int kernelRadius) {
		this.kernelRadius = kernelRadius;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}
}
