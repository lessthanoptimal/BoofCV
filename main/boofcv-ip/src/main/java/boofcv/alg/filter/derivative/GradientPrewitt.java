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
import boofcv.alg.filter.convolve.border.ConvolveJustBorder_General_SB;
import boofcv.alg.filter.derivative.impl.GradientPrewitt_Shared;
import boofcv.alg.filter.derivative.impl.GradientPrewitt_Shared_MT;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.border.ImageBorder_F32;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.convolve.Kernel1D_S32;
import boofcv.struct.convolve.Kernel2D;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.convolve.Kernel2D_S32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import org.jetbrains.annotations.Nullable;

/// Prewitt image derivative operator. Contains 2D kernels and specialized efficient functions for computing image
/// gradient.
///
/// For integer images, divide the result by [#divisor] for proper scaling. Floating
/// point kernels already include this scaling; without it, output magnitudes are
/// inflated by that factor.
///
/// y-axis
///
/// | -1 | -1 | -1 |
/// |----|----|----|
/// |  0 |  0 |  0 |
/// |  1 |  1 |  1 |
///
/// x-axis
///
/// | -1 | 0 | 1 |
/// |----|---|---|
/// | -1 | 0 | 1 |
/// | -1 | 0 | 1 |
///
/// Prewitt operator is equivalent to convolving the image with the following 1D
/// kernels: `conv2([1/3 1/3 1/3], [-1 0 1])`
public class GradientPrewitt {
	/// Integer divisor to ensure brightness does not change
	public static final int divisor = 6;
	public static Kernel1D_S32 kernelSmooth = new Kernel1D_S32(3, new int[]{1, 1, 1});
	public static Kernel1D_S32 kernelDiff = new Kernel1D_S32(3, new int[]{-1, 0, 1});
	public static Kernel2D_S32 kernelX_I32 = new Kernel2D_S32(3, new int[]{-1, 0, 1, -1, 0, 1, -1, 0, 1});
	public static Kernel2D_S32 kernelY_I32 = new Kernel2D_S32(3, new int[]{-1, -1, -1, 0, 0, 0, 1, 1, 1});
	public static Kernel2D_F32 kernelX_F32 = new Kernel2D_F32(
			3, new float[]{-1f/6f, 0, 1f/6f, -1f/6f, 0, 1f/6f, -1f/6f, 0, 1f/6f});
	public static Kernel2D_F32 kernelY_F32 = new Kernel2D_F32(
			3, new float[]{-1f/6f, -1f/6f, -1f/6f, 0, 0, 0, 1f/6f, 1f/6f, 1f/6f});

	/// Returns the kernel for computing the derivative along the x-axis.
	public static Kernel2D getKernelX( boolean isInteger ) {
		if (isInteger)
			return kernelX_I32;
		else
			return kernelX_F32;
	}

	public static <I extends ImageGray<I>, D extends ImageGray<D>> void process( I input, D derivX, D derivY,
	                                                                             @Nullable ImageBorder border ) {
		switch (input.getImageType().getDataType()) {
			case U8 -> process((GrayU8)input, (GrayS16)derivX, (GrayS16)derivY, (ImageBorder_S32)border);
			case S16 -> process((GrayS16)input, (GrayS16)derivX, (GrayS16)derivY, (ImageBorder_S32)border);
			case F32 -> process((GrayF32)input, (GrayF32)derivX, (GrayF32)derivY, (ImageBorder_F32)border);
			default -> throw new IllegalArgumentException("Unknow input image type");
		}
	}

	/// Computes the derivative in the X and Y direction using an integer Prewitt edge detector.
	///
	/// @param orig Input image. Not modified.
	/// @param derivX Storage for image derivative along the x-axis. Modified.
	/// @param derivY Storage for image derivative along the y-axis. Modified.
	/// @param border Specifies how the image border is handled. If null the border is not processed.
	public static void process( GrayU8 orig, GrayS16 derivX, GrayS16 derivY, @Nullable ImageBorder_S32 border ) {
		InputSanityCheck.reshapeOneIn(orig, derivX, derivY);

		if (BoofConcurrency.USE_CONCURRENT) {
			GradientPrewitt_Shared_MT.process(orig, derivX, derivY);
		} else {
			GradientPrewitt_Shared.process(orig, derivX, derivY);
		}

		if (border != null) {
			border.setImage(orig);
			ConvolveJustBorder_General_SB.convolve(kernelX_I32, border, derivX);
			ConvolveJustBorder_General_SB.convolve(kernelY_I32, border, derivY);
		}
	}

	/// Computes the derivative in the X and Y direction using an integer Prewitt edge detector.
	///
	/// @param orig Input image. Not modified.
	/// @param derivX Storage for image derivative along the x-axis. Modified.
	/// @param derivY Storage for image derivative along the y-axis. Modified.
	/// @param border Specifies how the image border is handled. If null the border is not processed.
	public static void process( GrayS16 orig, GrayS16 derivX, GrayS16 derivY, @Nullable ImageBorder_S32 border ) {
		InputSanityCheck.reshapeOneIn(orig, derivX, derivY);

		if (BoofConcurrency.USE_CONCURRENT) {
			GradientPrewitt_Shared_MT.process(orig, derivX, derivY);
		} else {
			GradientPrewitt_Shared.process(orig, derivX, derivY);
		}

		if (border != null) {
			border.setImage(orig);
			ConvolveJustBorder_General_SB.convolve(kernelX_I32, border, derivX);
			ConvolveJustBorder_General_SB.convolve(kernelY_I32, border, derivY);
		}
	}

	/// Computes the derivative in the X and Y direction using a floating point Prewitt edge detector.
	///
	/// @param orig Input image. Not modified.
	/// @param derivX Storage for image derivative along the x-axis. Modified.
	/// @param derivY Storage for image derivative along the y-axis. Modified.
	/// @param border Specifies how the image border is handled. If null the border is not processed.
	public static void process( GrayF32 orig, GrayF32 derivX, GrayF32 derivY, @Nullable ImageBorder_F32 border ) {
		InputSanityCheck.reshapeOneIn(orig, derivX, derivY);

		if (BoofConcurrency.USE_CONCURRENT) {
			GradientPrewitt_Shared_MT.process(orig, derivX, derivY);
		} else {
			GradientPrewitt_Shared.process(orig, derivX, derivY);
		}

		if (border != null) {
			border.setImage(orig);
			ConvolveJustBorder_General_SB.convolve(kernelX_F32, border, derivX);
			ConvolveJustBorder_General_SB.convolve(kernelY_F32, border, derivY);
		}
	}
}
