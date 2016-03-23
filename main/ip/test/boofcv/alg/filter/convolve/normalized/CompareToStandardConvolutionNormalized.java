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

package boofcv.alg.filter.convolve.normalized;

import boofcv.alg.filter.convolve.ConvolutionTestHelper;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.struct.convolve.KernelBase;
import boofcv.struct.image.ImageGray;
import boofcv.testing.CompareIdenticalFunctions;

import java.lang.reflect.Method;
import java.util.Random;

/**
 * Compares the target class to functions in the standard convolution class.
 *
 * @author Peter Abeles
 */
public class CompareToStandardConvolutionNormalized extends CompareIdenticalFunctions
{
	protected Random rand = new Random(0xFF);

	protected int width = 7;
	protected int height = 8;
	protected int kernelRadius = 1;
	protected int offset = 1;

	public CompareToStandardConvolutionNormalized( Class<?> targetClass ) {
		super(targetClass, ConvolveNormalizedNaive.class);
	}

	public void setImageDimension( int width , int height ) {
		this.width = width;
		this.height = height;
	}

	public void setKernelRadius(int kernelRadius , int offset) {
		this.kernelRadius = kernelRadius;
		this.offset = offset;
	}

	public void compareMethod( Method target , String validationName , int radius ) {
		this.kernelRadius = radius;
		super.compareMethod(target,validationName);
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {
		Class<?> paramTypes[] = candidate.getParameterTypes();

		Object[][] ret = new Object[1][paramTypes.length];

		ret[0][0]= FactoryKernel.random((Class) paramTypes[0], kernelRadius,1,10,rand);
		((KernelBase)ret[0][0]).offset = offset;

		int index = 1;
		if( KernelBase.class.isAssignableFrom(paramTypes[1]) ) {
			ret[0][index] = FactoryKernel.random((Class) paramTypes[1], kernelRadius, 1, 10, rand);
			((KernelBase)ret[0][index]).offset = offset;
			index++;
		}

		ImageGray src = ConvolutionTestHelper.createImage(paramTypes[index], width, height);
		ret[0][index++] = src;
		GImageMiscOps.fillUniform(src, rand, 0, 120);
		ImageGray dst = ConvolutionTestHelper.createImage(paramTypes[index], width, height);
		ret[0][index] = dst;


		return ret;
	}
}
