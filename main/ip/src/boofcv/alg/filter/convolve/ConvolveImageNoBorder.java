/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.filter.convolve.noborder.*;
import boofcv.struct.convolve.*;
import boofcv.struct.image.*;


/**
 * <p>
 * Provides functions for convolving 1D and 2D kernels across an image, excluding the image border.  1D kernels can either
 * be convolved along each row or column in the image.  No checks are done for overflow or underflow.
 * </p>
 * <p>
 * When convolving with division the convolution is computed as usual, but then the result is divided by
 * the divisor.  This is typically done when performing convolution inside of integer images to normalize
 * it by the sum of all the elements in the convolution kernel.
 * </p>
 *
 * <p>
 * Image Edges:  There is no general purpose way for handling convolutions along the image edges.  Therefor unless
 * the whole kernel can be convolved image borders are skipped.  In special cases where there is a clear way to
 * handle image edges specialized functions are provided.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"ForLoopReplaceableByForEach"})
public class ConvolveImageNoBorder {

	/**
	 * Performs a horizontal 1D convolution across the image.  The horizontal border is not processed.
	 *
	 * @param input	 The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void horizontal(Kernel1D_F32 kernel,
								  GrayF32 input, GrayF32 output) {
		InputSanityCheck.checkSameShape(input, output);

		if (!ConvolveImageUnrolled_F32_F32.horizontal(kernel, input, output))
			ConvolveImageStandard.horizontal(kernel, input, output);
	}

	/**
	 * Performs a horizontal 1D convolution across the image.  The horizontal border is not processed.
	 *
	 * @param input	 The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void horizontal(Kernel1D_F64 kernel,
								  GrayF64 input, GrayF64 output) {
		InputSanityCheck.checkSameShape(input, output);

		if (!ConvolveImageUnrolled_F64_F64.horizontal(kernel, input, output))
			ConvolveImageStandard.horizontal(kernel, input, output);
	}

	/**
	 * Performs a horizontal 1D convolution across the image.  The horizontal border is not processed.
	 *
	 * @param input	 The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void horizontal(Kernel1D_I32 kernel,
								  GrayU8 input, GrayI8 output, int divisor ) {
		InputSanityCheck.checkSameShape(input, output);

		if( !ConvolveImageUnrolled_U8_I8_Div.horizontal(kernel, input,  output, divisor))
			ConvolveImageStandard.horizontal(kernel, input,  output, divisor);
	}

	/**
	 * Performs a horizontal 1D convolution across the image.  The horizontal border is not processed.
	 *
	 * @param input	 The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void horizontal(Kernel1D_I32 kernel, GrayU8 input, GrayI16 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (!ConvolveImageUnrolled_U8_I16.horizontal(kernel, input,  output ))
			ConvolveImageStandard.horizontal(kernel, input,  output);
	}

	/**
	 * Performs a horizontal 1D convolution across the image.  The horizontal border is not processed.
	 *
	 * @param input	 The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void horizontal(Kernel1D_I32 kernel,
								  GrayU8 input, GrayS32 output ) {
		InputSanityCheck.checkSameShape(input, output);

		// todo add unroll
		ConvolveImageStandard.horizontal(kernel, input, output);
	}

	/**
	 * Performs a horizontal 1D convolution across the image.  The horizontal border is not processed.
	 *
	 * @param input	 The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void horizontal(Kernel1D_I32 kernel, GrayS16 input, GrayI16 output) {
		InputSanityCheck.checkSameShape(input, output);

		if (!ConvolveImageUnrolled_S16_I16.horizontal(kernel, input, output))
			ConvolveImageStandard.horizontal(kernel, input, output);
	}

	/**
	 * Performs a horizontal 1D convolution across the image.  The horizontal border is not processed.
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 * @param divisor The value that the convolved image is divided by.
	 */
	public static void horizontal(Kernel1D_I32 kernel,
								  GrayS16 input, GrayI16 output, int divisor) {
		InputSanityCheck.checkSameShape(input, output);

		if (!ConvolveImageUnrolled_S16_I16_Div.horizontal(kernel, input, output, divisor))
			ConvolveImageStandard.horizontal(kernel, input, output, divisor);
	}

	/**
	 * Performs a horizontal 1D convolution across the image.  The horizontal border is not processed.
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 * @param divisor The value that the convolved image is divided by.
	 */
	public static void horizontal(Kernel1D_I32 kernel,
								  GrayS32 input, GrayS32 output, int divisor) {
		InputSanityCheck.checkSameShape(input, output);

		if (!ConvolveImageUnrolled_S32_S32_Div.horizontal(kernel, input, output, divisor))
			ConvolveImageStandard.horizontal(kernel, input, output, divisor);
	}

	/**
	 * Performs a vertical 1D convolution across the image in the vertical direction.
	 * The vertical border is not processed.
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void vertical(Kernel1D_F32 kernel, GrayF32 input, GrayF32 output) {
		InputSanityCheck.checkSameShape(input, output);

		if (!ConvolveImageUnrolled_F32_F32.vertical(kernel, input,  output))
			ConvolveImageStandard.vertical(kernel, input,  output);
	}

	/**
	 * Performs a vertical 1D convolution across the image in the vertical direction.
	 * The vertical border is not processed.
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void vertical(Kernel1D_F64 kernel, GrayF64 input, GrayF64 output) {
		InputSanityCheck.checkSameShape(input, output);

		if (!ConvolveImageUnrolled_F64_F64.vertical(kernel, input,  output))
			ConvolveImageStandard.vertical(kernel, input,  output);
	}

	/**
	 * Performs a vertical 1D convolution across the image in the vertical direction.
	 * The vertical border is not processed.
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void vertical(Kernel1D_I32 kernel,
								GrayU8 input, GrayI8 output, int divisor) {
		InputSanityCheck.checkSameShape(input, output);

		if( !ConvolveImageUnrolled_U8_I8_Div.vertical(kernel, input,  output, divisor))
			ConvolveImageStandard.vertical(kernel, input,  output, divisor);
	}

	/**
	 * Performs a vertical 1D convolution across the image in the vertical direction.
	 * The vertical border is not processed.
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void vertical(Kernel1D_I32 kernel,
								GrayU16 input, GrayI8 output, int divisor) {
		InputSanityCheck.checkSameShape(input, output);

		// TODO unroll
		ConvolveImageStandard.vertical(kernel, input,  output, divisor);
	}

	/**
	 * Performs a vertical 1D convolution across the image in the vertical direction.
	 * The vertical border is not processed.
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void vertical(Kernel1D_I32 kernel, GrayU8 input, GrayI16 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (!ConvolveImageUnrolled_U8_I16.vertical(kernel, input,  output))
			ConvolveImageStandard.vertical(kernel, input,  output);
	}

	/**
	 * Performs a vertical 1D convolution across the image in the vertical direction.
	 * The vertical border is not processed.
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void vertical(Kernel1D_I32 kernel, GrayU8 input, GrayS32 output ) {
		InputSanityCheck.checkSameShape(input, output);

		// todo add unroll
		ConvolveImageStandard.vertical(kernel, input,  output);
	}
	/**
	 * Performs a vertical 1D convolution across the image in the vertical direction.
	 * The vertical border is not processed.
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void vertical(Kernel1D_I32 kernel, GrayS16 input, GrayI16 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (!ConvolveImageUnrolled_S16_I16.vertical(kernel, input,  output))
			ConvolveImageStandard.vertical(kernel, input,  output);
	}

	/**
	 * Performs a vertical 1D convolution across the image in the vertical direction.
	 * The vertical border is not processed.
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 * @param divisor The value that the convolved image is divided by.
	 */
	public static void vertical(Kernel1D_I32 kernel, GrayS16 input, GrayI16 output, int divisor ) {
		InputSanityCheck.checkSameShape(input, output);

		if (!ConvolveImageUnrolled_S16_I16_Div.vertical(kernel, input, output, divisor))
			ConvolveImageStandard.vertical(kernel, input, output, divisor);
	}

	/**
	 * Performs a vertical 1D convolution across the image in the vertical direction.
	 * The vertical border is not processed.
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 * @param divisor The value that the convolved image is divided by.
	 */
	public static void vertical(Kernel1D_I32 kernel, GrayS32 input, GrayI16 output, int divisor ) {
		InputSanityCheck.checkSameShape(input, output);

		// todo unroll
		ConvolveImageStandard.vertical(kernel, input, output, divisor);
	}

	/**
	 * Performs a vertical 1D convolution across the image in the vertical direction.
	 * The vertical border is not processed.
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 * @param divisor The value that the convolved image is divided by.
	 */
	public static void vertical(Kernel1D_I32 kernel, GrayS32 input, GrayS32 output, int divisor ) {
		InputSanityCheck.checkSameShape(input, output);

		if (!ConvolveImageUnrolled_S32_S32_Div.vertical(kernel, input, output, divisor))
			ConvolveImageStandard.vertical(kernel, input, output, divisor);
	}

	/**
	 * Performs a 2D convolution across the image.  The image's borders are not processed.
	 *
	 * @param kernel A square kernel that will be convolved across the source image
	 * @param input  The source image that is to be convolved
	 * @param output   The results of the convolution
	 */
	public static void convolve(Kernel2D_F32 kernel, GrayF32 input, GrayF32 output) {
		InputSanityCheck.checkSameShape(input, output);

		if( !ConvolveImageUnrolled_F32_F32.convolve(kernel,input,output))
			ConvolveImageStandard.convolve(kernel, input,  output);
	}

	/**
	 * Performs a 2D convolution across the image.  The image's borders are not processed.
	 *
	 * @param kernel A square kernel that will be convolved across the source image
	 * @param input  The source image that is to be convolved
	 * @param output   The results of the convolution
	 */
	public static void convolve(Kernel2D_F64 kernel, GrayF64 input, GrayF64 output) {
		InputSanityCheck.checkSameShape(input, output);

		if( !ConvolveImageUnrolled_F64_F64.convolve(kernel,input,output))
			ConvolveImageStandard.convolve(kernel, input,  output);
	}

	/**
	 * Performs a 2D convolution with division across the image.  The image's borders are not processed.
	 *
	 * @param kernel  A square kernel that will be convolved across the source image
	 * @param input   The source image that is to be convolved
	 * @param output	The results of the convolution
	 * @param divisor The value that the convolved image is divided by.
	 */
	public static void convolve(Kernel2D_I32 kernel,
								GrayU8 input, GrayI8 output, int divisor) {
		InputSanityCheck.checkSameShape(input, output);

		if( !ConvolveImageUnrolled_U8_I8_Div.convolve(kernel,input,output,divisor))
			ConvolveImageStandard.convolve(kernel, input,  output, divisor);
	}

	/**
	 * Performs a 2D convolution across the image.  The image's borders are not processed.
	 *
	 * @param kernel A square kernel that will be convolved across the source image
	 * @param input  The source image that is to be convolved
	 * @param output   The results of the convolution
	 */
	public static void convolve(Kernel2D_I32 kernel, GrayU8 input, GrayI16 output) {
		InputSanityCheck.checkSameShape(input, output);

		if( !ConvolveImageUnrolled_U8_I16.convolve(kernel,input,output))
			ConvolveImageStandard.convolve(kernel, input,  output);
	}

	/**
	 * Performs a 2D convolution across the image.  The image's borders are not processed.
	 *
	 * @param kernel A square kernel that will be convolved across the source image
	 * @param input  The source image that is to be convolved
	 * @param output   The results of the convolution
	 */
	public static void convolve(Kernel2D_I32 kernel, GrayU8 input, GrayS32 output) {
		InputSanityCheck.checkSameShape(input, output);

		// todo add unrolled
		ConvolveImageStandard.convolve(kernel, input,  output);
	}

	/**
	 * Performs a 2D convolution across the image.  The image's borders are not processed.
	 *
	 * @param kernel A square kernel that will be convolved across the source image
	 * @param input  The source image that is to be convolved
	 * @param output   The results of the convolution
	 */
	public static void convolve(Kernel2D_I32 kernel, GrayS16 input, GrayI16 output) {
		InputSanityCheck.checkSameShape(input, output);

		if( !ConvolveImageUnrolled_S16_I16.convolve(kernel,input,output))
			ConvolveImageStandard.convolve(kernel, input,  output);
	}

	/**
	 * Performs a 2D convolution across the image.  The image's borders are not processed.
	 *
	 * @param kernel A square kernel that will be convolved across the source image
	 * @param input  The source image that is to be convolved
	 * @param output   The results of the convolution
	 * @param divisor The value that the convolved image is divided by.
	 */
	public static void convolve(Kernel2D_I32 kernel, GrayS16 input, GrayI16 output, int divisor ) {
		InputSanityCheck.checkSameShape(input, output);

		if( !ConvolveImageUnrolled_S16_I16_Div.convolve(kernel,input,output,divisor))
			ConvolveImageStandard.convolve(kernel, input,  output, divisor);
	}
}