/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.misc;

import boofcv.struct.image.*;

/**
 * <p>
 * Operations related to down sampling image by computing the average within square regions. The first square region is
 * from (0,0) to
 * (w-1,w-1), inclusive.  Each square region after that is found by skipping over 'w' pixels in x and y directions.
 * partial regions along the right and bottom borders are handled by computing the average with the rectangle defined
 * by the intersection of the image and the square region.
 * </p>
 *
 * <p>
 * NOTE: Errors are reduced in integer images by rounding instead of standard integer division.
 * </p>
 *
 *
 * @author Peter Abeles
 */
public class AverageDownSampleOps {
	/**
	 * Computes the length of a down sampled image based on the original length and the square width
	 * @param length Length of side in input image
	 * @param squareWidth Width of region used to down sample images
	 * @return Length of side in down sampled image
	 */
	public static int downSampleSize( int length , int squareWidth ) {
		int ret = length/squareWidth;
		if( length%squareWidth != 0 )
			ret++;

		return ret;
	}

	/**
	 * Reshapes an image so that it is the correct size to store the down sampled image
	 */
	public static void reshapeDown(ImageBase image, int inputWidth, int inputHeight, int squareWidth) {
		int w = downSampleSize(inputWidth,squareWidth);
		int h = downSampleSize(inputHeight,squareWidth);

		image.reshape(w,h);
	}

	/**
	 * Down samples image.  Type checking is done at runtime.
	 *
	 * @param input Input image. Not modified.
	 * @param sampleWidth Width of square region.
	 * @param output Output image. Modified.
	 */
	public static void down( ImageSingleBand input , int sampleWidth , ImageSingleBand output ) {
		if( sampleWidth == 2 ) {
			if( input instanceof ImageUInt8 ) {
				ImplAverageDownSample2.down((ImageUInt8) input, (ImageInt8) output);
			} else if( input instanceof ImageSInt8) {
				ImplAverageDownSample2.down((ImageSInt8) input, (ImageInt8) output);
			} else if( input instanceof ImageUInt16) {
				ImplAverageDownSample2.down((ImageUInt16) input, (ImageInt16) output);
			} else if( input instanceof ImageSInt16) {
				ImplAverageDownSample2.down((ImageSInt16) input, (ImageInt16) output);
			} else if( input instanceof ImageSInt32) {
				ImplAverageDownSample2.down((ImageSInt32) input, (ImageSInt32) output);
			} else if( input instanceof ImageFloat32) {
				ImplAverageDownSample2.down((ImageFloat32) input, (ImageFloat32) output);
			} else if( input instanceof ImageFloat64) {
				ImplAverageDownSample2.down((ImageFloat64) input, (ImageFloat64) output);
			} else {
				throw new IllegalArgumentException("Unknown image type");
			}
		} else {
			if( input instanceof ImageUInt8 ) {
				ImplAverageDownSampleN.down((ImageUInt8) input, sampleWidth , (ImageInt8) output);
			} else if( input instanceof ImageSInt8) {
				ImplAverageDownSampleN.down((ImageSInt8) input, sampleWidth , (ImageInt8) output);
			} else if( input instanceof ImageUInt16) {
				ImplAverageDownSampleN.down((ImageUInt16) input, sampleWidth , (ImageInt16) output);
			} else if( input instanceof ImageSInt16) {
				ImplAverageDownSampleN.down((ImageSInt16) input, sampleWidth , (ImageInt16) output);
			} else if( input instanceof ImageSInt32) {
				ImplAverageDownSampleN.down((ImageSInt32) input, sampleWidth , (ImageSInt32) output);
			} else if( input instanceof ImageFloat32) {
				ImplAverageDownSampleN.down((ImageFloat32) input, sampleWidth , (ImageFloat32) output);
			} else if( input instanceof ImageFloat64) {
				ImplAverageDownSampleN.down((ImageFloat64) input, sampleWidth , (ImageFloat64) output);
			} else {
				throw new IllegalArgumentException("Unknown image type");
			}
		}
	}

	/**
	 * Down samples image.  Type checking is done at runtime.
	 *
	 * @param input Input image. Not modified.
	 * @param output Output image. Modified.
	 */
	public static <T extends ImageSingleBand>
	void down( T input , T output ) {
		if( input instanceof ImageUInt8 ) {
			ImageFloat32 middle = new ImageFloat32(output.width,input.height);
			ImplAverageDownSample.horizontal((ImageUInt8) input, middle);
			ImplAverageDownSample.vertical(middle, (ImageInt8) output);
		} else if( input instanceof ImageUInt16) {
			ImageFloat32 middle = new ImageFloat32(output.width,input.height);
			ImplAverageDownSample.horizontal((ImageUInt16) input, middle);
			ImplAverageDownSample.vertical(middle, (ImageUInt16) output);
		} else if( input instanceof ImageFloat32) {
			ImageFloat32 middle = new ImageFloat32(output.width,input.height);
			ImplAverageDownSample.horizontal((ImageFloat32) input, middle);
			ImplAverageDownSample.vertical(middle, (ImageFloat32) output);
		} else if( input instanceof ImageFloat64) {
			ImageFloat64 middle = new ImageFloat64(output.width,input.height);
			ImplAverageDownSample.horizontal((ImageFloat64) input, middle);
			ImplAverageDownSample.vertical(middle, (ImageFloat64) output);
		} else {
			throw new IllegalArgumentException("Unknown image type");
		}
	}

	/**
	 * Down samples a multi-spectral image.  Type checking is done at runtime.
	 *
	 * @param input Input image. Not modified.
	 * @param sampleWidth Width of square region.
	 * @param output Output image. Modified.
	 */
	public static <T extends ImageSingleBand> void down( MultiSpectral<T> input ,
														 int sampleWidth , MultiSpectral<T> output )
	{
		for( int band = 0; band < input.getNumBands(); band++ ) {
			down(input.getBand(band), sampleWidth, output.getBand(band));
		}
	}

	/**
	 * Down samples a multi-spectral image.  Type checking is done at runtime.
	 *
	 * @param input Input image. Not modified.
	 * @param output Output image. Modified.
	 */
	public static <T extends ImageSingleBand> void down( MultiSpectral<T> input , MultiSpectral<T> output )
	{
		for( int band = 0; band < input.getNumBands(); band++ ) {
			down(input.getBand(band), output.getBand(band));
		}
	}

	/**
	 * Down samples the image.
	 *
	 * @param input Input image. Not modified.
	 * @param sampleWidth Width of square region.
	 * @param output Output image. Modified.
	 */
	public static void down( ImageUInt8 input , int sampleWidth , ImageInt8 output ) {
		if( sampleWidth == 2 ) {
			ImplAverageDownSample2.down( input, output);
		} else {
			ImplAverageDownSampleN.down (input, sampleWidth , output);
		}
	}

	/**
	 * Down samples the image.
	 *
	 * @param input Input image. Not modified.
	 * @param sampleWidth Width of square region.
	 * @param output Output image. Modified.
	 */
	public static void down( ImageSInt8 input , int sampleWidth , ImageInt8 output ) {
		if( sampleWidth == 2 ) {
			ImplAverageDownSample2.down( input, output);
		} else {
			ImplAverageDownSampleN.down( input, sampleWidth , output);
		}
	}

	/**
	 * Down samples the image.
	 *
	 * @param input Input image. Not modified.
	 * @param sampleWidth Width of square region.
	 * @param output Output image. Modified.
	 */
	public static void down( ImageUInt16 input , int sampleWidth , ImageInt16 output ) {
		if( sampleWidth == 2 ) {
			ImplAverageDownSample2.down( input, output);
		} else {
			ImplAverageDownSampleN.down( input, sampleWidth , output);
		}
	}

	/**
	 * Down samples the image.
	 *
	 * @param input Input image. Not modified.
	 * @param sampleWidth Width of square region.
	 * @param output Output image. Modified.
	 */
	public static void down( ImageSInt16 input , int sampleWidth , ImageInt16 output ) {
		if( sampleWidth == 2 ) {
			ImplAverageDownSample2.down( input, output);
		} else {
			ImplAverageDownSampleN.down( input, sampleWidth , output);
		}
	}

	/**
	 * Down samples the image.
	 *
	 * @param input Input image. Not modified.
	 * @param sampleWidth Width of square region.
	 * @param output Output image. Modified.
	 */
	public static void down( ImageSInt32 input , int sampleWidth , ImageSInt32 output ) {
		if( sampleWidth == 2 ) {
			ImplAverageDownSample2.down( input, output);
		} else {
			ImplAverageDownSampleN.down( input, sampleWidth , output);
		}
	}

	/**
	 * Down samples the image.
	 *
	 * @param input Input image. Not modified.
	 * @param sampleWidth Width of square region.
	 * @param output Output image. Modified.
	 */
	public static void down( ImageFloat32 input , int sampleWidth , ImageFloat32 output ) {
		if( sampleWidth == 2 ) {
			ImplAverageDownSample2.down( input, output);
		} else {
			ImplAverageDownSampleN.down( input, sampleWidth , output);
		}
	}

	/**
	 * Down samples the image.
	 *
	 * @param input Input image. Not modified.
	 * @param sampleWidth Width of square region.
	 * @param output Output image. Modified.
	 */
	public static void down( ImageFloat64 input , int sampleWidth , ImageFloat64 output ) {
		if( sampleWidth == 2 ) {
			ImplAverageDownSample2.down( input, output);
		} else {
			ImplAverageDownSampleN.down( input, sampleWidth , output);
		}
	}
}
