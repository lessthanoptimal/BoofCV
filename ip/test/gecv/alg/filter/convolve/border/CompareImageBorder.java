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

package gecv.alg.filter.convolve.border;

import gecv.alg.filter.convolve.ConvolveImageNoBorder;
import gecv.alg.filter.kernel.FactoryKernelGaussian;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.convolve.Kernel2D_I32;
import gecv.struct.image.ImageBase;
import gecv.testing.CompareEquivalentFunctions;

import java.lang.reflect.Method;
import java.util.Random;

/**
 * Validates an algorithm that convolves the image's border by convolving a larger image using {@link gecv.alg.filter.convolve.ConvolveImageNoBorder}.
 * The larger image has been designed so that by only convolving the inner portion it will produce the same result as convolving the borders
 * of the smaller image image.
 *
 * @author Peter Abeles
 */
public abstract class CompareImageBorder extends CompareEquivalentFunctions {

	protected Random rand = new Random(234324);
	protected int kernelRadius = 2;
	protected int width = 30;
	protected int height = 40;

	public CompareImageBorder(Class<?> targetClass ) {
		super(targetClass, ConvolveImageNoBorder.class);
	}

	@Override
	protected boolean isTestMethod(Method m) {
		Class<?> e[] = m.getParameterTypes();

		for( Class<?> c : e ) {
			if( ImageBase.class.isAssignableFrom(c))
				return true;
		}
		return false;
	}
	
	protected ImageBase stripBorder( ImageBase a ) {
		return a.subimage(kernelRadius,kernelRadius,width+kernelRadius,height+kernelRadius);
	}

	protected Object createKernel(Class<?> kernelType) {
		Object kernel;
		if (Kernel1D_F32.class == kernelType) {
			kernel = FactoryKernelGaussian.gaussian1D_F32(kernelRadius,true);
		} else if (Kernel1D_I32.class == kernelType) {
			kernel = FactoryKernelGaussian.gaussian1D_I32(kernelRadius);
		} else if (Kernel2D_F32.class == kernelType) {
			kernel = FactoryKernelGaussian.gaussian2D_F32(1,kernelRadius,true);
		} else if (Kernel2D_I32.class == kernelType) {
			kernel = FactoryKernelGaussian.gaussian2D_I32(1,kernelRadius);
		} else {
			throw new RuntimeException("Unknown kernel type");
		}
		return kernel;
	}
}
