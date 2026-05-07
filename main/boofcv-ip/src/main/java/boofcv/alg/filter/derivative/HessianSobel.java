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
import boofcv.alg.filter.derivative.impl.HessianSobel_Shared;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.border.ImageBorder_F32;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.convolve.Kernel2D_S32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import org.jetbrains.annotations.Nullable;

/// Second order (Hessian) Sobel image derivatives. This hessian is derived using the [GradientSobel]
/// gradient function.
///
/// For integer images, divide the result by [#divisor] for proper scaling. Floating
/// point kernels already include this scaling; without it, output magnitudes are
/// inflated by that factor.
///
/// WARNING: It is computationally more expensive to compute the Hessian with this operation than applying the Sobel
/// gradient operator multiple times. However, this does not require the creation additional storage to save
/// intermediate results.
///
/// Kernel for ∂²f/∂y²:
///
/// |  1 |  4 |   6 |  4 |  1 |
/// |----|----|-----|----|----|
/// |  0 |  0 |   0 |  0 |  0 |
/// | -2 | -8 | -12 | -8 | -2 |
/// |  0 |  0 |   0 |  0 |  0 |
/// |  1 |  4 |   6 |  4 |  1 |
///
/// `[1 0 -2 0 1]` and ∂²f/∂x∂y is:
///
/// |  1 |  2 | 0 | -2 | -1 |
/// |----|----|---|----|----|
/// |  2 |  4 | 0 | -4 | -2 |
/// |  0 |  0 | 0 |  0 |  0 |
/// | -2 | -4 | 0 |  4 |  2 |
/// | -1 | -2 | 0 |  2 |  1 |
public class HessianSobel {
	/// Integer divisor to ensure brightness does not change
	public static final int divisor = 64;

	public static Kernel2D_S32 kernelYY_I32 = new Kernel2D_S32(5, new int[]
			{1, 4, 6, 4, 1,
			 0, 0, 0, 0, 0,
			-2, -8, -12, -8, -2,
			 0, 0, 0, 0, 0,
			 1, 4, 6, 4, 1});
	public static Kernel2D_S32 kernelXX_I32 = new Kernel2D_S32(5, new int[]
			{1, 0, -2, 0, 1,
			4, 0, -8, 0, 4,
			6, 0, -12, 0, 6,
			4, 0, -8, 0, 4,
			1, 0, -2, 0, 1});
	public static Kernel2D_S32 kernelXY_I32 = new Kernel2D_S32(5, new int[]
			{1, 2, 0, -2, -1,
			2, 4, 0, -4, -2,
			0, 0, 0, 0, 0,
			-2, -4, 0, 4, 2,
			-1, -2, 0, 2, 1});
	public static Kernel2D_F32 kernelYY_F32 = new Kernel2D_F32(5, new float[]
			{1.0f/64.0f, 4.0f/64.0f, 6.0f/64.0f, 4.0f/64.0f, 1.0f/64.0f,
			0, 0, 0, 0, 0,
			-2.0f/64.0f, -8.0f/64.0f, -12.0f/64.0f, -8.0f/64.0f, -2.0f/64.0f,
			0, 0, 0, 0, 0,
			1.0f/64.0f, 4.0f/64.0f, 6.0f/64.0f, 4.0f/64.0f, 1.0f/64.0f});
	public static Kernel2D_F32 kernelXX_F32 = new Kernel2D_F32(5, new float[]
			{1.0f/64.0f, 0, -2.0f/64.0f, 0, 1.0f/64.0f,
			4.0f/64.0f, 0, -8.0f/64.0f, 0, 4.0f/64.0f,
			6.0f/64.0f, 0, -12.0f/64.0f, 0, 6.0f/64.0f,
			4.0f/64.0f, 0, -8.0f/64.0f, 0, 4.0f/64.0f,
			1.0f/64.0f, 0, -2.0f/64.0f, 0, 1.0f/64.0f});
	public static Kernel2D_F32 kernelXY_F32 = new Kernel2D_F32(5, new float[]
			{1f/64f, 2f/64f, 0, -2f/64f, -1f/64f,
			2f/64f, 4f/64f, 0, -4f/64f, -2f/64f,
			0, 0, 0, 0, 0,
			-2f/64f, -4f/64f, 0, 4f/64f, 2f/64f,
			-1f/64f, -2f/64f, 0, 2f/64f, 1f/64f});

	public static <I extends ImageGray<I>, D extends ImageGray<D>> void process( I input,
																				 D derivXX, D derivYY, D derivXY,
																				 @Nullable ImageBorder border ) {
		switch (input.getImageType().getDataType()) {
			case U8 -> process((GrayU8)input, (GrayS16)derivXX, (GrayS16)derivYY, (GrayS16)derivXY, (ImageBorder_S32)border);
			case F32 -> process((GrayF32)input, (GrayF32)derivXX, (GrayF32)derivYY, (GrayF32)derivXY, (ImageBorder_F32)border);
			default -> throw new IllegalArgumentException("Unknown input image type");
		}
	}

	/// Computes the image's second derivatives.
	///
	/// @param orig Input image that is differentiated. Not Modified.
	/// @param derivXX Second derivative along the x-axis. Modified.
	/// @param derivYY Second derivative along the y-axis. Modified.
	/// @param derivXY Second cross derivative. Modified.
	/// @param border Specifies how the image border is handled. If null the border is not processed.
	public static void process( GrayU8 orig,
								GrayS16 derivXX, GrayS16 derivYY, GrayS16 derivXY,
								@Nullable ImageBorder_S32 border ) {
		InputSanityCheck.reshapeOneIn(orig, derivXX, derivYY, derivXY);
		HessianSobel_Shared.process(orig, derivXX, derivYY, derivXY);

		if (border != null) {
			border.setImage(orig);
			ConvolveJustBorder_General_SB.convolve(kernelXX_I32, border, derivXX);
			ConvolveJustBorder_General_SB.convolve(kernelYY_I32, border, derivYY);
			ConvolveJustBorder_General_SB.convolve(kernelXY_I32, border, derivXY);
		}
	}

	/// Computes the image's second derivatives.
	///
	/// @param orig Input image that is differentiated. Not Modified.
	/// @param derivXX Second derivative along the x-axis. Modified.
	/// @param derivYY Second derivative along the y-axis. Modified.
	/// @param derivXY Second cross derivative. Modified.
	/// @param border Specifies how the image border is handled. If null the border is not processed.
	public static void process( GrayF32 orig,
								GrayF32 derivXX, GrayF32 derivYY, GrayF32 derivXY,
								@Nullable ImageBorder_F32 border ) {
		InputSanityCheck.reshapeOneIn(orig, derivXX, derivYY, derivXY);
		HessianSobel_Shared.process(orig, derivXX, derivYY, derivXY);

		if (border != null) {
			border.setImage(orig);
			ConvolveJustBorder_General_SB.convolve(kernelXX_F32, border, derivXX);
			ConvolveJustBorder_General_SB.convolve(kernelYY_F32, border, derivYY);
			ConvolveJustBorder_General_SB.convolve(kernelXY_F32, border, derivXY);
		}
	}
}
