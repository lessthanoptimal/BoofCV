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

package boofcv.alg.filter.convolve.normalized;

import boofcv.alg.filter.convolve.ConvolutionTestHelper;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.image.ImageSingleBand;
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

	public CompareToStandardConvolutionNormalized( Class<?> targetClass ) {
		super(targetClass, ConvolveNormalizedNaive.class);
	}

	public void setImageDimension( int width , int height ) {
		this.width = width;
		this.height = height;
	}

	public void setKernelRadius(int kernelRadius) {
		this.kernelRadius = kernelRadius;
	}

	public void compareMethod( Method target , String validationName , int radius ) {
		this.kernelRadius = radius;
		super.compareMethod(target,validationName);
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {
		Class<?> paramTypes[] = candidate.getParameterTypes();

		Object kernel = FactoryKernelGaussian.gaussian((Class)paramTypes[0],-1,kernelRadius);

		ImageSingleBand src = ConvolutionTestHelper.createImage(paramTypes[1], width, height);
		GImageMiscOps.fillUniform(src, rand, 0, 5);
		ImageSingleBand dst = ConvolutionTestHelper.createImage(paramTypes[2], width, height);

		Object[][] ret = new Object[1][paramTypes.length];
		ret[0][0] = kernel;
		ret[0][1] = src;
		ret[0][2] = dst;
		if( paramTypes.length == 4) {
			ret[0][3] = 11;
		}

		return ret;
	}
}
