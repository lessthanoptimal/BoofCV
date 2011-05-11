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
import gecv.alg.filter.convolve.noborder.*;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.convolve.Kernel2D_I32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageSInt32;
import gecv.struct.image.ImageUInt8;


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
	 * Performs a horizontal 1D convolution across the image.  The horizontal border
	 * is not processed and the vertical border is optionally processed.  The border is as wide
	 * as the radius of the kernel.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 * @param includeVerticalBorder Should the vertical border of the image be processed?
	 */
	public static void horizontal(Kernel1D_F32 kernel,
								  ImageFloat32 image, ImageFloat32 dest,
								  boolean includeVerticalBorder) {
		InputSanityCheck.checkSameShape(image, dest);

		if (!ConvolveImageUnrolled_F32_F32.horizontal(kernel, image, dest, includeVerticalBorder))
			ConvolveImageStandard.horizontal(kernel, image, dest, includeVerticalBorder);
	}

	/**
	 * Performs a horizontal 1D convolution across the image with division.  The horizontal border
	 * is not processed and the vertical border is optionally processed.  The border is as wide
	 * as the radius of the kernel.
	 *
	 * @param image		The original image. Not modified.
	 * @param dest		Where the resulting image is written to. Modified.
	 * @param kernel	The kernel that is being convolved. Not modified.
	 * @param divisor	The value that the convolved image is divided by.
	 * @param includeVerticalBorder Should the vertical border of the image be processed?
	 */
	public static void horizontal(Kernel1D_I32 kernel,
								  ImageUInt8 image, ImageUInt8 dest, int divisor, boolean includeVerticalBorder) {
		InputSanityCheck.checkSameShape(image, dest);

		if( !ConvolveImageUnrolled_I8_I8_Div.horizontal(kernel, image, dest, divisor, includeVerticalBorder))
			ConvolveImageStandard.horizontal(kernel, image, dest, divisor, includeVerticalBorder);
	}

	/**
	 * Performs a horizontal 1D convolution across the image.  The horizontal border
	 * is not processed and the vertical border is optionally processed.  The border is as wide
	 * as the radius of the kernel.
	 *
	 * @param image		 The original image. Not modified.
	 * @param dest		  Where the resulting image is written to. Modified.
	 * @param kernel		The kernel that is being convolved. Not modified.
	 * @param includeVerticalBorder Should the vertical border of the image be processed?
	 */
	public static void horizontal(Kernel1D_I32 kernel,
								  ImageUInt8 image, ImageSInt16 dest, boolean includeVerticalBorder) {
		InputSanityCheck.checkSameShape(image, dest);

		if (!ConvolveImageUnrolled_I8_I16.horizontal(kernel, image, dest, includeVerticalBorder))
			ConvolveImageStandard.horizontal(kernel, image, dest, includeVerticalBorder);
	}

	/**
	 * Performs a horizontal 1D convolution across the image.  The horizontal border
	 * is not processed and the vertical border is optionally processed.  The border is as wide
	 * as the radius of the kernel.
	 *
	 * @param image		 The original image. Not modified.
	 * @param dest		  Where the resulting image is written to. Modified.
	 * @param kernel		The kernel that is being convolved. Not modified.
	 * @param includeVerticalBorder Should the vertical border of the image be processed?
	 */
	public static void horizontal(Kernel1D_I32 kernel,
								  ImageUInt8 image, ImageSInt32 dest, boolean includeVerticalBorder) {
		InputSanityCheck.checkSameShape(image, dest);

		// todo add unroll
		ConvolveImageStandard.horizontal(kernel, image, dest, includeVerticalBorder);
	}

	/**
	 * Performs a horizontal 1D convolution across the image.  The horizontal border
	 * is not processed and the vertical border is optionally processed.  The border is as wide
	 * as the radius of the kernel.
	 *
	 * @param image		 The original image. Not modified.
	 * @param dest		  Where the resulting image is written to. Modified.
	 * @param kernel		The kernel that is being convolved. Not modified.
	 * @param includeVerticalBorder Should the vertical border of the image be processed?
	 */
	public static void horizontal(Kernel1D_I32 kernel,
								  ImageSInt16 image, ImageSInt16 dest, boolean includeVerticalBorder) {
		InputSanityCheck.checkSameShape(image, dest);

		if (!ConvolveImageUnrolled_I16_I16.horizontal(kernel, image, dest, includeVerticalBorder))
			ConvolveImageStandard.horizontal(kernel, image, dest, includeVerticalBorder);
	}

/**
	 * Performs a horizontal 1D convolution across the image.  The horizontal border
	 * is not processed and the vertical border is optionally processed.  The border is as wide
	 * as the radius of the kernel.
	 *
	 * @param image		 The original image. Not modified.
	 * @param dest		  Where the resulting image is written to. Modified.
	 * @param kernel		The kernel that is being convolved. Not modified.
	 * @param divisor	   The value that the convolved image is divided by.
	 * @param includeVerticalBorder Should the vertical border of the image be processed?
	 */
	public static void horizontal(Kernel1D_I32 kernel,
								  ImageSInt16 image, ImageSInt16 dest, int divisor, boolean includeVerticalBorder) {
		InputSanityCheck.checkSameShape(image, dest);

		if (!ConvolveImageUnrolled_I16_I16_Div.horizontal(kernel, image, dest, divisor , includeVerticalBorder))
			ConvolveImageStandard.horizontal(kernel, image, dest, divisor, includeVerticalBorder);
	}

	/**
	 * Performs a vertical 1D convolution across the image.  The vertical border
	 * is not processed and the horizontal border is optionally processed.  The border is as wide
	 * as the radius of the kernel.
	 *
	 * @param image		 The original image. Not modified.
	 * @param dest		  Where the resulting image is written to. Modified.
	 * @param kernel		The kernel that is being convolved. Not modified.
	 * @param includeHorizontalBorder Should the horizontal border of the image be processed?
	 */
	public static void vertical(Kernel1D_F32 kernel,
								ImageFloat32 image, ImageFloat32 dest,
								boolean includeHorizontalBorder) {
		InputSanityCheck.checkSameShape(image, dest);

		if (!ConvolveImageUnrolled_F32_F32.vertical(kernel, image, dest, includeHorizontalBorder))
			ConvolveImageStandard.vertical(kernel, image, dest, includeHorizontalBorder);
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
	 * @param includeHorizontalBorder Should the horizontal border of the image be processed?
	 */
	public static void vertical(Kernel1D_I32 kernel,
								ImageUInt8 image, ImageUInt8 dest, int divisor, boolean includeHorizontalBorder) {
		InputSanityCheck.checkSameShape(image, dest);

		if( !ConvolveImageUnrolled_I8_I8_Div.vertical(kernel, image, dest, divisor, includeHorizontalBorder))
			ConvolveImageStandard.vertical(kernel, image, dest, divisor, includeHorizontalBorder);
	}

	/**
	 * Performs a vertical 1D convolution across the image.  The vertical border
	 * is not processed and the horizontal border is optionally processed.  The border is as wide
	 * as the radius of the kernel.
	 *
	 * @param image		 The original image. Not modified.
	 * @param dest		  Where the resulting image is written to. Modified.
	 * @param kernel		The kernel that is being convolved. Not modified.
	 * @param includeHorizontalBorder Should the horizontal border of the image be processed?
	 */
	public static void vertical(Kernel1D_I32 kernel,
								ImageUInt8 image, ImageSInt16 dest,
								boolean includeHorizontalBorder) {
		InputSanityCheck.checkSameShape(image, dest);

		if (!ConvolveImageUnrolled_I8_I16.vertical(kernel, image, dest, includeHorizontalBorder))
			ConvolveImageStandard.vertical(kernel, image, dest, includeHorizontalBorder);
	}

	/**
	 * Performs a vertical 1D convolution across the image.  The vertical border
	 * is not processed and the horizontal border is optionally processed.  The border is as wide
	 * as the radius of the kernel.
	 *
	 * @param image		 The original image. Not modified.
	 * @param dest		  Where the resulting image is written to. Modified.
	 * @param kernel		The kernel that is being convolved. Not modified.
	 * @param includeHorizontalBorder Should the horizontal border of the image be processed?
	 */
	public static void vertical(Kernel1D_I32 kernel,
								ImageUInt8 image, ImageSInt32 dest,
								boolean includeHorizontalBorder) {
		InputSanityCheck.checkSameShape(image, dest);

		// todo add unroll
		ConvolveImageStandard.vertical(kernel, image, dest, includeHorizontalBorder);
	}

	/**
	 * Performs a vertical 1D convolution across the image.  The vertical border
	 * is not processed and the horizontal border is optionally processed.  The border is as wide
	 * as the radius of the kernel.
	 *
	 * @param image		 The original image. Not modified.
	 * @param dest		  Where the resulting image is written to. Modified.
	 * @param kernel		The kernel that is being convolved. Not modified.
	 * @param includeHorizontalBorder Should the horizontal border of the image be processed?
	 */
	public static void vertical(Kernel1D_I32 kernel,
								ImageSInt16 image, ImageSInt16 dest,
								boolean includeHorizontalBorder) {
		InputSanityCheck.checkSameShape(image, dest);

		if (!ConvolveImageUnrolled_I16_I16.vertical(kernel, image, dest, includeHorizontalBorder))
			ConvolveImageStandard.vertical(kernel, image, dest, includeHorizontalBorder);
	}

/**
	 * Performs a vertical 1D convolution across the image.  The vertical border
	 * is not processed and the horizontal border is optionally processed.  The border is as wide
	 * as the radius of the kernel.
	 *
	 * @param image		 The original image. Not modified.
	 * @param dest		  Where the resulting image is written to. Modified.
	 * @param kernel		The kernel that is being convolved. Not modified.
	 * @param divisor	   The value that the convolved image is divided by.
	 * @param includeHorizontalBorder Should the horizontal border of the image be processed?
	 */
	public static void vertical(Kernel1D_I32 kernel,
								ImageSInt16 image, ImageSInt16 dest,
								int divisor , boolean includeHorizontalBorder) {
		InputSanityCheck.checkSameShape(image, dest);

		if (!ConvolveImageUnrolled_I16_I16_Div.vertical(kernel, image, dest, divisor , includeHorizontalBorder))
			ConvolveImageStandard.vertical(kernel, image, dest, divisor , includeHorizontalBorder);
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

		if( !ConvolveImageUnrolled_F32_F32.convolve(kernel,image,dest))
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
								ImageUInt8 image, ImageUInt8 dest, int divisor) {
		InputSanityCheck.checkSameShape(image, dest);

		if( !ConvolveImageUnrolled_I8_I8_Div.convolve(kernel,image,dest,divisor))
			ConvolveImageStandard.convolve(kernel, image, dest, divisor);
	}

	/**
	 * Performs a 2D convolution across the image.  The image's borders are not processed.
	 *
	 * @param kernel A square kernel that will be convolved across the source image
	 * @param image  The source image that is to be convolved
	 * @param dest   The results of the convolution
	 */
	public static void convolve(Kernel2D_I32 kernel, ImageUInt8 image, ImageSInt16 dest) {
		InputSanityCheck.checkSameShape(image, dest);

		if( !ConvolveImageUnrolled_I8_I16.convolve(kernel,image,dest))
			ConvolveImageStandard.convolve(kernel, image, dest);
	}

	/**
	 * Performs a 2D convolution across the image.  The image's borders are not processed.
	 *
	 * @param kernel A square kernel that will be convolved across the source image
	 * @param image  The source image that is to be convolved
	 * @param dest   The results of the convolution
	 */
	public static void convolve(Kernel2D_I32 kernel, ImageUInt8 image, ImageSInt32 dest) {
		InputSanityCheck.checkSameShape(image, dest);

		// todo add unrolled
		ConvolveImageStandard.convolve(kernel, image, dest);
	}

	/**
	 * Performs a 2D convolution across the image.  The image's borders are not processed.
	 *
	 * @param kernel A square kernel that will be convolved across the source image
	 * @param image  The source image that is to be convolved
	 * @param dest   The results of the convolution
	 */
	public static void convolve(Kernel2D_I32 kernel, ImageSInt16 image, ImageSInt16 dest) {
		InputSanityCheck.checkSameShape(image, dest);

		if( !ConvolveImageUnrolled_I16_I16.convolve(kernel,image,dest))
			ConvolveImageStandard.convolve(kernel, image, dest);
	}

	/**
	 * Performs a 2D convolution across the image.  The image's borders are not processed.
	 *
	 * @param kernel A square kernel that will be convolved across the source image
	 * @param image  The source image that is to be convolved
	 * @param dest   The results of the convolution
	 * @param divisor The value that the convolved image is divided by.
	 */
	public static void convolve(Kernel2D_I32 kernel, ImageSInt16 image, ImageSInt16 dest, int divisor ) {
		InputSanityCheck.checkSameShape(image, dest);

		if( !ConvolveImageUnrolled_I16_I16_Div.convolve(kernel,image,dest,divisor))
			ConvolveImageStandard.convolve(kernel, image, dest, divisor);
	}
}