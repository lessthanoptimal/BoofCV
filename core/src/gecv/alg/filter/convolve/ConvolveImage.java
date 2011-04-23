/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.filter.convolve;

import gecv.alg.InputSanityCheck;
import gecv.alg.filter.convolve.impl.*;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.convolve.Kernel2D_I32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt16;
import gecv.struct.image.ImageInt32;
import gecv.struct.image.ImageInt8;


/**
 * <p>
 * Provides functions for convolving 1D and 2D kernels across an image.  1D kernels can either
 * be convolved along each row or column in the image.  No checks are done for overflow or underflow.
 * </p>
 * <p/>
 * <p>
 * When convolving with division the convolution is computed as usual, but then the result is divided by
 * the divisor.  This is typically done when performing convolution inside of integer images to normalize
 * it by the sum of all the elements in the convolution kernel.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"ForLoopReplaceableByForEach"})
public class ConvolveImage {

	/**
	 * Performs a horizontal 1D convolution across the image.  The horizontal border
	 * is not processed and the vertical border is optionally processed.  The border is as wide
	 * as the radius of the kernel.
	 *
	 * @param image		 The original image. Not modified.
	 * @param dest		  Where the resulting image is written to. Modified.
	 * @param kernel		The kernel that is being convolved. Not modified.
	 * @param includeBorder Should the vertical border of the image be processed?
	 */
	public static void horizontal(Kernel1D_F32 kernel,
								  ImageFloat32 image, ImageFloat32 dest,
								  boolean includeBorder) {
		InputSanityCheck.checkSameShape(image, dest);

		if (!ConvolveImageUnrolled_F32_F32.horizontal(kernel, image, dest, includeBorder))
			ConvolveImageStandard.horizontal(kernel, image, dest, includeBorder);
	}

	/**
	 * Performs a horizontal 1D convolution across the image with division.  The horizontal border
	 * is not processed and the vertical border is optionally processed.  The border is as wide
	 * as the radius of the kernel.
	 *
	 * @param image		 The original image. Not modified.
	 * @param dest		  Where the resulting image is written to. Modified.
	 * @param kernel		The kernel that is being convolved. Not modified.
	 * @param divisor	   The value that the convolved image is divided by.
	 * @param includeBorder Should the vertical border of the image be processed?
	 */
	public static void horizontal(Kernel1D_I32 kernel,
								  ImageInt8 image, ImageInt8 dest, int divisor, boolean includeBorder) {
		InputSanityCheck.checkSameShape(image, dest);

		if( !ConvolveImageUnrolled_I8_I8.horizontal(kernel, image, dest, divisor, includeBorder))
			ConvolveImageStandard.horizontal(kernel, image, dest, divisor, includeBorder);
	}

	/**
	 * Performs a horizontal 1D convolution across the image.  The horizontal border
	 * is not processed and the vertical border is optionally processed.  The border is as wide
	 * as the radius of the kernel.
	 *
	 * @param image		 The original image. Not modified.
	 * @param dest		  Where the resulting image is written to. Modified.
	 * @param kernel		The kernel that is being convolved. Not modified.
	 * @param includeBorder Should the vertical border of the image be processed?
	 */
	public static void horizontal(Kernel1D_I32 kernel,
								  ImageInt8 image, ImageInt16 dest, boolean includeBorder) {
		InputSanityCheck.checkSameShape(image, dest);

		if (!ConvolveImageUnrolled_I8_I16.horizontal(kernel, image, dest, includeBorder))
			ConvolveImageStandard.horizontal(kernel, image, dest, includeBorder);
	}

	/**
	 * Performs a horizontal 1D convolution across the image.  The horizontal border
	 * is not processed and the vertical border is optionally processed.  The border is as wide
	 * as the radius of the kernel.
	 *
	 * @param image		 The original image. Not modified.
	 * @param dest		  Where the resulting image is written to. Modified.
	 * @param kernel		The kernel that is being convolved. Not modified.
	 * @param includeBorder Should the vertical border of the image be processed?
	 */
	public static void horizontal(Kernel1D_I32 kernel,
								  ImageInt8 image, ImageInt32 dest, boolean includeBorder) {
		InputSanityCheck.checkSameShape(image, dest);

		// todo add unroll
		ConvolveImageStandard.horizontal(kernel, image, dest, includeBorder);
	}

	/**
	 * Performs a horizontal 1D convolution across the image.  The horizontal border
	 * is not processed and the vertical border is optionally processed.  The border is as wide
	 * as the radius of the kernel.
	 *
	 * @param image		 The original image. Not modified.
	 * @param dest		  Where the resulting image is written to. Modified.
	 * @param kernel		The kernel that is being convolved. Not modified.
	 * @param includeBorder Should the vertical border of the image be processed?
	 */
	public static void horizontal(Kernel1D_I32 kernel,
								  ImageInt16 image, ImageInt16 dest, boolean includeBorder) {
		InputSanityCheck.checkSameShape(image, dest);

		if (!ConvolveImageUnrolled_I16_I16.horizontal(kernel, image, dest, includeBorder))
			ConvolveImageStandard.horizontal(kernel, image, dest, includeBorder);
	}

	/**
	 * Performs a vertical 1D convolution across the image.  The vertical border
	 * is not processed and the horizontal border is optionally processed.  The border is as wide
	 * as the radius of the kernel.
	 *
	 * @param image		 The original image. Not modified.
	 * @param dest		  Where the resulting image is written to. Modified.
	 * @param kernel		The kernel that is being convolved. Not modified.
	 * @param includeBorder Should the horizontal border of the image be processed?
	 */
	public static void vertical(Kernel1D_F32 kernel,
								ImageFloat32 image, ImageFloat32 dest,
								boolean includeBorder) {
		InputSanityCheck.checkSameShape(image, dest);

		if (!ConvolveImageUnrolled_F32_F32.vertical(kernel, image, dest, includeBorder))
			ConvolveImageStandard.vertical(kernel, image, dest, includeBorder);
	}

	/**
	 * Performs a vertical 1D convolution with division across the image.  The vertical border
	 * is not processed and the horizontal border is optionally processed.  The border is as wide
	 * as the radius of the kernel.
	 *
	 * @param image		 The original image. Not modified.
	 * @param dest		  Where the resulting image is written to. Modified.
	 * @param kernel		The kernel that is being convolved. Not modified.
	 * @param divisor	   The value that the convolved image is divided by.
	 * @param includeBorder Should the horizontal border of the image be processed?
	 */
	public static void vertical(Kernel1D_I32 kernel,
								ImageInt8 image, ImageInt8 dest, int divisor, boolean includeBorder) {
		InputSanityCheck.checkSameShape(image, dest);

		if( !ConvolveImageUnrolled_I8_I8.vertical(kernel, image, dest, divisor, includeBorder))
			ConvolveImageStandard.vertical(kernel, image, dest, divisor, includeBorder);
	}

	/**
	 * Performs a vertical 1D convolution across the image.  The vertical border
	 * is not processed and the horizontal border is optionally processed.  The border is as wide
	 * as the radius of the kernel.
	 *
	 * @param image		 The original image. Not modified.
	 * @param dest		  Where the resulting image is written to. Modified.
	 * @param kernel		The kernel that is being convolved. Not modified.
	 * @param includeBorder Should the horizontal border of the image be processed?
	 */
	public static void vertical(Kernel1D_I32 kernel,
								ImageInt8 image, ImageInt16 dest,
								boolean includeBorder) {
		InputSanityCheck.checkSameShape(image, dest);

		if (!ConvolveImageUnrolled_I8_I16.vertical(kernel, image, dest, includeBorder))
			ConvolveImageStandard.vertical(kernel, image, dest, includeBorder);
	}

	/**
	 * Performs a vertical 1D convolution across the image.  The vertical border
	 * is not processed and the horizontal border is optionally processed.  The border is as wide
	 * as the radius of the kernel.
	 *
	 * @param image		 The original image. Not modified.
	 * @param dest		  Where the resulting image is written to. Modified.
	 * @param kernel		The kernel that is being convolved. Not modified.
	 * @param includeBorder Should the horizontal border of the image be processed?
	 */
	public static void vertical(Kernel1D_I32 kernel,
								ImageInt8 image, ImageInt32 dest,
								boolean includeBorder) {
		InputSanityCheck.checkSameShape(image, dest);

		// todo add unroll
		ConvolveImageStandard.vertical(kernel, image, dest, includeBorder);
	}

	/**
	 * Performs a vertical 1D convolution across the image.  The vertical border
	 * is not processed and the horizontal border is optionally processed.  The border is as wide
	 * as the radius of the kernel.
	 *
	 * @param image		 The original image. Not modified.
	 * @param dest		  Where the resulting image is written to. Modified.
	 * @param kernel		The kernel that is being convolved. Not modified.
	 * @param includeBorder Should the horizontal border of the image be processed?
	 */
	public static void vertical(Kernel1D_I32 kernel,
								ImageInt16 image, ImageInt16 dest,
								boolean includeBorder) {
		InputSanityCheck.checkSameShape(image, dest);

		if (!ConvolveImageUnrolled_I16_I16.vertical(kernel, image, dest, includeBorder))
			ConvolveImageStandard.vertical(kernel, image, dest, includeBorder);
	}

	/**
	 * Performs a 2D convolution across the image.  The image's borders are not processed.
	 *
	 * @param kernel A square kernel that will be convolved across the source image
	 * @param image  The source image that is to be convolved
	 * @param dest   The results of the convolution
	 */
	public static void convolve(Kernel2D_F32 kernel, ImageFloat32 image, ImageFloat32 dest) {
		InputSanityCheck.checkSameShape(image, dest);

		ConvolveImageStandard.convolve(kernel, image, dest);
	}

	/**
	 * Performs a 2D convolution with division across the image.  The image's borders are not processed.
	 *
	 * @param kernel  A square kernel that will be convolved across the source image
	 * @param image   The source image that is to be convolved
	 * @param dest	The results of the convolution
	 * @param divisor The value that the convolved image is divided by.
	 */
	public static void convolve(Kernel2D_I32 kernel,
								ImageInt8 image, ImageInt8 dest, int divisor) {
		InputSanityCheck.checkSameShape(image, dest);

		ConvolveImageStandard.convolve(kernel, image, dest, divisor);
	}

	/**
	 * Performs a 2D convolution across the image.  The image's borders are not processed.
	 *
	 * @param kernel A square kernel that will be convolved across the source image
	 * @param image  The source image that is to be convolved
	 * @param dest   The results of the convolution
	 */
	public static void convolve(Kernel2D_I32 kernel, ImageInt8 image, ImageInt16 dest) {
		InputSanityCheck.checkSameShape(image, dest);

		ConvolveImageStandard.convolve(kernel, image, dest);
	}

	/**
	 * Performs a 2D convolution across the image.  The image's borders are not processed.
	 *
	 * @param kernel A square kernel that will be convolved across the source image
	 * @param image  The source image that is to be convolved
	 * @param dest   The results of the convolution
	 */
	public static void convolve(Kernel2D_I32 kernel, ImageInt16 image, ImageInt16 dest) {
		InputSanityCheck.checkSameShape(image, dest);

		ConvolveImageStandard.convolve(kernel, image, dest);
	}
}