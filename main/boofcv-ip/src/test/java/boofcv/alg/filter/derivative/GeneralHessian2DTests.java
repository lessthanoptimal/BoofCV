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

package boofcv.alg.filter.derivative;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.border.ImageBorder_F32;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.convolve.Kernel2D_S32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests that are designed for 2D gradient kernels. Ensures coding standards are following, floating point
/// kernels are correctly normalized, and that scale has been set correctly.
///
/// These tests are derived from kernels needing
///
/// 1) Preserves brightness. Sum g[i] == 1
/// 2) Must be zero on flat regions Sum a*g[i] == 0
public abstract class GeneralHessian2DTests extends BoofStandardJUnit {
	int width = 20;
	int height = 25;

	Class targetClass;

	public GeneralHessian2DTests( Class targetClass ) {
		this.targetClass = targetClass;
	}

	/// Derive the class being tested from the file name. Preferred approach.
	public GeneralHessian2DTests() {
		String testClassName = getClass().getSimpleName().substring(4);
		try {
			targetClass = Class.forName(getClass().getPackageName() + "." + testClassName);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Test void kernelInt() throws NoSuchFieldException, IllegalAccessException {
		int divisor = (int)targetClass.getDeclaredField("divisor").get(null);
		var kernelXX = (Kernel2D_S32)targetClass.getDeclaredField("kernelXX_I32").get(null);
		var kernelXY = (Kernel2D_S32)targetClass.getDeclaredField("kernelXY_I32").get(null);
		var kernelYY = (Kernel2D_S32)targetClass.getDeclaredField("kernelYY_I32").get(null);

		// Test that they are transpose of each other
		for (int row = 0; row < kernelXX.width; row++) {
			for (int col = 0; col < kernelXX.width; col++) {
				assertEquals(kernelXX.get(row, col), kernelYY.get(col, row));
			}
		}

		int sumXX = 0;
		for (int y = 0; y < kernelXX.width; y++) {
			for (int x = 0; x < kernelXX.width; x++) {
				sumXX += Math.abs(kernelXX.get(x, y));
			}
		}
		assertEquals(sumXX, divisor);

		// This is a very weak test. In general, it would be better if we tested 2nd order by deriving them
		// from the 1st order derivatives.
		int sumXY = 0;
		for (int y = 0; y < kernelXY.width; y++) {
			for (int x = 0; x < kernelXY.width; x++) {
				sumXY += Math.abs(kernelXY.get(x, y));
			}
		}
		assertTrue(sumXY <= sumXX);
	}

	@Test void kernelFloat() throws NoSuchFieldException, IllegalAccessException {
		var kernelXX = (Kernel2D_F32)targetClass.getDeclaredField("kernelXX_F32").get(null);
		var kernelXY = (Kernel2D_F32)targetClass.getDeclaredField("kernelXY_F32").get(null);
		var kernelYY = (Kernel2D_F32)targetClass.getDeclaredField("kernelYY_F32").get(null);

		// Test that they are transpose of each other
		for (int row = 0; row < kernelXX.width; row++) {
			for (int col = 0; col < kernelXX.width; col++) {
				assertEquals(kernelXX.get(row, col), kernelYY.get(col, row));
			}
		}

		float sumXX = 0.0f;
		for (int y = 0; y < kernelXX.width; y++) {
			for (int x = 0; x < kernelXX.width; x++) {
				sumXX += Math.abs(kernelXX.get(x, y));
			}
		}
		assertEquals(1.0f, sumXX);

		int divisor = (int)targetClass.getDeclaredField("divisor").get(null);
		var kernelIntXY = (Kernel2D_S32)targetClass.getDeclaredField("kernelXY_I32").get(null);

		int sumIntXY = 0;
		float sumXY = 0;
		for (int y = 0; y < kernelXY.width; y++) {
			for (int x = 0; x < kernelXY.width; x++) {
				sumXY += Math.abs(kernelXY.get(x, y));
				sumIntXY += Math.abs(kernelIntXY.get(x, y));
			}
		}
		assertEquals(sumIntXY/(float)divisor, sumXY);
	}

	@Test void compareToConvolve_I8() throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
		var kernelXX = (Kernel2D_S32)targetClass.getDeclaredField("kernelXX_I32").get(null);
		var kernelXY = (Kernel2D_S32)targetClass.getDeclaredField("kernelXY_I32").get(null);
		var kernelYY = (Kernel2D_S32)targetClass.getDeclaredField("kernelYY_I32").get(null);

		var validator = new CompareDerivativeToConvolution();
		validator.setTarget(targetClass.getMethod("process",
				GrayU8.class, GrayS16.class, GrayS16.class, GrayS16.class, ImageBorder_S32.class));

		validator.setKernel(0, kernelXX);
		validator.setKernel(1, kernelYY);
		validator.setKernel(2, kernelXY);

		var input = new GrayU8(width, height);
		ImageMiscOps.fillUniform(input, rand, 0, 200);
		var derivXX = new GrayS16(width, height);
		var derivYY = new GrayS16(width, height);
		var derivXY = new GrayS16(width, height);

		validator.compare(input, derivXX, derivYY, derivXY);
	}

	@Test void compareToConvolve_F32() throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
		var kernelXX = (Kernel2D_F32)targetClass.getDeclaredField("kernelXX_F32").get(null);
		var kernelXY = (Kernel2D_F32)targetClass.getDeclaredField("kernelXY_F32").get(null);
		var kernelYY = (Kernel2D_F32)targetClass.getDeclaredField("kernelYY_F32").get(null);

		var validator = new CompareDerivativeToConvolution();
		validator.setTarget(targetClass.getMethod("process",
				GrayF32.class, GrayF32.class, GrayF32.class, GrayF32.class, ImageBorder_F32.class));

		validator.setKernel(0, kernelXX);
		validator.setKernel(1, kernelYY);
		validator.setKernel(2, kernelXY);

		var input = new GrayF32(width, height);
		ImageMiscOps.fillUniform(input, rand, 0, 10);
		var derivXX = new GrayF32(width, height);
		var derivYY = new GrayF32(width, height);
		var derivXY = new GrayF32(width, height);

		validator.compare(input, derivXX, derivYY, derivXY);
	}
}
