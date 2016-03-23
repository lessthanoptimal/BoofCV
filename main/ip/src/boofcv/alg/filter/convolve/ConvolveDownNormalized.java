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

import boofcv.alg.filter.convolve.down.ConvolveDownNormalizedNaive;
import boofcv.alg.filter.convolve.down.ConvolveDownNormalized_JustBorder;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_I32;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.convolve.Kernel2D_I32;
import boofcv.struct.image.*;

import static boofcv.alg.filter.convolve.ConvolveDownNoBorder.checkParameters;


/**
 * <p>
 * Specialized convolution where the center of the convolution skips over a constant number
 * of pixels in the x and/or y axis.  Image borders are handled by renormalizing the kernel.
 * The output it written into an image in a dense fashion, resulting in it being at a lower resolution.
 * A typical application for this is down sampling inside an image pyramid.
 * </p>
 * 
 * @author Peter Abeles
 */
public class ConvolveDownNormalized {

	/**
	 * Performs a horizontal 1D down convolution across the image while re-normalizing the kernel depending on its
	 * overlap with the image.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void horizontal(Kernel1D_F32 kernel, GrayF32 image, GrayF32 dest , int skip ) {
		checkParameters(image, dest, skip);

		if( kernel.width >= image.width ) {
			ConvolveDownNormalizedNaive.horizontal(kernel,image,dest,skip);
		} else {
			ConvolveDownNoBorder.horizontal(kernel,image,dest,skip);
			ConvolveDownNormalized_JustBorder.horizontal(kernel,image,dest,skip);
		}
	}

	/**
	 * Performs a vertical 1D down convolution across the image while re-normalizing the kernel depending on its
	 * overlap with the image.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void vertical(Kernel1D_F32 kernel, GrayF32 image, GrayF32 dest , int skip ) {
		checkParameters(image, dest, skip);

		if( kernel.width >= image.width ) {
			ConvolveDownNormalizedNaive.vertical(kernel,image,dest,skip);
		} else {
			ConvolveDownNoBorder.vertical(kernel,image,dest,skip);
			ConvolveDownNormalized_JustBorder.vertical(kernel,image,dest,skip);
		}
	}

	/**
	 * Performs a 2D down convolution across the image while re-normalizing the kernel depending on its
	 * overlap with the image.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void convolve(Kernel2D_F32 kernel, GrayF32 image, GrayF32 dest , int skip ) {
		checkParameters(image, dest, skip);

		if( kernel.width >= image.width ) {
			ConvolveDownNormalizedNaive.convolve(kernel,image,dest,skip);
		} else {
			ConvolveDownNoBorder.convolve(kernel,image,dest,skip);
			ConvolveDownNormalized_JustBorder.convolve(kernel,image,dest,skip);
		}
	}

	/**
	 * Performs a horizontal 1D down convolution across the image while re-normalizing the kernel depending on its
	 * overlap with the image.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void horizontal(Kernel1D_I32 kernel, GrayU8 image, GrayI8 dest , int skip ) {
		checkParameters(image, dest, skip);

		if( kernel.width >= image.width ) {
			ConvolveDownNormalizedNaive.horizontal(kernel,image,dest,skip);
		} else {
			ConvolveDownNoBorder.horizontal(kernel,image,dest,skip,kernel.computeSum());
			ConvolveDownNormalized_JustBorder.horizontal(kernel,image,dest,skip);
		}
	}

	/**
	 * Performs a vertical 1D down convolution across the image while re-normalizing the kernel depending on its
	 * overlap with the image.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void vertical(Kernel1D_I32 kernel, GrayU8 image, GrayI8 dest , int skip ) {
		checkParameters(image, dest, skip);

		if( kernel.width >= image.width ) {
			ConvolveDownNormalizedNaive.vertical(kernel,image,dest,skip);
		} else {
			ConvolveDownNoBorder.vertical(kernel,image,dest,skip,kernel.computeSum());
			ConvolveDownNormalized_JustBorder.vertical(kernel,image,dest,skip);
		}
	}

	/**
	 * Performs a 2D down convolution across the image while re-normalizing the kernel depending on its
	 * overlap with the image.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void convolve(Kernel2D_I32 kernel, GrayU8 image, GrayI8 dest , int skip ) {
		checkParameters(image, dest, skip);

		if( kernel.width >= image.width ) {
			ConvolveDownNormalizedNaive.convolve(kernel,image,dest,skip);
		} else {
			ConvolveDownNoBorder.convolve(kernel,image,dest,skip,kernel.computeSum());
			ConvolveDownNormalized_JustBorder.convolve(kernel,image,dest,skip);
		}
	}

	/**
	 * Performs a horizontal 1D down convolution across the image while re-normalizing the kernel depending on its
	 * overlap with the image.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void horizontal(Kernel1D_I32 kernel, GrayS16 image, GrayI16 dest , int skip ) {
		checkParameters(image, dest, skip);

		if( kernel.width >= image.width ) {
			ConvolveDownNormalizedNaive.horizontal(kernel,image,dest,skip);
		} else {
			ConvolveDownNoBorder.horizontal(kernel,image,dest,skip,kernel.computeSum());
			ConvolveDownNormalized_JustBorder.horizontal(kernel,image,dest,skip);
		}
	}

	/**
	 * Performs a vertical 1D down convolution across the image while re-normalizing the kernel depending on its
	 * overlap with the image.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void vertical(Kernel1D_I32 kernel, GrayS16 image, GrayI16 dest , int skip ) {
		checkParameters(image, dest, skip);

		if( kernel.width >= image.width ) {
			ConvolveDownNormalizedNaive.vertical(kernel,image,dest,skip);
		} else {
			ConvolveDownNoBorder.vertical(kernel,image,dest,skip,kernel.computeSum());
			ConvolveDownNormalized_JustBorder.vertical(kernel,image,dest,skip);
		}
	}

	/**
	 * Performs a 2D down convolution across the image while re-normalizing the kernel depending on its
	 * overlap with the image.
	 *
	 * @param image	 The original image. Not modified.
	 * @param dest	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static void convolve(Kernel2D_I32 kernel, GrayS16 image, GrayI16 dest , int skip ) {
		checkParameters(image, dest, skip);

		if( kernel.width >= image.width ) {
			ConvolveDownNormalizedNaive.convolve(kernel,image,dest,skip);
		} else {
			ConvolveDownNoBorder.convolve(kernel,image,dest,skip,kernel.computeSum());
			ConvolveDownNormalized_JustBorder.convolve(kernel,image,dest,skip);
		}
	}
}
