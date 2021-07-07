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

package boofcv.alg.filter.misc;

import boofcv.alg.filter.misc.impl.*;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.image.*;

/**
 * <p>
 * Operations related to down sampling image by computing the average within square regions. The first square region is
 * from (0,0) to
 * (w-1,w-1), inclusive. Each square region after that is found by skipping over 'w' pixels in x and y directions.
 * partial regions along the right and bottom borders are handled by computing the average with the rectangle defined
 * by the intersection of the image and the square region.
 * </p>
 *
 * <p>NOTE: Errors are reduced in integer images by rounding instead of standard integer division.</p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"Duplicates", "rawtypes"})
public class AverageDownSampleOps {
	/**
	 * Computes the length of a down sampled image based on the original length and the square width
	 *
	 * @param length Length of side in input image
	 * @param squareWidth Width of region used to down sample images
	 * @return Length of side in down sampled image
	 */
	public static int downSampleSize( int length, int squareWidth ) {
		int ret = length/squareWidth;
		if (length%squareWidth != 0)
			ret++;

		return ret;
	}

	/**
	 * Reshapes an image so that it is the correct size to store the down sampled image
	 */
	public static void reshapeDown( ImageBase image, int inputWidth, int inputHeight, int squareWidth ) {
		int w = downSampleSize(inputWidth, squareWidth);
		int h = downSampleSize(inputHeight, squareWidth);

		image.reshape(w, h);
	}

	/**
	 * If the full resolution image is too large it's down sampled to match the maximum allowed pixels.
	 *
	 * @param full Full resolution image
	 * @param small Small image to store down sampled, if applicable
	 * @param maxPixels Maximum number of pixels in the image
	 * @return Either full resolution image or the small image
	 */
	public static <T extends ImageGray<T>> T downMaxPixels( T full, T small, int maxPixels ) {
		if (full.width*full.height > maxPixels) {
			double scale = Math.sqrt(maxPixels)/Math.sqrt(full.width*full.height);
			small.reshape((int)(full.width*scale + 0.5), (int)(full.height*scale + 0.5));

			// Use average to reduce aliasing. This causes a shift in the image center.
			AverageDownSampleOps.down(full, small);
			return small;
		}
		return full;
	}

	public static void down( ImageBase input, int sampleWidth, ImageBase output ) {
		switch (input.getImageType().getFamily()) {
			case GRAY -> {
				down((ImageGray)input, sampleWidth, (ImageGray)output);
				return;
			}
			case PLANAR -> {
				down((Planar)input, sampleWidth, (Planar)output);
				return;
			}
			case INTERLEAVED -> throw new IllegalArgumentException("Interleaved images are not yet supported");
		}
		throw new IllegalArgumentException("Unknown image type");
	}

	/**
	 * Down samples image. Type checking is done at runtime.
	 *
	 * @param input Input image. Not modified.
	 * @param sampleWidth Width of square region.
	 * @param output Output image. Modified.
	 */
	public static void down( ImageGray input, int sampleWidth, ImageGray output ) {
		if (input instanceof GrayU8) {
			down((GrayU8)input, sampleWidth, (GrayI8)output);
		} else if (input instanceof GrayS8) {
			down((GrayS8)input, sampleWidth, (GrayI8)output);
		} else if (input instanceof GrayU16) {
			down((GrayU16)input, sampleWidth, (GrayI16)output);
		} else if (input instanceof GrayS16) {
			down((GrayS16)input, sampleWidth, (GrayI16)output);
		} else if (input instanceof GrayS32) {
			down((GrayS32)input, sampleWidth, (GrayS32)output);
		} else if (input instanceof GrayF32) {
			down((GrayF32)input, sampleWidth, (GrayF32)output);
		} else if (input instanceof GrayF64) {
			down((GrayF64)input, sampleWidth, (GrayF64)output);
		} else {
			throw new IllegalArgumentException("Unknown image type");
		}
	}

	/**
	 * Down samples image. Type checking is done at runtime.
	 *
	 * @param input Input image. Not modified.
	 * @param output Output image. Modified.
	 */
	public static <T extends ImageBase<T>>
	void down( T input, T output ) {
		if (ImageGray.class.isAssignableFrom(input.getClass())) {
			if (BoofConcurrency.USE_CONCURRENT) {
				if (input instanceof GrayU8) {
					GrayF32 middle = new GrayF32(output.width, input.height);
					ImplAverageDownSample_MT.horizontal((GrayU8)input, middle);
					ImplAverageDownSample_MT.vertical(middle, (GrayI8)output);
				} else if (input instanceof GrayU16) {
					GrayF32 middle = new GrayF32(output.width, input.height);
					ImplAverageDownSample_MT.horizontal((GrayU16)input, middle);
					ImplAverageDownSample_MT.vertical(middle, (GrayU16)output);
				} else if (input instanceof GrayF32) {
					GrayF32 middle = new GrayF32(output.width, input.height);
					ImplAverageDownSample_MT.horizontal((GrayF32)input, middle);
					ImplAverageDownSample_MT.vertical(middle, (GrayF32)output);
				} else if (input instanceof GrayF64) {
					GrayF64 middle = new GrayF64(output.width, input.height);
					ImplAverageDownSample_MT.horizontal((GrayF64)input, middle);
					ImplAverageDownSample_MT.vertical(middle, (GrayF64)output);
				} else {
					throw new IllegalArgumentException("Unknown image type");
				}
			} else {
				if (input instanceof GrayU8) {
					GrayF32 middle = new GrayF32(output.width, input.height);
					ImplAverageDownSample.horizontal((GrayU8)input, middle);
					ImplAverageDownSample.vertical(middle, (GrayI8)output);
				} else if (input instanceof GrayU16) {
					GrayF32 middle = new GrayF32(output.width, input.height);
					ImplAverageDownSample.horizontal((GrayU16)input, middle);
					ImplAverageDownSample.vertical(middle, (GrayU16)output);
				} else if (input instanceof GrayF32) {
					GrayF32 middle = new GrayF32(output.width, input.height);
					ImplAverageDownSample.horizontal((GrayF32)input, middle);
					ImplAverageDownSample.vertical(middle, (GrayF32)output);
				} else if (input instanceof GrayF64) {
					GrayF64 middle = new GrayF64(output.width, input.height);
					ImplAverageDownSample.horizontal((GrayF64)input, middle);
					ImplAverageDownSample.vertical(middle, (GrayF64)output);
				} else {
					throw new IllegalArgumentException("Unknown image type");
				}
			}
		} else if (Planar.class.isAssignableFrom(input.getClass())) {
			down((Planar)input, (Planar)output);
		}
	}

	/**
	 * Down samples a planar image. Type checking is done at runtime.
	 *
	 * @param input Input image. Not modified.
	 * @param sampleWidth Width of square region.
	 * @param output Output image. Modified.
	 */
	public static <T extends ImageGray<T>> void down( Planar<T> input,
													  int sampleWidth, Planar<T> output ) {
		for (int band = 0; band < input.getNumBands(); band++) {
			down(input.getBand(band), sampleWidth, output.getBand(band));
		}
	}

	/**
	 * Down samples a planar image. Type checking is done at runtime.
	 *
	 * @param input Input image. Not modified.
	 * @param output Output image. Modified.
	 */
	public static <T extends ImageGray<T>> void down( Planar<T> input, Planar<T> output ) {
		output.setNumberOfBands(input.getNumBands());
		for (int band = 0; band < input.getNumBands(); band++) {
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
	public static void down( GrayU8 input, int sampleWidth, GrayI8 output ) {
		if (sampleWidth == 2) {
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplAverageDownSample2_MT.down(input, output);
			} else {
				ImplAverageDownSample2.down(input, output);
			}
		} else {
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplAverageDownSampleN_MT.down(input, sampleWidth, output);
			} else {
				ImplAverageDownSampleN.down(input, sampleWidth, output);
			}
		}
	}

	/**
	 * Down samples the image.
	 *
	 * @param input Input image. Not modified.
	 * @param sampleWidth Width of square region.
	 * @param output Output image. Modified.
	 */
	public static void down( GrayS8 input, int sampleWidth, GrayI8 output ) {
		if (sampleWidth == 2) {
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplAverageDownSample2_MT.down(input, output);
			} else {
				ImplAverageDownSample2.down(input, output);
			}
		} else {
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplAverageDownSampleN_MT.down(input, sampleWidth, output);
			} else {
				ImplAverageDownSampleN.down(input, sampleWidth, output);
			}
		}
	}

	/**
	 * Down samples the image.
	 *
	 * @param input Input image. Not modified.
	 * @param sampleWidth Width of square region.
	 * @param output Output image. Modified.
	 */
	public static void down( GrayU16 input, int sampleWidth, GrayI16 output ) {
		if (sampleWidth == 2) {
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplAverageDownSample2_MT.down(input, output);
			} else {
				ImplAverageDownSample2.down(input, output);
			}
		} else {
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplAverageDownSampleN_MT.down(input, sampleWidth, output);
			} else {
				ImplAverageDownSampleN.down(input, sampleWidth, output);
			}
		}
	}

	/**
	 * Down samples the image.
	 *
	 * @param input Input image. Not modified.
	 * @param sampleWidth Width of square region.
	 * @param output Output image. Modified.
	 */
	public static void down( GrayS16 input, int sampleWidth, GrayI16 output ) {
		if (sampleWidth == 2) {
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplAverageDownSample2_MT.down(input, output);
			} else {
				ImplAverageDownSample2.down(input, output);
			}
		} else {
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplAverageDownSampleN_MT.down(input, sampleWidth, output);
			} else {
				ImplAverageDownSampleN.down(input, sampleWidth, output);
			}
		}
	}

	/**
	 * Down samples the image.
	 *
	 * @param input Input image. Not modified.
	 * @param sampleWidth Width of square region.
	 * @param output Output image. Modified.
	 */
	public static void down( GrayS32 input, int sampleWidth, GrayS32 output ) {
		if (sampleWidth == 2) {
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplAverageDownSample2_MT.down(input, output);
			} else {
				ImplAverageDownSample2.down(input, output);
			}
		} else {
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplAverageDownSampleN_MT.down(input, sampleWidth, output);
			} else {
				ImplAverageDownSampleN.down(input, sampleWidth, output);
			}
		}
	}

	/**
	 * Down samples the image.
	 *
	 * @param input Input image. Not modified.
	 * @param sampleWidth Width of square region.
	 * @param output Output image. Modified.
	 */
	public static void down( GrayF32 input, int sampleWidth, GrayF32 output ) {
		if (sampleWidth == 2) {
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplAverageDownSample2_MT.down(input, output);
			} else {
				ImplAverageDownSample2.down(input, output);
			}
		} else {
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplAverageDownSampleN_MT.down(input, sampleWidth, output);
			} else {
				ImplAverageDownSampleN.down(input, sampleWidth, output);
			}
		}
	}

	/**
	 * Down samples the image.
	 *
	 * @param input Input image. Not modified.
	 * @param sampleWidth Width of square region.
	 * @param output Output image. Modified.
	 */
	public static void down( GrayF64 input, int sampleWidth, GrayF64 output ) {
		if (sampleWidth == 2) {
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplAverageDownSample2_MT.down(input, output);
			} else {
				ImplAverageDownSample2.down(input, output);
			}
		} else {
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplAverageDownSampleN_MT.down(input, sampleWidth, output);
			} else {
				ImplAverageDownSampleN.down(input, sampleWidth, output);
			}
		}
	}
}
