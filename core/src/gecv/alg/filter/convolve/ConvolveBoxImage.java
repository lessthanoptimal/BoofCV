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
import gecv.alg.filter.convolve.impl.ConvolveBox_F32_F32;
import gecv.alg.filter.convolve.impl.ConvolveBox_I8_I16;
import gecv.alg.filter.convolve.impl.ConvolveBox_I8_I32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt16;
import gecv.struct.image.ImageInt32;
import gecv.struct.image.ImageInt8;

/**
 * Convolves a kernel which is composed entirely of 1's across an image.  This special kernel can be highly optimized
 * and has a computational complexity independent of the kernel size. 
 *
 * @author Peter Abeles
 */
public class ConvolveBoxImage {

	/**
	 * Performs a horizontal 1D convolution of a box kernel across the image
	 *
	 * @param input	 The original image. Not modified.
	 * @param output	 Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 * @param includeBorder Should the vertical border of the image be processed?
	 */
	public static void horizontal(ImageFloat32 input, ImageFloat32 output, int radius, boolean includeBorder) {
		InputSanityCheck.checkSameShape(input , output);

		ConvolveBox_F32_F32.horizontal(input, output, radius, includeBorder);
	}

	/**
	 * Performs a horizontal 1D convolution of a box kernel across the image
	 *
	 * @param input	 The original image. Not modified.
	 * @param output	 Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 * @param includeBorder Should the vertical border of the image be processed?
	 */
	public static void horizontal(ImageInt8 input, ImageInt16 output, int radius, boolean includeBorder) {
		InputSanityCheck.checkSameShape(input , false , output, true);

		ConvolveBox_I8_I16.horizontal(input, output, radius, includeBorder);
	}

	/**
	 * Performs a horizontal 1D convolution of a box kernel across the image
	 *
	 * @param input	 The original image. Not modified.
	 * @param output	 Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 * @param includeBorder Should the vertical border of the image be processed?
	 */
	public static void horizontal(ImageInt8 input, ImageInt32 output, int radius, boolean includeBorder) {
		InputSanityCheck.checkSameShape(input , false , output, true);

		ConvolveBox_I8_I32.horizontal(input, output, radius, includeBorder);
	}

	/**
	 * Performs a vertical 1D convolution of a box kernel across the image
	 *
	 * @param input	 The original image. Not modified.
	 * @param output	 Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 * @param includeBorder Should the horizontal border of the image be processed?
	 */
	public static void vertical(ImageFloat32 input, ImageFloat32 output, int radius, boolean includeBorder) {
		InputSanityCheck.checkSameShape(input , output );

		ConvolveBox_F32_F32.vertical(input, output, radius, includeBorder);
	}

	/**
	 * Performs a vertical 1D convolution of a box kernel across the image
	 *
	 * @param input	 The original image. Not modified.
	 * @param output	 Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 * @param includeBorder Should the horizontal border of the image be processed?
	 */
	public static void vertical(ImageInt8 input, ImageInt16 output, int radius, boolean includeBorder) {
		InputSanityCheck.checkSameShape(input , false , output, true);
		
		ConvolveBox_I8_I16.vertical(input, output, radius, includeBorder);
	}

	/**
	 * Performs a vertical 1D convolution of a box kernel across the image
	 *
	 * @param input	 The original image. Not modified.
	 * @param output	 Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 * @param includeBorder Should the horizontal border of the image be processed?
	 */
	public static void vertical(ImageInt8 input, ImageInt32 output, int radius, boolean includeBorder) {
		InputSanityCheck.checkSameShape(input , false , output, true);
		
		ConvolveBox_I8_I32.vertical(input, output, radius, includeBorder);
	}
}
