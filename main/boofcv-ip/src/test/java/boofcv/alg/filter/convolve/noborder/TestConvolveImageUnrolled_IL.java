/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.convolve.noborder;

import boofcv.BoofTesting;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.struct.convolve.*;
import boofcv.struct.image.ImageBase;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public class TestConvolveImageUnrolled_IL extends BoofStandardJUnit {
	int[] kernelWidths = {3, 5, 7};
	int[] bandCounts = {2, 3, 4};
	int width = 20;
	int height = 21;

	@Test void compareToStandard() throws Exception {
		int numExpected = 35;
		int numFound = 0;

		for (Method m : ConvolveImageUnrolled_IL.class.getMethods()) {
			if (!isTestMethod(m))
				continue;

			for (int kernelWidth : kernelWidths) {
				for (int numBands : bandCounts) {
					compareToStandard(m, kernelWidth, numBands);
				}
			}
			numFound++;
		}

		assertEquals(numExpected, numFound);
	}

	@Test void unsupportedKernelWidthReturnsFalse() throws Exception {
		for (Method m : ConvolveImageUnrolled_IL.class.getMethods()) {
			if (!isTestMethod(m))
				continue;

			Object[] args = createInputParam(m, 9, 4, 3);
			assertFalse((Boolean)m.invoke(null, args));
		}
	}

	@Test void unsupportedKernelOffsetReturnsFalse() throws Exception {
		for (Method m : ConvolveImageUnrolled_IL.class.getMethods()) {
			if (!isTestMethod(m))
				continue;

			Object[] args = createInputParam(m, 5, 1, 3);
			assertFalse((Boolean)m.invoke(null, args));
		}
	}

	@Test void unsupportedBandCountReturnsFalse() throws Exception {
		for (Method m : ConvolveImageUnrolled_IL.class.getMethods()) {
			if (!isTestMethod(m))
				continue;

			Object[] args = createInputParam(m, 3, 1, 5);
			assertFalse((Boolean)m.invoke(null, args));
		}
	}

	private void compareToStandard( Method candidate, int kernelWidth, int numBands ) throws Exception {
		Method validation = ConvolveImageStandard_IL.class.getMethod(candidate.getName(), candidate.getParameterTypes());

		Object[] candidateArgs = createInputParam(candidate, kernelWidth, kernelWidth/2, numBands);
		Object[] validationArgs = candidateArgs.clone();
		validationArgs[2] = createImage(candidate.getParameterTypes()[2], numBands);

		GImageMiscOps.fill((ImageBase)candidateArgs[2], 12);
		GImageMiscOps.fill((ImageBase)validationArgs[2], 12);

		assertTrue((Boolean)candidate.invoke(null, candidateArgs));
		validation.invoke(null, validationArgs);

		BoofTesting.assertEquals((ImageBase)validationArgs[2], (ImageBase)candidateArgs[2], tolerance((ImageBase)candidateArgs[2]));
	}

	private Object[] createInputParam( Method m, int kernelWidth, int kernelOffset, int numBands ) {
		Class[] paramTypes = m.getParameterTypes();
		Object[] args = new Object[paramTypes.length];

		ImageBase src = createImage(paramTypes[1], numBands);
		GImageMiscOps.fillUniform(src, rand, 0, 20);

		args[0] = createKernel(paramTypes[0], kernelWidth, kernelOffset);
		args[1] = src;
		args[2] = createImage(paramTypes[2], numBands);
		if (paramTypes.length == 4)
			args[3] = 11;

		return args;
	}

	private ImageBase createImage( Class imageType, int numBands ) {
		return GeneralizedImageOps.createImage(imageType, width, height, numBands);
	}

	private KernelBase createKernel( Class<?> paramType, int kernelWidth, int kernelOffset ) {
		if (Kernel1D_F32.class == paramType)
			return FactoryKernel.random1D_F32(kernelWidth, kernelOffset, -1, 1, rand);
		if (Kernel1D_F64.class == paramType)
			return FactoryKernel.random1D_F64(kernelWidth, kernelOffset, -1, 1, rand);
		if (Kernel1D_S32.class == paramType)
			return FactoryKernel.random1D_I32(kernelWidth, kernelOffset, -1, 1, rand);
		if (Kernel2D_F32.class == paramType)
			return FactoryKernel.random2D_F32(kernelWidth, kernelOffset, -1, 1, rand);
		if (Kernel2D_F64.class == paramType)
			return FactoryKernel.random2D_F64(kernelWidth, kernelOffset, -1, 1, rand);
		if (Kernel2D_S32.class == paramType)
			return FactoryKernel.random2D_I32(kernelWidth, kernelOffset, -1, 1, rand);
		throw new RuntimeException("Unknown kernel type " + paramType.getSimpleName());
	}

	private boolean isTestMethod( Method m ) {
		Class<?>[] params = m.getParameterTypes();

		if (params.length < 3 || m.getReturnType() != boolean.class)
			return false;

		return KernelBase.class.isAssignableFrom(params[0]);
	}

	private double tolerance( ImageBase image ) {
		return image.getImageType().getDataType().isInteger() ? 0.0 : 1e-4;
	}
}
