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

package boofcv.alg.filter.convolve;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.filter.convolve.noborder.ImplConvolveBox;
import boofcv.alg.filter.convolve.noborder.ImplConvolveBox_MT;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.image.*;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.DogArray_I16;
import org.ddogleg.struct.DogArray_I32;
import pabeles.concurrency.GrowArray;

/**
 * Convolves a kernel which is composed entirely of 1's across an image. This special kernel can be highly optimized
 * and has a computational complexity independent of the kernel size.
 *
 * @author Peter Abeles
 */
public class ConvolveImageBox {
	/// Images with fewer pixels than this are processed single threaded by vertical(), since dispatching to
	/// threads costs more
	public static int CONCURRENT_MIN_PIXELS_VERTICAL = 800*800;

	/// Same as CONCURRENT_MIN_PIXELS_VERTICAL for horizontal(), which is slower single threaded
	public static int CONCURRENT_MIN_PIXELS_HORIZONTAL = 800*800/4;

	// Row buffers of the vertical pass, for each calling thread
	private static final ThreadLocal<DogArray<DogArray_F32>> rowsF32 =
			ThreadLocal.withInitial(() -> new DogArray<>(DogArray_F32::new));
	private static final ThreadLocal<DogArray<DogArray_I16>> rowsI16 =
			ThreadLocal.withInitial(() -> new DogArray<>(DogArray_I16::new));
	private static final ThreadLocal<DogArray<DogArray_I32>> rowsI32 =
			ThreadLocal.withInitial(() -> new DogArray<>(DogArray_I32::new));
	private static final ThreadLocal<GrowArray<DogArray<DogArray_F32>>> workspacesF32 =
			ThreadLocal.withInitial(() -> new GrowArray<>(() -> new DogArray<>(DogArray_F32::new)));
	private static final ThreadLocal<GrowArray<DogArray<DogArray_I16>>> workspacesI16 =
			ThreadLocal.withInitial(() -> new GrowArray<>(() -> new DogArray<>(DogArray_I16::new)));
	private static final ThreadLocal<GrowArray<DogArray<DogArray_I32>>> workspacesI32 =
			ThreadLocal.withInitial(() -> new GrowArray<>(() -> new DogArray<>(DogArray_I32::new)));

	/**
	 * Performs a horizontal 1D convolution of a box kernel across the image
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 */
	public static void horizontal( GrayF32 input, GrayF32 output, int radius ) {
		InputSanityCheck.checkSameShape(input, output);

		if (isConcurrent(input, CONCURRENT_MIN_PIXELS_HORIZONTAL)) {
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

		if (isConcurrent(input, CONCURRENT_MIN_PIXELS_HORIZONTAL)) {
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

		if (isConcurrent(input, CONCURRENT_MIN_PIXELS_HORIZONTAL)) {
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

		if (isConcurrent(input, CONCURRENT_MIN_PIXELS_HORIZONTAL)) {
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
	public static void vertical( GrayF32 input, GrayF32 output, int radius ) {
		InputSanityCheck.checkSameShape(input, output);

		if (isConcurrent(input, CONCURRENT_MIN_PIXELS_VERTICAL)) {
			ImplConvolveBox_MT.vertical(input, output, radius, workspacesF32.get());
		} else {
			ImplConvolveBox.vertical(input, output, radius, rowsF32.get());
		}
	}

	/**
	 * Performs a vertical 1D convolution of a box kernel across the image
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 */
	public static void vertical( GrayU8 input, GrayI16 output, int radius ) {
		InputSanityCheck.checkSameShape(input, output);

		if (isConcurrent(input, CONCURRENT_MIN_PIXELS_VERTICAL)) {
			ImplConvolveBox_MT.vertical(input, output, radius, workspacesI16.get());
		} else {
			ImplConvolveBox.vertical(input, output, radius, rowsI16.get());
		}
	}

	/**
	 * Performs a vertical 1D convolution of a box kernel across the image
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 */
	public static void vertical( GrayU8 input, GrayS32 output, int radius ) {
		InputSanityCheck.checkSameShape(input, output);

		if (isConcurrent(input, CONCURRENT_MIN_PIXELS_VERTICAL)) {
			ImplConvolveBox_MT.vertical(input, output, radius, workspacesI32.get());
		} else {
			ImplConvolveBox.vertical(input, output, radius, rowsI32.get());
		}
	}

	/**
	 * Performs a vertical 1D convolution of a box kernel across the image
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 */
	public static void vertical( GrayS16 input, GrayI16 output, int radius ) {
		InputSanityCheck.checkSameShape(input, output);

		if (isConcurrent(input, CONCURRENT_MIN_PIXELS_VERTICAL)) {
			ImplConvolveBox_MT.vertical(input, output, radius, workspacesI16.get());
		} else {
			ImplConvolveBox.vertical(input, output, radius, rowsI16.get());
		}
	}

	private static boolean isConcurrent( ImageGray<?> input, int minPixels ) {
		return BoofConcurrency.USE_CONCURRENT && input.width*input.height >= minPixels;
	}
}
