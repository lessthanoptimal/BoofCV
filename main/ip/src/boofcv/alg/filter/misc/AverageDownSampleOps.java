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
	public static void down(ImageGray input , int sampleWidth , ImageGray output ) {
		if( sampleWidth == 2 ) {
			if( input instanceof GrayU8) {
				ImplAverageDownSample2.down((GrayU8) input, (GrayI8) output);
			} else if( input instanceof GrayS8) {
				ImplAverageDownSample2.down((GrayS8) input, (GrayI8) output);
			} else if( input instanceof GrayU16) {
				ImplAverageDownSample2.down((GrayU16) input, (GrayI16) output);
			} else if( input instanceof GrayS16) {
				ImplAverageDownSample2.down((GrayS16) input, (GrayI16) output);
			} else if( input instanceof GrayS32) {
				ImplAverageDownSample2.down((GrayS32) input, (GrayS32) output);
			} else if( input instanceof GrayF32) {
				ImplAverageDownSample2.down((GrayF32) input, (GrayF32) output);
			} else if( input instanceof GrayF64) {
				ImplAverageDownSample2.down((GrayF64) input, (GrayF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image type");
			}
		} else {
			if( input instanceof GrayU8) {
				ImplAverageDownSampleN.down((GrayU8) input, sampleWidth , (GrayI8) output);
			} else if( input instanceof GrayS8) {
				ImplAverageDownSampleN.down((GrayS8) input, sampleWidth , (GrayI8) output);
			} else if( input instanceof GrayU16) {
				ImplAverageDownSampleN.down((GrayU16) input, sampleWidth , (GrayI16) output);
			} else if( input instanceof GrayS16) {
				ImplAverageDownSampleN.down((GrayS16) input, sampleWidth , (GrayI16) output);
			} else if( input instanceof GrayS32) {
				ImplAverageDownSampleN.down((GrayS32) input, sampleWidth , (GrayS32) output);
			} else if( input instanceof GrayF32) {
				ImplAverageDownSampleN.down((GrayF32) input, sampleWidth , (GrayF32) output);
			} else if( input instanceof GrayF64) {
				ImplAverageDownSampleN.down((GrayF64) input, sampleWidth , (GrayF64) output);
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
	public static <T extends ImageBase>
	void down( T input , T output ) {
		if( ImageGray.class.isAssignableFrom(input.getClass())  ) {
			if (input instanceof GrayU8) {
				GrayF32 middle = new GrayF32(output.width, input.height);
				ImplAverageDownSample.horizontal((GrayU8) input, middle);
				ImplAverageDownSample.vertical(middle, (GrayI8) output);
			} else if (input instanceof GrayU16) {
				GrayF32 middle = new GrayF32(output.width, input.height);
				ImplAverageDownSample.horizontal((GrayU16) input, middle);
				ImplAverageDownSample.vertical(middle, (GrayU16) output);
			} else if (input instanceof GrayF32) {
				GrayF32 middle = new GrayF32(output.width, input.height);
				ImplAverageDownSample.horizontal((GrayF32) input, middle);
				ImplAverageDownSample.vertical(middle, (GrayF32) output);
			} else if (input instanceof GrayF64) {
				GrayF64 middle = new GrayF64(output.width, input.height);
				ImplAverageDownSample.horizontal((GrayF64) input, middle);
				ImplAverageDownSample.vertical(middle, (GrayF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image type");
			}
		} else if( Planar.class.isAssignableFrom(input.getClass())  ) {
			Planar in = (Planar)input;
			Planar out = (Planar)output;

			int N = in.getNumBands();

			for (int i = 0; i < N; i++) {
				down(in.getBand(i),out.getBand(i));
			}
		}
	}

	/**
	 * Down samples a planar image.  Type checking is done at runtime.
	 *
	 * @param input Input image. Not modified.
	 * @param sampleWidth Width of square region.
	 * @param output Output image. Modified.
	 */
	public static <T extends ImageGray> void down(Planar<T> input ,
												  int sampleWidth , Planar<T> output )
	{
		for( int band = 0; band < input.getNumBands(); band++ ) {
			down(input.getBand(band), sampleWidth, output.getBand(band));
		}
	}

	/**
	 * Down samples a planar image.  Type checking is done at runtime.
	 *
	 * @param input Input image. Not modified.
	 * @param output Output image. Modified.
	 */
	public static <T extends ImageGray> void down(Planar<T> input , Planar<T> output )
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
	public static void down(GrayU8 input , int sampleWidth , GrayI8 output ) {
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
	public static void down(GrayS8 input , int sampleWidth , GrayI8 output ) {
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
	public static void down(GrayU16 input , int sampleWidth , GrayI16 output ) {
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
	public static void down(GrayS16 input , int sampleWidth , GrayI16 output ) {
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
	public static void down(GrayS32 input , int sampleWidth , GrayS32 output ) {
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
	public static void down(GrayF32 input , int sampleWidth , GrayF32 output ) {
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
	public static void down(GrayF64 input , int sampleWidth , GrayF64 output ) {
		if( sampleWidth == 2 ) {
			ImplAverageDownSample2.down( input, output);
		} else {
			ImplAverageDownSampleN.down( input, sampleWidth , output);
		}
	}
}
