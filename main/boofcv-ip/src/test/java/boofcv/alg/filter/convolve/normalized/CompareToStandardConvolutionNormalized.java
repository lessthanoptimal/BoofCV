/*
 * Copyright (c) 2025, Peter Abeles. All Rights Reserved.
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
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.convolve.KernelBase;
import boofcv.struct.image.ImageBase;
import boofcv.testing.CompareIdenticalFunctions;
import pabeles.concurrency.GrowArray;

import java.lang.reflect.Method;

/**
 * Compares the target class to functions in the standard convolution class.
 *
 * @author Peter Abeles
 */
public class CompareToStandardConvolutionNormalized extends CompareIdenticalFunctions {
	protected int width = 7;
	protected int height = 8;
	protected int kernelRadius = 1;
	protected int offset = 1;

	public CompareToStandardConvolutionNormalized( Class<?> targetClass ) {
		super(targetClass, ConvolveNormalizedNaive_SB.class, ConvolveNormalizedNaive_IL.class);
	}

	public void setImageDimension( int width, int height ) {
		this.width = width;
		this.height = height;
	}

	public void setKernelRadius( int kernelRadius, int offset ) {
		this.kernelRadius = kernelRadius;
		this.offset = offset;
	}

	public void compareMethod( Method target, String validationName, int radius ) {
		this.kernelRadius = radius;
		super.compareMethod(target, validationName);
	}

	@Override
	protected Object[] reformatForValidation( Method m, Object[] targetParam ) {
		// validation wont have workspace parameters
		int count = m.getParameterCount();
		Object[] ret = new Object[count];

		for (int i = 0; i < count; i++) {
			if (targetParam[i] == null)
				continue;
			if (ImageBase.class.isAssignableFrom(targetParam[i].getClass())) {
				ret[i] = ((ImageBase)targetParam[i]).clone();
			} else {
				ret[i] = targetParam[i];
			}
		}

		return ret;
	}

	@Override
	protected Object[][] createInputParam( Method candidate, Method validation ) {
		Class<?>[] candidateTypes = candidate.getParameterTypes();
		Class<?>[] validTypes = validation.getParameterTypes();

		boolean hasWorkspace = candidateTypes[candidateTypes.length - 1].isAssignableFrom(GrowArray.class);

		Object[][] ret = new Object[1][candidateTypes.length];

		ret[0][0] = FactoryKernel.random((Class)candidateTypes[0], kernelRadius, 1, 10, rand);
		((KernelBase)ret[0][0]).offset = offset;

		int index = 1;
		if (KernelBase.class.isAssignableFrom(candidateTypes[1])) {
			ret[0][index] = FactoryKernel.random((Class)candidateTypes[1], kernelRadius, 1, 10, rand);
			((KernelBase)ret[0][index]).offset = offset;
			index++;
		}

		ImageBase src = ConvolutionTestHelper.createImage(candidateTypes[index], width, height);
		ret[0][index++] = src;

		double maxValue = Math.min(10_000, src.getImageType().getDataType().getMaxValue()/5);
		GImageMiscOps.fillUniform(src, rand, 0, maxValue);

		ImageBase dst = ConvolutionTestHelper.createImage(candidateTypes[index], width, height);
		ret[0][index] = dst;

		if (validTypes.length == 4 && ImageBorder.class.isAssignableFrom(candidateTypes[3]) && !hasWorkspace) {
			ret[0][3] = FactoryImageBorder.generic(BorderType.REFLECT, src.getImageType());
		}

		return ret;
	}
}
