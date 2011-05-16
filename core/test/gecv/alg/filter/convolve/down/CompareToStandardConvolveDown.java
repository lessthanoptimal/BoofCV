/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.filter.convolve.down;

import gecv.alg.filter.convolve.ConvolutionTestHelper;
import gecv.alg.filter.convolve.ConvolveDownNoBorder;
import gecv.alg.filter.convolve.KernelFactory;
import gecv.core.image.GeneralizedImageOps;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.convolve.Kernel2D_I32;
import gecv.struct.image.ImageBase;
import gecv.testing.CompareIdenticalFunctions;

import java.lang.reflect.Method;
import java.util.Random;

/**
 * Compares the target class to functions in {@link ConvolveDownNoBorder}.
 *
 * @author Peter Abeles
 */
public class CompareToStandardConvolveDown extends CompareIdenticalFunctions
{
	protected Random rand = new Random(0xFF);

	protected int width = 20;
	protected int height = 30;
	protected int kernelRadius = 1;
	protected int skip = 2;

	public CompareToStandardConvolveDown( Class<?> targetClass ) {
		super(targetClass, ConvolveDownNoBorderStandard.class);
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
			kernel = KernelFactory.random1D_F32(kernelRadius, -1, 1, rand);
		} else if (Kernel1D_I32.class == paramTypes[0]) {
			kernel = KernelFactory.random1D_I32(kernelRadius, 0, 5, rand);
		} else if (Kernel2D_I32.class == paramTypes[0]) {
			kernel = KernelFactory.random2D_I32(kernelRadius, -1, 1, rand);
		} else if (Kernel2D_F32.class == paramTypes[0]) {
			kernel = KernelFactory.random2D_F32(kernelRadius, 0, 5, rand);
		} else {
			throw new RuntimeException("Unknown kernel type");
		}

		ImageBase src = ConvolutionTestHelper.createImage(paramTypes[1], width, height);
		GeneralizedImageOps.randomize(src, 0, 130, rand);
		ImageBase dst = ConvolutionTestHelper.createImage(paramTypes[2], width, height);

		Object[][] ret = new Object[1][paramTypes.length];
		ret[0][0] = kernel;
		ret[0][1] = src;
		ret[0][2] = dst;
		ret[0][3] = skip;
		if( paramTypes.length == 5) {
			ret[0][4] = 11;
		}

		return ret;
	}
}
