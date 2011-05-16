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
import gecv.alg.filter.convolve.border.ConvolveJustBorder_General;
import gecv.core.image.border.ImageBorderExtended;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.convolve.Kernel2D_I32;
import gecv.struct.image.*;

/**
 * <p>
 * Convolves a kernel across an image and handles the image border by extended the last pixel outwards.
 * </p>
 *
 * @author Peter Abeles
 */
public class ConvolveExtended {
	/**
	 * Performs a horizontal 1D convolution across the image.  Borders are handled by extending outwards to the closest
	 * pixel along the image border.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void horizontal(Kernel1D_F32 kernel,
								  ImageFloat32 image, ImageFloat32 dest ) {
		InputSanityCheck.checkSameShape(image, dest);

		ConvolveImageNoBorder.horizontal(kernel,image,dest,true);
		ConvolveJustBorder_General.horizontal(kernel, ImageBorderExtended.wrap(image),dest,kernel.getRadius());
	}

	/**
	 * Performs a vertical 1D convolution across the image.  Borders are handled by extending outwards to the closest
	 * pixel along the image border.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void vertical(Kernel1D_F32 kernel,
								ImageFloat32 image, ImageFloat32 dest ) {
		InputSanityCheck.checkSameShape(image, dest);

		ConvolveImageNoBorder.vertical(kernel,image,dest,true);
		ConvolveJustBorder_General.vertical(kernel, ImageBorderExtended.wrap(image),dest,kernel.getRadius());
	}

	/**
	 * Performs a 2D convolution across the image.  Borders are handled by extending outwards to the closest
	 * pixel along the image border.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void convolve(Kernel2D_F32 kernel,
								ImageFloat32 image, ImageFloat32 dest ) {
		InputSanityCheck.checkSameShape(image, dest);

		ConvolveImageNoBorder.convolve(kernel,image,dest);
		ConvolveJustBorder_General.convolve(kernel,ImageBorderExtended.wrap(image),dest,kernel.getRadius());
	}

	/**
	 * Performs a horizontal 1D convolution across the image.  Borders are handled by extending outwards to the closest
	 * pixel along the image border.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void horizontal(Kernel1D_I32 kernel, ImageUInt8 image, ImageInt16 dest ) {
		InputSanityCheck.checkSameShape(image, dest);

		ConvolveImageNoBorder.horizontal(kernel,image,dest,true);
		ConvolveJustBorder_General.horizontal(kernel, ImageBorderExtended.wrap(image),dest,kernel.getRadius());
	}

	/**
	 * Performs a vertical 1D convolution across the image.  Borders are handled by extending outwards to the closest
	 * pixel along the image border.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void vertical(Kernel1D_I32 kernel, ImageUInt8 image, ImageInt16 dest ) {
		InputSanityCheck.checkSameShape(image, dest);

		ConvolveImageNoBorder.vertical(kernel,image,dest,true);
		ConvolveJustBorder_General.vertical(kernel, ImageBorderExtended.wrap(image),dest,kernel.getRadius());
	}

	/**
	 * Performs a 2D convolution across the image.  Borders are handled by extending outwards to the closest
	 * pixel along the image border.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void convolve(Kernel2D_I32 kernel, ImageUInt8 image, ImageInt16 dest ) {
		InputSanityCheck.checkSameShape(image, dest);

		ConvolveImageNoBorder.convolve(kernel,image,dest);
		ConvolveJustBorder_General.convolve(kernel,ImageBorderExtended.wrap(image),dest,kernel.getRadius());
	}

	/**
	 * Performs a horizontal 1D convolution across the image.  Borders are handled by extending outwards to the closest
	 * pixel along the image border.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void horizontal(Kernel1D_I32 kernel, ImageUInt8 image, ImageSInt32 dest ) {
		InputSanityCheck.checkSameShape(image, dest);

		ConvolveImageNoBorder.horizontal(kernel,image,dest,true);
		ConvolveJustBorder_General.horizontal(kernel, ImageBorderExtended.wrap(image),dest,kernel.getRadius());
	}

	/**
	 * Performs a vertical 1D convolution across the image.  Borders are handled by extending outwards to the closest
	 * pixel along the image border.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void vertical(Kernel1D_I32 kernel, ImageUInt8 image, ImageSInt32 dest ) {
		InputSanityCheck.checkSameShape(image, dest);

		ConvolveImageNoBorder.vertical(kernel,image,dest,true);
		ConvolveJustBorder_General.vertical(kernel, ImageBorderExtended.wrap(image),dest,kernel.getRadius());
	}

	/**
	 * Performs a 2D convolution across the image.  Borders are handled by extending outwards to the closest
	 * pixel along the image border.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void convolve(Kernel2D_I32 kernel, ImageUInt8 image, ImageSInt32 dest ) {
		InputSanityCheck.checkSameShape(image, dest);

		ConvolveImageNoBorder.convolve(kernel,image,dest);
		ConvolveJustBorder_General.convolve(kernel,ImageBorderExtended.wrap(image),dest,kernel.getRadius());
	}

	/**
	 * Performs a horizontal 1D convolution across the image.  Borders are handled by extending outwards to the closest
	 * pixel along the image border.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void horizontal(Kernel1D_I32 kernel, ImageSInt16 image, ImageInt16 dest ) {
		InputSanityCheck.checkSameShape(image, dest);

		ConvolveImageNoBorder.horizontal(kernel,image,dest,true);
		ConvolveJustBorder_General.horizontal(kernel, ImageBorderExtended.wrap(image),dest,kernel.getRadius());
	}

	/**
	 * Performs a vertical 1D convolution across the image.  Borders are handled by extending outwards to the closest
	 * pixel along the image border.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void vertical(Kernel1D_I32 kernel, ImageSInt16 image, ImageInt16 dest ) {
		InputSanityCheck.checkSameShape(image, dest);

		ConvolveImageNoBorder.vertical(kernel,image,dest,true);
		ConvolveJustBorder_General.vertical(kernel, ImageBorderExtended.wrap(image),dest,kernel.getRadius());
	}

	/**
	 * Performs a 2D convolution across the image.  Borders are handled by extending outwards to the closest
	 * pixel along the image border.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void convolve(Kernel2D_I32 kernel, ImageSInt16 image, ImageInt16 dest ) {
		InputSanityCheck.checkSameShape(image, dest);

		ConvolveImageNoBorder.convolve(kernel,image,dest);
		ConvolveJustBorder_General.convolve(kernel,ImageBorderExtended.wrap(image),dest,kernel.getRadius());
	}
}
