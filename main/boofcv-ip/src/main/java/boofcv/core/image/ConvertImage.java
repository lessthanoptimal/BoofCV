/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.core.image;

import boofcv.concurrency.BoofConcurrency;
import boofcv.core.image.impl.*;
import boofcv.struct.image.*;

import javax.annotation.processing.Generated;

/**
 * <p>
 * Functions for converting between different image types. Pixel values are converted by typecasting.
 * When converting between signed and unsigned types, care should be taken to avoid numerical overflow.
 * </p>
 *
 * <p>DO NOT MODIFY. Automatically generated code created by GenerateConvertImage</p>
 *
 * @author Peter Abeles
 */
@Generated("boofcv.core.image.GenerateConvertImage")
@SuppressWarnings("Duplicates")
public class ConvertImage {

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayU8} into a {@link boofcv.struct.image.GrayS8}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS8 convert( GrayU8 input, GrayS8 output ) {
		if (output == null) {
			output = new GrayS8(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedU8} into a {@link boofcv.struct.image.InterleavedS8}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS8 convert( InterleavedU8 input, InterleavedS8 output ) {
		if (output == null) {
			output = new InterleavedS8(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayU8} into a {@link boofcv.struct.image.GrayU16}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayU16 convert( GrayU8 input, GrayU16 output ) {
		if (output == null) {
			output = new GrayU16(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedU8} into a {@link boofcv.struct.image.InterleavedU16}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedU16 convert( InterleavedU8 input, InterleavedU16 output ) {
		if (output == null) {
			output = new InterleavedU16(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayU8} into a {@link boofcv.struct.image.GrayS16}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS16 convert( GrayU8 input, GrayS16 output ) {
		if (output == null) {
			output = new GrayS16(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedU8} into a {@link boofcv.struct.image.InterleavedS16}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS16 convert( InterleavedU8 input, InterleavedS16 output ) {
		if (output == null) {
			output = new InterleavedS16(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayU8} into a {@link boofcv.struct.image.GrayS32}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS32 convert( GrayU8 input, GrayS32 output ) {
		if (output == null) {
			output = new GrayS32(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedU8} into a {@link boofcv.struct.image.InterleavedS32}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS32 convert( InterleavedU8 input, InterleavedS32 output ) {
		if (output == null) {
			output = new InterleavedS32(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayU8} into a {@link boofcv.struct.image.GrayS64}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS64 convert( GrayU8 input, GrayS64 output ) {
		if (output == null) {
			output = new GrayS64(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedU8} into a {@link boofcv.struct.image.InterleavedS64}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS64 convert( InterleavedU8 input, InterleavedS64 output ) {
		if (output == null) {
			output = new InterleavedS64(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayU8} into a {@link boofcv.struct.image.GrayF32}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayF32 convert( GrayU8 input, GrayF32 output ) {
		if (output == null) {
			output = new GrayF32(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedU8} into a {@link boofcv.struct.image.InterleavedF32}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedF32 convert( InterleavedU8 input, InterleavedF32 output ) {
		if (output == null) {
			output = new InterleavedF32(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayU8} into a {@link boofcv.struct.image.GrayF64}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayF64 convert( GrayU8 input, GrayF64 output ) {
		if (output == null) {
			output = new GrayF64(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedU8} into a {@link boofcv.struct.image.InterleavedF64}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedF64 convert( InterleavedU8 input, InterleavedF64 output ) {
		if (output == null) {
			output = new InterleavedF64(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * Converts a {@link Planar} into a {@link ImageGray} by computing the average value of each pixel
	 * across all the bands.
	 *
	 * @param input Input Planar image that is being converted. Not modified.
	 * @param output (Optional) The single band output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayU8 average( Planar<GrayU8> input , GrayU8 output ) {
		if (output == null) {
			output = new GrayU8(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		if( BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertPlanarToGray_MT.average(input, output);
		} else {
			ImplConvertPlanarToGray.average(input, output);
		}

		return output;
	}

	/**
	 * Converts a {@link Planar} into the equivalent {@link InterleavedU8}
	 *
	 * @param input (Input) Planar image that is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedU8 convert( Planar<GrayU8> input , InterleavedU8 output ) {
		if (output == null) {
			output = new InterleavedU8(input.width, input.height,input.getNumBands());
		} else {
			output.reshapeTo(input);
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplConvertImage_MT.convert(input, output);
		} else {
			ImplConvertImage.convert(input, output);
		}

		return output;
	}

	/**
	 * Converts a {@link InterleavedU8} into a {@link GrayU8} by computing the average value of each pixel
	 * across all the bands.
	 *
	 * @param input (Input) The ImageInterleaved that is being converted. Not modified.
	 * @param output (Optional) The single band output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayU8 average( InterleavedU8 input , GrayU8 output ) {
		if (output == null) {
			output = new GrayU8(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvertInterleavedToSingle_MT.average(input, output);
		} else {
			ConvertInterleavedToSingle.average(input, output);
		}

		return output;
	}

	/**
	 * Converts a {@link InterleavedU8} into the equivalent {@link Planar}
	 *
	 * @param input (Input) ImageInterleaved that is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static Planar<GrayU8> convert( InterleavedU8 input , Planar<GrayU8> output ) {
		if (output == null) {
			output = new Planar<>(GrayU8.class,input.width, input.height,input.numBands);
		} else {
			output.reshapeTo(input);
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplConvertImage_MT.convert(input, output);
		} else {
			ImplConvertImage.convert(input, output);
		}

		return output;
	}

	/**
	 * Converts pixel values in the input image into an integer values from 0 to numValues.
	 * @param input Input image
	 * @param min minimum input pixel value, inclusive
	 * @param max maximum input pixel value, inclusive
	 * @param numValues Number of possible pixel values in output image
	 * @param output (Optional) Storage for the output image. Can be null.
	 * @return The converted output image.
	 */
	public static GrayU8 convert( GrayU8 input, int min, int max, int numValues , GrayU8 output ) {
		if (output == null) {
			output = new GrayU8(input.width, input.height);
		} else {
			output.reshape(input.width,input.height);
		}
		if (numValues < 0 || numValues > 256)
			throw new IllegalArgumentException("0 <= numValues <= 256");

		numValues -= 1;
		int range = max-min;

		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + y*input.stride;
			int indexOut = output.startIndex + y*output.stride;

			for (int x = 0; x < input.width; x++) {
				int value = (int)(numValues*((input.data[indexIn++]& 0xFF)-min)/range );
				output.data[indexOut++] = (byte)value;
			}
		}
	return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayS8} into a {@link boofcv.struct.image.GrayU8}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayU8 convert( GrayS8 input, GrayU8 output ) {
		if (output == null) {
			output = new GrayU8(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedS8} into a {@link boofcv.struct.image.InterleavedU8}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedU8 convert( InterleavedS8 input, InterleavedU8 output ) {
		if (output == null) {
			output = new InterleavedU8(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayS8} into a {@link boofcv.struct.image.GrayU16}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayU16 convert( GrayS8 input, GrayU16 output ) {
		if (output == null) {
			output = new GrayU16(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedS8} into a {@link boofcv.struct.image.InterleavedU16}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedU16 convert( InterleavedS8 input, InterleavedU16 output ) {
		if (output == null) {
			output = new InterleavedU16(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayS8} into a {@link boofcv.struct.image.GrayS16}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS16 convert( GrayS8 input, GrayS16 output ) {
		if (output == null) {
			output = new GrayS16(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedS8} into a {@link boofcv.struct.image.InterleavedS16}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS16 convert( InterleavedS8 input, InterleavedS16 output ) {
		if (output == null) {
			output = new InterleavedS16(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayS8} into a {@link boofcv.struct.image.GrayS32}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS32 convert( GrayS8 input, GrayS32 output ) {
		if (output == null) {
			output = new GrayS32(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedS8} into a {@link boofcv.struct.image.InterleavedS32}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS32 convert( InterleavedS8 input, InterleavedS32 output ) {
		if (output == null) {
			output = new InterleavedS32(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayS8} into a {@link boofcv.struct.image.GrayS64}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS64 convert( GrayS8 input, GrayS64 output ) {
		if (output == null) {
			output = new GrayS64(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedS8} into a {@link boofcv.struct.image.InterleavedS64}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS64 convert( InterleavedS8 input, InterleavedS64 output ) {
		if (output == null) {
			output = new InterleavedS64(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayS8} into a {@link boofcv.struct.image.GrayF32}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayF32 convert( GrayS8 input, GrayF32 output ) {
		if (output == null) {
			output = new GrayF32(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedS8} into a {@link boofcv.struct.image.InterleavedF32}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedF32 convert( InterleavedS8 input, InterleavedF32 output ) {
		if (output == null) {
			output = new InterleavedF32(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayS8} into a {@link boofcv.struct.image.GrayF64}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayF64 convert( GrayS8 input, GrayF64 output ) {
		if (output == null) {
			output = new GrayF64(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedS8} into a {@link boofcv.struct.image.InterleavedF64}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedF64 convert( InterleavedS8 input, InterleavedF64 output ) {
		if (output == null) {
			output = new InterleavedF64(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * Converts a {@link Planar} into a {@link ImageGray} by computing the average value of each pixel
	 * across all the bands.
	 *
	 * @param input Input Planar image that is being converted. Not modified.
	 * @param output (Optional) The single band output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS8 average( Planar<GrayS8> input , GrayS8 output ) {
		if (output == null) {
			output = new GrayS8(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		if( BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertPlanarToGray_MT.average(input, output);
		} else {
			ImplConvertPlanarToGray.average(input, output);
		}

		return output;
	}

	/**
	 * Converts a {@link Planar} into the equivalent {@link InterleavedS8}
	 *
	 * @param input (Input) Planar image that is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS8 convert( Planar<GrayS8> input , InterleavedS8 output ) {
		if (output == null) {
			output = new InterleavedS8(input.width, input.height,input.getNumBands());
		} else {
			output.reshapeTo(input);
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplConvertImage_MT.convert(input, output);
		} else {
			ImplConvertImage.convert(input, output);
		}

		return output;
	}

	/**
	 * Converts a {@link InterleavedS8} into a {@link GrayS8} by computing the average value of each pixel
	 * across all the bands.
	 *
	 * @param input (Input) The ImageInterleaved that is being converted. Not modified.
	 * @param output (Optional) The single band output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS8 average( InterleavedS8 input , GrayS8 output ) {
		if (output == null) {
			output = new GrayS8(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvertInterleavedToSingle_MT.average(input, output);
		} else {
			ConvertInterleavedToSingle.average(input, output);
		}

		return output;
	}

	/**
	 * Converts a {@link InterleavedS8} into the equivalent {@link Planar}
	 *
	 * @param input (Input) ImageInterleaved that is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static Planar<GrayS8> convert( InterleavedS8 input , Planar<GrayS8> output ) {
		if (output == null) {
			output = new Planar<>(GrayS8.class,input.width, input.height,input.numBands);
		} else {
			output.reshapeTo(input);
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplConvertImage_MT.convert(input, output);
		} else {
			ImplConvertImage.convert(input, output);
		}

		return output;
	}

	/**
	 * Converts pixel values in the input image into an integer values from 0 to numValues.
	 * @param input Input image
	 * @param min minimum input pixel value, inclusive
	 * @param max maximum input pixel value, inclusive
	 * @param numValues Number of possible pixel values in output image
	 * @param output (Optional) Storage for the output image. Can be null.
	 * @return The converted output image.
	 */
	public static GrayU8 convert( GrayS8 input, int min, int max, int numValues , GrayU8 output ) {
		if (output == null) {
			output = new GrayU8(input.width, input.height);
		} else {
			output.reshape(input.width,input.height);
		}
		if (numValues < 0 || numValues > 256)
			throw new IllegalArgumentException("0 <= numValues <= 256");

		numValues -= 1;
		int range = max-min;

		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + y*input.stride;
			int indexOut = output.startIndex + y*output.stride;

			for (int x = 0; x < input.width; x++) {
				int value = (int)(numValues*((input.data[indexIn++])-min)/range );
				output.data[indexOut++] = (byte)value;
			}
		}
	return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayU16} into a {@link boofcv.struct.image.GrayU8}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayU8 convert( GrayU16 input, GrayU8 output ) {
		if (output == null) {
			output = new GrayU8(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedU16} into a {@link boofcv.struct.image.InterleavedU8}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedU8 convert( InterleavedU16 input, InterleavedU8 output ) {
		if (output == null) {
			output = new InterleavedU8(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayU16} into a {@link boofcv.struct.image.GrayS8}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS8 convert( GrayU16 input, GrayS8 output ) {
		if (output == null) {
			output = new GrayS8(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedU16} into a {@link boofcv.struct.image.InterleavedS8}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS8 convert( InterleavedU16 input, InterleavedS8 output ) {
		if (output == null) {
			output = new InterleavedS8(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayU16} into a {@link boofcv.struct.image.GrayS16}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS16 convert( GrayU16 input, GrayS16 output ) {
		if (output == null) {
			output = new GrayS16(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedU16} into a {@link boofcv.struct.image.InterleavedS16}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS16 convert( InterleavedU16 input, InterleavedS16 output ) {
		if (output == null) {
			output = new InterleavedS16(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayU16} into a {@link boofcv.struct.image.GrayS32}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS32 convert( GrayU16 input, GrayS32 output ) {
		if (output == null) {
			output = new GrayS32(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedU16} into a {@link boofcv.struct.image.InterleavedS32}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS32 convert( InterleavedU16 input, InterleavedS32 output ) {
		if (output == null) {
			output = new InterleavedS32(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayU16} into a {@link boofcv.struct.image.GrayS64}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS64 convert( GrayU16 input, GrayS64 output ) {
		if (output == null) {
			output = new GrayS64(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedU16} into a {@link boofcv.struct.image.InterleavedS64}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS64 convert( InterleavedU16 input, InterleavedS64 output ) {
		if (output == null) {
			output = new InterleavedS64(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayU16} into a {@link boofcv.struct.image.GrayF32}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayF32 convert( GrayU16 input, GrayF32 output ) {
		if (output == null) {
			output = new GrayF32(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedU16} into a {@link boofcv.struct.image.InterleavedF32}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedF32 convert( InterleavedU16 input, InterleavedF32 output ) {
		if (output == null) {
			output = new InterleavedF32(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayU16} into a {@link boofcv.struct.image.GrayF64}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayF64 convert( GrayU16 input, GrayF64 output ) {
		if (output == null) {
			output = new GrayF64(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedU16} into a {@link boofcv.struct.image.InterleavedF64}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedF64 convert( InterleavedU16 input, InterleavedF64 output ) {
		if (output == null) {
			output = new InterleavedF64(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * Converts a {@link Planar} into a {@link ImageGray} by computing the average value of each pixel
	 * across all the bands.
	 *
	 * @param input Input Planar image that is being converted. Not modified.
	 * @param output (Optional) The single band output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayU16 average( Planar<GrayU16> input , GrayU16 output ) {
		if (output == null) {
			output = new GrayU16(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		if( BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertPlanarToGray_MT.average(input, output);
		} else {
			ImplConvertPlanarToGray.average(input, output);
		}

		return output;
	}

	/**
	 * Converts a {@link Planar} into the equivalent {@link InterleavedU16}
	 *
	 * @param input (Input) Planar image that is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedU16 convert( Planar<GrayU16> input , InterleavedU16 output ) {
		if (output == null) {
			output = new InterleavedU16(input.width, input.height,input.getNumBands());
		} else {
			output.reshapeTo(input);
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplConvertImage_MT.convert(input, output);
		} else {
			ImplConvertImage.convert(input, output);
		}

		return output;
	}

	/**
	 * Converts a {@link InterleavedU16} into a {@link GrayU16} by computing the average value of each pixel
	 * across all the bands.
	 *
	 * @param input (Input) The ImageInterleaved that is being converted. Not modified.
	 * @param output (Optional) The single band output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayU16 average( InterleavedU16 input , GrayU16 output ) {
		if (output == null) {
			output = new GrayU16(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvertInterleavedToSingle_MT.average(input, output);
		} else {
			ConvertInterleavedToSingle.average(input, output);
		}

		return output;
	}

	/**
	 * Converts a {@link InterleavedU16} into the equivalent {@link Planar}
	 *
	 * @param input (Input) ImageInterleaved that is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static Planar<GrayU16> convert( InterleavedU16 input , Planar<GrayU16> output ) {
		if (output == null) {
			output = new Planar<>(GrayU16.class,input.width, input.height,input.numBands);
		} else {
			output.reshapeTo(input);
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplConvertImage_MT.convert(input, output);
		} else {
			ImplConvertImage.convert(input, output);
		}

		return output;
	}

	/**
	 * Converts pixel values in the input image into an integer values from 0 to numValues.
	 * @param input Input image
	 * @param min minimum input pixel value, inclusive
	 * @param max maximum input pixel value, inclusive
	 * @param numValues Number of possible pixel values in output image
	 * @param output (Optional) Storage for the output image. Can be null.
	 * @return The converted output image.
	 */
	public static GrayU8 convert( GrayU16 input, int min, int max, int numValues , GrayU8 output ) {
		if (output == null) {
			output = new GrayU8(input.width, input.height);
		} else {
			output.reshape(input.width,input.height);
		}
		if (numValues < 0 || numValues > 256)
			throw new IllegalArgumentException("0 <= numValues <= 256");

		numValues -= 1;
		int range = max-min;

		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + y*input.stride;
			int indexOut = output.startIndex + y*output.stride;

			for (int x = 0; x < input.width; x++) {
				int value = (int)(numValues*((input.data[indexIn++]& 0xFFFF)-min)/range );
				output.data[indexOut++] = (byte)value;
			}
		}
	return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayS16} into a {@link boofcv.struct.image.GrayU8}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayU8 convert( GrayS16 input, GrayU8 output ) {
		if (output == null) {
			output = new GrayU8(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedS16} into a {@link boofcv.struct.image.InterleavedU8}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedU8 convert( InterleavedS16 input, InterleavedU8 output ) {
		if (output == null) {
			output = new InterleavedU8(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayS16} into a {@link boofcv.struct.image.GrayS8}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS8 convert( GrayS16 input, GrayS8 output ) {
		if (output == null) {
			output = new GrayS8(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedS16} into a {@link boofcv.struct.image.InterleavedS8}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS8 convert( InterleavedS16 input, InterleavedS8 output ) {
		if (output == null) {
			output = new InterleavedS8(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayS16} into a {@link boofcv.struct.image.GrayU16}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayU16 convert( GrayS16 input, GrayU16 output ) {
		if (output == null) {
			output = new GrayU16(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedS16} into a {@link boofcv.struct.image.InterleavedU16}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedU16 convert( InterleavedS16 input, InterleavedU16 output ) {
		if (output == null) {
			output = new InterleavedU16(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayS16} into a {@link boofcv.struct.image.GrayS32}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS32 convert( GrayS16 input, GrayS32 output ) {
		if (output == null) {
			output = new GrayS32(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedS16} into a {@link boofcv.struct.image.InterleavedS32}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS32 convert( InterleavedS16 input, InterleavedS32 output ) {
		if (output == null) {
			output = new InterleavedS32(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayS16} into a {@link boofcv.struct.image.GrayS64}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS64 convert( GrayS16 input, GrayS64 output ) {
		if (output == null) {
			output = new GrayS64(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedS16} into a {@link boofcv.struct.image.InterleavedS64}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS64 convert( InterleavedS16 input, InterleavedS64 output ) {
		if (output == null) {
			output = new InterleavedS64(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayS16} into a {@link boofcv.struct.image.GrayF32}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayF32 convert( GrayS16 input, GrayF32 output ) {
		if (output == null) {
			output = new GrayF32(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedS16} into a {@link boofcv.struct.image.InterleavedF32}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedF32 convert( InterleavedS16 input, InterleavedF32 output ) {
		if (output == null) {
			output = new InterleavedF32(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayS16} into a {@link boofcv.struct.image.GrayF64}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayF64 convert( GrayS16 input, GrayF64 output ) {
		if (output == null) {
			output = new GrayF64(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedS16} into a {@link boofcv.struct.image.InterleavedF64}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedF64 convert( InterleavedS16 input, InterleavedF64 output ) {
		if (output == null) {
			output = new InterleavedF64(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * Converts a {@link Planar} into a {@link ImageGray} by computing the average value of each pixel
	 * across all the bands.
	 *
	 * @param input Input Planar image that is being converted. Not modified.
	 * @param output (Optional) The single band output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS16 average( Planar<GrayS16> input , GrayS16 output ) {
		if (output == null) {
			output = new GrayS16(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		if( BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertPlanarToGray_MT.average(input, output);
		} else {
			ImplConvertPlanarToGray.average(input, output);
		}

		return output;
	}

	/**
	 * Converts a {@link Planar} into the equivalent {@link InterleavedS16}
	 *
	 * @param input (Input) Planar image that is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS16 convert( Planar<GrayS16> input , InterleavedS16 output ) {
		if (output == null) {
			output = new InterleavedS16(input.width, input.height,input.getNumBands());
		} else {
			output.reshapeTo(input);
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplConvertImage_MT.convert(input, output);
		} else {
			ImplConvertImage.convert(input, output);
		}

		return output;
	}

	/**
	 * Converts a {@link InterleavedS16} into a {@link GrayS16} by computing the average value of each pixel
	 * across all the bands.
	 *
	 * @param input (Input) The ImageInterleaved that is being converted. Not modified.
	 * @param output (Optional) The single band output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS16 average( InterleavedS16 input , GrayS16 output ) {
		if (output == null) {
			output = new GrayS16(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvertInterleavedToSingle_MT.average(input, output);
		} else {
			ConvertInterleavedToSingle.average(input, output);
		}

		return output;
	}

	/**
	 * Converts a {@link InterleavedS16} into the equivalent {@link Planar}
	 *
	 * @param input (Input) ImageInterleaved that is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static Planar<GrayS16> convert( InterleavedS16 input , Planar<GrayS16> output ) {
		if (output == null) {
			output = new Planar<>(GrayS16.class,input.width, input.height,input.numBands);
		} else {
			output.reshapeTo(input);
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplConvertImage_MT.convert(input, output);
		} else {
			ImplConvertImage.convert(input, output);
		}

		return output;
	}

	/**
	 * Converts pixel values in the input image into an integer values from 0 to numValues.
	 * @param input Input image
	 * @param min minimum input pixel value, inclusive
	 * @param max maximum input pixel value, inclusive
	 * @param numValues Number of possible pixel values in output image
	 * @param output (Optional) Storage for the output image. Can be null.
	 * @return The converted output image.
	 */
	public static GrayU8 convert( GrayS16 input, int min, int max, int numValues , GrayU8 output ) {
		if (output == null) {
			output = new GrayU8(input.width, input.height);
		} else {
			output.reshape(input.width,input.height);
		}
		if (numValues < 0 || numValues > 256)
			throw new IllegalArgumentException("0 <= numValues <= 256");

		numValues -= 1;
		int range = max-min;

		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + y*input.stride;
			int indexOut = output.startIndex + y*output.stride;

			for (int x = 0; x < input.width; x++) {
				int value = (int)(numValues*((input.data[indexIn++])-min)/range );
				output.data[indexOut++] = (byte)value;
			}
		}
	return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayS32} into a {@link boofcv.struct.image.GrayU8}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayU8 convert( GrayS32 input, GrayU8 output ) {
		if (output == null) {
			output = new GrayU8(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedS32} into a {@link boofcv.struct.image.InterleavedU8}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedU8 convert( InterleavedS32 input, InterleavedU8 output ) {
		if (output == null) {
			output = new InterleavedU8(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayS32} into a {@link boofcv.struct.image.GrayS8}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS8 convert( GrayS32 input, GrayS8 output ) {
		if (output == null) {
			output = new GrayS8(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedS32} into a {@link boofcv.struct.image.InterleavedS8}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS8 convert( InterleavedS32 input, InterleavedS8 output ) {
		if (output == null) {
			output = new InterleavedS8(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayS32} into a {@link boofcv.struct.image.GrayU16}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayU16 convert( GrayS32 input, GrayU16 output ) {
		if (output == null) {
			output = new GrayU16(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedS32} into a {@link boofcv.struct.image.InterleavedU16}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedU16 convert( InterleavedS32 input, InterleavedU16 output ) {
		if (output == null) {
			output = new InterleavedU16(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayS32} into a {@link boofcv.struct.image.GrayS16}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS16 convert( GrayS32 input, GrayS16 output ) {
		if (output == null) {
			output = new GrayS16(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedS32} into a {@link boofcv.struct.image.InterleavedS16}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS16 convert( InterleavedS32 input, InterleavedS16 output ) {
		if (output == null) {
			output = new InterleavedS16(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayS32} into a {@link boofcv.struct.image.GrayS64}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS64 convert( GrayS32 input, GrayS64 output ) {
		if (output == null) {
			output = new GrayS64(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedS32} into a {@link boofcv.struct.image.InterleavedS64}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS64 convert( InterleavedS32 input, InterleavedS64 output ) {
		if (output == null) {
			output = new InterleavedS64(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayS32} into a {@link boofcv.struct.image.GrayF32}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayF32 convert( GrayS32 input, GrayF32 output ) {
		if (output == null) {
			output = new GrayF32(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedS32} into a {@link boofcv.struct.image.InterleavedF32}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedF32 convert( InterleavedS32 input, InterleavedF32 output ) {
		if (output == null) {
			output = new InterleavedF32(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayS32} into a {@link boofcv.struct.image.GrayF64}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayF64 convert( GrayS32 input, GrayF64 output ) {
		if (output == null) {
			output = new GrayF64(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedS32} into a {@link boofcv.struct.image.InterleavedF64}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedF64 convert( InterleavedS32 input, InterleavedF64 output ) {
		if (output == null) {
			output = new InterleavedF64(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * Converts a {@link Planar} into a {@link ImageGray} by computing the average value of each pixel
	 * across all the bands.
	 *
	 * @param input Input Planar image that is being converted. Not modified.
	 * @param output (Optional) The single band output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS32 average( Planar<GrayS32> input , GrayS32 output ) {
		if (output == null) {
			output = new GrayS32(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		if( BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertPlanarToGray_MT.average(input, output);
		} else {
			ImplConvertPlanarToGray.average(input, output);
		}

		return output;
	}

	/**
	 * Converts a {@link Planar} into the equivalent {@link InterleavedS32}
	 *
	 * @param input (Input) Planar image that is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS32 convert( Planar<GrayS32> input , InterleavedS32 output ) {
		if (output == null) {
			output = new InterleavedS32(input.width, input.height,input.getNumBands());
		} else {
			output.reshapeTo(input);
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplConvertImage_MT.convert(input, output);
		} else {
			ImplConvertImage.convert(input, output);
		}

		return output;
	}

	/**
	 * Converts a {@link InterleavedS32} into a {@link GrayS32} by computing the average value of each pixel
	 * across all the bands.
	 *
	 * @param input (Input) The ImageInterleaved that is being converted. Not modified.
	 * @param output (Optional) The single band output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS32 average( InterleavedS32 input , GrayS32 output ) {
		if (output == null) {
			output = new GrayS32(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvertInterleavedToSingle_MT.average(input, output);
		} else {
			ConvertInterleavedToSingle.average(input, output);
		}

		return output;
	}

	/**
	 * Converts a {@link InterleavedS32} into the equivalent {@link Planar}
	 *
	 * @param input (Input) ImageInterleaved that is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static Planar<GrayS32> convert( InterleavedS32 input , Planar<GrayS32> output ) {
		if (output == null) {
			output = new Planar<>(GrayS32.class,input.width, input.height,input.numBands);
		} else {
			output.reshapeTo(input);
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplConvertImage_MT.convert(input, output);
		} else {
			ImplConvertImage.convert(input, output);
		}

		return output;
	}

	/**
	 * Converts pixel values in the input image into an integer values from 0 to numValues.
	 * @param input Input image
	 * @param min minimum input pixel value, inclusive
	 * @param max maximum input pixel value, inclusive
	 * @param numValues Number of possible pixel values in output image
	 * @param output (Optional) Storage for the output image. Can be null.
	 * @return The converted output image.
	 */
	public static GrayU8 convert( GrayS32 input, int min, int max, int numValues , GrayU8 output ) {
		if (output == null) {
			output = new GrayU8(input.width, input.height);
		} else {
			output.reshape(input.width,input.height);
		}
		if (numValues < 0 || numValues > 256)
			throw new IllegalArgumentException("0 <= numValues <= 256");

		numValues -= 1;
		int range = max-min;

		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + y*input.stride;
			int indexOut = output.startIndex + y*output.stride;

			for (int x = 0; x < input.width; x++) {
				int value = (int)(numValues*((input.data[indexIn++])-min)/range );
				output.data[indexOut++] = (byte)value;
			}
		}
	return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayS64} into a {@link boofcv.struct.image.GrayU8}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayU8 convert( GrayS64 input, GrayU8 output ) {
		if (output == null) {
			output = new GrayU8(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedS64} into a {@link boofcv.struct.image.InterleavedU8}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedU8 convert( InterleavedS64 input, InterleavedU8 output ) {
		if (output == null) {
			output = new InterleavedU8(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayS64} into a {@link boofcv.struct.image.GrayS8}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS8 convert( GrayS64 input, GrayS8 output ) {
		if (output == null) {
			output = new GrayS8(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedS64} into a {@link boofcv.struct.image.InterleavedS8}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS8 convert( InterleavedS64 input, InterleavedS8 output ) {
		if (output == null) {
			output = new InterleavedS8(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayS64} into a {@link boofcv.struct.image.GrayU16}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayU16 convert( GrayS64 input, GrayU16 output ) {
		if (output == null) {
			output = new GrayU16(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedS64} into a {@link boofcv.struct.image.InterleavedU16}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedU16 convert( InterleavedS64 input, InterleavedU16 output ) {
		if (output == null) {
			output = new InterleavedU16(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayS64} into a {@link boofcv.struct.image.GrayS16}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS16 convert( GrayS64 input, GrayS16 output ) {
		if (output == null) {
			output = new GrayS16(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedS64} into a {@link boofcv.struct.image.InterleavedS16}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS16 convert( InterleavedS64 input, InterleavedS16 output ) {
		if (output == null) {
			output = new InterleavedS16(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayS64} into a {@link boofcv.struct.image.GrayS32}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS32 convert( GrayS64 input, GrayS32 output ) {
		if (output == null) {
			output = new GrayS32(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedS64} into a {@link boofcv.struct.image.InterleavedS32}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS32 convert( InterleavedS64 input, InterleavedS32 output ) {
		if (output == null) {
			output = new InterleavedS32(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayS64} into a {@link boofcv.struct.image.GrayF32}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayF32 convert( GrayS64 input, GrayF32 output ) {
		if (output == null) {
			output = new GrayF32(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedS64} into a {@link boofcv.struct.image.InterleavedF32}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedF32 convert( InterleavedS64 input, InterleavedF32 output ) {
		if (output == null) {
			output = new InterleavedF32(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayS64} into a {@link boofcv.struct.image.GrayF64}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayF64 convert( GrayS64 input, GrayF64 output ) {
		if (output == null) {
			output = new GrayF64(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedS64} into a {@link boofcv.struct.image.InterleavedF64}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedF64 convert( InterleavedS64 input, InterleavedF64 output ) {
		if (output == null) {
			output = new InterleavedF64(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * Converts a {@link Planar} into a {@link ImageGray} by computing the average value of each pixel
	 * across all the bands.
	 *
	 * @param input Input Planar image that is being converted. Not modified.
	 * @param output (Optional) The single band output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS64 average( Planar<GrayS64> input , GrayS64 output ) {
		if (output == null) {
			output = new GrayS64(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		if( BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertPlanarToGray_MT.average(input, output);
		} else {
			ImplConvertPlanarToGray.average(input, output);
		}

		return output;
	}

	/**
	 * Converts a {@link Planar} into the equivalent {@link InterleavedS64}
	 *
	 * @param input (Input) Planar image that is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS64 convert( Planar<GrayS64> input , InterleavedS64 output ) {
		if (output == null) {
			output = new InterleavedS64(input.width, input.height,input.getNumBands());
		} else {
			output.reshapeTo(input);
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplConvertImage_MT.convert(input, output);
		} else {
			ImplConvertImage.convert(input, output);
		}

		return output;
	}

	/**
	 * Converts a {@link InterleavedS64} into a {@link GrayS64} by computing the average value of each pixel
	 * across all the bands.
	 *
	 * @param input (Input) The ImageInterleaved that is being converted. Not modified.
	 * @param output (Optional) The single band output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS64 average( InterleavedS64 input , GrayS64 output ) {
		if (output == null) {
			output = new GrayS64(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvertInterleavedToSingle_MT.average(input, output);
		} else {
			ConvertInterleavedToSingle.average(input, output);
		}

		return output;
	}

	/**
	 * Converts a {@link InterleavedS64} into the equivalent {@link Planar}
	 *
	 * @param input (Input) ImageInterleaved that is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static Planar<GrayS64> convert( InterleavedS64 input , Planar<GrayS64> output ) {
		if (output == null) {
			output = new Planar<>(GrayS64.class,input.width, input.height,input.numBands);
		} else {
			output.reshapeTo(input);
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplConvertImage_MT.convert(input, output);
		} else {
			ImplConvertImage.convert(input, output);
		}

		return output;
	}

	/**
	 * Converts pixel values in the input image into an integer values from 0 to numValues.
	 * @param input Input image
	 * @param min minimum input pixel value, inclusive
	 * @param max maximum input pixel value, inclusive
	 * @param numValues Number of possible pixel values in output image
	 * @param output (Optional) Storage for the output image. Can be null.
	 * @return The converted output image.
	 */
	public static GrayU8 convert( GrayS64 input, long min, long max, int numValues , GrayU8 output ) {
		if (output == null) {
			output = new GrayU8(input.width, input.height);
		} else {
			output.reshape(input.width,input.height);
		}
		if (numValues < 0 || numValues > 256)
			throw new IllegalArgumentException("0 <= numValues <= 256");

		numValues -= 1;
		long range = max-min;

		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + y*input.stride;
			int indexOut = output.startIndex + y*output.stride;

			for (int x = 0; x < input.width; x++) {
				int value = (int)(numValues*((input.data[indexIn++])-min)/range );
				output.data[indexOut++] = (byte)value;
			}
		}
	return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayF32} into a {@link boofcv.struct.image.GrayU8}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayU8 convert( GrayF32 input, GrayU8 output ) {
		if (output == null) {
			output = new GrayU8(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedF32} into a {@link boofcv.struct.image.InterleavedU8}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedU8 convert( InterleavedF32 input, InterleavedU8 output ) {
		if (output == null) {
			output = new InterleavedU8(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayF32} into a {@link boofcv.struct.image.GrayS8}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS8 convert( GrayF32 input, GrayS8 output ) {
		if (output == null) {
			output = new GrayS8(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedF32} into a {@link boofcv.struct.image.InterleavedS8}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS8 convert( InterleavedF32 input, InterleavedS8 output ) {
		if (output == null) {
			output = new InterleavedS8(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayF32} into a {@link boofcv.struct.image.GrayU16}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayU16 convert( GrayF32 input, GrayU16 output ) {
		if (output == null) {
			output = new GrayU16(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedF32} into a {@link boofcv.struct.image.InterleavedU16}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedU16 convert( InterleavedF32 input, InterleavedU16 output ) {
		if (output == null) {
			output = new InterleavedU16(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayF32} into a {@link boofcv.struct.image.GrayS16}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS16 convert( GrayF32 input, GrayS16 output ) {
		if (output == null) {
			output = new GrayS16(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedF32} into a {@link boofcv.struct.image.InterleavedS16}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS16 convert( InterleavedF32 input, InterleavedS16 output ) {
		if (output == null) {
			output = new InterleavedS16(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayF32} into a {@link boofcv.struct.image.GrayS32}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS32 convert( GrayF32 input, GrayS32 output ) {
		if (output == null) {
			output = new GrayS32(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedF32} into a {@link boofcv.struct.image.InterleavedS32}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS32 convert( InterleavedF32 input, InterleavedS32 output ) {
		if (output == null) {
			output = new InterleavedS32(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayF32} into a {@link boofcv.struct.image.GrayS64}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS64 convert( GrayF32 input, GrayS64 output ) {
		if (output == null) {
			output = new GrayS64(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedF32} into a {@link boofcv.struct.image.InterleavedS64}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS64 convert( InterleavedF32 input, InterleavedS64 output ) {
		if (output == null) {
			output = new InterleavedS64(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayF32} into a {@link boofcv.struct.image.GrayF64}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayF64 convert( GrayF32 input, GrayF64 output ) {
		if (output == null) {
			output = new GrayF64(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedF32} into a {@link boofcv.struct.image.InterleavedF64}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedF64 convert( InterleavedF32 input, InterleavedF64 output ) {
		if (output == null) {
			output = new InterleavedF64(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * Converts a {@link Planar} into a {@link ImageGray} by computing the average value of each pixel
	 * across all the bands.
	 *
	 * @param input Input Planar image that is being converted. Not modified.
	 * @param output (Optional) The single band output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayF32 average( Planar<GrayF32> input , GrayF32 output ) {
		if (output == null) {
			output = new GrayF32(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		if( BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertPlanarToGray_MT.average(input, output);
		} else {
			ImplConvertPlanarToGray.average(input, output);
		}

		return output;
	}

	/**
	 * Converts a {@link Planar} into the equivalent {@link InterleavedF32}
	 *
	 * @param input (Input) Planar image that is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedF32 convert( Planar<GrayF32> input , InterleavedF32 output ) {
		if (output == null) {
			output = new InterleavedF32(input.width, input.height,input.getNumBands());
		} else {
			output.reshapeTo(input);
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplConvertImage_MT.convert(input, output);
		} else {
			ImplConvertImage.convert(input, output);
		}

		return output;
	}

	/**
	 * Converts a {@link InterleavedF32} into a {@link GrayF32} by computing the average value of each pixel
	 * across all the bands.
	 *
	 * @param input (Input) The ImageInterleaved that is being converted. Not modified.
	 * @param output (Optional) The single band output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayF32 average( InterleavedF32 input , GrayF32 output ) {
		if (output == null) {
			output = new GrayF32(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvertInterleavedToSingle_MT.average(input, output);
		} else {
			ConvertInterleavedToSingle.average(input, output);
		}

		return output;
	}

	/**
	 * Converts a {@link InterleavedF32} into the equivalent {@link Planar}
	 *
	 * @param input (Input) ImageInterleaved that is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static Planar<GrayF32> convert( InterleavedF32 input , Planar<GrayF32> output ) {
		if (output == null) {
			output = new Planar<>(GrayF32.class,input.width, input.height,input.numBands);
		} else {
			output.reshapeTo(input);
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplConvertImage_MT.convert(input, output);
		} else {
			ImplConvertImage.convert(input, output);
		}

		return output;
	}

	/**
	 * Converts pixel values in the input image into an integer values from 0 to numValues.
	 * @param input Input image
	 * @param min minimum input pixel value, inclusive
	 * @param max maximum input pixel value, inclusive
	 * @param numValues Number of possible pixel values in output image
	 * @param output (Optional) Storage for the output image. Can be null.
	 * @return The converted output image.
	 */
	public static GrayU8 convert( GrayF32 input, float min, float max, int numValues , GrayU8 output ) {
		if (output == null) {
			output = new GrayU8(input.width, input.height);
		} else {
			output.reshape(input.width,input.height);
		}
		if (numValues < 0 || numValues > 256)
			throw new IllegalArgumentException("0 <= numValues <= 256");

		numValues -= 1;
		float range = max-min;

		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + y*input.stride;
			int indexOut = output.startIndex + y*output.stride;

			for (int x = 0; x < input.width; x++) {
				int value = (int)(numValues*((input.data[indexIn++])-min)/range + 0.5f);
				output.data[indexOut++] = (byte)value;
			}
		}
	return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayF64} into a {@link boofcv.struct.image.GrayU8}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayU8 convert( GrayF64 input, GrayU8 output ) {
		if (output == null) {
			output = new GrayU8(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedF64} into a {@link boofcv.struct.image.InterleavedU8}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedU8 convert( InterleavedF64 input, InterleavedU8 output ) {
		if (output == null) {
			output = new InterleavedU8(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayF64} into a {@link boofcv.struct.image.GrayS8}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS8 convert( GrayF64 input, GrayS8 output ) {
		if (output == null) {
			output = new GrayS8(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedF64} into a {@link boofcv.struct.image.InterleavedS8}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS8 convert( InterleavedF64 input, InterleavedS8 output ) {
		if (output == null) {
			output = new InterleavedS8(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayF64} into a {@link boofcv.struct.image.GrayU16}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayU16 convert( GrayF64 input, GrayU16 output ) {
		if (output == null) {
			output = new GrayU16(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedF64} into a {@link boofcv.struct.image.InterleavedU16}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedU16 convert( InterleavedF64 input, InterleavedU16 output ) {
		if (output == null) {
			output = new InterleavedU16(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayF64} into a {@link boofcv.struct.image.GrayS16}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS16 convert( GrayF64 input, GrayS16 output ) {
		if (output == null) {
			output = new GrayS16(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedF64} into a {@link boofcv.struct.image.InterleavedS16}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS16 convert( InterleavedF64 input, InterleavedS16 output ) {
		if (output == null) {
			output = new InterleavedS16(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayF64} into a {@link boofcv.struct.image.GrayS32}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS32 convert( GrayF64 input, GrayS32 output ) {
		if (output == null) {
			output = new GrayS32(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedF64} into a {@link boofcv.struct.image.InterleavedS32}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS32 convert( InterleavedF64 input, InterleavedS32 output ) {
		if (output == null) {
			output = new InterleavedS32(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayF64} into a {@link boofcv.struct.image.GrayS64}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayS64 convert( GrayF64 input, GrayS64 output ) {
		if (output == null) {
			output = new GrayS64(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedF64} into a {@link boofcv.struct.image.InterleavedS64}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedS64 convert( InterleavedF64 input, InterleavedS64 output ) {
		if (output == null) {
			output = new InterleavedS64(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.GrayF64} into a {@link boofcv.struct.image.GrayF32}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayF32 convert( GrayF64 input, GrayF32 output ) {
		if (output == null) {
			output = new GrayF32(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * <p>
	 * Converts an {@link boofcv.struct.image.InterleavedF64} into a {@link boofcv.struct.image.InterleavedF32}.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedF32 convert( InterleavedF64 input, InterleavedF32 output ) {
		if (output == null) {
			output = new InterleavedF32(input.width, input.height, input.numBands);
		} else {
			output.reshapeTo(input);
		}

		// threaded code is not significantly faster here
		ImplConvertImage.convert(input, output);

		return output;
	}

	/**
	 * Converts a {@link Planar} into a {@link ImageGray} by computing the average value of each pixel
	 * across all the bands.
	 *
	 * @param input Input Planar image that is being converted. Not modified.
	 * @param output (Optional) The single band output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayF64 average( Planar<GrayF64> input , GrayF64 output ) {
		if (output == null) {
			output = new GrayF64(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		if( BoofConcurrency.USE_CONCURRENT ) {
			ImplConvertPlanarToGray_MT.average(input, output);
		} else {
			ImplConvertPlanarToGray.average(input, output);
		}

		return output;
	}

	/**
	 * Converts a {@link Planar} into the equivalent {@link InterleavedF64}
	 *
	 * @param input (Input) Planar image that is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedF64 convert( Planar<GrayF64> input , InterleavedF64 output ) {
		if (output == null) {
			output = new InterleavedF64(input.width, input.height,input.getNumBands());
		} else {
			output.reshapeTo(input);
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplConvertImage_MT.convert(input, output);
		} else {
			ImplConvertImage.convert(input, output);
		}

		return output;
	}

	/**
	 * Converts a {@link InterleavedF64} into a {@link GrayF64} by computing the average value of each pixel
	 * across all the bands.
	 *
	 * @param input (Input) The ImageInterleaved that is being converted. Not modified.
	 * @param output (Optional) The single band output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static GrayF64 average( InterleavedF64 input , GrayF64 output ) {
		if (output == null) {
			output = new GrayF64(input.width, input.height);
		} else {
			output.reshapeTo(input);
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvertInterleavedToSingle_MT.average(input, output);
		} else {
			ConvertInterleavedToSingle.average(input, output);
		}

		return output;
	}

	/**
	 * Converts a {@link InterleavedF64} into the equivalent {@link Planar}
	 *
	 * @param input (Input) ImageInterleaved that is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static Planar<GrayF64> convert( InterleavedF64 input , Planar<GrayF64> output ) {
		if (output == null) {
			output = new Planar<>(GrayF64.class,input.width, input.height,input.numBands);
		} else {
			output.reshapeTo(input);
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplConvertImage_MT.convert(input, output);
		} else {
			ImplConvertImage.convert(input, output);
		}

		return output;
	}

	/**
	 * Converts pixel values in the input image into an integer values from 0 to numValues.
	 * @param input Input image
	 * @param min minimum input pixel value, inclusive
	 * @param max maximum input pixel value, inclusive
	 * @param numValues Number of possible pixel values in output image
	 * @param output (Optional) Storage for the output image. Can be null.
	 * @return The converted output image.
	 */
	public static GrayU8 convert( GrayF64 input, double min, double max, int numValues , GrayU8 output ) {
		if (output == null) {
			output = new GrayU8(input.width, input.height);
		} else {
			output.reshape(input.width,input.height);
		}
		if (numValues < 0 || numValues > 256)
			throw new IllegalArgumentException("0 <= numValues <= 256");

		numValues -= 1;
		double range = max-min;

		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + y*input.stride;
			int indexOut = output.startIndex + y*output.stride;

			for (int x = 0; x < input.width; x++) {
				int value = (int)(numValues*((input.data[indexIn++])-min)/range + 0.5);
				output.data[indexOut++] = (byte)value;
			}
		}
	return output;
	}

	/**
	 * Converts a {@link InterleavedU8} into the equivalent {@link Planar}
	 *
	 * @param input (Input) ImageInterleaved that is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static Planar<GrayF32> convertU8F32( InterleavedU8 input , Planar<GrayF32> output ) {
		if (output == null) {
			output = new Planar<>(GrayF32.class,input.width, input.height,input.numBands);
		} else {
			output.reshapeTo(input);
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplConvertImage_MT.convertU8F32(input, output);
		} else {
			ImplConvertImage.convertU8F32(input, output);
		}

		return output;
	}

	/**
	 * Converts a {@link InterleavedF32} into the equivalent {@link Planar}
	 *
	 * @param input (Input) ImageInterleaved that is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static Planar<GrayU8> convertF32U8( InterleavedF32 input , Planar<GrayU8> output ) {
		if (output == null) {
			output = new Planar<>(GrayU8.class,input.width, input.height,input.numBands);
		} else {
			output.reshapeTo(input);
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplConvertImage_MT.convertF32U8(input, output);
		} else {
			ImplConvertImage.convertF32U8(input, output);
		}

		return output;
	}

	/**
	 * Converts a {@link Planar} into the equivalent {@link InterleavedF32}
	 *
	 * @param input (Input) Planar image that is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedF32 convertU8F32( Planar<GrayU8> input , InterleavedF32 output ) {
		if (output == null) {
			output = new InterleavedF32(input.width, input.height,input.getNumBands());
		} else {
			output.reshapeTo(input);
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplConvertImage_MT.convertU8F32(input, output);
		} else {
			ImplConvertImage.convertU8F32(input, output);
		}

		return output;
	}

	/**
	 * Converts a {@link Planar} into the equivalent {@link InterleavedU8}
	 *
	 * @param input (Input) Planar image that is being converted. Not modified.
	 * @param output (Optional) The output image. If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static InterleavedU8 convertF32U8( Planar<GrayF32> input , InterleavedU8 output ) {
		if (output == null) {
			output = new InterleavedU8(input.width, input.height,input.getNumBands());
		} else {
			output.reshapeTo(input);
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplConvertImage_MT.convertF32U8(input, output);
		} else {
			ImplConvertImage.convertF32U8(input, output);
		}

		return output;
	}


}
