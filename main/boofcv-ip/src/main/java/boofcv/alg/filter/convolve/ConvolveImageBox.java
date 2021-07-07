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

package boofcv.alg.filter.convolve;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.filter.convolve.border.ConvolveJustBorder_General_SB;
import boofcv.alg.filter.convolve.noborder.ImplConvolveBox;
import boofcv.alg.filter.convolve.noborder.ImplConvolveBox_MT;
import boofcv.concurrency.BoofConcurrency;
import boofcv.core.image.ImageBorderValue;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_S32;
import boofcv.struct.image.*;
import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;
import pabeles.concurrency.GrowArray;

/**
 * Convolves a kernel which is composed entirely of 1's across an image. This special kernel can be highly optimized
 * and has a computational complexity independent of the kernel size.
 *
 * @author Peter Abeles
 */
public class ConvolveImageBox {

	/**
	 * Performs a horizontal 1D convolution of a box kernel across the image
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 */
	public static void horizontal( GrayF32 input, GrayF32 output, int radius ) {
		InputSanityCheck.checkSameShape(input, output);

		Kernel1D_F32 kernel = FactoryKernel.table1D_F32(radius, false);
		ConvolveJustBorder_General_SB.horizontal(kernel, ImageBorderValue.wrap(input, 0), output);
		if (BoofConcurrency.USE_CONCURRENT) {
			ImplConvolveBox_MT.horizontal(input, output, radius);
		} else {
			ImplConvolveBox.horizontal(input, output, radius);
		}
	}

	/**
	 * Performs a horizontal 1D convolution of a box kernel across the image
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 */
	public static void horizontal( GrayU8 input, GrayI16 output, int radius ) {
		InputSanityCheck.checkSameShape(input, output);

		Kernel1D_S32 kernel = FactoryKernel.table1D_S32(radius);
		ConvolveJustBorder_General_SB.horizontal(kernel, ImageBorderValue.wrap(input, 0), output);
		if (BoofConcurrency.USE_CONCURRENT) {
			ImplConvolveBox_MT.horizontal(input, output, radius);
		} else {
			ImplConvolveBox.horizontal(input, output, radius);
		}
	}

	/**
	 * Performs a horizontal 1D convolution of a box kernel across the image
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 */
	public static void horizontal( GrayU8 input, GrayS32 output, int radius ) {
		InputSanityCheck.checkSameShape(input, output);

		Kernel1D_S32 kernel = FactoryKernel.table1D_S32(radius);
		ConvolveJustBorder_General_SB.horizontal(kernel, ImageBorderValue.wrap(input, 0), output);
		if (BoofConcurrency.USE_CONCURRENT) {
			ImplConvolveBox_MT.horizontal(input, output, radius);
		} else {
			ImplConvolveBox.horizontal(input, output, radius);
		}
	}

	/**
	 * Performs a horizontal 1D convolution of a box kernel across the image
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 */
	public static void horizontal( GrayS16 input, GrayI16 output, int radius ) {
		InputSanityCheck.checkSameShape(input, output);

		Kernel1D_S32 kernel = FactoryKernel.table1D_S32(radius);
		ConvolveJustBorder_General_SB.horizontal(kernel, ImageBorderValue.wrap(input, 0), output);
		if (BoofConcurrency.USE_CONCURRENT) {
			ImplConvolveBox_MT.horizontal(input, output, radius);
		} else {
			ImplConvolveBox.horizontal(input, output, radius);
		}
	}

	/**
	 * Performs a vertical 1D convolution of a box kernel across the image
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 */
	public static void vertical( GrayF32 input, GrayF32 output, int radius, @Nullable GrowArray<DogArray_F32> work ) {
		InputSanityCheck.checkSameShape(input, output);

		Kernel1D_F32 kernel = FactoryKernel.table1D_F32(radius, false);
		ConvolveJustBorder_General_SB.vertical(kernel, ImageBorderValue.wrap(input, 0), output);
		if (BoofConcurrency.USE_CONCURRENT) {
			ImplConvolveBox_MT.vertical(input, output, radius, work);
		} else {
			ImplConvolveBox.vertical(input, output, radius, work);
		}
	}

	/**
	 * Performs a vertical 1D convolution of a box kernel across the image
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 */
	public static void vertical( GrayU8 input, GrayI16 output, int radius, @Nullable GrowArray<DogArray_I32> work ) {
		InputSanityCheck.checkSameShape(input, output);

		Kernel1D_S32 kernel = FactoryKernel.table1D_S32(radius);
		ConvolveJustBorder_General_SB.vertical(kernel, ImageBorderValue.wrap(input, 0), output);
		if (BoofConcurrency.USE_CONCURRENT) {
			ImplConvolveBox_MT.vertical(input, output, radius, work);
		} else {
			ImplConvolveBox.vertical(input, output, radius, work);
		}
	}

	/**
	 * Performs a vertical 1D convolution of a box kernel across the image
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 */
	public static void vertical( GrayU8 input, GrayS32 output, int radius, @Nullable GrowArray<DogArray_I32> work ) {
		InputSanityCheck.checkSameShape(input, output);

		Kernel1D_S32 kernel = FactoryKernel.table1D_S32(radius);
		ConvolveJustBorder_General_SB.vertical(kernel, ImageBorderValue.wrap(input, 0), output);
		if (BoofConcurrency.USE_CONCURRENT) {
			ImplConvolveBox_MT.vertical(input, output, radius, work);
		} else {
			ImplConvolveBox.vertical(input, output, radius, work);
		}
	}

	/**
	 * Performs a vertical 1D convolution of a box kernel across the image
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 */
	public static void vertical( GrayS16 input, GrayI16 output, int radius, @Nullable GrowArray<DogArray_I32> work ) {
		InputSanityCheck.checkSameShape(input, output);

		Kernel1D_S32 kernel = FactoryKernel.table1D_S32(radius);
		ConvolveJustBorder_General_SB.vertical(kernel, ImageBorderValue.wrap(input, 0), output);
		if (BoofConcurrency.USE_CONCURRENT) {
			ImplConvolveBox_MT.vertical(input, output, radius, work);
		} else {
			ImplConvolveBox.vertical(input, output, radius, work);
		}
	}
}
