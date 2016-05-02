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
import boofcv.alg.filter.convolve.noborder.ImplConvolveMean;
import boofcv.alg.filter.convolve.normalized.ConvolveNormalized_JustBorder;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_F64;
import boofcv.struct.convolve.Kernel1D_I32;
import boofcv.struct.image.*;


/**
 * <p>
 * Convolves a mean filter across the image.  The mean value of all the pixels are computed inside the kernel.
 * </p>
 *
 * @author Peter Abeles
 */
public class ConvolveImageMean {

	/**
	 * Performs a horizontal 1D convolution which computes the mean value of elements
	 * inside the kernel.
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 */
	public static void horizontal(GrayF32 input, GrayF32 output, int radius) {

		Kernel1D_F32 kernel = FactoryKernel.table1D_F32(radius,true);
		if( kernel.width > input.width ) {
			ConvolveNormalized.horizontal(kernel,input,output);
		} else {
			InputSanityCheck.checkSameShape(input , output);
			ConvolveNormalized_JustBorder.horizontal(kernel, input ,output );
			ImplConvolveMean.horizontal(input, output, radius);
		}
	}

	/**
	 * Performs a vertical 1D convolution which computes the mean value of elements
	 * inside the kernel.
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 */
	public static void vertical(GrayF32 input, GrayF32 output, int radius) {

		Kernel1D_F32 kernel = FactoryKernel.table1D_F32(radius,true);
		if( kernel.width > input.height ) {
			ConvolveNormalized.vertical(kernel, input, output);
		} else {
			InputSanityCheck.checkSameShape(input , output);
			ConvolveNormalized_JustBorder.vertical(kernel, input, output);
			ImplConvolveMean.vertical(input, output, radius);
		}
	}

		/**
	 * Performs a horizontal 1D convolution which computes the mean value of elements
	 * inside the kernel.
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 */
	public static void horizontal(GrayF64 input, GrayF64 output, int radius) {

		Kernel1D_F64 kernel = FactoryKernel.table1D_F64(radius,true);
		if( kernel.width > input.width ) {
			ConvolveNormalized.horizontal(kernel,input,output);
		} else {
			InputSanityCheck.checkSameShape(input , output);
			ConvolveNormalized_JustBorder.horizontal(kernel, input ,output );
			ImplConvolveMean.horizontal(input, output, radius);
		}
	}

	/**
	 * Performs a vertical 1D convolution which computes the mean value of elements
	 * inside the kernel.
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 */
	public static void vertical(GrayF64 input, GrayF64 output, int radius) {

		Kernel1D_F64 kernel = FactoryKernel.table1D_F64(radius,true);
		if( kernel.width > input.height ) {
			ConvolveNormalized.vertical(kernel, input, output);
		} else {
			InputSanityCheck.checkSameShape(input , output);
			ConvolveNormalized_JustBorder.vertical(kernel, input, output);
			ImplConvolveMean.vertical(input, output, radius);
		}
	}

	/**
	 * Performs a horizontal 1D convolution which computes the mean value of elements
	 * inside the kernel.
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 */
	public static void horizontal(GrayU8 input, GrayU8 output, int radius) {
		Kernel1D_I32 kernel = FactoryKernel.table1D_I32(radius);
		if( kernel.width > input.width ) {
			ConvolveNormalized.horizontal(kernel,input,output);
		} else {
			InputSanityCheck.checkSameShape(input , output);
			ConvolveNormalized_JustBorder.horizontal(kernel, input ,output );
			ImplConvolveMean.horizontal(input, output, radius);
		}
	}

	/**
	 * Performs a vertical 1D convolution which computes the mean value of elements
	 * inside the kernel.
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 */
	public static void vertical(GrayU8 input, GrayI8 output, int radius) {

		Kernel1D_I32 kernel = FactoryKernel.table1D_I32(radius);
		if( kernel.width > input.height ) {
			ConvolveNormalized.vertical(kernel,input,output);
		} else {
			InputSanityCheck.checkSameShape(input , output);
			ConvolveNormalized_JustBorder.vertical(kernel, input ,output );
			ImplConvolveMean.vertical(input, output, radius);
		}
	}

	/**
	 * Performs a horizontal 1D convolution which computes the mean value of elements
	 * inside the kernel.
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 */
	public static void horizontal(GrayS16 input, GrayI16 output, int radius) {
		Kernel1D_I32 kernel = FactoryKernel.table1D_I32(radius);
		if( kernel.width > input.width ) {
			ConvolveNormalized.horizontal(kernel,input,output);
		} else {
			InputSanityCheck.checkSameShape(input , output);
			ConvolveNormalized_JustBorder.horizontal(kernel, input ,output );
			ImplConvolveMean.horizontal(input, output, radius);
		}
	}

	/**
	 * Performs a vertical 1D convolution which computes the mean value of elements
	 * inside the kernel.
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 */
	public static void vertical(GrayS16 input, GrayI16 output, int radius ) {

		Kernel1D_I32 kernel = FactoryKernel.table1D_I32(radius);
		if( kernel.width > input.height ) {
			ConvolveNormalized.vertical(kernel,input,output);
		} else {
			InputSanityCheck.checkSameShape(input , output);
			ConvolveNormalized_JustBorder.vertical(kernel, input ,output );
			ImplConvolveMean.vertical(input, output, radius);
		}
	}
}
