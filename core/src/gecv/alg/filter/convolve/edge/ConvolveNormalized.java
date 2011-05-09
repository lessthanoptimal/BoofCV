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

package gecv.alg.filter.convolve.edge;

import gecv.alg.InputSanityCheck;
import gecv.alg.filter.convolve.ConvolveImage;
import gecv.alg.filter.convolve.edge.impl.ConvolveNormalized_JustBorder;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;

/**
 * Performs a convolution for kernels which can be renormalized depending on which elements in the kernel are used.
 * These types of kernels have a clear definition along the image border and are typically used to blur images.
 *
 * @author Peter Abeles
 */
public class ConvolveNormalized {
	/**
	 * Performs a horizontal 1D convolution across the image while re-normalizing the kernel depending on its
	 * overlap with the image.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void horizontal(Kernel1D_F32 kernel,
								  ImageFloat32 image, ImageFloat32 dest ) {
		InputSanityCheck.checkSameShape(image, dest);

		ConvolveImage.horizontal(kernel,image,dest,true);
		ConvolveNormalized_JustBorder.horizontal(kernel,image,dest);
	}

	/**
	 * Performs a vertical 1D convolution across the image while re-normalizing the kernel depending on its
	 * overlap with the image.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void vertical(Kernel1D_F32 kernel,
								ImageFloat32 image, ImageFloat32 dest ) {
		InputSanityCheck.checkSameShape(image, dest);

		ConvolveImage.vertical(kernel,image,dest,true);
		ConvolveNormalized_JustBorder.vertical(kernel,image,dest);
	}

	/**
	 * Performs a horizontal 1D convolution across the image while re-normalizing the kernel depending on its
	 * overlap with the image.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void horizontal(Kernel1D_I32 kernel,
								  ImageUInt8 image, ImageUInt8 dest ) {
		InputSanityCheck.checkSameShape(image, dest);


		ConvolveImage.horizontal(kernel,image,dest,kernel.computeSum(),true);
		ConvolveNormalized_JustBorder.horizontal(kernel,image,dest);
	}

	/**
	 * Performs a vertical 1D convolution across the image while re-normalizing the kernel depending on its
	 * overlap with the image.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void vertical(Kernel1D_I32 kernel,
								ImageUInt8 image, ImageUInt8 dest ) {
		InputSanityCheck.checkSameShape(image, dest);

		ConvolveImage.vertical(kernel,image,dest,kernel.computeSum(),true);
		ConvolveNormalized_JustBorder.vertical(kernel,image,dest);
	}

	/**
	 * Performs a horizontal 1D convolution across the image while re-normalizing the kernel depending on its
	 * overlap with the image.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void horizontal(Kernel1D_I32 kernel,
								  ImageSInt16 image, ImageSInt16 dest ) {
		InputSanityCheck.checkSameShape(image, dest);


		ConvolveImage.horizontal(kernel,image,dest,kernel.computeSum(),true);
		ConvolveNormalized_JustBorder.horizontal(kernel,image,dest);
	}

	/**
	 * Performs a vertical 1D convolution across the image while re-normalizing the kernel depending on its
	 * overlap with the image.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void vertical(Kernel1D_I32 kernel,
								ImageSInt16 image, ImageSInt16 dest ) {
		InputSanityCheck.checkSameShape(image, dest);

		ConvolveImage.vertical(kernel,image,dest,kernel.computeSum(),true);
		ConvolveNormalized_JustBorder.vertical(kernel,image,dest);
	}
}
