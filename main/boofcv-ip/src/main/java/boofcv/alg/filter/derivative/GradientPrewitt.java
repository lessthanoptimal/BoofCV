/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.convolve.Kernel2D;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.convolve.Kernel2D_S32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import org.jetbrains.annotations.Nullable;

/**
 * Operations for computing Prewitt image gradient.
 *
 * @author Peter Abeles
 */
public class GradientPrewitt {
	public static Kernel2D_S32 kernelDerivX_I32 = new Kernel2D_S32(3, new int[]{-1, 0, 1, -1, 0, 1, -1, 0, 1});
	public static Kernel2D_S32 kernelDerivY_I32 = new Kernel2D_S32(3, new int[]{-1, -1, -1, 0, 0, 0, 1, 1, 1});
	public static Kernel2D_F32 kernelDerivX_F32 = new Kernel2D_F32(
			3, new float[]{-1f, 0, 1f, -1f, 0, 1f, -1f, 0, 1f});
	public static Kernel2D_F32 kernelDerivY_F32 = new Kernel2D_F32(
			3, new float[]{-1f, -1f, -1f, 0, 0, 0, 1f, 1f, 1f});

	/**
	 * Returns the kernel for computing the derivative along the x-axis.
	 */
	public static Kernel2D getKernelX( boolean isInteger ) {
		if (isInteger)
			return kernelDerivX_I32;
		else
			return kernelDerivX_F32;
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

	/**
	 * Computes the derivative in the X and Y direction using an integer Prewitt edge detector.
	 *
	 * @param orig Input image. Not modified.
	 * @param derivX Storage for image derivative along the x-axis. Modified.
	 * @param derivY Storage for image derivative along the y-axis. Modified.
	 * @param border Specifies how the image border is handled. If null the border is not processed.
	 */
	public static void process( GrayU8 orig, GrayS16 derivX, GrayS16 derivY, @Nullable ImageBorder_S32 border ) {
		InputSanityCheck.reshapeOneIn(orig, derivX, derivY);

		if (BoofConcurrency.USE_CONCURRENT) {
			GradientPrewitt_Shared_MT.process(orig, derivX, derivY);
		} else {
			GradientPrewitt_Shared.process(orig, derivX, derivY);
		}

		if (border != null) {
			border.setImage(orig);
			ConvolveJustBorder_General_SB.convolve(kernelDerivX_I32, border, derivX);
			ConvolveJustBorder_General_SB.convolve(kernelDerivY_I32, border, derivY);
		}
	}

	/**
	 * Computes the derivative in the X and Y direction using an integer Prewitt edge detector.
	 *
	 * @param orig Input image. Not modified.
	 * @param derivX Storage for image derivative along the x-axis. Modified.
	 * @param derivY Storage for image derivative along the y-axis. Modified.
	 * @param border Specifies how the image border is handled. If null the border is not processed.
	 */
	public static void process( GrayS16 orig, GrayS16 derivX, GrayS16 derivY, @Nullable ImageBorder_S32 border ) {
		InputSanityCheck.reshapeOneIn(orig, derivX, derivY);

		if (BoofConcurrency.USE_CONCURRENT) {
			GradientPrewitt_Shared_MT.process(orig, derivX, derivY);
		} else {
			GradientPrewitt_Shared.process(orig, derivX, derivY);
		}

		if (border != null) {
			border.setImage(orig);
			ConvolveJustBorder_General_SB.convolve(kernelDerivX_I32, border, derivX);
			ConvolveJustBorder_General_SB.convolve(kernelDerivY_I32, border, derivY);
		}
	}

	/**
	 * Computes the derivative in the X and Y direction using a floating point Prewitt edge detector.
	 *
	 * @param orig Input image. Not modified.
	 * @param derivX Storage for image derivative along the x-axis. Modified.
	 * @param derivY Storage for image derivative along the y-axis. Modified.
	 * @param border Specifies how the image border is handled. If null the border is not processed.
	 */
	public static void process( GrayF32 orig, GrayF32 derivX, GrayF32 derivY, @Nullable ImageBorder_F32 border ) {
		InputSanityCheck.reshapeOneIn(orig, derivX, derivY);

		if (BoofConcurrency.USE_CONCURRENT) {
			GradientPrewitt_Shared_MT.process(orig, derivX, derivY);
		} else {
			GradientPrewitt_Shared.process(orig, derivX, derivY);
		}

		if (border != null) {
			border.setImage(orig);
			ConvolveJustBorder_General_SB.convolve(kernelDerivX_F32, border, derivX);
			ConvolveJustBorder_General_SB.convolve(kernelDerivY_F32, border, derivY);
		}
	}
}
