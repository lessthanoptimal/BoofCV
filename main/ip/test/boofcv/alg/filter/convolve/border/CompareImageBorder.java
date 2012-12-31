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

package boofcv.alg.filter.convolve.border;

import boofcv.alg.filter.convolve.ConvolveImageNoBorder;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.image.ImageSingleBand;
import boofcv.testing.CompareEquivalentFunctions;

import java.lang.reflect.Method;
import java.util.Random;

/**
 * Validates an algorithm that convolves the image's border by convolving a larger image using {@link boofcv.alg.filter.convolve.ConvolveImageNoBorder}.
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
			if( ImageSingleBand.class.isAssignableFrom(c))
				return true;
		}
		return false;
	}
	
	protected ImageSingleBand stripBorder( ImageSingleBand a ) {
		return a.subimage(kernelRadius,kernelRadius,width+kernelRadius,height+kernelRadius);
	}

	protected Object createKernel(Class kernelType) {
		return FactoryKernelGaussian.gaussian(kernelType,-1,kernelRadius);
	}
}
