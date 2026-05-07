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

import boofcv.alg.InputSanityCheck;
import boofcv.alg.filter.derivative.impl.GradientThree_Standard;
import boofcv.alg.filter.derivative.impl.GradientThree_Standard_MT;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.border.ImageBorder_F32;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.convolve.Kernel1D;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_S32;
import boofcv.struct.image.*;
import org.jetbrains.annotations.Nullable;

/// Central-difference first derivative. 3x1 kernel: `[-1 0 1]`.
///
/// For integer images, divide the result by [#divisor] for proper scaling. Floating
/// point kernels already include this scaling; without it, output magnitudes are
/// inflated by that factor.
///
/// For example in an integer image:
///
/// ```
/// derivX(x,y) = img(x+1,y) - img(x-1,y)
/// derivY(x,y) = img(x,y+1) - img(x,y-1)
/// ```
public class GradientThree {
	/// Integer divisor to ensure brightness does not change
	public static final int divisor = 2;
	public static Kernel1D_S32 kernelSmooth = new Kernel1D_S32(1, new int[]{1});
	public static Kernel1D_S32 kernelDiff = new Kernel1D_S32(3, new int[]{-1, 0, 1});
	public static Kernel1D_S32 kernel_I32 = new Kernel1D_S32(3, new int[]{-1, 0, 1});
	public static Kernel1D_F32 kernel_F32 = new Kernel1D_F32(3, new float[]{-0.5f, 0, 0.5f});

	/// Returns the kernel for computing the derivative along the x-axis.
	public static Kernel1D getKernelX( boolean isInteger ) {
		if (isInteger)
			return kernel_I32;
		else
			return kernel_F32;
	}

	public static <I extends ImageGray<I>, D extends ImageGray<D>>
	void process( I input, D derivX, D derivY, @Nullable ImageBorder border ) {
		switch (input.getImageType().getDataType()) {
			case U8 -> process((GrayU8)input, (GrayS16)derivX, (GrayS16)derivY, (ImageBorder_S32)border);
			case S16 -> process((GrayS16)input, (GrayS16)derivX, (GrayS16)derivY, (ImageBorder_S32)border);
			case F32 -> process((GrayF32)input, (GrayF32)derivX, (GrayF32)derivY, (ImageBorder_F32)border);
			default -> throw new IllegalArgumentException("Unknown input image type");
		}
	}

	/// Computes the derivative of an [GrayU8] along the x and y axes.
	///
	/// @param orig Input image that is differentiated. Not Modified.
	/// @param derivX Derivative along the x-axis. Modified.
	/// @param derivY Derivative along the y-axis. Modified.
	/// @param border Specifies how the image border is handled. If null the border is not processed.
	public static void process( GrayU8 orig, GrayS16 derivX, GrayS16 derivY, @Nullable ImageBorder_S32 border ) {
		InputSanityCheck.reshapeOneIn(orig, derivX, derivY);

		if (BoofConcurrency.USE_CONCURRENT) {
			GradientThree_Standard_MT.process(orig, derivX, derivY);
		} else {
			GradientThree_Standard.process(orig, derivX, derivY);
		}

		if (border != null) {
			DerivativeHelperFunctions.processBorderHorizontal(orig, derivX, kernel_I32, border);
			DerivativeHelperFunctions.processBorderVertical(orig, derivY, kernel_I32, border);
		}
	}

	/// Computes the derivative of an [GrayU8] along the x and y axes.
	///
	/// @param orig Input image that is differentiated. Not Modified.
	/// @param derivX Derivative along the x-axis. Modified.
	/// @param derivY Derivative along the y-axis. Modified.
	/// @param border Specifies how the image border is handled. If null the border is not processed.
	public static void process( GrayU8 orig, GrayS32 derivX, GrayS32 derivY, @Nullable ImageBorder_S32 border ) {
		InputSanityCheck.reshapeOneIn(orig, derivX, derivY);

		if (BoofConcurrency.USE_CONCURRENT) {
			GradientThree_Standard_MT.process(orig, derivX, derivY);
		} else {
			GradientThree_Standard.process(orig, derivX, derivY);
		}

		if (border != null) {
			DerivativeHelperFunctions.processBorderHorizontal(orig, derivX, kernel_I32, border);
			DerivativeHelperFunctions.processBorderVertical(orig, derivY, kernel_I32, border);
		}
	}

	/// Computes the derivative of an [GrayS16] along the x and y axes.
	///
	/// @param orig Input image that is differentiated. Not Modified.
	/// @param derivX Derivative along the x-axis. Modified.
	/// @param derivY Derivative along the y-axis. Modified.
	/// @param border Specifies how the image border is handled. If null the border is not processed.
	public static void process( GrayS16 orig, GrayS16 derivX, GrayS16 derivY, @Nullable ImageBorder_S32 border ) {
		InputSanityCheck.reshapeOneIn(orig, derivX, derivY);

		if (BoofConcurrency.USE_CONCURRENT) {
			GradientThree_Standard_MT.process(orig, derivX, derivY);
		} else {
			GradientThree_Standard.process(orig, derivX, derivY);
		}

		if (border != null) {
			DerivativeHelperFunctions.processBorderHorizontal(orig, derivX, kernel_I32, border);
			DerivativeHelperFunctions.processBorderVertical(orig, derivY, kernel_I32, border);
		}
	}

	/// Computes the derivative of an [GrayF32] along the x and y axes.
	///
	/// @param orig Input image that is differentiated. Not Modified.
	/// @param derivX Derivative along the x-axis. Modified.
	/// @param derivY Derivative along the y-axis. Modified.
	/// @param border Specifies how the image border is handled. If null the border is not processed.
	public static void process( GrayF32 orig, GrayF32 derivX, GrayF32 derivY, @Nullable ImageBorder_F32 border ) {
		InputSanityCheck.reshapeOneIn(orig, derivX, derivY);

		if (BoofConcurrency.USE_CONCURRENT) {
			GradientThree_Standard_MT.process(orig, derivX, derivY);
		} else {
			GradientThree_Standard.process(orig, derivX, derivY);
		}
		if (border != null) {
			DerivativeHelperFunctions.processBorderHorizontal(orig, derivX, kernel_F32, border);
			DerivativeHelperFunctions.processBorderVertical(orig, derivY, kernel_F32, border);
		}
	}
}
