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

package boofcv.alg.filter.convolve.border;

import boofcv.alg.filter.convolve.ConvolveImageNoBorder;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.struct.convolve.Kernel1D;
import boofcv.struct.convolve.KernelBase;
import boofcv.struct.image.ImageGray;
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
	protected int width = 30;
	protected int height = 40;

	protected int borderX0=0,borderY0=0;
	protected int borderX1=0,borderY1=0;

	public CompareImageBorder(Class<?> targetClass ) {
		super(targetClass, ConvolveImageNoBorder.class);
	}

	@Override
	protected boolean isTestMethod(Method m) {
		Class<?> e[] = m.getParameterTypes();

		for( Class<?> c : e ) {
			if( ImageGray.class.isAssignableFrom(c))
				return true;
		}
		return false;
	}

	protected void computeBorder( KernelBase kernel , String functionName ) {
		borderX0=borderY0=0;
		borderX1=borderY1=0;

		if( kernel instanceof Kernel1D) {
			if( functionName.contains("horizontal")) {
				borderX0 = kernel.getOffset();
				borderX1 = kernel.getWidth() - kernel.getOffset() - 1;
			} else {
				borderY0 = kernel.getOffset();
				borderY1 = kernel.getWidth() - kernel.getOffset() - 1;
			}
		} else {
			borderX0 = borderY0 = kernel.getOffset();
			borderX1 = borderY1 = kernel.getWidth()-kernel.getOffset()-1;
		}
	}

	protected ImageGray stripBorder(ImageGray a ,
									int borderX0 , int borderY0,
									int borderX1 , int borderY1 ) {
		return a.subimage(borderX0,borderY0,a.width-borderX1,a.height-borderY1, null);
	}

	protected KernelBase createKernel(Class kernelType, int kernelWidth, int kernelOffset) {
		KernelBase k = FactoryKernel.random(kernelType, kernelWidth, kernelOffset, -12, 10, rand);
		return k;
	}
}
