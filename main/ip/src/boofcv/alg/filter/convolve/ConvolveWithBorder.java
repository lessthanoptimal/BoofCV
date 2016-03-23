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
import boofcv.alg.filter.convolve.border.ConvolveJustBorder_General;
import boofcv.core.image.border.ImageBorder_F32;
import boofcv.core.image.border.ImageBorder_S32;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_I32;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.convolve.Kernel2D_I32;
import boofcv.struct.image.*;

/**
 * <p>
 * Convolves a kernel across an image and handles the image border using the specified method.
 * </p>
 *
 * @author Peter Abeles
 */
public class ConvolveWithBorder {
	/**
	 * Performs a horizontal 1D convolution across the image.  Borders are handled as specified by the 'border'
	 * parameter.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 * @param border How the image borders are handled.
	 */
	public static void horizontal(Kernel1D_F32 kernel,
								  GrayF32 image, GrayF32 dest , ImageBorder_F32 border ) {
		InputSanityCheck.checkSameShape(image, dest);

		border.setImage(image);
		ConvolveImageNoBorder.horizontal(kernel,image,dest);
		ConvolveJustBorder_General.horizontal(kernel, border,dest);
	}

	/**
	 * Performs a vertical 1D convolution across the image.  Borders are handled as specified by the 'border'
	 * parameter.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 * @param border How the image borders are handled.
	 */
	public static void vertical(Kernel1D_F32 kernel,
								GrayF32 image, GrayF32 dest , ImageBorder_F32 border ) {
		InputSanityCheck.checkSameShape(image, dest);

		border.setImage(image);
		ConvolveImageNoBorder.vertical(kernel,image,dest);
		ConvolveJustBorder_General.vertical(kernel, border,dest);
	}

	/**
	 * Performs a 2D convolution across the image.  Borders are handled as specified by the 'border'
	 * parameter.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 * @param border How the image borders are handled.
	 */
	public static void convolve(Kernel2D_F32 kernel,
								GrayF32 image, GrayF32 dest , ImageBorder_F32 border ) {
		InputSanityCheck.checkSameShape(image, dest);

		border.setImage(image);
		ConvolveImageNoBorder.convolve(kernel,image,dest);
		ConvolveJustBorder_General.convolve(kernel,border,dest);
	}

	/**
	 * Performs a horizontal 1D convolution across the image.  Borders are handled as specified by the 'border'
	 * parameter.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 * @param border How the image borders are handled.
	 */
	public static void horizontal(Kernel1D_I32 kernel,
								  GrayU8 image, GrayI16 dest , ImageBorder_S32 border ) {
		InputSanityCheck.checkSameShape(image, dest);

		border.setImage(image);
		ConvolveImageNoBorder.horizontal(kernel,image,dest);
		ConvolveJustBorder_General.horizontal(kernel, border,dest);
	}

	/**
	 * Performs a vertical 1D convolution across the image.  Borders are handled as specified by the 'border'
	 * parameter.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 * @param border How the image borders are handled.
	 */
	public static void vertical(Kernel1D_I32 kernel,
								GrayU8 image, GrayI16 dest , ImageBorder_S32 border ) {
		InputSanityCheck.checkSameShape(image, dest);

		border.setImage(image);
		ConvolveImageNoBorder.vertical(kernel,image,dest);
		ConvolveJustBorder_General.vertical(kernel, border,dest);
	}

	/**
	 * Performs a 2D convolution across the image.  Borders are handled as specified by the 'border'
	 * parameter.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 * @param border How the image borders are handled.
	 */
	public static void convolve(Kernel2D_I32 kernel,
								GrayU8 image, GrayI16 dest , ImageBorder_S32 border ) {
		InputSanityCheck.checkSameShape(image, dest);

		border.setImage(image);
		ConvolveImageNoBorder.convolve(kernel,image,dest);
		ConvolveJustBorder_General.convolve(kernel,border,dest);
	}

	/**
	 * Performs a horizontal 1D convolution across the image.  Borders are handled as specified by the 'border'
	 * parameter.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 * @param border How the image borders are handled.
	 */
	public static void horizontal(Kernel1D_I32 kernel,
								  GrayU8 image, GrayS32 dest , ImageBorder_S32 border ) {
		InputSanityCheck.checkSameShape(image, dest);

		border.setImage(image);
		ConvolveImageNoBorder.horizontal(kernel, image, dest);
		ConvolveJustBorder_General.horizontal(kernel, border, dest);
	}

	/**
	 * Performs a vertical 1D convolution across the image.  Borders are handled as specified by the 'border'
	 * parameter.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 * @param border How the image borders are handled.
	 */
	public static void vertical(Kernel1D_I32 kernel,
								GrayU8 image, GrayS32 dest , ImageBorder_S32 border ) {
		InputSanityCheck.checkSameShape(image, dest);

		border.setImage(image);
		ConvolveImageNoBorder.vertical(kernel,image,dest);
		ConvolveJustBorder_General.vertical(kernel, border,dest);
	}

	/**
	 * Performs a 2D convolution across the image.  Borders are handled as specified by the 'border'
	 * parameter.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 * @param border How the image borders are handled.
	 */
	public static void convolve(Kernel2D_I32 kernel,
								GrayU8 image, GrayS32 dest , ImageBorder_S32 border ) {
		InputSanityCheck.checkSameShape(image, dest);

		border.setImage(image);
		ConvolveImageNoBorder.convolve(kernel,image,dest);
		ConvolveJustBorder_General.convolve(kernel,border,dest);
	}

	/**
	 * Performs a horizontal 1D convolution across the image.  Borders are handled as specified by the 'border'
	 * parameter.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 * @param border How the image borders are handled.
	 */
	public static void horizontal(Kernel1D_I32 kernel,
								  GrayS16 image, GrayI16 dest , ImageBorder_S32 border ) {
		InputSanityCheck.checkSameShape(image, dest);

		border.setImage(image);
		ConvolveImageNoBorder.horizontal(kernel,image,dest);
		ConvolveJustBorder_General.horizontal(kernel, border, dest);
	}

	/**
	 * Performs a vertical 1D convolution across the image.  Borders are handled as specified by the 'border'
	 * parameter.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 * @param border How the image borders are handled.
	 */
	public static void vertical(Kernel1D_I32 kernel,
								GrayS16 image, GrayI16 dest , ImageBorder_S32 border ) {
		InputSanityCheck.checkSameShape(image, dest);

		border.setImage(image);
		ConvolveImageNoBorder.vertical(kernel,image,dest);
		ConvolveJustBorder_General.vertical(kernel, border,dest);
	}

	/**
	 * Performs a 2D convolution across the image.  Borders are handled as specified by the 'border'
	 * parameter.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 * @param border How the image borders are handled.
	 */
	public static void convolve(Kernel2D_I32 kernel,
								GrayS16 image, GrayI16 dest , ImageBorder_S32 border ) {
		InputSanityCheck.checkSameShape(image, dest);

		border.setImage(image);
		ConvolveImageNoBorder.convolve(kernel,image,dest);
		ConvolveJustBorder_General.convolve(kernel,border,dest);
	}
}
