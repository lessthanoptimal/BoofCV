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

package boofcv.alg.misc;

import boofcv.struct.image.*;

/**
 * Generalized version of {@link ImageStatistics}.  Type checking is performed at runtime instead of at compile type.
 *
 * @author Peter Abeles
 */
public class GImageStatistics {
	/**
	 * Returns the absolute value of the element with the largest absolute value, across all bands
	 *
	 * @param input Input image. Not modified.
	 * @return Largest pixel absolute value.
	 */
	public static double maxAbs( ImageBase input ) {
		if( input instanceof ImageGray) {
			if (GrayU8.class == input.getClass()) {
				return ImageStatistics.maxAbs((GrayU8) input);
			} else if (GrayS8.class == input.getClass()) {
				return ImageStatistics.maxAbs((GrayS8) input);
			} else if (GrayU16.class == input.getClass()) {
				return ImageStatistics.maxAbs((GrayU16) input);
			} else if (GrayS16.class == input.getClass()) {
				return ImageStatistics.maxAbs((GrayS16) input);
			} else if (GrayS32.class == input.getClass()) {
				return ImageStatistics.maxAbs((GrayS32) input);
			} else if (GrayS64.class == input.getClass()) {
				return ImageStatistics.maxAbs((GrayS64) input);
			} else if (GrayF32.class == input.getClass()) {
				return ImageStatistics.maxAbs((GrayF32) input);
			} else if (GrayF64.class == input.getClass()) {
				return ImageStatistics.maxAbs((GrayF64) input);
			} else {
				throw new IllegalArgumentException("Unknown Image Type: " + input.getClass().getSimpleName());
			}
		} else if( input instanceof ImageInterleaved ) {
			if (InterleavedU8.class == input.getClass()) {
				return ImageStatistics.maxAbs((InterleavedU8) input);
			} else if (InterleavedS8.class == input.getClass()) {
				return ImageStatistics.maxAbs((InterleavedS8) input);
			} else if (InterleavedU16.class == input.getClass()) {
				return ImageStatistics.maxAbs((InterleavedU16) input);
			} else if (InterleavedS16.class == input.getClass()) {
				return ImageStatistics.maxAbs((InterleavedS16) input);
			} else if (InterleavedS32.class == input.getClass()) {
				return ImageStatistics.maxAbs((InterleavedS32) input);
			} else if (InterleavedS64.class == input.getClass()) {
				return ImageStatistics.maxAbs((InterleavedS64) input);
			} else if (InterleavedF32.class == input.getClass()) {
				return ImageStatistics.maxAbs((InterleavedF32) input);
			} else if (InterleavedF64.class == input.getClass()) {
				return ImageStatistics.maxAbs((InterleavedF64) input);
			} else {
				throw new IllegalArgumentException("Unknown Image Type: " + input.getClass().getSimpleName());
			}
		} else {
			throw new IllegalArgumentException("Planar image support needs to be added");
		}
	}

	/**
	 * Returns the maximum pixel value across all bands.
	 *
	 * @param input Input image. Not modified.
	 * @return Maximum pixel value.
	 */
	public static double max( ImageBase input ) {
		if( input instanceof ImageGray) {
			if (GrayU8.class == input.getClass()) {
				return ImageStatistics.max((GrayU8) input);
			} else if (GrayS8.class == input.getClass()) {
				return ImageStatistics.max((GrayS8) input);
			} else if (GrayU16.class == input.getClass()) {
				return ImageStatistics.max((GrayU16) input);
			} else if (GrayS16.class == input.getClass()) {
				return ImageStatistics.max((GrayS16) input);
			} else if (GrayS32.class == input.getClass()) {
				return ImageStatistics.max((GrayS32) input);
			} else if (GrayS64.class == input.getClass()) {
				return ImageStatistics.max((GrayS64) input);
			} else if (GrayF32.class == input.getClass()) {
				return ImageStatistics.max((GrayF32) input);
			} else if (GrayF64.class == input.getClass()) {
				return ImageStatistics.max((GrayF64) input);
			} else {
				throw new IllegalArgumentException("Unknown Image Type");
			}
		} else if( input instanceof ImageInterleaved ) {
			if (InterleavedU8.class == input.getClass()) {
				return ImageStatistics.max((InterleavedU8) input);
			} else if (InterleavedS8.class == input.getClass()) {
				return ImageStatistics.max((InterleavedS8) input);
			} else if (InterleavedU16.class == input.getClass()) {
				return ImageStatistics.max((InterleavedU16) input);
			} else if (InterleavedS16.class == input.getClass()) {
				return ImageStatistics.max((InterleavedS16) input);
			} else if (InterleavedS32.class == input.getClass()) {
				return ImageStatistics.max((InterleavedS32) input);
			} else if (InterleavedS64.class == input.getClass()) {
				return ImageStatistics.max((InterleavedS64) input);
			} else if (InterleavedF32.class == input.getClass()) {
				return ImageStatistics.max((InterleavedF32) input);
			} else if (InterleavedF64.class == input.getClass()) {
				return ImageStatistics.max((InterleavedF64) input);
			} else {
				throw new IllegalArgumentException("Unknown Image Type");
			}
		} else {
			throw new IllegalArgumentException("Planar image support needs to be added");
		}
	}

	/**
	 * Returns the minimum pixel value across all bands
	 *
	 * @param input Input image. Not modified.
	 * @return Minimum pixel value.
	 */
	public static double min( ImageBase input ) {
		if( input instanceof ImageGray) {
			if (GrayU8.class == input.getClass()) {
				return ImageStatistics.min((GrayU8) input);
			} else if (GrayS8.class == input.getClass()) {
				return ImageStatistics.min((GrayS8) input);
			} else if (GrayU16.class == input.getClass()) {
				return ImageStatistics.min((GrayU16) input);
			} else if (GrayS16.class == input.getClass()) {
				return ImageStatistics.min((GrayS16) input);
			} else if (GrayS32.class == input.getClass()) {
				return ImageStatistics.min((GrayS32) input);
			} else if (GrayS64.class == input.getClass()) {
				return ImageStatistics.min((GrayS64) input);
			} else if (GrayF32.class == input.getClass()) {
				return ImageStatistics.min((GrayF32) input);
			} else if (GrayF64.class == input.getClass()) {
				return ImageStatistics.min((GrayF64) input);
			} else {
				throw new IllegalArgumentException("Unknown Image Type: " + input.getClass().getSimpleName());
			}
		} else if( input instanceof ImageInterleaved ) {
			if (InterleavedU8.class == input.getClass()) {
				return ImageStatistics.min((InterleavedU8) input);
			} else if (InterleavedS8.class == input.getClass()) {
				return ImageStatistics.min((InterleavedS8) input);
			} else if (InterleavedU16.class == input.getClass()) {
				return ImageStatistics.min((InterleavedU16) input);
			} else if (InterleavedS16.class == input.getClass()) {
				return ImageStatistics.min((InterleavedS16) input);
			} else if (InterleavedS32.class == input.getClass()) {
				return ImageStatistics.min((InterleavedS32) input);
			} else if (InterleavedS64.class == input.getClass()) {
				return ImageStatistics.min((InterleavedS64) input);
			} else if (InterleavedF32.class == input.getClass()) {
				return ImageStatistics.min((InterleavedF32) input);
			} else if (InterleavedF64.class == input.getClass()) {
				return ImageStatistics.min((InterleavedF64) input);
			} else {
				throw new IllegalArgumentException("Unknown Image Type: " + input.getClass().getSimpleName());
			}
		} else {
			throw new IllegalArgumentException("Planar image support needs to be added");
		}
	}

	/**
	 * <p>
	 * Returns the sum of all the pixels in the image across all bands.
	 * </p>
	 *
	 * @param input Input image. Not modified.
	 * @return Sum of pixel intensity values
	 */
	public static double sum( ImageBase input ) {

		if( input instanceof ImageGray) {
			if (GrayU8.class == input.getClass()) {
				return ImageStatistics.sum((GrayU8) input);
			} else if (GrayS8.class == input.getClass()) {
				return ImageStatistics.sum((GrayS8) input);
			} else if (GrayU16.class == input.getClass()) {
				return ImageStatistics.sum((GrayU16) input);
			} else if (GrayS16.class == input.getClass()) {
				return ImageStatistics.sum((GrayS16) input);
			} else if (GrayS32.class == input.getClass()) {
				return ImageStatistics.sum((GrayS32) input);
			} else if (GrayS64.class == input.getClass()) {
				return ImageStatistics.sum((GrayS64) input);
			} else if (GrayF32.class == input.getClass()) {
				return ImageStatistics.sum((GrayF32) input);
			} else if (GrayF64.class == input.getClass()) {
				return ImageStatistics.sum((GrayF64) input);
			} else {
				throw new IllegalArgumentException("Unknown image Type");
			}
		} else if( input instanceof ImageInterleaved ) {
			if (InterleavedU8.class == input.getClass()) {
				return ImageStatistics.sum((InterleavedU8) input);
			} else if (InterleavedS8.class == input.getClass()) {
				return ImageStatistics.sum((InterleavedS8) input);
			} else if (InterleavedU16.class == input.getClass()) {
				return ImageStatistics.sum((InterleavedU16) input);
			} else if (InterleavedS16.class == input.getClass()) {
				return ImageStatistics.sum((InterleavedS16) input);
			} else if (InterleavedS32.class == input.getClass()) {
				return ImageStatistics.sum((InterleavedS32) input);
			} else if (InterleavedS64.class == input.getClass()) {
				return ImageStatistics.sum((InterleavedS64) input);
			} else if (InterleavedF32.class == input.getClass()) {
				return ImageStatistics.sum((InterleavedF32) input);
			} else if (InterleavedF64.class == input.getClass()) {
				return ImageStatistics.sum((InterleavedF64) input);
			} else {
				throw new IllegalArgumentException("Unknown image Type");
			}
		} else if( input instanceof Planar ) {
			double sum = 0;
			Planar in = (Planar) input;
			for (int i = 0; i < in.getNumBands(); i++) {
				sum += sum( in.getBand(i));
			}
			return sum;
		} else {
			throw new IllegalArgumentException("Planar image support needs to be added");
		}
	}

	/**
	 * Returns the mean pixel intensity value.
	 *
	 * @param input Input image. Not modified.
	 * @return Mean pixel value
	 */
	public static double mean( ImageBase input ) {

		if( input instanceof ImageGray) {
			if (GrayU8.class == input.getClass()) {
				return ImageStatistics.mean((GrayU8) input);
			} else if (GrayS8.class == input.getClass()) {
				return ImageStatistics.mean((GrayS8) input);
			} else if (GrayU16.class == input.getClass()) {
				return ImageStatistics.mean((GrayU16) input);
			} else if (GrayS16.class == input.getClass()) {
				return ImageStatistics.mean((GrayS16) input);
			} else if (GrayS32.class == input.getClass()) {
				return ImageStatistics.mean((GrayS32) input);
			} else if (GrayS64.class == input.getClass()) {
				return ImageStatistics.mean((GrayS64) input);
			} else if (GrayF32.class == input.getClass()) {
				return ImageStatistics.mean((GrayF32) input);
			} else if (GrayF64.class == input.getClass()) {
				return ImageStatistics.mean((GrayF64) input);
			} else {
				throw new IllegalArgumentException("Unknown image Type");
			}
		} else if( input instanceof ImageInterleaved ) {
			if (InterleavedU8.class == input.getClass()) {
				return ImageStatistics.mean((InterleavedU8) input);
			} else if (InterleavedS8.class == input.getClass()) {
				return ImageStatistics.mean((InterleavedS8) input);
			} else if (InterleavedU16.class == input.getClass()) {
				return ImageStatistics.mean((InterleavedU16) input);
			} else if (InterleavedS16.class == input.getClass()) {
				return ImageStatistics.mean((InterleavedS16) input);
			} else if (InterleavedS32.class == input.getClass()) {
				return ImageStatistics.mean((InterleavedS32) input);
			} else if (InterleavedS64.class == input.getClass()) {
				return ImageStatistics.mean((InterleavedS64) input);
			} else if (InterleavedF32.class == input.getClass()) {
				return ImageStatistics.mean((InterleavedF32) input);
			} else if (InterleavedF64.class == input.getClass()) {
				return ImageStatistics.mean((InterleavedF64) input);
			} else {
				throw new IllegalArgumentException("Unknown image Type");
			}
		} else {
			throw new IllegalArgumentException("Planar image support needs to be added");
		}
	}

	/**
	 * Computes the variance of pixel intensity values inside the image.
	 *
	 * @param input Input image. Not modified.
	 * @param mean Mean pixel intensity value.
	 * @return Pixel variance
	 */
	public static <T extends ImageGray> double variance(T input , double mean ) {

		if( GrayU8.class == input.getClass() ) {
			return ImageStatistics.variance((GrayU8)input,mean);
		} else if( GrayS8.class == input.getClass() ) {
			return ImageStatistics.variance((GrayS8)input,mean);
		} else if( GrayU16.class == input.getClass() ) {
			return ImageStatistics.variance((GrayU16)input,mean);
		} else if( GrayS16.class == input.getClass() ) {
			return ImageStatistics.variance((GrayS16)input,mean);
		} else if( GrayS32.class == input.getClass() ) {
			return ImageStatistics.variance((GrayS32)input,mean);
		} else if( GrayS64.class == input.getClass() ) {
			return ImageStatistics.variance((GrayS64)input,mean);
		} else if( GrayF32.class == input.getClass() ) {
			return ImageStatistics.variance((GrayF32)input,mean);
		} else if( GrayF64.class == input.getClass() ) {
			return ImageStatistics.variance((GrayF64)input,mean);
		} else {
			throw new IllegalArgumentException("Unknown image Type");
		}
	}

	/**
	 * Computes the mean of the difference squared between the two images.
	 *
	 * @param inputA Input image. Not modified.
	 * @param inputB Input image. Not modified.
	 * @return Mean difference squared
	 */
	public static <T extends ImageBase> double meanDiffSq( T inputA , T inputB ) {

		if( inputA instanceof ImageGray) {
			if (GrayU8.class == inputA.getClass()) {
				return ImageStatistics.meanDiffSq((GrayU8) inputA, (GrayU8) inputB);
			} else if (GrayS8.class == inputA.getClass()) {
				return ImageStatistics.meanDiffSq((GrayS8) inputA, (GrayS8) inputB);
			} else if (GrayU16.class == inputA.getClass()) {
				return ImageStatistics.meanDiffSq((GrayU16) inputA, (GrayU16) inputB);
			} else if (GrayS16.class == inputA.getClass()) {
				return ImageStatistics.meanDiffSq((GrayS16) inputA, (GrayS16) inputB);
			} else if (GrayS32.class == inputA.getClass()) {
				return ImageStatistics.meanDiffSq((GrayS32) inputA, (GrayS32) inputB);
			} else if (GrayS64.class == inputA.getClass()) {
				return ImageStatistics.meanDiffSq((GrayS64) inputA, (GrayS64) inputB);
			} else if (GrayF32.class == inputA.getClass()) {
				return ImageStatistics.meanDiffSq((GrayF32) inputA, (GrayF32) inputB);
			} else if (GrayF64.class == inputA.getClass()) {
				return ImageStatistics.meanDiffSq((GrayF64) inputA, (GrayF64) inputB);
			} else {
				throw new IllegalArgumentException("Unknown image Type");
			}
		} else if( inputA instanceof ImageInterleaved ) {
			if (InterleavedU8.class == inputA.getClass()) {
				return ImageStatistics.meanDiffSq((InterleavedU8) inputA, (InterleavedU8) inputB);
			} else if (InterleavedS8.class == inputA.getClass()) {
				return ImageStatistics.meanDiffSq((InterleavedS8) inputA, (InterleavedS8) inputB);
			} else if (InterleavedU16.class == inputA.getClass()) {
				return ImageStatistics.meanDiffSq((InterleavedU16) inputA, (InterleavedU16) inputB);
			} else if (InterleavedS16.class == inputA.getClass()) {
				return ImageStatistics.meanDiffSq((InterleavedS16) inputA, (InterleavedS16) inputB);
			} else if (InterleavedS32.class == inputA.getClass()) {
				return ImageStatistics.meanDiffSq((InterleavedS32) inputA, (InterleavedS32) inputB);
			} else if (InterleavedS64.class == inputA.getClass()) {
				return ImageStatistics.meanDiffSq((InterleavedS64) inputA, (InterleavedS64) inputB);
			} else if (InterleavedF32.class == inputA.getClass()) {
				return ImageStatistics.meanDiffSq((InterleavedF32) inputA, (InterleavedF32) inputB);
			} else if (InterleavedF64.class == inputA.getClass()) {
				return ImageStatistics.meanDiffSq((InterleavedF64) inputA, (InterleavedF64) inputB);
			} else {
				throw new IllegalArgumentException("Unknown image Type");
			}
		} else {
			throw new IllegalArgumentException("Planar images needs to be added");
		}
	}

	/**
	 * Computes the mean of the absolute value of the difference between the two images across all bands
	 *
	 * @param inputA Input image. Not modified.
	 * @param inputB Input image. Not modified.
	 * @return Mean absolute difference
	 */
	public static <T extends ImageBase> double meanDiffAbs( T inputA , T inputB ) {

		if( inputA instanceof ImageGray) {
			if (GrayU8.class == inputA.getClass()) {
				return ImageStatistics.meanDiffAbs((GrayU8) inputA, (GrayU8) inputB);
			} else if (GrayS8.class == inputA.getClass()) {
				return ImageStatistics.meanDiffAbs((GrayS8) inputA, (GrayS8) inputB);
			} else if (GrayU16.class == inputA.getClass()) {
				return ImageStatistics.meanDiffAbs((GrayU16) inputA, (GrayU16) inputB);
			} else if (GrayS16.class == inputA.getClass()) {
				return ImageStatistics.meanDiffAbs((GrayS16) inputA, (GrayS16) inputB);
			} else if (GrayS32.class == inputA.getClass()) {
				return ImageStatistics.meanDiffAbs((GrayS32) inputA, (GrayS32) inputB);
			} else if (GrayS64.class == inputA.getClass()) {
				return ImageStatistics.meanDiffAbs((GrayS64) inputA, (GrayS64) inputB);
			} else if (GrayF32.class == inputA.getClass()) {
				return ImageStatistics.meanDiffAbs((GrayF32) inputA, (GrayF32) inputB);
			} else if (GrayF64.class == inputA.getClass()) {
				return ImageStatistics.meanDiffAbs((GrayF64) inputA, (GrayF64) inputB);
			} else {
				throw new IllegalArgumentException("Unknown image Type");
			}
		} else if( inputA instanceof ImageInterleaved ) {
			if (InterleavedU8.class == inputA.getClass()) {
				return ImageStatistics.meanDiffAbs((InterleavedU8) inputA, (InterleavedU8) inputB);
			} else if (InterleavedS8.class == inputA.getClass()) {
				return ImageStatistics.meanDiffAbs((InterleavedS8) inputA, (InterleavedS8) inputB);
			} else if (InterleavedU16.class == inputA.getClass()) {
				return ImageStatistics.meanDiffAbs((InterleavedU16) inputA, (InterleavedU16) inputB);
			} else if (InterleavedS16.class == inputA.getClass()) {
				return ImageStatistics.meanDiffAbs((InterleavedS16) inputA, (InterleavedS16) inputB);
			} else if (InterleavedS32.class == inputA.getClass()) {
				return ImageStatistics.meanDiffAbs((InterleavedS32) inputA, (InterleavedS32) inputB);
			} else if (InterleavedS64.class == inputA.getClass()) {
				return ImageStatistics.meanDiffAbs((InterleavedS64) inputA, (InterleavedS64) inputB);
			} else if (InterleavedF32.class == inputA.getClass()) {
				return ImageStatistics.meanDiffAbs((InterleavedF32) inputA, (InterleavedF32) inputB);
			} else if (InterleavedF64.class == inputA.getClass()) {
				return ImageStatistics.meanDiffAbs((InterleavedF64) inputA, (InterleavedF64) inputB);
			} else {
				throw new IllegalArgumentException("Unknown image Type");
			}
		} else {
			throw new IllegalArgumentException("Planar images needs to be added");
		}
	}

	/**
	 * Computes the histogram of intensity values for the image.  For floating point images it is rounded
	 * to the nearest integer using "(int)value".
	 *
	 * @param input (input) Image.
	 * @param minValue (input) Minimum possible intensity value   Ignored for unsigned images.
	 * @param histogram (output) Storage for histogram. Number of elements must be equal to max value.
	 */
	public static void histogram(ImageGray input , int minValue , int histogram[] ) {
		if( GrayU8.class == input.getClass() ) {
			ImageStatistics.histogram((GrayU8)input,histogram);
		} else if( GrayS8.class == input.getClass() ) {
			ImageStatistics.histogram((GrayS8)input,minValue,histogram);
		} else if( GrayU16.class == input.getClass() ) {
			ImageStatistics.histogram((GrayU16)input,histogram);
		} else if( GrayS16.class == input.getClass() ) {
			ImageStatistics.histogram((GrayS16)input,minValue,histogram);
		} else if( GrayS32.class == input.getClass() ) {
			ImageStatistics.histogram((GrayS32)input,minValue,histogram);
		} else if( GrayS64.class == input.getClass() ) {
			ImageStatistics.histogram((GrayS64)input,minValue,histogram);
		} else if( GrayF32.class == input.getClass() ) {
			ImageStatistics.histogram((GrayF32)input,minValue,histogram);
		} else if( GrayF64.class == input.getClass() ) {
			ImageStatistics.histogram((GrayF64)input,minValue,histogram);
		} else {
			throw new IllegalArgumentException("Unknown image Type");
		}
	}
}
