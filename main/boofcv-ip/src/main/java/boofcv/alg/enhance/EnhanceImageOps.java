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

package boofcv.alg.enhance;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.enhance.impl.ImplEnhanceFilter;
import boofcv.alg.enhance.impl.ImplEnhanceFilter_MT;
import boofcv.alg.enhance.impl.ImplEnhanceHistogram;
import boofcv.alg.enhance.impl.ImplEnhanceHistogram_MT;
import boofcv.alg.misc.ImageStatistics;
import boofcv.concurrency.BoofConcurrency;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.convolve.Kernel2D_S32;
import boofcv.struct.image.*;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;
import pabeles.concurrency.GrowArray;

/**
 * <p>
 * Operations for improving the visibility of images. See [1] for a discussion of algorithms found in this class.
 * </p>
 *
 * <p>
 * [1] R. C. Gonzalez, R. E. Woods, "Digitial Image Processing" 2nd Ed. 2002
 * </p>
 *
 * @author Peter Abeles
 */
// TODO Add laplacian enhancement?
@SuppressWarnings("Duplicates")
public class EnhanceImageOps {

	// used in unit tests, here for documentation
	public static Kernel2D_S32 kernelEnhance4_I32 = new Kernel2D_S32(3, new int[]{0, -1, 0, -1, 5, -1, 0, -1, 0});
	public static Kernel2D_F32 kernelEnhance4_F32 = new Kernel2D_F32(3, new float[]{0, -1, 0, -1, 5, -1, 0, -1, 0});
	public static Kernel2D_S32 kernelEnhance8_I32 = new Kernel2D_S32(3, new int[]{-1, -1, -1, -1, 9, -1, -1, -1, -1});
	public static Kernel2D_F32 kernelEnhance8_F32 = new Kernel2D_F32(3, new float[]{-1, -1, -1, -1, 9, -1, -1, -1, -1});

	/**
	 * Computes a transformation table which will equalize the provided histogram. An equalized histogram spreads
	 * the 'weight' across the whole spectrum of values. Often used to make dim images easier for people to see.
	 *
	 * @param histogram Input image histogram.
	 * @param transform Output transformation table.
	 */
	public static void equalize( int[] histogram, int[] transform ) {

		int sum = 0;
		for (int i = 0; i < histogram.length; i++) {
			transform[i] = sum += histogram[i];
		}

		int maxValue = histogram.length - 1;

		for (int i = 0; i < histogram.length; i++) {
			transform[i] = (transform[i]*maxValue)/sum;
		}
	}

	/**
	 * Applies the transformation table to the provided input image.
	 *
	 * @param input Input image.
	 * @param transform Input transformation table.
	 * @param output Output image.
	 */
	public static void applyTransform( GrayU8 input, int[] transform, GrayU8 output ) {
		output.reshape(input.width, input.height);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplEnhanceHistogram_MT.applyTransform(input, transform, output);
		} else {
			ImplEnhanceHistogram.applyTransform(input, transform, output);
		}
	}

	/**
	 * Applies the transformation table to the provided input image.
	 *
	 * @param input Input image.
	 * @param transform Input transformation table.
	 * @param output Output image.
	 */
	public static void applyTransform( GrayU16 input, int[] transform, GrayU16 output ) {
		output.reshape(input.width, input.height);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplEnhanceHistogram_MT.applyTransform(input, transform, output);
		} else {
			ImplEnhanceHistogram.applyTransform(input, transform, output);
		}
	}

	/**
	 * Applies the transformation table to the provided input image.
	 *
	 * @param input Input image.
	 * @param minValue Minimum possible pixel value.
	 * @param transform Input transformation table.
	 * @param output Output image.
	 */
	public static void applyTransform( GrayS8 input, int[] transform, int minValue, GrayS8 output ) {
		output.reshape(input.width, input.height);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplEnhanceHistogram_MT.applyTransform(input, transform, minValue, output);
		} else {
			ImplEnhanceHistogram.applyTransform(input, transform, minValue, output);
		}
	}

	/**
	 * Applies the transformation table to the provided input image.
	 *
	 * @param input Input image.
	 * @param minValue Minimum possible pixel value.
	 * @param transform Input transformation table.
	 * @param output Output image.
	 */
	public static void applyTransform( GrayS16 input, int[] transform, int minValue, GrayS16 output ) {
		output.reshape(input.width, input.height);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplEnhanceHistogram_MT.applyTransform(input, transform, minValue, output);
		} else {
			ImplEnhanceHistogram.applyTransform(input, transform, minValue, output);
		}
	}

	/**
	 * Applies the transformation table to the provided input image.
	 *
	 * @param input Input image.
	 * @param minValue Minimum possible pixel value.
	 * @param transform Input transformation table.
	 * @param output Output image.
	 */
	public static void applyTransform( GrayS32 input, int[] transform, int minValue, GrayS32 output ) {
		output.reshape(input.width, input.height);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplEnhanceHistogram_MT.applyTransform(input, transform, minValue, output);
		} else {
			ImplEnhanceHistogram.applyTransform(input, transform, minValue, output);
		}
	}

	/**
	 * Equalizes the local image histogram on a per pixel basis.
	 *
	 * @param input Input image.
	 * @param radius Radius of square local histogram.
	 * @param output Output image.
	 * @param histogramLength Number of elements in the histogram. 256 for 8-bit images
	 * @param workspaces (Optional) Used to create work arrays. Nullable
	 */
	public static void equalizeLocal( GrayU8 input, int radius, GrayU8 output,
									  int histogramLength, @Nullable GrowArray<DogArray_I32> workspaces ) {

		output.reshape(input.width, input.height);
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);

		int width = radius*2 + 1;

		// use more efficient algorithms if possible
		if (input.width >= width && input.height >= width) {
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplEnhanceHistogram_MT.equalizeLocalInner(input, radius, histogramLength, output, workspaces);

				// top border
				ImplEnhanceHistogram_MT.equalizeLocalRow(input, radius, histogramLength, 0, output, workspaces);
				// bottom border
				ImplEnhanceHistogram_MT.equalizeLocalRow(input, radius, histogramLength, input.height - radius, output, workspaces);

				// left border
				ImplEnhanceHistogram_MT.equalizeLocalCol(input, radius, histogramLength, 0, output, workspaces);
				// right border
				ImplEnhanceHistogram_MT.equalizeLocalCol(input, radius, histogramLength, input.width - radius, output, workspaces);
			} else {
				ImplEnhanceHistogram.equalizeLocalInner(input, radius, histogramLength, output, workspaces);

				// top border
				ImplEnhanceHistogram.equalizeLocalRow(input, radius, histogramLength, 0, output, workspaces);
				// bottom border
				ImplEnhanceHistogram.equalizeLocalRow(input, radius, histogramLength, input.height - radius, output, workspaces);

				// left border
				ImplEnhanceHistogram.equalizeLocalCol(input, radius, histogramLength, 0, output, workspaces);
				// right border
				ImplEnhanceHistogram.equalizeLocalCol(input, radius, histogramLength, input.width - radius, output, workspaces);
			}
		} else if (input.width < width && input.height < width) {
			// the local region is larger than the image. just use the full image algorithm
			workspaces.reset();
			int[] histogram = BoofMiscOps.checkDeclare(workspaces.grow(), histogramLength, false);
			int[] transform = BoofMiscOps.checkDeclare(workspaces.grow(), histogramLength, false);

			ImageStatistics.histogram(input, 0, histogram);
			equalize(histogram, transform);
			applyTransform(input, transform, output);
		} else {
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplEnhanceHistogram_MT.equalizeLocalNaive(input, radius, histogramLength, output, workspaces);
			} else {
				ImplEnhanceHistogram.equalizeLocalNaive(input, radius, histogramLength, output, workspaces);
			}
		}
	}

	/**
	 * Equalizes the local image histogram on a per pixel basis.
	 *
	 * @param input Input image.
	 * @param radius Radius of square local histogram.
	 * @param output Output image.
	 * @param histogramLength Number of elements in the histogram. 256 for 8-bit images
	 * @param workspaces Used to create work arrays. can be null
	 */
	public static void equalizeLocal( GrayU16 input, int radius, GrayU16 output,
									  int histogramLength, @Nullable GrowArray<DogArray_I32> workspaces ) {

		InputSanityCheck.checkReshape(input, output);
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);

		int width = radius*2 + 1;

		// use more efficient algorithms if possible
		if (input.width >= width && input.height >= width) {
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplEnhanceHistogram_MT.equalizeLocalInner(input, radius, histogramLength, output, workspaces);

				// top border
				ImplEnhanceHistogram_MT.equalizeLocalRow(input, radius, histogramLength, 0, output, workspaces);
				// bottom border
				ImplEnhanceHistogram_MT.equalizeLocalRow(input, radius, histogramLength, input.height - radius, output, workspaces);

				// left border
				ImplEnhanceHistogram_MT.equalizeLocalCol(input, radius, histogramLength, 0, output, workspaces);
				// right border
				ImplEnhanceHistogram_MT.equalizeLocalCol(input, radius, histogramLength, input.width - radius, output, workspaces);
			} else {
				ImplEnhanceHistogram.equalizeLocalInner(input, radius, histogramLength, output, workspaces);

				// top border
				ImplEnhanceHistogram.equalizeLocalRow(input, radius, histogramLength, 0, output, workspaces);
				// bottom border
				ImplEnhanceHistogram.equalizeLocalRow(input, radius, histogramLength, input.height - radius, output, workspaces);

				// left border
				ImplEnhanceHistogram.equalizeLocalCol(input, radius, histogramLength, 0, output, workspaces);
				// right border
				ImplEnhanceHistogram.equalizeLocalCol(input, radius, histogramLength, input.width - radius, output, workspaces);
			}
		} else if (input.width < width && input.height < width) {
			// the local region is larger than the image. just use the full image algorithm
			workspaces.reset();
			int[] histogram = BoofMiscOps.checkDeclare(workspaces.grow(), histogramLength, false);
			int[] transform = BoofMiscOps.checkDeclare(workspaces.grow(), histogramLength, false);

			ImageStatistics.histogram(input, 0, histogram);
			equalize(histogram, transform);
			applyTransform(input, transform, output);
		} else {
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplEnhanceHistogram_MT.equalizeLocalNaive(input, radius, histogramLength, output, workspaces);
			} else {
				ImplEnhanceHistogram.equalizeLocalNaive(input, radius, histogramLength, output, workspaces);
			}
		}
	}

	/**
	 * Applies a Laplacian-4 based sharpen filter to the image.
	 *
	 * @param input Input image.
	 * @param output Output image.
	 */
	public static void sharpen4( GrayU8 input, GrayU8 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplEnhanceFilter_MT.sharpenInner4(input, output, 0, 255);
			ImplEnhanceFilter_MT.sharpenBorder4(input, output, 0, 255);
		} else {
			ImplEnhanceFilter.sharpenInner4(input, output, 0, 255);
			ImplEnhanceFilter.sharpenBorder4(input, output, 0, 255);
		}
	}

	/**
	 * Applies a Laplacian-4 based sharpen filter to the image.
	 *
	 * @param input Input image.
	 * @param output Output image.
	 */
	public static void sharpen4( GrayF32 input, GrayF32 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplEnhanceFilter_MT.sharpenInner4(input, output, 0, 255);
			ImplEnhanceFilter_MT.sharpenBorder4(input, output, 0, 255);
		} else {
			ImplEnhanceFilter.sharpenInner4(input, output, 0, 255);
			ImplEnhanceFilter.sharpenBorder4(input, output, 0, 255);
		}
	}

	/**
	 * Applies a Laplacian-8 based sharpen filter to the image.
	 *
	 * @param input Input image.
	 * @param output Output image.
	 */
	public static void sharpen8( GrayU8 input, GrayU8 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplEnhanceFilter_MT.sharpenInner8(input, output, 0, 255);
			ImplEnhanceFilter_MT.sharpenBorder8(input, output, 0, 255);
		} else {
			ImplEnhanceFilter.sharpenInner8(input, output, 0, 255);
			ImplEnhanceFilter.sharpenBorder8(input, output, 0, 255);
		}
	}

	/**
	 * Applies a Laplacian-8 based sharpen filter to the image.
	 *
	 * @param input Input image.
	 * @param output Output image.
	 */
	public static void sharpen8( GrayF32 input, GrayF32 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplEnhanceFilter_MT.sharpenInner8(input, output, 0, 255);
			ImplEnhanceFilter_MT.sharpenBorder8(input, output, 0, 255);
		} else {
			ImplEnhanceFilter.sharpenInner8(input, output, 0, 255);
			ImplEnhanceFilter.sharpenBorder8(input, output, 0, 255);
		}
	}
}
