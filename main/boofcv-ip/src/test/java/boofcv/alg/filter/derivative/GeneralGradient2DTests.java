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

import boofcv.alg.filter.kernel.KernelMath;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.border.ImageBorder_F32;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.convolve.Kernel1D_S32;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.convolve.Kernel2D_S32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
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
public abstract class GeneralGradient2DTests extends BoofStandardJUnit {
	int width = 20;
	int height = 25;

	Class targetClass;

	public GeneralGradient2DTests( Class targetClass ) {
		this.targetClass = targetClass;
	}

	/// Derive the class being tested from the file name. Preferred approach.
	public GeneralGradient2DTests() {
		String testClassName = getClass().getSimpleName().substring(4);
		try {
			targetClass = Class.forName(getClass().getPackageName() + "." + testClassName);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Test void kernelInt() throws NoSuchFieldException, IllegalAccessException {
		int divisor = (int)targetClass.getDeclaredField("divisor").get(null);
		var derivX = (Kernel2D_S32)targetClass.getDeclaredField("kernelX_I32").get(null);
		var derivY = (Kernel2D_S32)targetClass.getDeclaredField("kernelY_I32").get(null);

		var smooth = (Kernel1D_S32)targetClass.getDeclaredField("kernelSmooth").get(null);
		var diff = (Kernel1D_S32)targetClass.getDeclaredField("kernelDiff").get(null);

		Kernel2D_S32 expectedX = kernel2D(smooth, diff, true);
		Kernel2D_S32 expectedY = kernel2D(smooth, diff, false);

		assertTrue(expectedX.isIdentical(derivX));
		assertTrue(expectedY.isIdentical(derivY));

		// Check the sign convention
		for (int row = 0; row < derivY.width/2; row++) {
			for (int col = 0; col < derivY.width; col++) {
				assertTrue(derivY.get(col, row) < 0);
			}
		}

		assertEquals(divisor(smooth, diff), divisor);
	}

	@Test void kernelFloat() throws NoSuchFieldException, IllegalAccessException {
		var derivX = (Kernel2D_F32)targetClass.getDeclaredField("kernelX_F32").get(null);
		var derivY = (Kernel2D_F32)targetClass.getDeclaredField("kernelY_F32").get(null);

		// Middle should be zeros
		for (int i = 0; i < derivX.width; i++) {
			assertEquals(0.0, derivX.get(1, i));
			assertEquals(0.0, derivY.get(i, 1));
		}

		// Test that they are transpose of each other
		for (int row = 0; row < derivX.width; row++) {
			for (int col = 0; col < derivX.width; col++) {
				assertEquals(derivX.get(row, col), derivY.get(col, row));
			}
		}

		// Check the sign convention
		for (int row = 0; row < derivY.width/2; row++) {
			for (int col = 0; col < derivY.width; col++) {
				assertTrue(derivY.get(col, row) < 0);
			}
		}

		// Test normalization
		float sum = 0.0f;
		for (int y = 0; y < derivY.width; y++) {
			for (int x = 0; x < derivY.width; x++) {
				sum += Math.abs(derivY.get(x, y));
			}
		}
		assertEquals(1.0f, sum, UtilEjml.TEST_F32);
	}

	@Test void compareToConvolve_I8() throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
		var kernelX = (Kernel2D_S32)targetClass.getDeclaredField("kernelX_I32").get(null);
		var kernelY = (Kernel2D_S32)targetClass.getDeclaredField("kernelY_I32").get(null);

		var validator = new CompareDerivativeToConvolution();
		validator.setTarget(targetClass.getMethod("process",
				GrayU8.class, GrayS16.class, GrayS16.class, ImageBorder_S32.class));

		validator.setKernel(0, kernelX);
		validator.setKernel(1, kernelY);

		var input = new GrayU8(width, height);
		ImageMiscOps.fillUniform(input, rand, 0, 200);
		var derivX = new GrayS16(width, height);
		var derivY = new GrayS16(width, height);

		validator.compare(input, derivX, derivY);
	}

	@Test void compareToConvolve_I16() throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
		var kernelX = (Kernel2D_S32)targetClass.getDeclaredField("kernelX_I32").get(null);
		var kernelY = (Kernel2D_S32)targetClass.getDeclaredField("kernelY_I32").get(null);

		var validator = new CompareDerivativeToConvolution();
		validator.setTarget(targetClass.getMethod("process",
				GrayS16.class, GrayS16.class, GrayS16.class, ImageBorder_S32.class));

		validator.setKernel(0, kernelX);
		validator.setKernel(1, kernelY);

		var input = new GrayS16(width, height);
		ImageMiscOps.fillUniform(input, rand, 0, 1000);
		var derivX = new GrayS16(width, height);
		var derivY = new GrayS16(width, height);

		validator.compare(input, derivX, derivY);
	}

	@Test void compareToConvolve_F32() throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
		var kernelX = (Kernel2D_F32)targetClass.getDeclaredField("kernelX_F32").get(null);
		var kernelY = (Kernel2D_F32)targetClass.getDeclaredField("kernelY_F32").get(null);

		var validator = new CompareDerivativeToConvolution();
		validator.setTarget(targetClass.getMethod("process",
				GrayF32.class, GrayF32.class, GrayF32.class, ImageBorder_F32.class));

		validator.setKernel(0, kernelX);
		validator.setKernel(1, kernelY);

		var input = new GrayF32(width, height);
		ImageMiscOps.fillUniform(input, rand, 0, 10);
		var derivX = new GrayF32(width, height);
		var derivY = new GrayF32(width, height);

		validator.compare(input, derivX, derivY);
	}

	public static Kernel2D_S32 kernel2D( Kernel1D_S32 smoothing, Kernel1D_S32 differentiate, boolean axisX ) {
		var ks = new Kernel2D_S32(smoothing.width);
		var kd = new Kernel2D_S32(differentiate.width);

		if (axisX) {
			for (int i = 0; i < ks.width; i++) {
				ks.set(ks.width/2, i, smoothing.get(i));
			}
			for (int i = 0; i < kd.width; i++) {
				kd.set(i, kd.width/2, differentiate.get(i));
			}
		} else {
			for (int i = 0; i < ks.width; i++) {
				ks.set(i, ks.width/2, smoothing.get(i));
			}
			for (int i = 0; i < kd.width; i++) {
				kd.set(kd.width/2, i, differentiate.get(i));
			}
		}

		Kernel2D_S32 a = KernelMath.convolve2D(ks, kd);
		return a.trimBorder(a.countZeroBorder());
	}

	public static int divisor( Kernel1D_S32 smoothing, Kernel1D_S32 differentiate ) {
		int sum = smoothing.computeSum();
		int numZeros = 0;
		for (int i = differentiate.width/2; i < differentiate.width; i++) {
			if (differentiate.get(i) != 0)
				break;
			numZeros++;
		}
		return sum*(numZeros + 1);
	}
}
