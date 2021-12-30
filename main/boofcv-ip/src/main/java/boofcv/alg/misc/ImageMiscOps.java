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

package boofcv.alg.misc;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.misc.impl.ImplImageMiscOps;
import boofcv.alg.misc.impl.ImplImageMiscOps_MT;
import boofcv.concurrency.BoofConcurrency;
import boofcv.misc.BoofLambdas;
import boofcv.struct.border.ImageBorder_F32;
import boofcv.struct.border.ImageBorder_F64;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.border.ImageBorder_S64;
import boofcv.struct.image.*;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Generated;
import java.util.Random;

/**
 * Basic image operations which have no place better to go.
 *
 * <p>DO NOT MODIFY. Automatically generated code created by GenerateImageMiscOps</p>
 *
 * @author Peter Abeles
 */
@Generated("boofcv.alg.misc.GenerateImageMiscOps")
public class ImageMiscOps {
	/**
	 * If the image has fewer than this elements do not run the concurrent version of the function since it could
	 * run slower
	 */
	public static int MIN_ELEMENTS_CONCURRENT = 400*400;
	
	public static boolean runConcurrent( ImageBase image ) {
		return runConcurrent(image.width*image.height);
	}
	public static boolean runConcurrent( int numElements ) {
		return BoofConcurrency.isUseConcurrent() && (numElements >= MIN_ELEMENTS_CONCURRENT);
	}

	/**
	 * Copies a rectangular region from one image into another. The region can go outside the input image's border.<br>
	 * output[dstX:(dstX+width) , dstY:(dstY+height-1)] = input[srcX:(srcX+width) , srcY:(srcY+height-1)]
	 *
	 * @param srcX x-coordinate of corner in input image
	 * @param srcY y-coordinate of corner in input image
	 * @param dstX x-coordinate of corner in output image
	 * @param dstY y-coordinate of corner in output image
	 * @param width Width of region to be copied
	 * @param height Height of region to be copied
	 * @param input Input image
	 * @param border Border for input image
	 * @param output output image
	 */
	public static < T extends GrayI8<T>> void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 T input, ImageBorder_S32<T> border, GrayI8 output ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent((dstY-srcY)*(dstX-srcX))) {
//			ImplImageMiscOps_MT.copy(srcX, srcY, dstX, dstY, width, height, input, border, output);
//		} else {
		ImplImageMiscOps.copy(srcX, srcY, dstX, dstY, width, height, input, border, output);
//		}
	}

	/**
	 * Copies a rectangular region from one image into another.<br>
	 * output[dstX:(dstX+width) , dstY:(dstY+height-1)] = input[srcX:(srcX+width) , srcY:(srcY+height-1)]
	 *
	 * @param srcX x-coordinate of corner in input image
	 * @param srcY y-coordinate of corner in input image
	 * @param dstX x-coordinate of corner in output image
	 * @param dstY y-coordinate of corner in output image
	 * @param width Width of region to be copied
	 * @param height Height of region to be copied
	 * @param input Input image
	 * @param output output image
	 */
	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 GrayI8 input, GrayI8 output ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent((dstY-srcY)*(dstX-srcX))) {
//			ImplImageMiscOps_MT.copy(srcX, srcY, dstX, dstY, width, height, input, output);
//		} else {
		ImplImageMiscOps.copy(srcX, srcY, dstX, dstY, width, height, input, output);
//		}
	}

	/**
	 * Copies a rectangular region from one image into another.<br>
	 * output[dstX:(dstX+width) , dstY:(dstY+height-1)] = input[srcX:(srcX+width) , srcY:(srcY+height-1)]
	 *
	 * @param srcX x-coordinate of corner in input image
	 * @param srcY y-coordinate of corner in input image
	 * @param dstX x-coordinate of corner in output image
	 * @param dstY y-coordinate of corner in output image
	 * @param width Width of region to be copied
	 * @param height Height of region to be copied
	 * @param input Input image
	 * @param output output image
	 */
	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 InterleavedI8 input, InterleavedI8 output ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent((dstY-srcY)*(dstX-srcX))) {
//			ImplImageMiscOps_MT.copy(srcX, srcY, dstX, dstY, width, height, input, output);
//		} else {
		ImplImageMiscOps.copy(srcX, srcY, dstX, dstY, width, height, input, output);
//		}
	}

	/**
	 * Fills the whole image with the specified value
	 *
	 * @param image An image. Modified.
	 * @param value The value that the image is being filled with.
	 */
	public static void fill( GrayI8 image, int value ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fill(image, value);
//		} else {
		ImplImageMiscOps.fill(image, value);
//		}
	}

	/**
	 * Fills the whole image with the specified value
	 *
	 * @param image An image. Modified.
	 * @param value The value that the image is being filled with.
	 */
	public static void fill( InterleavedI8 image, int value ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.fill(image, value);
		} else {
			ImplImageMiscOps.fill(image, value);
		}
	}

	/**
	 * Fills each band in the image with the specified values
	 *
	 * @param image An image. Modified.
	 * @param values Array which contains the values each band is to be filled with.
	 */
	public static void fill( InterleavedI8 image, int[] values ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.fill(image, values);
		} else {
			ImplImageMiscOps.fill(image, values);
		}
	}

	/**
	 * Fills one band in the image with the specified value
	 *
	 * @param image An image. Modified.
	 * @param band Which band is to be filled with the specified value
	 * @param value The value that the image is being filled with.
	 */
	public static void fillBand( InterleavedI8 image, int band, int value ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.fillBand(image, band, value);
		} else {
			ImplImageMiscOps.fillBand(image, band, value);
		}
	}

	/**
	 * Inserts a single band into a multi-band image overwriting the original band
	 *
	 * @param input Single band image
	 * @param band Which band the image is to be inserted into
	 * @param output The multi-band image which the input image is to be inserted into
	 */
	public static void insertBand( GrayI8 input, int band, InterleavedI8 output) {
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.insertBand(input, band, output);
		} else {
			ImplImageMiscOps.insertBand(input, band, output);
		}
	}

	/**
	 * Extracts a single band from a multi-band image
	 *
	 * @param input Multi-band image.
	 * @param band which bad is to be extracted
	 * @param output The single band image. Modified.
	 */
	public static void extractBand( InterleavedI8 input, int band, GrayI8 output) {
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.extractBand(input, band, output);
		} else {
			ImplImageMiscOps.extractBand(input, band, output);
		}
	}

	/**
	 * Fills the outside border with the specified value
	 *
	 * @param image An image. Modified.
	 * @param value The value that the image is being filled with.
	 * @param radius Border width.
	 */
	public static void fillBorder( GrayI8 image, int value, int radius ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fillBorder(image, value, radius);
//		} else {
		ImplImageMiscOps.fillBorder(image, value, radius);
//		}
	}

	/**
	 * Fills the border with independent border widths for each side
	 *
	 * @param image An image.
	 * @param value The value that the image is being filled with.
	 * @param borderX0 Width of border on left
	 * @param borderY0 Width of border on top
	 * @param borderX1 Width of border on right
	 * @param borderY1 Width of border on bottom
	 */
	public static void fillBorder( GrayI8 image, int value, int borderX0, int borderY0, int borderX1, int borderY1 ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fillBorder(image, value, borderX0, borderY0, borderX1, borderY1);
//		} else {
		ImplImageMiscOps.fillBorder(image, value, borderX0, borderY0, borderX1, borderY1);
//		}
	}

	/**
	 * Draws a filled rectangle that is aligned along the image axis inside the image.
	 *
	 * @param image The image the rectangle is drawn in. Modified
	 * @param value Value of the rectangle
	 * @param x0 Top left x-coordinate
	 * @param y0 Top left y-coordinate
	 * @param width Rectangle width
	 * @param height Rectangle height
	 */
	public static void fillRectangle( GrayI8 image, int value, int x0, int y0, int width, int height ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fillRectangle(image, value, x0, y0, width, height);
//		} else {
		ImplImageMiscOps.fillRectangle(image, value, x0, y0, width, height);
//		}
	}

	/**
	 * Draws a filled rectangle that is aligned along the image axis inside the image. All bands
	 * are filled with the same value.
	 *
	 * @param image The image the rectangle is drawn in. Modified
	 * @param value Value of the rectangle
	 * @param x0 Top left x-coordinate
	 * @param y0 Top left y-coordinate
	 * @param width Rectangle width
	 * @param height Rectangle height
	 */
	public static void fillRectangle( InterleavedI8 image, int value, int x0, int y0, int width, int height ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fillRectangle(image, value, x0, y0, width, height);
//		} else {
		ImplImageMiscOps.fillRectangle(image, value, x0, y0, width, height);
//		}
	}	/**
	 * Sets each value in the image to a value drawn from an uniform distribution that has a range of min &le; X &lt; max.
	 *
	 * @param img Image which is to be filled. Modified.
	 * @param rand Random number generator
	 * @param min Minimum value of the distribution, inclusive
	 * @param max Maximum value of the distribution, exclusive
	 */
	public static void fillUniform( GrayI8 img, Random rand, int min, int max ) {
		ImplImageMiscOps.fillUniform(img, rand, min, max);
	}

	/**
	 * Sets each value in the image to a value drawn from an uniform distribution that has a range of min &le; X &lt; max.
	 *
	 * @param img Image which is to be filled. Modified.
	 * @param rand Random number generator
	 * @param min Minimum value of the distribution, inclusive
	 * @param max Maximum value of the distribution, exclusive
	 */
	public static void fillUniform( InterleavedI8 img, Random rand, int min, int max ) {
		ImplImageMiscOps.fillUniform(img, rand, min, max);
	}

	/**
	 * Sets each value in the image to a value drawn from a Gaussian distribution. A user
	 * specified lower and upper bound is provided to ensure that the values are within a legal
	 * range. A drawn value outside the allowed range will be set to the closest bound.
	 * 
	 * @param input Input image. Modified.
	 * @param rand Random number generator
	 * @param mean Distribution's mean.
	 * @param sigma Distribution's standard deviation.
	 * @param lowerBound Lower bound of value clip
	 * @param upperBound Upper bound of value clip
	 */
	public static void fillGaussian( GrayI8 input, Random rand, double mean, double sigma, int lowerBound, int upperBound ) {
		ImplImageMiscOps.fillGaussian(input, rand, mean, sigma, lowerBound, upperBound);
	}

	/**
	 * Sets each value in the image to a value drawn from a Gaussian distribution. A user
	 * specified lower and upper bound is provided to ensure that the values are within a legal
	 * range. A drawn value outside the allowed range will be set to the closest bound.
	 * 
	 * @param input Input image. Modified.
	 * @param rand Random number generator
	 * @param mean Distribution's mean.
	 * @param sigma Distribution's standard deviation.
	 * @param lowerBound Lower bound of value clip
	 * @param upperBound Upper bound of value clip
	 */
	public static void fillGaussian( InterleavedI8 input, Random rand, double mean, double sigma, int lowerBound, int upperBound ) {
		ImplImageMiscOps.fillGaussian(input, rand, mean, sigma, lowerBound, upperBound);
	}

	/** Flips the image from top to bottom */
	public static void flipVertical( GrayI8 image ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.flipVertical(image);
		} else {
			ImplImageMiscOps.flipVertical(image);
		}
	}

	/** Flips the image from left to right */
	public static void flipHorizontal( GrayI8 image ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.flipHorizontal(image);
		} else {
			ImplImageMiscOps.flipHorizontal(image);
		}
	}

	/** Transposes the image */
	public static <T extends GrayI8<T>> T transpose( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		//if (runConcurrent(input)) {
		//	ImplImageMiscOps_MT.transpose(input, output);
		//} else {
		ImplImageMiscOps.transpose(input, output);
		//}
		return output;
	}

	/** In-place 90 degree image rotation in the clockwise direction. Only works on square images. */
	public static void rotateCW( GrayI8 image ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.rotateCW(image);
		} else {
			ImplImageMiscOps.rotateCW(image);
		}
	}

	/** Transposes the image */
	public static <T extends InterleavedI8<T>> T transpose( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.transpose(input, output);
		} else {
			ImplImageMiscOps.transpose(input, output);
		}
		return output;
	}

	/** Rotates the image 90 degrees in the clockwise direction. */
	public static <T extends GrayI8<T>> T rotateCW( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.rotateCW(input, output);
		} else {
			ImplImageMiscOps.rotateCW(input, output);
		}
		return output;
	}

	/** Rotates the image 90 degrees in the clockwise direction. */
	public static <T extends InterleavedI8<T>> T rotateCW( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.rotateCW(input, output);
		} else {
			ImplImageMiscOps.rotateCW(input, output);
		}
		return output;
	}

	/** In-place 90 degree image rotation in the counter-clockwise direction. Only works on square images. */
	public static void rotateCCW( GrayI8 image ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.rotateCCW(image);
		} else {
			ImplImageMiscOps.rotateCCW(image);
		}
	}

	/** Rotates the image 90 degrees in the counter-clockwise direction. */
	public static <T extends GrayI8<T>> T rotateCCW( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.rotateCCW(input, output);
		} else {
			ImplImageMiscOps.rotateCCW(input, output);
		}
		return output;
	}

	/** Rotates the image 90 degrees in the counter-clockwise direction. */
	public static <T extends InterleavedI8<T>> T rotateCCW( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.rotateCCW(input, output);
		} else {
			ImplImageMiscOps.rotateCCW(input, output);
		}
		return output;
	}

	/**
	 * Creates a new image which is a copy of the src image but extended with border pixels.
	 * 
	 * @param src (Input) source image
	 * @param border (Input) image border generator
	 * @param borderX0 (Input) Border x-axis lower extent
	 * @param borderY0 (Input) Border y-axis lower extent
	 * @param borderX1 (Input) Border x-axis upper extent
	 * @param borderY1 (Input) Border y-axis upper extent
	 * @param dst (Output) Output image. width=src.width+2*radiusX and height=src.height+2*radiusY
	 */
	public static <T extends GrayI8<T>>
	void growBorder( T src, ImageBorder_S32<T> border, int borderX0, int borderY0, int borderX1, int borderY1, T dst ) {
		if (runConcurrent(src)) {
			ImplImageMiscOps_MT.growBorder(src, border, borderX0, borderY0, borderX1, borderY1, dst);
		} else {
			ImplImageMiscOps.growBorder(src, border, borderX0, borderY0, borderX1, borderY1, dst);
		}
	}

	/**
	 * Using the provided functions, finds all pixel values which match then calls the process function
	 *
	 * @param input (Input) Image
	 * @param finder (Input) Checks to see if the pixel value matches the criteria
	 * @param process (Input) When a match is found this function is called and given the coordinates. true = continue
	 */
	public static void findAndProcess( GrayI8 input, BoofLambdas.Match_I8 finder, BoofLambdas.ProcessIIB process ) {
		ImplImageMiscOps.findAndProcess(input, finder, process);
	}

	/**
	 * Copies a rectangular region from one image into another. The region can go outside the input image's border.<br>
	 * output[dstX:(dstX+width) , dstY:(dstY+height-1)] = input[srcX:(srcX+width) , srcY:(srcY+height-1)]
	 *
	 * @param srcX x-coordinate of corner in input image
	 * @param srcY y-coordinate of corner in input image
	 * @param dstX x-coordinate of corner in output image
	 * @param dstY y-coordinate of corner in output image
	 * @param width Width of region to be copied
	 * @param height Height of region to be copied
	 * @param input Input image
	 * @param border Border for input image
	 * @param output output image
	 */
	public static < T extends GrayI16<T>> void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 T input, ImageBorder_S32<T> border, GrayI16 output ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent((dstY-srcY)*(dstX-srcX))) {
//			ImplImageMiscOps_MT.copy(srcX, srcY, dstX, dstY, width, height, input, border, output);
//		} else {
		ImplImageMiscOps.copy(srcX, srcY, dstX, dstY, width, height, input, border, output);
//		}
	}

	/**
	 * Copies a rectangular region from one image into another.<br>
	 * output[dstX:(dstX+width) , dstY:(dstY+height-1)] = input[srcX:(srcX+width) , srcY:(srcY+height-1)]
	 *
	 * @param srcX x-coordinate of corner in input image
	 * @param srcY y-coordinate of corner in input image
	 * @param dstX x-coordinate of corner in output image
	 * @param dstY y-coordinate of corner in output image
	 * @param width Width of region to be copied
	 * @param height Height of region to be copied
	 * @param input Input image
	 * @param output output image
	 */
	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 GrayI16 input, GrayI16 output ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent((dstY-srcY)*(dstX-srcX))) {
//			ImplImageMiscOps_MT.copy(srcX, srcY, dstX, dstY, width, height, input, output);
//		} else {
		ImplImageMiscOps.copy(srcX, srcY, dstX, dstY, width, height, input, output);
//		}
	}

	/**
	 * Copies a rectangular region from one image into another.<br>
	 * output[dstX:(dstX+width) , dstY:(dstY+height-1)] = input[srcX:(srcX+width) , srcY:(srcY+height-1)]
	 *
	 * @param srcX x-coordinate of corner in input image
	 * @param srcY y-coordinate of corner in input image
	 * @param dstX x-coordinate of corner in output image
	 * @param dstY y-coordinate of corner in output image
	 * @param width Width of region to be copied
	 * @param height Height of region to be copied
	 * @param input Input image
	 * @param output output image
	 */
	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 InterleavedI16 input, InterleavedI16 output ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent((dstY-srcY)*(dstX-srcX))) {
//			ImplImageMiscOps_MT.copy(srcX, srcY, dstX, dstY, width, height, input, output);
//		} else {
		ImplImageMiscOps.copy(srcX, srcY, dstX, dstY, width, height, input, output);
//		}
	}

	/**
	 * Fills the whole image with the specified value
	 *
	 * @param image An image. Modified.
	 * @param value The value that the image is being filled with.
	 */
	public static void fill( GrayI16 image, int value ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fill(image, value);
//		} else {
		ImplImageMiscOps.fill(image, value);
//		}
	}

	/**
	 * Fills the whole image with the specified value
	 *
	 * @param image An image. Modified.
	 * @param value The value that the image is being filled with.
	 */
	public static void fill( InterleavedI16 image, int value ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.fill(image, value);
		} else {
			ImplImageMiscOps.fill(image, value);
		}
	}

	/**
	 * Fills each band in the image with the specified values
	 *
	 * @param image An image. Modified.
	 * @param values Array which contains the values each band is to be filled with.
	 */
	public static void fill( InterleavedI16 image, int[] values ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.fill(image, values);
		} else {
			ImplImageMiscOps.fill(image, values);
		}
	}

	/**
	 * Fills one band in the image with the specified value
	 *
	 * @param image An image. Modified.
	 * @param band Which band is to be filled with the specified value
	 * @param value The value that the image is being filled with.
	 */
	public static void fillBand( InterleavedI16 image, int band, int value ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.fillBand(image, band, value);
		} else {
			ImplImageMiscOps.fillBand(image, band, value);
		}
	}

	/**
	 * Inserts a single band into a multi-band image overwriting the original band
	 *
	 * @param input Single band image
	 * @param band Which band the image is to be inserted into
	 * @param output The multi-band image which the input image is to be inserted into
	 */
	public static void insertBand( GrayI16 input, int band, InterleavedI16 output) {
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.insertBand(input, band, output);
		} else {
			ImplImageMiscOps.insertBand(input, band, output);
		}
	}

	/**
	 * Extracts a single band from a multi-band image
	 *
	 * @param input Multi-band image.
	 * @param band which bad is to be extracted
	 * @param output The single band image. Modified.
	 */
	public static void extractBand( InterleavedI16 input, int band, GrayI16 output) {
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.extractBand(input, band, output);
		} else {
			ImplImageMiscOps.extractBand(input, band, output);
		}
	}

	/**
	 * Fills the outside border with the specified value
	 *
	 * @param image An image. Modified.
	 * @param value The value that the image is being filled with.
	 * @param radius Border width.
	 */
	public static void fillBorder( GrayI16 image, int value, int radius ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fillBorder(image, value, radius);
//		} else {
		ImplImageMiscOps.fillBorder(image, value, radius);
//		}
	}

	/**
	 * Fills the border with independent border widths for each side
	 *
	 * @param image An image.
	 * @param value The value that the image is being filled with.
	 * @param borderX0 Width of border on left
	 * @param borderY0 Width of border on top
	 * @param borderX1 Width of border on right
	 * @param borderY1 Width of border on bottom
	 */
	public static void fillBorder( GrayI16 image, int value, int borderX0, int borderY0, int borderX1, int borderY1 ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fillBorder(image, value, borderX0, borderY0, borderX1, borderY1);
//		} else {
		ImplImageMiscOps.fillBorder(image, value, borderX0, borderY0, borderX1, borderY1);
//		}
	}

	/**
	 * Draws a filled rectangle that is aligned along the image axis inside the image.
	 *
	 * @param image The image the rectangle is drawn in. Modified
	 * @param value Value of the rectangle
	 * @param x0 Top left x-coordinate
	 * @param y0 Top left y-coordinate
	 * @param width Rectangle width
	 * @param height Rectangle height
	 */
	public static void fillRectangle( GrayI16 image, int value, int x0, int y0, int width, int height ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fillRectangle(image, value, x0, y0, width, height);
//		} else {
		ImplImageMiscOps.fillRectangle(image, value, x0, y0, width, height);
//		}
	}

	/**
	 * Draws a filled rectangle that is aligned along the image axis inside the image. All bands
	 * are filled with the same value.
	 *
	 * @param image The image the rectangle is drawn in. Modified
	 * @param value Value of the rectangle
	 * @param x0 Top left x-coordinate
	 * @param y0 Top left y-coordinate
	 * @param width Rectangle width
	 * @param height Rectangle height
	 */
	public static void fillRectangle( InterleavedI16 image, int value, int x0, int y0, int width, int height ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fillRectangle(image, value, x0, y0, width, height);
//		} else {
		ImplImageMiscOps.fillRectangle(image, value, x0, y0, width, height);
//		}
	}	/**
	 * Sets each value in the image to a value drawn from an uniform distribution that has a range of min &le; X &lt; max.
	 *
	 * @param img Image which is to be filled. Modified.
	 * @param rand Random number generator
	 * @param min Minimum value of the distribution, inclusive
	 * @param max Maximum value of the distribution, exclusive
	 */
	public static void fillUniform( GrayI16 img, Random rand, int min, int max ) {
		ImplImageMiscOps.fillUniform(img, rand, min, max);
	}

	/**
	 * Sets each value in the image to a value drawn from an uniform distribution that has a range of min &le; X &lt; max.
	 *
	 * @param img Image which is to be filled. Modified.
	 * @param rand Random number generator
	 * @param min Minimum value of the distribution, inclusive
	 * @param max Maximum value of the distribution, exclusive
	 */
	public static void fillUniform( InterleavedI16 img, Random rand, int min, int max ) {
		ImplImageMiscOps.fillUniform(img, rand, min, max);
	}

	/**
	 * Sets each value in the image to a value drawn from a Gaussian distribution. A user
	 * specified lower and upper bound is provided to ensure that the values are within a legal
	 * range. A drawn value outside the allowed range will be set to the closest bound.
	 * 
	 * @param input Input image. Modified.
	 * @param rand Random number generator
	 * @param mean Distribution's mean.
	 * @param sigma Distribution's standard deviation.
	 * @param lowerBound Lower bound of value clip
	 * @param upperBound Upper bound of value clip
	 */
	public static void fillGaussian( GrayI16 input, Random rand, double mean, double sigma, int lowerBound, int upperBound ) {
		ImplImageMiscOps.fillGaussian(input, rand, mean, sigma, lowerBound, upperBound);
	}

	/**
	 * Sets each value in the image to a value drawn from a Gaussian distribution. A user
	 * specified lower and upper bound is provided to ensure that the values are within a legal
	 * range. A drawn value outside the allowed range will be set to the closest bound.
	 * 
	 * @param input Input image. Modified.
	 * @param rand Random number generator
	 * @param mean Distribution's mean.
	 * @param sigma Distribution's standard deviation.
	 * @param lowerBound Lower bound of value clip
	 * @param upperBound Upper bound of value clip
	 */
	public static void fillGaussian( InterleavedI16 input, Random rand, double mean, double sigma, int lowerBound, int upperBound ) {
		ImplImageMiscOps.fillGaussian(input, rand, mean, sigma, lowerBound, upperBound);
	}

	/** Flips the image from top to bottom */
	public static void flipVertical( GrayI16 image ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.flipVertical(image);
		} else {
			ImplImageMiscOps.flipVertical(image);
		}
	}

	/** Flips the image from left to right */
	public static void flipHorizontal( GrayI16 image ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.flipHorizontal(image);
		} else {
			ImplImageMiscOps.flipHorizontal(image);
		}
	}

	/** Transposes the image */
	public static <T extends GrayI16<T>> T transpose( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		//if (runConcurrent(input)) {
		//	ImplImageMiscOps_MT.transpose(input, output);
		//} else {
		ImplImageMiscOps.transpose(input, output);
		//}
		return output;
	}

	/** In-place 90 degree image rotation in the clockwise direction. Only works on square images. */
	public static void rotateCW( GrayI16 image ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.rotateCW(image);
		} else {
			ImplImageMiscOps.rotateCW(image);
		}
	}

	/** Transposes the image */
	public static <T extends InterleavedI16<T>> T transpose( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.transpose(input, output);
		} else {
			ImplImageMiscOps.transpose(input, output);
		}
		return output;
	}

	/** Rotates the image 90 degrees in the clockwise direction. */
	public static <T extends GrayI16<T>> T rotateCW( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.rotateCW(input, output);
		} else {
			ImplImageMiscOps.rotateCW(input, output);
		}
		return output;
	}

	/** Rotates the image 90 degrees in the clockwise direction. */
	public static <T extends InterleavedI16<T>> T rotateCW( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.rotateCW(input, output);
		} else {
			ImplImageMiscOps.rotateCW(input, output);
		}
		return output;
	}

	/** In-place 90 degree image rotation in the counter-clockwise direction. Only works on square images. */
	public static void rotateCCW( GrayI16 image ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.rotateCCW(image);
		} else {
			ImplImageMiscOps.rotateCCW(image);
		}
	}

	/** Rotates the image 90 degrees in the counter-clockwise direction. */
	public static <T extends GrayI16<T>> T rotateCCW( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.rotateCCW(input, output);
		} else {
			ImplImageMiscOps.rotateCCW(input, output);
		}
		return output;
	}

	/** Rotates the image 90 degrees in the counter-clockwise direction. */
	public static <T extends InterleavedI16<T>> T rotateCCW( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.rotateCCW(input, output);
		} else {
			ImplImageMiscOps.rotateCCW(input, output);
		}
		return output;
	}

	/**
	 * Creates a new image which is a copy of the src image but extended with border pixels.
	 * 
	 * @param src (Input) source image
	 * @param border (Input) image border generator
	 * @param borderX0 (Input) Border x-axis lower extent
	 * @param borderY0 (Input) Border y-axis lower extent
	 * @param borderX1 (Input) Border x-axis upper extent
	 * @param borderY1 (Input) Border y-axis upper extent
	 * @param dst (Output) Output image. width=src.width+2*radiusX and height=src.height+2*radiusY
	 */
	public static <T extends GrayI16<T>>
	void growBorder( T src, ImageBorder_S32<T> border, int borderX0, int borderY0, int borderX1, int borderY1, T dst ) {
		if (runConcurrent(src)) {
			ImplImageMiscOps_MT.growBorder(src, border, borderX0, borderY0, borderX1, borderY1, dst);
		} else {
			ImplImageMiscOps.growBorder(src, border, borderX0, borderY0, borderX1, borderY1, dst);
		}
	}

	/**
	 * Using the provided functions, finds all pixel values which match then calls the process function
	 *
	 * @param input (Input) Image
	 * @param finder (Input) Checks to see if the pixel value matches the criteria
	 * @param process (Input) When a match is found this function is called and given the coordinates. true = continue
	 */
	public static void findAndProcess( GrayI16 input, BoofLambdas.Match_I16 finder, BoofLambdas.ProcessIIB process ) {
		ImplImageMiscOps.findAndProcess(input, finder, process);
	}

	/**
	 * Copies a rectangular region from one image into another. The region can go outside the input image's border.<br>
	 * output[dstX:(dstX+width) , dstY:(dstY+height-1)] = input[srcX:(srcX+width) , srcY:(srcY+height-1)]
	 *
	 * @param srcX x-coordinate of corner in input image
	 * @param srcY y-coordinate of corner in input image
	 * @param dstX x-coordinate of corner in output image
	 * @param dstY y-coordinate of corner in output image
	 * @param width Width of region to be copied
	 * @param height Height of region to be copied
	 * @param input Input image
	 * @param border Border for input image
	 * @param output output image
	 */
	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 GrayS32 input, ImageBorder_S32 border, GrayS32 output ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent((dstY-srcY)*(dstX-srcX))) {
//			ImplImageMiscOps_MT.copy(srcX, srcY, dstX, dstY, width, height, input, border, output);
//		} else {
		ImplImageMiscOps.copy(srcX, srcY, dstX, dstY, width, height, input, border, output);
//		}
	}

	/**
	 * Copies a rectangular region from one image into another.<br>
	 * output[dstX:(dstX+width) , dstY:(dstY+height-1)] = input[srcX:(srcX+width) , srcY:(srcY+height-1)]
	 *
	 * @param srcX x-coordinate of corner in input image
	 * @param srcY y-coordinate of corner in input image
	 * @param dstX x-coordinate of corner in output image
	 * @param dstY y-coordinate of corner in output image
	 * @param width Width of region to be copied
	 * @param height Height of region to be copied
	 * @param input Input image
	 * @param output output image
	 */
	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 GrayS32 input, GrayS32 output ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent((dstY-srcY)*(dstX-srcX))) {
//			ImplImageMiscOps_MT.copy(srcX, srcY, dstX, dstY, width, height, input, output);
//		} else {
		ImplImageMiscOps.copy(srcX, srcY, dstX, dstY, width, height, input, output);
//		}
	}

	/**
	 * Copies a rectangular region from one image into another.<br>
	 * output[dstX:(dstX+width) , dstY:(dstY+height-1)] = input[srcX:(srcX+width) , srcY:(srcY+height-1)]
	 *
	 * @param srcX x-coordinate of corner in input image
	 * @param srcY y-coordinate of corner in input image
	 * @param dstX x-coordinate of corner in output image
	 * @param dstY y-coordinate of corner in output image
	 * @param width Width of region to be copied
	 * @param height Height of region to be copied
	 * @param input Input image
	 * @param output output image
	 */
	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 InterleavedS32 input, InterleavedS32 output ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent((dstY-srcY)*(dstX-srcX))) {
//			ImplImageMiscOps_MT.copy(srcX, srcY, dstX, dstY, width, height, input, output);
//		} else {
		ImplImageMiscOps.copy(srcX, srcY, dstX, dstY, width, height, input, output);
//		}
	}

	/**
	 * Fills the whole image with the specified value
	 *
	 * @param image An image. Modified.
	 * @param value The value that the image is being filled with.
	 */
	public static void fill( GrayS32 image, int value ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fill(image, value);
//		} else {
		ImplImageMiscOps.fill(image, value);
//		}
	}

	/**
	 * Fills the whole image with the specified value
	 *
	 * @param image An image. Modified.
	 * @param value The value that the image is being filled with.
	 */
	public static void fill( InterleavedS32 image, int value ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.fill(image, value);
		} else {
			ImplImageMiscOps.fill(image, value);
		}
	}

	/**
	 * Fills each band in the image with the specified values
	 *
	 * @param image An image. Modified.
	 * @param values Array which contains the values each band is to be filled with.
	 */
	public static void fill( InterleavedS32 image, int[] values ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.fill(image, values);
		} else {
			ImplImageMiscOps.fill(image, values);
		}
	}

	/**
	 * Fills one band in the image with the specified value
	 *
	 * @param image An image. Modified.
	 * @param band Which band is to be filled with the specified value
	 * @param value The value that the image is being filled with.
	 */
	public static void fillBand( InterleavedS32 image, int band, int value ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.fillBand(image, band, value);
		} else {
			ImplImageMiscOps.fillBand(image, band, value);
		}
	}

	/**
	 * Inserts a single band into a multi-band image overwriting the original band
	 *
	 * @param input Single band image
	 * @param band Which band the image is to be inserted into
	 * @param output The multi-band image which the input image is to be inserted into
	 */
	public static void insertBand( GrayS32 input, int band, InterleavedS32 output) {
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.insertBand(input, band, output);
		} else {
			ImplImageMiscOps.insertBand(input, band, output);
		}
	}

	/**
	 * Extracts a single band from a multi-band image
	 *
	 * @param input Multi-band image.
	 * @param band which bad is to be extracted
	 * @param output The single band image. Modified.
	 */
	public static void extractBand( InterleavedS32 input, int band, GrayS32 output) {
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.extractBand(input, band, output);
		} else {
			ImplImageMiscOps.extractBand(input, band, output);
		}
	}

	/**
	 * Fills the outside border with the specified value
	 *
	 * @param image An image. Modified.
	 * @param value The value that the image is being filled with.
	 * @param radius Border width.
	 */
	public static void fillBorder( GrayS32 image, int value, int radius ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fillBorder(image, value, radius);
//		} else {
		ImplImageMiscOps.fillBorder(image, value, radius);
//		}
	}

	/**
	 * Fills the border with independent border widths for each side
	 *
	 * @param image An image.
	 * @param value The value that the image is being filled with.
	 * @param borderX0 Width of border on left
	 * @param borderY0 Width of border on top
	 * @param borderX1 Width of border on right
	 * @param borderY1 Width of border on bottom
	 */
	public static void fillBorder( GrayS32 image, int value, int borderX0, int borderY0, int borderX1, int borderY1 ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fillBorder(image, value, borderX0, borderY0, borderX1, borderY1);
//		} else {
		ImplImageMiscOps.fillBorder(image, value, borderX0, borderY0, borderX1, borderY1);
//		}
	}

	/**
	 * Draws a filled rectangle that is aligned along the image axis inside the image.
	 *
	 * @param image The image the rectangle is drawn in. Modified
	 * @param value Value of the rectangle
	 * @param x0 Top left x-coordinate
	 * @param y0 Top left y-coordinate
	 * @param width Rectangle width
	 * @param height Rectangle height
	 */
	public static void fillRectangle( GrayS32 image, int value, int x0, int y0, int width, int height ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fillRectangle(image, value, x0, y0, width, height);
//		} else {
		ImplImageMiscOps.fillRectangle(image, value, x0, y0, width, height);
//		}
	}

	/**
	 * Draws a filled rectangle that is aligned along the image axis inside the image. All bands
	 * are filled with the same value.
	 *
	 * @param image The image the rectangle is drawn in. Modified
	 * @param value Value of the rectangle
	 * @param x0 Top left x-coordinate
	 * @param y0 Top left y-coordinate
	 * @param width Rectangle width
	 * @param height Rectangle height
	 */
	public static void fillRectangle( InterleavedS32 image, int value, int x0, int y0, int width, int height ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fillRectangle(image, value, x0, y0, width, height);
//		} else {
		ImplImageMiscOps.fillRectangle(image, value, x0, y0, width, height);
//		}
	}	/**
	 * Sets each value in the image to a value drawn from an uniform distribution that has a range of min &le; X &lt; max.
	 *
	 * @param img Image which is to be filled. Modified.
	 * @param rand Random number generator
	 * @param min Minimum value of the distribution, inclusive
	 * @param max Maximum value of the distribution, exclusive
	 */
	public static void fillUniform( GrayS32 img, Random rand, int min, int max ) {
		ImplImageMiscOps.fillUniform(img, rand, min, max);
	}

	/**
	 * Sets each value in the image to a value drawn from an uniform distribution that has a range of min &le; X &lt; max.
	 *
	 * @param img Image which is to be filled. Modified.
	 * @param rand Random number generator
	 * @param min Minimum value of the distribution, inclusive
	 * @param max Maximum value of the distribution, exclusive
	 */
	public static void fillUniform( InterleavedS32 img, Random rand, int min, int max ) {
		ImplImageMiscOps.fillUniform(img, rand, min, max);
	}

	/**
	 * Sets each value in the image to a value drawn from a Gaussian distribution. A user
	 * specified lower and upper bound is provided to ensure that the values are within a legal
	 * range. A drawn value outside the allowed range will be set to the closest bound.
	 * 
	 * @param input Input image. Modified.
	 * @param rand Random number generator
	 * @param mean Distribution's mean.
	 * @param sigma Distribution's standard deviation.
	 * @param lowerBound Lower bound of value clip
	 * @param upperBound Upper bound of value clip
	 */
	public static void fillGaussian( GrayS32 input, Random rand, double mean, double sigma, int lowerBound, int upperBound ) {
		ImplImageMiscOps.fillGaussian(input, rand, mean, sigma, lowerBound, upperBound);
	}

	/**
	 * Sets each value in the image to a value drawn from a Gaussian distribution. A user
	 * specified lower and upper bound is provided to ensure that the values are within a legal
	 * range. A drawn value outside the allowed range will be set to the closest bound.
	 * 
	 * @param input Input image. Modified.
	 * @param rand Random number generator
	 * @param mean Distribution's mean.
	 * @param sigma Distribution's standard deviation.
	 * @param lowerBound Lower bound of value clip
	 * @param upperBound Upper bound of value clip
	 */
	public static void fillGaussian( InterleavedS32 input, Random rand, double mean, double sigma, int lowerBound, int upperBound ) {
		ImplImageMiscOps.fillGaussian(input, rand, mean, sigma, lowerBound, upperBound);
	}

	/** Flips the image from top to bottom */
	public static void flipVertical( GrayS32 image ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.flipVertical(image);
		} else {
			ImplImageMiscOps.flipVertical(image);
		}
	}

	/** Flips the image from left to right */
	public static void flipHorizontal( GrayS32 image ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.flipHorizontal(image);
		} else {
			ImplImageMiscOps.flipHorizontal(image);
		}
	}

	/** Transposes the image */
	public static <T extends GrayS32> T transpose( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		//if (runConcurrent(input)) {
		//	ImplImageMiscOps_MT.transpose(input, output);
		//} else {
		ImplImageMiscOps.transpose(input, output);
		//}
		return output;
	}

	/** In-place 90 degree image rotation in the clockwise direction. Only works on square images. */
	public static void rotateCW( GrayS32 image ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.rotateCW(image);
		} else {
			ImplImageMiscOps.rotateCW(image);
		}
	}

	/** Transposes the image */
	public static <T extends InterleavedS32> T transpose( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.transpose(input, output);
		} else {
			ImplImageMiscOps.transpose(input, output);
		}
		return output;
	}

	/** Rotates the image 90 degrees in the clockwise direction. */
	public static <T extends GrayS32> T rotateCW( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.rotateCW(input, output);
		} else {
			ImplImageMiscOps.rotateCW(input, output);
		}
		return output;
	}

	/** Rotates the image 90 degrees in the clockwise direction. */
	public static <T extends InterleavedS32> T rotateCW( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.rotateCW(input, output);
		} else {
			ImplImageMiscOps.rotateCW(input, output);
		}
		return output;
	}

	/** In-place 90 degree image rotation in the counter-clockwise direction. Only works on square images. */
	public static void rotateCCW( GrayS32 image ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.rotateCCW(image);
		} else {
			ImplImageMiscOps.rotateCCW(image);
		}
	}

	/** Rotates the image 90 degrees in the counter-clockwise direction. */
	public static <T extends GrayS32> T rotateCCW( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.rotateCCW(input, output);
		} else {
			ImplImageMiscOps.rotateCCW(input, output);
		}
		return output;
	}

	/** Rotates the image 90 degrees in the counter-clockwise direction. */
	public static <T extends InterleavedS32> T rotateCCW( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.rotateCCW(input, output);
		} else {
			ImplImageMiscOps.rotateCCW(input, output);
		}
		return output;
	}

	/**
	 * Creates a new image which is a copy of the src image but extended with border pixels.
	 * 
	 * @param src (Input) source image
	 * @param border (Input) image border generator
	 * @param borderX0 (Input) Border x-axis lower extent
	 * @param borderY0 (Input) Border y-axis lower extent
	 * @param borderX1 (Input) Border x-axis upper extent
	 * @param borderY1 (Input) Border y-axis upper extent
	 * @param dst (Output) Output image. width=src.width+2*radiusX and height=src.height+2*radiusY
	 */
	public static void growBorder( GrayS32 src, ImageBorder_S32 border, int borderX0, int borderY0, int borderX1, int borderY1, GrayS32 dst ) {
		if (runConcurrent(src)) {
			ImplImageMiscOps_MT.growBorder(src, border, borderX0, borderY0, borderX1, borderY1, dst);
		} else {
			ImplImageMiscOps.growBorder(src, border, borderX0, borderY0, borderX1, borderY1, dst);
		}
	}

	/**
	 * Using the provided functions, finds all pixel values which match then calls the process function
	 *
	 * @param input (Input) Image
	 * @param finder (Input) Checks to see if the pixel value matches the criteria
	 * @param process (Input) When a match is found this function is called and given the coordinates. true = continue
	 */
	public static void findAndProcess( GrayS32 input, BoofLambdas.Match_S32 finder, BoofLambdas.ProcessIIB process ) {
		ImplImageMiscOps.findAndProcess(input, finder, process);
	}

	/**
	 * Copies a rectangular region from one image into another. The region can go outside the input image's border.<br>
	 * output[dstX:(dstX+width) , dstY:(dstY+height-1)] = input[srcX:(srcX+width) , srcY:(srcY+height-1)]
	 *
	 * @param srcX x-coordinate of corner in input image
	 * @param srcY y-coordinate of corner in input image
	 * @param dstX x-coordinate of corner in output image
	 * @param dstY y-coordinate of corner in output image
	 * @param width Width of region to be copied
	 * @param height Height of region to be copied
	 * @param input Input image
	 * @param border Border for input image
	 * @param output output image
	 */
	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 GrayS64 input, ImageBorder_S64 border, GrayS64 output ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent((dstY-srcY)*(dstX-srcX))) {
//			ImplImageMiscOps_MT.copy(srcX, srcY, dstX, dstY, width, height, input, border, output);
//		} else {
		ImplImageMiscOps.copy(srcX, srcY, dstX, dstY, width, height, input, border, output);
//		}
	}

	/**
	 * Copies a rectangular region from one image into another.<br>
	 * output[dstX:(dstX+width) , dstY:(dstY+height-1)] = input[srcX:(srcX+width) , srcY:(srcY+height-1)]
	 *
	 * @param srcX x-coordinate of corner in input image
	 * @param srcY y-coordinate of corner in input image
	 * @param dstX x-coordinate of corner in output image
	 * @param dstY y-coordinate of corner in output image
	 * @param width Width of region to be copied
	 * @param height Height of region to be copied
	 * @param input Input image
	 * @param output output image
	 */
	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 GrayS64 input, GrayS64 output ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent((dstY-srcY)*(dstX-srcX))) {
//			ImplImageMiscOps_MT.copy(srcX, srcY, dstX, dstY, width, height, input, output);
//		} else {
		ImplImageMiscOps.copy(srcX, srcY, dstX, dstY, width, height, input, output);
//		}
	}

	/**
	 * Copies a rectangular region from one image into another.<br>
	 * output[dstX:(dstX+width) , dstY:(dstY+height-1)] = input[srcX:(srcX+width) , srcY:(srcY+height-1)]
	 *
	 * @param srcX x-coordinate of corner in input image
	 * @param srcY y-coordinate of corner in input image
	 * @param dstX x-coordinate of corner in output image
	 * @param dstY y-coordinate of corner in output image
	 * @param width Width of region to be copied
	 * @param height Height of region to be copied
	 * @param input Input image
	 * @param output output image
	 */
	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 InterleavedS64 input, InterleavedS64 output ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent((dstY-srcY)*(dstX-srcX))) {
//			ImplImageMiscOps_MT.copy(srcX, srcY, dstX, dstY, width, height, input, output);
//		} else {
		ImplImageMiscOps.copy(srcX, srcY, dstX, dstY, width, height, input, output);
//		}
	}

	/**
	 * Fills the whole image with the specified value
	 *
	 * @param image An image. Modified.
	 * @param value The value that the image is being filled with.
	 */
	public static void fill( GrayS64 image, long value ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fill(image, value);
//		} else {
		ImplImageMiscOps.fill(image, value);
//		}
	}

	/**
	 * Fills the whole image with the specified value
	 *
	 * @param image An image. Modified.
	 * @param value The value that the image is being filled with.
	 */
	public static void fill( InterleavedS64 image, long value ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.fill(image, value);
		} else {
			ImplImageMiscOps.fill(image, value);
		}
	}

	/**
	 * Fills each band in the image with the specified values
	 *
	 * @param image An image. Modified.
	 * @param values Array which contains the values each band is to be filled with.
	 */
	public static void fill( InterleavedS64 image, long[] values ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.fill(image, values);
		} else {
			ImplImageMiscOps.fill(image, values);
		}
	}

	/**
	 * Fills one band in the image with the specified value
	 *
	 * @param image An image. Modified.
	 * @param band Which band is to be filled with the specified value
	 * @param value The value that the image is being filled with.
	 */
	public static void fillBand( InterleavedS64 image, int band, long value ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.fillBand(image, band, value);
		} else {
			ImplImageMiscOps.fillBand(image, band, value);
		}
	}

	/**
	 * Inserts a single band into a multi-band image overwriting the original band
	 *
	 * @param input Single band image
	 * @param band Which band the image is to be inserted into
	 * @param output The multi-band image which the input image is to be inserted into
	 */
	public static void insertBand( GrayS64 input, int band, InterleavedS64 output) {
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.insertBand(input, band, output);
		} else {
			ImplImageMiscOps.insertBand(input, band, output);
		}
	}

	/**
	 * Extracts a single band from a multi-band image
	 *
	 * @param input Multi-band image.
	 * @param band which bad is to be extracted
	 * @param output The single band image. Modified.
	 */
	public static void extractBand( InterleavedS64 input, int band, GrayS64 output) {
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.extractBand(input, band, output);
		} else {
			ImplImageMiscOps.extractBand(input, band, output);
		}
	}

	/**
	 * Fills the outside border with the specified value
	 *
	 * @param image An image. Modified.
	 * @param value The value that the image is being filled with.
	 * @param radius Border width.
	 */
	public static void fillBorder( GrayS64 image, long value, int radius ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fillBorder(image, value, radius);
//		} else {
		ImplImageMiscOps.fillBorder(image, value, radius);
//		}
	}

	/**
	 * Fills the border with independent border widths for each side
	 *
	 * @param image An image.
	 * @param value The value that the image is being filled with.
	 * @param borderX0 Width of border on left
	 * @param borderY0 Width of border on top
	 * @param borderX1 Width of border on right
	 * @param borderY1 Width of border on bottom
	 */
	public static void fillBorder( GrayS64 image, long value, int borderX0, int borderY0, int borderX1, int borderY1 ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fillBorder(image, value, borderX0, borderY0, borderX1, borderY1);
//		} else {
		ImplImageMiscOps.fillBorder(image, value, borderX0, borderY0, borderX1, borderY1);
//		}
	}

	/**
	 * Draws a filled rectangle that is aligned along the image axis inside the image.
	 *
	 * @param image The image the rectangle is drawn in. Modified
	 * @param value Value of the rectangle
	 * @param x0 Top left x-coordinate
	 * @param y0 Top left y-coordinate
	 * @param width Rectangle width
	 * @param height Rectangle height
	 */
	public static void fillRectangle( GrayS64 image, long value, int x0, int y0, int width, int height ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fillRectangle(image, value, x0, y0, width, height);
//		} else {
		ImplImageMiscOps.fillRectangle(image, value, x0, y0, width, height);
//		}
	}

	/**
	 * Draws a filled rectangle that is aligned along the image axis inside the image. All bands
	 * are filled with the same value.
	 *
	 * @param image The image the rectangle is drawn in. Modified
	 * @param value Value of the rectangle
	 * @param x0 Top left x-coordinate
	 * @param y0 Top left y-coordinate
	 * @param width Rectangle width
	 * @param height Rectangle height
	 */
	public static void fillRectangle( InterleavedS64 image, long value, int x0, int y0, int width, int height ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fillRectangle(image, value, x0, y0, width, height);
//		} else {
		ImplImageMiscOps.fillRectangle(image, value, x0, y0, width, height);
//		}
	}	/**
	 * Sets each value in the image to a value drawn from an uniform distribution that has a range of min &le; X &lt; max.
	 *
	 * @param img Image which is to be filled. Modified.
	 * @param rand Random number generator
	 * @param min Minimum value of the distribution, inclusive
	 * @param max Maximum value of the distribution, exclusive
	 */
	public static void fillUniform( GrayS64 img, Random rand, long min, long max ) {
		ImplImageMiscOps.fillUniform(img, rand, min, max);
	}

	/**
	 * Sets each value in the image to a value drawn from an uniform distribution that has a range of min &le; X &lt; max.
	 *
	 * @param img Image which is to be filled. Modified.
	 * @param rand Random number generator
	 * @param min Minimum value of the distribution, inclusive
	 * @param max Maximum value of the distribution, exclusive
	 */
	public static void fillUniform( InterleavedS64 img, Random rand, long min, long max ) {
		ImplImageMiscOps.fillUniform(img, rand, min, max);
	}

	/**
	 * Sets each value in the image to a value drawn from a Gaussian distribution. A user
	 * specified lower and upper bound is provided to ensure that the values are within a legal
	 * range. A drawn value outside the allowed range will be set to the closest bound.
	 * 
	 * @param input Input image. Modified.
	 * @param rand Random number generator
	 * @param mean Distribution's mean.
	 * @param sigma Distribution's standard deviation.
	 * @param lowerBound Lower bound of value clip
	 * @param upperBound Upper bound of value clip
	 */
	public static void fillGaussian( GrayS64 input, Random rand, double mean, double sigma, long lowerBound, long upperBound ) {
		ImplImageMiscOps.fillGaussian(input, rand, mean, sigma, lowerBound, upperBound);
	}

	/**
	 * Sets each value in the image to a value drawn from a Gaussian distribution. A user
	 * specified lower and upper bound is provided to ensure that the values are within a legal
	 * range. A drawn value outside the allowed range will be set to the closest bound.
	 * 
	 * @param input Input image. Modified.
	 * @param rand Random number generator
	 * @param mean Distribution's mean.
	 * @param sigma Distribution's standard deviation.
	 * @param lowerBound Lower bound of value clip
	 * @param upperBound Upper bound of value clip
	 */
	public static void fillGaussian( InterleavedS64 input, Random rand, double mean, double sigma, long lowerBound, long upperBound ) {
		ImplImageMiscOps.fillGaussian(input, rand, mean, sigma, lowerBound, upperBound);
	}

	/** Flips the image from top to bottom */
	public static void flipVertical( GrayS64 image ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.flipVertical(image);
		} else {
			ImplImageMiscOps.flipVertical(image);
		}
	}

	/** Flips the image from left to right */
	public static void flipHorizontal( GrayS64 image ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.flipHorizontal(image);
		} else {
			ImplImageMiscOps.flipHorizontal(image);
		}
	}

	/** Transposes the image */
	public static <T extends GrayS64> T transpose( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		//if (runConcurrent(input)) {
		//	ImplImageMiscOps_MT.transpose(input, output);
		//} else {
		ImplImageMiscOps.transpose(input, output);
		//}
		return output;
	}

	/** In-place 90 degree image rotation in the clockwise direction. Only works on square images. */
	public static void rotateCW( GrayS64 image ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.rotateCW(image);
		} else {
			ImplImageMiscOps.rotateCW(image);
		}
	}

	/** Transposes the image */
	public static <T extends InterleavedS64> T transpose( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.transpose(input, output);
		} else {
			ImplImageMiscOps.transpose(input, output);
		}
		return output;
	}

	/** Rotates the image 90 degrees in the clockwise direction. */
	public static <T extends GrayS64> T rotateCW( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.rotateCW(input, output);
		} else {
			ImplImageMiscOps.rotateCW(input, output);
		}
		return output;
	}

	/** Rotates the image 90 degrees in the clockwise direction. */
	public static <T extends InterleavedS64> T rotateCW( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.rotateCW(input, output);
		} else {
			ImplImageMiscOps.rotateCW(input, output);
		}
		return output;
	}

	/** In-place 90 degree image rotation in the counter-clockwise direction. Only works on square images. */
	public static void rotateCCW( GrayS64 image ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.rotateCCW(image);
		} else {
			ImplImageMiscOps.rotateCCW(image);
		}
	}

	/** Rotates the image 90 degrees in the counter-clockwise direction. */
	public static <T extends GrayS64> T rotateCCW( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.rotateCCW(input, output);
		} else {
			ImplImageMiscOps.rotateCCW(input, output);
		}
		return output;
	}

	/** Rotates the image 90 degrees in the counter-clockwise direction. */
	public static <T extends InterleavedS64> T rotateCCW( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.rotateCCW(input, output);
		} else {
			ImplImageMiscOps.rotateCCW(input, output);
		}
		return output;
	}

	/**
	 * Creates a new image which is a copy of the src image but extended with border pixels.
	 * 
	 * @param src (Input) source image
	 * @param border (Input) image border generator
	 * @param borderX0 (Input) Border x-axis lower extent
	 * @param borderY0 (Input) Border y-axis lower extent
	 * @param borderX1 (Input) Border x-axis upper extent
	 * @param borderY1 (Input) Border y-axis upper extent
	 * @param dst (Output) Output image. width=src.width+2*radiusX and height=src.height+2*radiusY
	 */
	public static void growBorder( GrayS64 src, ImageBorder_S64 border, int borderX0, int borderY0, int borderX1, int borderY1, GrayS64 dst ) {
		if (runConcurrent(src)) {
			ImplImageMiscOps_MT.growBorder(src, border, borderX0, borderY0, borderX1, borderY1, dst);
		} else {
			ImplImageMiscOps.growBorder(src, border, borderX0, borderY0, borderX1, borderY1, dst);
		}
	}

	/**
	 * Using the provided functions, finds all pixel values which match then calls the process function
	 *
	 * @param input (Input) Image
	 * @param finder (Input) Checks to see if the pixel value matches the criteria
	 * @param process (Input) When a match is found this function is called and given the coordinates. true = continue
	 */
	public static void findAndProcess( GrayS64 input, BoofLambdas.Match_S64 finder, BoofLambdas.ProcessIIB process ) {
		ImplImageMiscOps.findAndProcess(input, finder, process);
	}

	/**
	 * Copies a rectangular region from one image into another. The region can go outside the input image's border.<br>
	 * output[dstX:(dstX+width) , dstY:(dstY+height-1)] = input[srcX:(srcX+width) , srcY:(srcY+height-1)]
	 *
	 * @param srcX x-coordinate of corner in input image
	 * @param srcY y-coordinate of corner in input image
	 * @param dstX x-coordinate of corner in output image
	 * @param dstY y-coordinate of corner in output image
	 * @param width Width of region to be copied
	 * @param height Height of region to be copied
	 * @param input Input image
	 * @param border Border for input image
	 * @param output output image
	 */
	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 GrayF32 input, ImageBorder_F32 border, GrayF32 output ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent((dstY-srcY)*(dstX-srcX))) {
//			ImplImageMiscOps_MT.copy(srcX, srcY, dstX, dstY, width, height, input, border, output);
//		} else {
		ImplImageMiscOps.copy(srcX, srcY, dstX, dstY, width, height, input, border, output);
//		}
	}

	/**
	 * Copies a rectangular region from one image into another.<br>
	 * output[dstX:(dstX+width) , dstY:(dstY+height-1)] = input[srcX:(srcX+width) , srcY:(srcY+height-1)]
	 *
	 * @param srcX x-coordinate of corner in input image
	 * @param srcY y-coordinate of corner in input image
	 * @param dstX x-coordinate of corner in output image
	 * @param dstY y-coordinate of corner in output image
	 * @param width Width of region to be copied
	 * @param height Height of region to be copied
	 * @param input Input image
	 * @param output output image
	 */
	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 GrayF32 input, GrayF32 output ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent((dstY-srcY)*(dstX-srcX))) {
//			ImplImageMiscOps_MT.copy(srcX, srcY, dstX, dstY, width, height, input, output);
//		} else {
		ImplImageMiscOps.copy(srcX, srcY, dstX, dstY, width, height, input, output);
//		}
	}

	/**
	 * Copies a rectangular region from one image into another.<br>
	 * output[dstX:(dstX+width) , dstY:(dstY+height-1)] = input[srcX:(srcX+width) , srcY:(srcY+height-1)]
	 *
	 * @param srcX x-coordinate of corner in input image
	 * @param srcY y-coordinate of corner in input image
	 * @param dstX x-coordinate of corner in output image
	 * @param dstY y-coordinate of corner in output image
	 * @param width Width of region to be copied
	 * @param height Height of region to be copied
	 * @param input Input image
	 * @param output output image
	 */
	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 InterleavedF32 input, InterleavedF32 output ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent((dstY-srcY)*(dstX-srcX))) {
//			ImplImageMiscOps_MT.copy(srcX, srcY, dstX, dstY, width, height, input, output);
//		} else {
		ImplImageMiscOps.copy(srcX, srcY, dstX, dstY, width, height, input, output);
//		}
	}

	/**
	 * Fills the whole image with the specified value
	 *
	 * @param image An image. Modified.
	 * @param value The value that the image is being filled with.
	 */
	public static void fill( GrayF32 image, float value ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fill(image, value);
//		} else {
		ImplImageMiscOps.fill(image, value);
//		}
	}

	/**
	 * Fills the whole image with the specified value
	 *
	 * @param image An image. Modified.
	 * @param value The value that the image is being filled with.
	 */
	public static void fill( InterleavedF32 image, float value ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.fill(image, value);
		} else {
			ImplImageMiscOps.fill(image, value);
		}
	}

	/**
	 * Fills each band in the image with the specified values
	 *
	 * @param image An image. Modified.
	 * @param values Array which contains the values each band is to be filled with.
	 */
	public static void fill( InterleavedF32 image, float[] values ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.fill(image, values);
		} else {
			ImplImageMiscOps.fill(image, values);
		}
	}

	/**
	 * Fills one band in the image with the specified value
	 *
	 * @param image An image. Modified.
	 * @param band Which band is to be filled with the specified value
	 * @param value The value that the image is being filled with.
	 */
	public static void fillBand( InterleavedF32 image, int band, float value ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.fillBand(image, band, value);
		} else {
			ImplImageMiscOps.fillBand(image, band, value);
		}
	}

	/**
	 * Inserts a single band into a multi-band image overwriting the original band
	 *
	 * @param input Single band image
	 * @param band Which band the image is to be inserted into
	 * @param output The multi-band image which the input image is to be inserted into
	 */
	public static void insertBand( GrayF32 input, int band, InterleavedF32 output) {
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.insertBand(input, band, output);
		} else {
			ImplImageMiscOps.insertBand(input, band, output);
		}
	}

	/**
	 * Extracts a single band from a multi-band image
	 *
	 * @param input Multi-band image.
	 * @param band which bad is to be extracted
	 * @param output The single band image. Modified.
	 */
	public static void extractBand( InterleavedF32 input, int band, GrayF32 output) {
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.extractBand(input, band, output);
		} else {
			ImplImageMiscOps.extractBand(input, band, output);
		}
	}

	/**
	 * Fills the outside border with the specified value
	 *
	 * @param image An image. Modified.
	 * @param value The value that the image is being filled with.
	 * @param radius Border width.
	 */
	public static void fillBorder( GrayF32 image, float value, int radius ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fillBorder(image, value, radius);
//		} else {
		ImplImageMiscOps.fillBorder(image, value, radius);
//		}
	}

	/**
	 * Fills the border with independent border widths for each side
	 *
	 * @param image An image.
	 * @param value The value that the image is being filled with.
	 * @param borderX0 Width of border on left
	 * @param borderY0 Width of border on top
	 * @param borderX1 Width of border on right
	 * @param borderY1 Width of border on bottom
	 */
	public static void fillBorder( GrayF32 image, float value, int borderX0, int borderY0, int borderX1, int borderY1 ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fillBorder(image, value, borderX0, borderY0, borderX1, borderY1);
//		} else {
		ImplImageMiscOps.fillBorder(image, value, borderX0, borderY0, borderX1, borderY1);
//		}
	}

	/**
	 * Draws a filled rectangle that is aligned along the image axis inside the image.
	 *
	 * @param image The image the rectangle is drawn in. Modified
	 * @param value Value of the rectangle
	 * @param x0 Top left x-coordinate
	 * @param y0 Top left y-coordinate
	 * @param width Rectangle width
	 * @param height Rectangle height
	 */
	public static void fillRectangle( GrayF32 image, float value, int x0, int y0, int width, int height ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fillRectangle(image, value, x0, y0, width, height);
//		} else {
		ImplImageMiscOps.fillRectangle(image, value, x0, y0, width, height);
//		}
	}

	/**
	 * Draws a filled rectangle that is aligned along the image axis inside the image. All bands
	 * are filled with the same value.
	 *
	 * @param image The image the rectangle is drawn in. Modified
	 * @param value Value of the rectangle
	 * @param x0 Top left x-coordinate
	 * @param y0 Top left y-coordinate
	 * @param width Rectangle width
	 * @param height Rectangle height
	 */
	public static void fillRectangle( InterleavedF32 image, float value, int x0, int y0, int width, int height ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fillRectangle(image, value, x0, y0, width, height);
//		} else {
		ImplImageMiscOps.fillRectangle(image, value, x0, y0, width, height);
//		}
	}	/**
	 * Sets each value in the image to a value drawn from an uniform distribution that has a range of min &le; X &lt; max.
	 *
	 * @param img Image which is to be filled. Modified.
	 * @param rand Random number generator
	 * @param min Minimum value of the distribution, inclusive
	 * @param max Maximum value of the distribution, inclusive
	 */
	public static void fillUniform( GrayF32 img, Random rand, float min, float max ) {
		ImplImageMiscOps.fillUniform(img, rand, min, max);
	}

	/**
	 * Sets each value in the image to a value drawn from an uniform distribution that has a range of min &le; X &lt; max.
	 *
	 * @param img Image which is to be filled. Modified.
	 * @param rand Random number generator
	 * @param min Minimum value of the distribution, inclusive
	 * @param max Maximum value of the distribution, inclusive
	 */
	public static void fillUniform( InterleavedF32 img, Random rand, float min, float max ) {
		ImplImageMiscOps.fillUniform(img, rand, min, max);
	}

	/**
	 * Sets each value in the image to a value drawn from a Gaussian distribution. A user
	 * specified lower and upper bound is provided to ensure that the values are within a legal
	 * range. A drawn value outside the allowed range will be set to the closest bound.
	 * 
	 * @param input Input image. Modified.
	 * @param rand Random number generator
	 * @param mean Distribution's mean.
	 * @param sigma Distribution's standard deviation.
	 * @param lowerBound Lower bound of value clip
	 * @param upperBound Upper bound of value clip
	 */
	public static void fillGaussian( GrayF32 input, Random rand, double mean, double sigma, float lowerBound, float upperBound ) {
		ImplImageMiscOps.fillGaussian(input, rand, mean, sigma, lowerBound, upperBound);
	}

	/**
	 * Sets each value in the image to a value drawn from a Gaussian distribution. A user
	 * specified lower and upper bound is provided to ensure that the values are within a legal
	 * range. A drawn value outside the allowed range will be set to the closest bound.
	 * 
	 * @param input Input image. Modified.
	 * @param rand Random number generator
	 * @param mean Distribution's mean.
	 * @param sigma Distribution's standard deviation.
	 * @param lowerBound Lower bound of value clip
	 * @param upperBound Upper bound of value clip
	 */
	public static void fillGaussian( InterleavedF32 input, Random rand, double mean, double sigma, float lowerBound, float upperBound ) {
		ImplImageMiscOps.fillGaussian(input, rand, mean, sigma, lowerBound, upperBound);
	}

	/** Flips the image from top to bottom */
	public static void flipVertical( GrayF32 image ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.flipVertical(image);
		} else {
			ImplImageMiscOps.flipVertical(image);
		}
	}

	/** Flips the image from left to right */
	public static void flipHorizontal( GrayF32 image ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.flipHorizontal(image);
		} else {
			ImplImageMiscOps.flipHorizontal(image);
		}
	}

	/** Transposes the image */
	public static <T extends GrayF32> T transpose( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		//if (runConcurrent(input)) {
		//	ImplImageMiscOps_MT.transpose(input, output);
		//} else {
		ImplImageMiscOps.transpose(input, output);
		//}
		return output;
	}

	/** In-place 90 degree image rotation in the clockwise direction. Only works on square images. */
	public static void rotateCW( GrayF32 image ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.rotateCW(image);
		} else {
			ImplImageMiscOps.rotateCW(image);
		}
	}

	/** Transposes the image */
	public static <T extends InterleavedF32> T transpose( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.transpose(input, output);
		} else {
			ImplImageMiscOps.transpose(input, output);
		}
		return output;
	}

	/** Rotates the image 90 degrees in the clockwise direction. */
	public static <T extends GrayF32> T rotateCW( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.rotateCW(input, output);
		} else {
			ImplImageMiscOps.rotateCW(input, output);
		}
		return output;
	}

	/** Rotates the image 90 degrees in the clockwise direction. */
	public static <T extends InterleavedF32> T rotateCW( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.rotateCW(input, output);
		} else {
			ImplImageMiscOps.rotateCW(input, output);
		}
		return output;
	}

	/** In-place 90 degree image rotation in the counter-clockwise direction. Only works on square images. */
	public static void rotateCCW( GrayF32 image ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.rotateCCW(image);
		} else {
			ImplImageMiscOps.rotateCCW(image);
		}
	}

	/** Rotates the image 90 degrees in the counter-clockwise direction. */
	public static <T extends GrayF32> T rotateCCW( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.rotateCCW(input, output);
		} else {
			ImplImageMiscOps.rotateCCW(input, output);
		}
		return output;
	}

	/** Rotates the image 90 degrees in the counter-clockwise direction. */
	public static <T extends InterleavedF32> T rotateCCW( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.rotateCCW(input, output);
		} else {
			ImplImageMiscOps.rotateCCW(input, output);
		}
		return output;
	}

	/**
	 * Creates a new image which is a copy of the src image but extended with border pixels.
	 * 
	 * @param src (Input) source image
	 * @param border (Input) image border generator
	 * @param borderX0 (Input) Border x-axis lower extent
	 * @param borderY0 (Input) Border y-axis lower extent
	 * @param borderX1 (Input) Border x-axis upper extent
	 * @param borderY1 (Input) Border y-axis upper extent
	 * @param dst (Output) Output image. width=src.width+2*radiusX and height=src.height+2*radiusY
	 */
	public static void growBorder( GrayF32 src, ImageBorder_F32 border, int borderX0, int borderY0, int borderX1, int borderY1, GrayF32 dst ) {
		if (runConcurrent(src)) {
			ImplImageMiscOps_MT.growBorder(src, border, borderX0, borderY0, borderX1, borderY1, dst);
		} else {
			ImplImageMiscOps.growBorder(src, border, borderX0, borderY0, borderX1, borderY1, dst);
		}
	}

	/**
	 * Using the provided functions, finds all pixel values which match then calls the process function
	 *
	 * @param input (Input) Image
	 * @param finder (Input) Checks to see if the pixel value matches the criteria
	 * @param process (Input) When a match is found this function is called and given the coordinates. true = continue
	 */
	public static void findAndProcess( GrayF32 input, BoofLambdas.Match_F32 finder, BoofLambdas.ProcessIIB process ) {
		ImplImageMiscOps.findAndProcess(input, finder, process);
	}

	/**
	 * Copies a rectangular region from one image into another. The region can go outside the input image's border.<br>
	 * output[dstX:(dstX+width) , dstY:(dstY+height-1)] = input[srcX:(srcX+width) , srcY:(srcY+height-1)]
	 *
	 * @param srcX x-coordinate of corner in input image
	 * @param srcY y-coordinate of corner in input image
	 * @param dstX x-coordinate of corner in output image
	 * @param dstY y-coordinate of corner in output image
	 * @param width Width of region to be copied
	 * @param height Height of region to be copied
	 * @param input Input image
	 * @param border Border for input image
	 * @param output output image
	 */
	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 GrayF64 input, ImageBorder_F64 border, GrayF64 output ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent((dstY-srcY)*(dstX-srcX))) {
//			ImplImageMiscOps_MT.copy(srcX, srcY, dstX, dstY, width, height, input, border, output);
//		} else {
		ImplImageMiscOps.copy(srcX, srcY, dstX, dstY, width, height, input, border, output);
//		}
	}

	/**
	 * Copies a rectangular region from one image into another.<br>
	 * output[dstX:(dstX+width) , dstY:(dstY+height-1)] = input[srcX:(srcX+width) , srcY:(srcY+height-1)]
	 *
	 * @param srcX x-coordinate of corner in input image
	 * @param srcY y-coordinate of corner in input image
	 * @param dstX x-coordinate of corner in output image
	 * @param dstY y-coordinate of corner in output image
	 * @param width Width of region to be copied
	 * @param height Height of region to be copied
	 * @param input Input image
	 * @param output output image
	 */
	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 GrayF64 input, GrayF64 output ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent((dstY-srcY)*(dstX-srcX))) {
//			ImplImageMiscOps_MT.copy(srcX, srcY, dstX, dstY, width, height, input, output);
//		} else {
		ImplImageMiscOps.copy(srcX, srcY, dstX, dstY, width, height, input, output);
//		}
	}

	/**
	 * Copies a rectangular region from one image into another.<br>
	 * output[dstX:(dstX+width) , dstY:(dstY+height-1)] = input[srcX:(srcX+width) , srcY:(srcY+height-1)]
	 *
	 * @param srcX x-coordinate of corner in input image
	 * @param srcY y-coordinate of corner in input image
	 * @param dstX x-coordinate of corner in output image
	 * @param dstY y-coordinate of corner in output image
	 * @param width Width of region to be copied
	 * @param height Height of region to be copied
	 * @param input Input image
	 * @param output output image
	 */
	public static void copy( int srcX, int srcY, int dstX, int dstY, int width, int height,
							 InterleavedF64 input, InterleavedF64 output ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent((dstY-srcY)*(dstX-srcX))) {
//			ImplImageMiscOps_MT.copy(srcX, srcY, dstX, dstY, width, height, input, output);
//		} else {
		ImplImageMiscOps.copy(srcX, srcY, dstX, dstY, width, height, input, output);
//		}
	}

	/**
	 * Fills the whole image with the specified value
	 *
	 * @param image An image. Modified.
	 * @param value The value that the image is being filled with.
	 */
	public static void fill( GrayF64 image, double value ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fill(image, value);
//		} else {
		ImplImageMiscOps.fill(image, value);
//		}
	}

	/**
	 * Fills the whole image with the specified value
	 *
	 * @param image An image. Modified.
	 * @param value The value that the image is being filled with.
	 */
	public static void fill( InterleavedF64 image, double value ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.fill(image, value);
		} else {
			ImplImageMiscOps.fill(image, value);
		}
	}

	/**
	 * Fills each band in the image with the specified values
	 *
	 * @param image An image. Modified.
	 * @param values Array which contains the values each band is to be filled with.
	 */
	public static void fill( InterleavedF64 image, double[] values ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.fill(image, values);
		} else {
			ImplImageMiscOps.fill(image, values);
		}
	}

	/**
	 * Fills one band in the image with the specified value
	 *
	 * @param image An image. Modified.
	 * @param band Which band is to be filled with the specified value
	 * @param value The value that the image is being filled with.
	 */
	public static void fillBand( InterleavedF64 image, int band, double value ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.fillBand(image, band, value);
		} else {
			ImplImageMiscOps.fillBand(image, band, value);
		}
	}

	/**
	 * Inserts a single band into a multi-band image overwriting the original band
	 *
	 * @param input Single band image
	 * @param band Which band the image is to be inserted into
	 * @param output The multi-band image which the input image is to be inserted into
	 */
	public static void insertBand( GrayF64 input, int band, InterleavedF64 output) {
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.insertBand(input, band, output);
		} else {
			ImplImageMiscOps.insertBand(input, band, output);
		}
	}

	/**
	 * Extracts a single band from a multi-band image
	 *
	 * @param input Multi-band image.
	 * @param band which bad is to be extracted
	 * @param output The single band image. Modified.
	 */
	public static void extractBand( InterleavedF64 input, int band, GrayF64 output) {
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.extractBand(input, band, output);
		} else {
			ImplImageMiscOps.extractBand(input, band, output);
		}
	}

	/**
	 * Fills the outside border with the specified value
	 *
	 * @param image An image. Modified.
	 * @param value The value that the image is being filled with.
	 * @param radius Border width.
	 */
	public static void fillBorder( GrayF64 image, double value, int radius ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fillBorder(image, value, radius);
//		} else {
		ImplImageMiscOps.fillBorder(image, value, radius);
//		}
	}

	/**
	 * Fills the border with independent border widths for each side
	 *
	 * @param image An image.
	 * @param value The value that the image is being filled with.
	 * @param borderX0 Width of border on left
	 * @param borderY0 Width of border on top
	 * @param borderX1 Width of border on right
	 * @param borderY1 Width of border on bottom
	 */
	public static void fillBorder( GrayF64 image, double value, int borderX0, int borderY0, int borderX1, int borderY1 ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fillBorder(image, value, borderX0, borderY0, borderX1, borderY1);
//		} else {
		ImplImageMiscOps.fillBorder(image, value, borderX0, borderY0, borderX1, borderY1);
//		}
	}

	/**
	 * Draws a filled rectangle that is aligned along the image axis inside the image.
	 *
	 * @param image The image the rectangle is drawn in. Modified
	 * @param value Value of the rectangle
	 * @param x0 Top left x-coordinate
	 * @param y0 Top left y-coordinate
	 * @param width Rectangle width
	 * @param height Rectangle height
	 */
	public static void fillRectangle( GrayF64 image, double value, int x0, int y0, int width, int height ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fillRectangle(image, value, x0, y0, width, height);
//		} else {
		ImplImageMiscOps.fillRectangle(image, value, x0, y0, width, height);
//		}
	}

	/**
	 * Draws a filled rectangle that is aligned along the image axis inside the image. All bands
	 * are filled with the same value.
	 *
	 * @param image The image the rectangle is drawn in. Modified
	 * @param value Value of the rectangle
	 * @param x0 Top left x-coordinate
	 * @param y0 Top left y-coordinate
	 * @param width Rectangle width
	 * @param height Rectangle height
	 */
	public static void fillRectangle( InterleavedF64 image, double value, int x0, int y0, int width, int height ) {
//		concurrent isn't faster in benchmark results
//		if (runConcurrent(image)) {
//			ImplImageMiscOps_MT.fillRectangle(image, value, x0, y0, width, height);
//		} else {
		ImplImageMiscOps.fillRectangle(image, value, x0, y0, width, height);
//		}
	}	/**
	 * Sets each value in the image to a value drawn from an uniform distribution that has a range of min &le; X &lt; max.
	 *
	 * @param img Image which is to be filled. Modified.
	 * @param rand Random number generator
	 * @param min Minimum value of the distribution, inclusive
	 * @param max Maximum value of the distribution, inclusive
	 */
	public static void fillUniform( GrayF64 img, Random rand, double min, double max ) {
		ImplImageMiscOps.fillUniform(img, rand, min, max);
	}

	/**
	 * Sets each value in the image to a value drawn from an uniform distribution that has a range of min &le; X &lt; max.
	 *
	 * @param img Image which is to be filled. Modified.
	 * @param rand Random number generator
	 * @param min Minimum value of the distribution, inclusive
	 * @param max Maximum value of the distribution, inclusive
	 */
	public static void fillUniform( InterleavedF64 img, Random rand, double min, double max ) {
		ImplImageMiscOps.fillUniform(img, rand, min, max);
	}

	/**
	 * Sets each value in the image to a value drawn from a Gaussian distribution. A user
	 * specified lower and upper bound is provided to ensure that the values are within a legal
	 * range. A drawn value outside the allowed range will be set to the closest bound.
	 * 
	 * @param input Input image. Modified.
	 * @param rand Random number generator
	 * @param mean Distribution's mean.
	 * @param sigma Distribution's standard deviation.
	 * @param lowerBound Lower bound of value clip
	 * @param upperBound Upper bound of value clip
	 */
	public static void fillGaussian( GrayF64 input, Random rand, double mean, double sigma, double lowerBound, double upperBound ) {
		ImplImageMiscOps.fillGaussian(input, rand, mean, sigma, lowerBound, upperBound);
	}

	/**
	 * Sets each value in the image to a value drawn from a Gaussian distribution. A user
	 * specified lower and upper bound is provided to ensure that the values are within a legal
	 * range. A drawn value outside the allowed range will be set to the closest bound.
	 * 
	 * @param input Input image. Modified.
	 * @param rand Random number generator
	 * @param mean Distribution's mean.
	 * @param sigma Distribution's standard deviation.
	 * @param lowerBound Lower bound of value clip
	 * @param upperBound Upper bound of value clip
	 */
	public static void fillGaussian( InterleavedF64 input, Random rand, double mean, double sigma, double lowerBound, double upperBound ) {
		ImplImageMiscOps.fillGaussian(input, rand, mean, sigma, lowerBound, upperBound);
	}

	/** Flips the image from top to bottom */
	public static void flipVertical( GrayF64 image ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.flipVertical(image);
		} else {
			ImplImageMiscOps.flipVertical(image);
		}
	}

	/** Flips the image from left to right */
	public static void flipHorizontal( GrayF64 image ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.flipHorizontal(image);
		} else {
			ImplImageMiscOps.flipHorizontal(image);
		}
	}

	/** Transposes the image */
	public static <T extends GrayF64> T transpose( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		//if (runConcurrent(input)) {
		//	ImplImageMiscOps_MT.transpose(input, output);
		//} else {
		ImplImageMiscOps.transpose(input, output);
		//}
		return output;
	}

	/** In-place 90 degree image rotation in the clockwise direction. Only works on square images. */
	public static void rotateCW( GrayF64 image ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.rotateCW(image);
		} else {
			ImplImageMiscOps.rotateCW(image);
		}
	}

	/** Transposes the image */
	public static <T extends InterleavedF64> T transpose( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.transpose(input, output);
		} else {
			ImplImageMiscOps.transpose(input, output);
		}
		return output;
	}

	/** Rotates the image 90 degrees in the clockwise direction. */
	public static <T extends GrayF64> T rotateCW( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.rotateCW(input, output);
		} else {
			ImplImageMiscOps.rotateCW(input, output);
		}
		return output;
	}

	/** Rotates the image 90 degrees in the clockwise direction. */
	public static <T extends InterleavedF64> T rotateCW( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.rotateCW(input, output);
		} else {
			ImplImageMiscOps.rotateCW(input, output);
		}
		return output;
	}

	/** In-place 90 degree image rotation in the counter-clockwise direction. Only works on square images. */
	public static void rotateCCW( GrayF64 image ) {
		if (runConcurrent(image)) {
			ImplImageMiscOps_MT.rotateCCW(image);
		} else {
			ImplImageMiscOps.rotateCCW(image);
		}
	}

	/** Rotates the image 90 degrees in the counter-clockwise direction. */
	public static <T extends GrayF64> T rotateCCW( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.rotateCCW(input, output);
		} else {
			ImplImageMiscOps.rotateCCW(input, output);
		}
		return output;
	}

	/** Rotates the image 90 degrees in the counter-clockwise direction. */
	public static <T extends InterleavedF64> T rotateCCW( T input, @Nullable T output ) {
		output = (T)InputSanityCheck.declareOutput(input, output);
		if (runConcurrent(input)) {
			ImplImageMiscOps_MT.rotateCCW(input, output);
		} else {
			ImplImageMiscOps.rotateCCW(input, output);
		}
		return output;
	}

	/**
	 * Creates a new image which is a copy of the src image but extended with border pixels.
	 * 
	 * @param src (Input) source image
	 * @param border (Input) image border generator
	 * @param borderX0 (Input) Border x-axis lower extent
	 * @param borderY0 (Input) Border y-axis lower extent
	 * @param borderX1 (Input) Border x-axis upper extent
	 * @param borderY1 (Input) Border y-axis upper extent
	 * @param dst (Output) Output image. width=src.width+2*radiusX and height=src.height+2*radiusY
	 */
	public static void growBorder( GrayF64 src, ImageBorder_F64 border, int borderX0, int borderY0, int borderX1, int borderY1, GrayF64 dst ) {
		if (runConcurrent(src)) {
			ImplImageMiscOps_MT.growBorder(src, border, borderX0, borderY0, borderX1, borderY1, dst);
		} else {
			ImplImageMiscOps.growBorder(src, border, borderX0, borderY0, borderX1, borderY1, dst);
		}
	}

	/**
	 * Using the provided functions, finds all pixel values which match then calls the process function
	 *
	 * @param input (Input) Image
	 * @param finder (Input) Checks to see if the pixel value matches the criteria
	 * @param process (Input) When a match is found this function is called and given the coordinates. true = continue
	 */
	public static void findAndProcess( GrayF64 input, BoofLambdas.Match_F64 finder, BoofLambdas.ProcessIIB process ) {
		ImplImageMiscOps.findAndProcess(input, finder, process);
	}

	/**
	 * Adds uniform i.i.d noise to each pixel in the image. Noise range is min &le; X &lt; max.
	 */
	public static void addUniform(GrayU8 input, Random rand, int min, int max) {
		ImplImageMiscOps.addUniform(input, rand, min, max);
	}

	/**
	 * Adds uniform i.i.d noise to each pixel in the image. Noise range is min &le; X &lt; max.
	 */
	public static void addUniform(InterleavedU8 input, Random rand, int min, int max) {
		ImplImageMiscOps.addUniform(input, rand, min, max);
	}

	/**
	 * Adds Gaussian/normal i.i.d noise to each pixel in the image. If a value exceeds the specified
	 * it will be set to the closest bound.
	 * @param image Input image. Modified.
	 * @param rand Random number generator.
	 * @param sigma Distributions standard deviation.
	 * @param lowerBound Allowed lower bound
	 * @param upperBound Allowed upper bound
	 */
	public static void addGaussian(GrayU8 image, Random rand, double sigma, int lowerBound, int upperBound ) {
		ImplImageMiscOps.addGaussian(image, rand, sigma, lowerBound, upperBound);
	}

	/**
	 * Adds Gaussian/normal i.i.d noise to each pixel in the image. If a value exceeds the specified
	 * it will be set to the closest bound.
	 * @param image Input image. Modified.
	 * @param rand Random number generator.
	 * @param sigma Distributions standard deviation.
	 * @param lowerBound Allowed lower bound
	 * @param upperBound Allowed upper bound
	 */
	public static void addGaussian(InterleavedU8 image, Random rand, double sigma, int lowerBound, int upperBound ) {
		ImplImageMiscOps.addGaussian(image, rand, sigma, lowerBound, upperBound);
	}

	/**
	 * Adds uniform i.i.d noise to each pixel in the image. Noise range is min &le; X &lt; max.
	 */
	public static void addUniform(GrayS8 input, Random rand, int min, int max) {
		ImplImageMiscOps.addUniform(input, rand, min, max);
	}

	/**
	 * Adds uniform i.i.d noise to each pixel in the image. Noise range is min &le; X &lt; max.
	 */
	public static void addUniform(InterleavedS8 input, Random rand, int min, int max) {
		ImplImageMiscOps.addUniform(input, rand, min, max);
	}

	/**
	 * Adds Gaussian/normal i.i.d noise to each pixel in the image. If a value exceeds the specified
	 * it will be set to the closest bound.
	 * @param image Input image. Modified.
	 * @param rand Random number generator.
	 * @param sigma Distributions standard deviation.
	 * @param lowerBound Allowed lower bound
	 * @param upperBound Allowed upper bound
	 */
	public static void addGaussian(GrayS8 image, Random rand, double sigma, int lowerBound, int upperBound ) {
		ImplImageMiscOps.addGaussian(image, rand, sigma, lowerBound, upperBound);
	}

	/**
	 * Adds Gaussian/normal i.i.d noise to each pixel in the image. If a value exceeds the specified
	 * it will be set to the closest bound.
	 * @param image Input image. Modified.
	 * @param rand Random number generator.
	 * @param sigma Distributions standard deviation.
	 * @param lowerBound Allowed lower bound
	 * @param upperBound Allowed upper bound
	 */
	public static void addGaussian(InterleavedS8 image, Random rand, double sigma, int lowerBound, int upperBound ) {
		ImplImageMiscOps.addGaussian(image, rand, sigma, lowerBound, upperBound);
	}

	/**
	 * Adds uniform i.i.d noise to each pixel in the image. Noise range is min &le; X &lt; max.
	 */
	public static void addUniform(GrayU16 input, Random rand, int min, int max) {
		ImplImageMiscOps.addUniform(input, rand, min, max);
	}

	/**
	 * Adds uniform i.i.d noise to each pixel in the image. Noise range is min &le; X &lt; max.
	 */
	public static void addUniform(InterleavedU16 input, Random rand, int min, int max) {
		ImplImageMiscOps.addUniform(input, rand, min, max);
	}

	/**
	 * Adds Gaussian/normal i.i.d noise to each pixel in the image. If a value exceeds the specified
	 * it will be set to the closest bound.
	 * @param image Input image. Modified.
	 * @param rand Random number generator.
	 * @param sigma Distributions standard deviation.
	 * @param lowerBound Allowed lower bound
	 * @param upperBound Allowed upper bound
	 */
	public static void addGaussian(GrayU16 image, Random rand, double sigma, int lowerBound, int upperBound ) {
		ImplImageMiscOps.addGaussian(image, rand, sigma, lowerBound, upperBound);
	}

	/**
	 * Adds Gaussian/normal i.i.d noise to each pixel in the image. If a value exceeds the specified
	 * it will be set to the closest bound.
	 * @param image Input image. Modified.
	 * @param rand Random number generator.
	 * @param sigma Distributions standard deviation.
	 * @param lowerBound Allowed lower bound
	 * @param upperBound Allowed upper bound
	 */
	public static void addGaussian(InterleavedU16 image, Random rand, double sigma, int lowerBound, int upperBound ) {
		ImplImageMiscOps.addGaussian(image, rand, sigma, lowerBound, upperBound);
	}

	/**
	 * Adds uniform i.i.d noise to each pixel in the image. Noise range is min &le; X &lt; max.
	 */
	public static void addUniform(GrayS16 input, Random rand, int min, int max) {
		ImplImageMiscOps.addUniform(input, rand, min, max);
	}

	/**
	 * Adds uniform i.i.d noise to each pixel in the image. Noise range is min &le; X &lt; max.
	 */
	public static void addUniform(InterleavedS16 input, Random rand, int min, int max) {
		ImplImageMiscOps.addUniform(input, rand, min, max);
	}

	/**
	 * Adds Gaussian/normal i.i.d noise to each pixel in the image. If a value exceeds the specified
	 * it will be set to the closest bound.
	 * @param image Input image. Modified.
	 * @param rand Random number generator.
	 * @param sigma Distributions standard deviation.
	 * @param lowerBound Allowed lower bound
	 * @param upperBound Allowed upper bound
	 */
	public static void addGaussian(GrayS16 image, Random rand, double sigma, int lowerBound, int upperBound ) {
		ImplImageMiscOps.addGaussian(image, rand, sigma, lowerBound, upperBound);
	}

	/**
	 * Adds Gaussian/normal i.i.d noise to each pixel in the image. If a value exceeds the specified
	 * it will be set to the closest bound.
	 * @param image Input image. Modified.
	 * @param rand Random number generator.
	 * @param sigma Distributions standard deviation.
	 * @param lowerBound Allowed lower bound
	 * @param upperBound Allowed upper bound
	 */
	public static void addGaussian(InterleavedS16 image, Random rand, double sigma, int lowerBound, int upperBound ) {
		ImplImageMiscOps.addGaussian(image, rand, sigma, lowerBound, upperBound);
	}

	/**
	 * Adds uniform i.i.d noise to each pixel in the image. Noise range is min &le; X &lt; max.
	 */
	public static void addUniform(GrayS32 input, Random rand, int min, int max) {
		ImplImageMiscOps.addUniform(input, rand, min, max);
	}

	/**
	 * Adds uniform i.i.d noise to each pixel in the image. Noise range is min &le; X &lt; max.
	 */
	public static void addUniform(InterleavedS32 input, Random rand, int min, int max) {
		ImplImageMiscOps.addUniform(input, rand, min, max);
	}

	/**
	 * Adds Gaussian/normal i.i.d noise to each pixel in the image. If a value exceeds the specified
	 * it will be set to the closest bound.
	 * @param image Input image. Modified.
	 * @param rand Random number generator.
	 * @param sigma Distributions standard deviation.
	 * @param lowerBound Allowed lower bound
	 * @param upperBound Allowed upper bound
	 */
	public static void addGaussian(GrayS32 image, Random rand, double sigma, int lowerBound, int upperBound ) {
		ImplImageMiscOps.addGaussian(image, rand, sigma, lowerBound, upperBound);
	}

	/**
	 * Adds Gaussian/normal i.i.d noise to each pixel in the image. If a value exceeds the specified
	 * it will be set to the closest bound.
	 * @param image Input image. Modified.
	 * @param rand Random number generator.
	 * @param sigma Distributions standard deviation.
	 * @param lowerBound Allowed lower bound
	 * @param upperBound Allowed upper bound
	 */
	public static void addGaussian(InterleavedS32 image, Random rand, double sigma, int lowerBound, int upperBound ) {
		ImplImageMiscOps.addGaussian(image, rand, sigma, lowerBound, upperBound);
	}

	/**
	 * Adds uniform i.i.d noise to each pixel in the image. Noise range is min &le; X &lt; max.
	 */
	public static void addUniform(GrayS64 input, Random rand, long min, long max) {
		ImplImageMiscOps.addUniform(input, rand, min, max);
	}

	/**
	 * Adds uniform i.i.d noise to each pixel in the image. Noise range is min &le; X &lt; max.
	 */
	public static void addUniform(InterleavedS64 input, Random rand, long min, long max) {
		ImplImageMiscOps.addUniform(input, rand, min, max);
	}

	/**
	 * Adds Gaussian/normal i.i.d noise to each pixel in the image. If a value exceeds the specified
	 * it will be set to the closest bound.
	 * @param image Input image. Modified.
	 * @param rand Random number generator.
	 * @param sigma Distributions standard deviation.
	 * @param lowerBound Allowed lower bound
	 * @param upperBound Allowed upper bound
	 */
	public static void addGaussian(GrayS64 image, Random rand, double sigma, long lowerBound, long upperBound ) {
		ImplImageMiscOps.addGaussian(image, rand, sigma, lowerBound, upperBound);
	}

	/**
	 * Adds Gaussian/normal i.i.d noise to each pixel in the image. If a value exceeds the specified
	 * it will be set to the closest bound.
	 * @param image Input image. Modified.
	 * @param rand Random number generator.
	 * @param sigma Distributions standard deviation.
	 * @param lowerBound Allowed lower bound
	 * @param upperBound Allowed upper bound
	 */
	public static void addGaussian(InterleavedS64 image, Random rand, double sigma, long lowerBound, long upperBound ) {
		ImplImageMiscOps.addGaussian(image, rand, sigma, lowerBound, upperBound);
	}

	/**
	 * Adds uniform i.i.d noise to each pixel in the image. Noise range is min &le; X &lt; max.
	 */
	public static void addUniform(GrayF32 input, Random rand, float min, float max) {
		ImplImageMiscOps.addUniform(input, rand, min, max);
	}

	/**
	 * Adds uniform i.i.d noise to each pixel in the image. Noise range is min &le; X &lt; max.
	 */
	public static void addUniform(InterleavedF32 input, Random rand, float min, float max) {
		ImplImageMiscOps.addUniform(input, rand, min, max);
	}

	/**
	 * Adds Gaussian/normal i.i.d noise to each pixel in the image. If a value exceeds the specified
	 * it will be set to the closest bound.
	 * @param image Input image. Modified.
	 * @param rand Random number generator.
	 * @param sigma Distributions standard deviation.
	 * @param lowerBound Allowed lower bound
	 * @param upperBound Allowed upper bound
	 */
	public static void addGaussian(GrayF32 image, Random rand, double sigma, float lowerBound, float upperBound ) {
		ImplImageMiscOps.addGaussian(image, rand, sigma, lowerBound, upperBound);
	}

	/**
	 * Adds Gaussian/normal i.i.d noise to each pixel in the image. If a value exceeds the specified
	 * it will be set to the closest bound.
	 * @param image Input image. Modified.
	 * @param rand Random number generator.
	 * @param sigma Distributions standard deviation.
	 * @param lowerBound Allowed lower bound
	 * @param upperBound Allowed upper bound
	 */
	public static void addGaussian(InterleavedF32 image, Random rand, double sigma, float lowerBound, float upperBound ) {
		ImplImageMiscOps.addGaussian(image, rand, sigma, lowerBound, upperBound);
	}

	/**
	 * Adds uniform i.i.d noise to each pixel in the image. Noise range is min &le; X &lt; max.
	 */
	public static void addUniform(GrayF64 input, Random rand, double min, double max) {
		ImplImageMiscOps.addUniform(input, rand, min, max);
	}

	/**
	 * Adds uniform i.i.d noise to each pixel in the image. Noise range is min &le; X &lt; max.
	 */
	public static void addUniform(InterleavedF64 input, Random rand, double min, double max) {
		ImplImageMiscOps.addUniform(input, rand, min, max);
	}

	/**
	 * Adds Gaussian/normal i.i.d noise to each pixel in the image. If a value exceeds the specified
	 * it will be set to the closest bound.
	 * @param image Input image. Modified.
	 * @param rand Random number generator.
	 * @param sigma Distributions standard deviation.
	 * @param lowerBound Allowed lower bound
	 * @param upperBound Allowed upper bound
	 */
	public static void addGaussian(GrayF64 image, Random rand, double sigma, double lowerBound, double upperBound ) {
		ImplImageMiscOps.addGaussian(image, rand, sigma, lowerBound, upperBound);
	}

	/**
	 * Adds Gaussian/normal i.i.d noise to each pixel in the image. If a value exceeds the specified
	 * it will be set to the closest bound.
	 * @param image Input image. Modified.
	 * @param rand Random number generator.
	 * @param sigma Distributions standard deviation.
	 * @param lowerBound Allowed lower bound
	 * @param upperBound Allowed upper bound
	 */
	public static void addGaussian(InterleavedF64 image, Random rand, double sigma, double lowerBound, double upperBound ) {
		ImplImageMiscOps.addGaussian(image, rand, sigma, lowerBound, upperBound);
	}

}
