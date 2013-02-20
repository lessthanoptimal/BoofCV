/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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
	 * Returns the absolute value of the element with the largest absolute value.
	 *
	 * @param input Input image. Not modified.
	 * @return Largest pixel absolute value.
	 */
	public static double maxAbs( ImageSingleBand input ) {
		if( ImageUInt8.class == input.getClass() ) {
			return ImageStatistics.maxAbs((ImageUInt8)input);
		} else if( ImageSInt8.class == input.getClass() ) {
			return ImageStatistics.maxAbs((ImageSInt8)input);
		} else if( ImageUInt16.class == input.getClass() ) {
			return ImageStatistics.maxAbs((ImageUInt16)input);
		} else if( ImageSInt16.class == input.getClass() ) {
			return ImageStatistics.maxAbs((ImageSInt16)input);
		} else if( ImageSInt32.class == input.getClass() ) {
			return ImageStatistics.maxAbs((ImageSInt32)input);
		} else if( ImageSInt64.class == input.getClass() ) {
			return ImageStatistics.maxAbs((ImageSInt64)input);
		} else if( ImageFloat32.class == input.getClass() ) {
			return ImageStatistics.maxAbs((ImageFloat32)input);
		} else if( ImageFloat64.class == input.getClass() ) {
			return ImageStatistics.maxAbs((ImageFloat64)input);
		} else {
			throw new IllegalArgumentException("Unknown Image Type: "+input.getClass().getSimpleName());
		}
	}

	/**
	 * Returns the maximum pixel value.
	 *
	 * @param input Input image. Not modified.
	 * @return Maximum pixel value.
	 */
	public static double max( ImageSingleBand input ) {
		if( ImageUInt8.class == input.getClass() ) {
			return ImageStatistics.max((ImageUInt8) input);
		} else if( ImageSInt8.class == input.getClass() ) {
			return ImageStatistics.max((ImageSInt8) input);
		} else if( ImageUInt16.class == input.getClass() ) {
			return ImageStatistics.max((ImageUInt16) input);
		} else if( ImageSInt16.class == input.getClass() ) {
			return ImageStatistics.max((ImageSInt16) input);
		} else if( ImageSInt32.class == input.getClass() ) {
			return ImageStatistics.max((ImageSInt32) input);
		} else if( ImageSInt64.class == input.getClass() ) {
			return ImageStatistics.max((ImageSInt64) input);
		} else if( ImageFloat32.class == input.getClass() ) {
			return ImageStatistics.max((ImageFloat32) input);
		} else if( ImageFloat64.class == input.getClass() ) {
			return ImageStatistics.max((ImageFloat64) input);
		} else {
			throw new IllegalArgumentException("Unknown Image Type");
		}
	}

	/**
	 * Returns the minimum pixel value.
	 *
	 * @param input Input image. Not modified.
	 * @return Minimum pixel value.
	 */
	public static double min( ImageSingleBand input ) {
		if( ImageUInt8.class == input.getClass() ) {
			return ImageStatistics.min((ImageUInt8) input);
		} else if( ImageSInt8.class == input.getClass() ) {
			return ImageStatistics.min((ImageSInt8) input);
		} else if( ImageUInt16.class == input.getClass() ) {
			return ImageStatistics.min((ImageUInt16) input);
		} else if( ImageSInt16.class == input.getClass() ) {
			return ImageStatistics.min((ImageSInt16) input);
		} else if( ImageSInt32.class == input.getClass() ) {
			return ImageStatistics.min((ImageSInt32) input);
		} else if( ImageSInt64.class == input.getClass() ) {
			return ImageStatistics.min((ImageSInt64) input);
		} else if( ImageFloat32.class == input.getClass() ) {
			return ImageStatistics.min((ImageFloat32) input);
		} else if( ImageFloat64.class == input.getClass() ) {
			return ImageStatistics.min((ImageFloat64) input);
		} else {
			throw new IllegalArgumentException("Unknown Image Type: "+input.getClass().getSimpleName());
		}
	}

	/**
	 * <p>
	 * Returns the sum of all the pixels in the image.
	 * </p>
	 *
	 * @param input Input image. Not modified.
	 * @return Sum of pixel intensity values
	 */
	public static <T extends ImageSingleBand> double sum( T input ) {

		if( ImageUInt8.class == input.getClass() ) {
			return ImageStatistics.sum((ImageUInt8)input);
		} else if( ImageSInt8.class == input.getClass() ) {
			return ImageStatistics.sum((ImageSInt8)input);
		} else if( ImageUInt16.class == input.getClass() ) {
			return ImageStatistics.sum((ImageUInt16)input);
		} else if( ImageSInt16.class == input.getClass() ) {
			return ImageStatistics.sum((ImageSInt16)input);
		} else if( ImageSInt32.class == input.getClass() ) {
			return ImageStatistics.sum((ImageSInt32)input);
		} else if( ImageSInt64.class == input.getClass() ) {
			return ImageStatistics.sum((ImageSInt64)input);
		} else if( ImageFloat32.class == input.getClass() ) {
			return ImageStatistics.sum((ImageFloat32)input);
		} else if( ImageFloat64.class == input.getClass() ) {
			return ImageStatistics.sum((ImageFloat64)input);
		} else {
			throw new IllegalArgumentException("Unknown image Type");
		}
	}

	/**
	 * Returns the mean pixel intensity value.
	 *
	 * @param input Input image. Not modified.
	 * @return Mean pixel value
	 */
	public static <T extends ImageSingleBand> double mean( T input ) {

		if( ImageUInt8.class == input.getClass() ) {
			return ImageStatistics.mean((ImageUInt8)input);
		} else if( ImageSInt8.class == input.getClass() ) {
			return ImageStatistics.mean((ImageSInt8)input);
		} else if( ImageUInt16.class == input.getClass() ) {
			return ImageStatistics.mean((ImageUInt16)input);
		} else if( ImageSInt16.class == input.getClass() ) {
			return ImageStatistics.mean((ImageSInt16)input);
		} else if( ImageSInt32.class == input.getClass() ) {
			return ImageStatistics.mean((ImageSInt32)input);
		} else if( ImageSInt64.class == input.getClass() ) {
			return ImageStatistics.mean((ImageSInt64)input);
		} else if( ImageFloat32.class == input.getClass() ) {
			return ImageStatistics.mean((ImageFloat32)input);
		} else if( ImageFloat64.class == input.getClass() ) {
			return ImageStatistics.mean((ImageFloat64)input);
		} else {
			throw new IllegalArgumentException("Unknown image Type");
		}
	}

	/**
	 * Computes the variance of pixel intensity values inside the image.
	 *
	 * @param input Input image. Not modified.
	 * @param mean Mean pixel intensity value.
	 * @return Pixel variance
	 */
	public static <T extends ImageSingleBand> double variance( T input , double mean ) {

		if( ImageUInt8.class == input.getClass() ) {
			return ImageStatistics.variance((ImageUInt8)input,mean);
		} else if( ImageSInt8.class == input.getClass() ) {
			return ImageStatistics.variance((ImageSInt8)input,mean);
		} else if( ImageUInt16.class == input.getClass() ) {
			return ImageStatistics.variance((ImageUInt16)input,mean);
		} else if( ImageSInt16.class == input.getClass() ) {
			return ImageStatistics.variance((ImageSInt16)input,mean);
		} else if( ImageSInt32.class == input.getClass() ) {
			return ImageStatistics.variance((ImageSInt32)input,mean);
		} else if( ImageSInt64.class == input.getClass() ) {
			return ImageStatistics.variance((ImageSInt64)input,mean);
		} else if( ImageFloat32.class == input.getClass() ) {
			return ImageStatistics.variance((ImageFloat32)input,mean);
		} else if( ImageFloat64.class == input.getClass() ) {
			return ImageStatistics.variance((ImageFloat64)input,mean);
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
	public static <T extends ImageSingleBand> double meanDiffSq( T inputA , T inputB ) {

		if( ImageUInt8.class == inputA.getClass() ) {
			return ImageStatistics.meanDiffSq((ImageUInt8)inputA,(ImageUInt8)inputB);
		} else if( ImageSInt8.class == inputA.getClass() ) {
			return ImageStatistics.meanDiffSq((ImageSInt8)inputA,(ImageSInt8)inputB);
		} else if( ImageUInt16.class == inputA.getClass() ) {
			return ImageStatistics.meanDiffSq((ImageUInt16)inputA,(ImageUInt16)inputB);
		} else if( ImageSInt16.class == inputA.getClass() ) {
			return ImageStatistics.meanDiffSq((ImageSInt16)inputA,(ImageSInt16)inputB);
		} else if( ImageSInt32.class == inputA.getClass() ) {
			return ImageStatistics.meanDiffSq((ImageSInt32)inputA,(ImageSInt32)inputB);
		} else if( ImageSInt64.class == inputA.getClass() ) {
			return ImageStatistics.meanDiffSq((ImageSInt64)inputA,(ImageSInt64)inputB);
		} else if( ImageFloat32.class == inputA.getClass() ) {
			return ImageStatistics.meanDiffSq((ImageFloat32)inputA,(ImageFloat32)inputB);
		} else if( ImageFloat64.class == inputA.getClass() ) {
			return ImageStatistics.meanDiffSq((ImageFloat64)inputA,(ImageFloat64)inputB);
		} else {
			throw new IllegalArgumentException("Unknown image Type");
		}
	}

	/**
	 * Computes the mean of the absolute value of the difference between the two images.
	 *
	 * @param inputA Input image. Not modified.
	 * @param inputB Input image. Not modified.
	 * @return Mean absolute difference
	 */
	public static <T extends ImageSingleBand> double meanDiffAbs( T inputA , T inputB ) {

		if( ImageUInt8.class == inputA.getClass() ) {
			return ImageStatistics.meanDiffAbs((ImageUInt8)inputA,(ImageUInt8)inputB);
		} else if( ImageSInt8.class == inputA.getClass() ) {
			return ImageStatistics.meanDiffAbs((ImageSInt8)inputA,(ImageSInt8)inputB);
		} else if( ImageUInt16.class == inputA.getClass() ) {
			return ImageStatistics.meanDiffAbs((ImageUInt16)inputA,(ImageUInt16)inputB);
		} else if( ImageSInt16.class == inputA.getClass() ) {
			return ImageStatistics.meanDiffAbs((ImageSInt16)inputA,(ImageSInt16)inputB);
		} else if( ImageSInt32.class == inputA.getClass() ) {
			return ImageStatistics.meanDiffAbs((ImageSInt32)inputA,(ImageSInt32)inputB);
		} else if( ImageSInt64.class == inputA.getClass() ) {
			return ImageStatistics.meanDiffAbs((ImageSInt64)inputA,(ImageSInt64)inputB);
		} else if( ImageFloat32.class == inputA.getClass() ) {
			return ImageStatistics.meanDiffAbs((ImageFloat32)inputA,(ImageFloat32)inputB);
		} else if( ImageFloat64.class == inputA.getClass() ) {
			return ImageStatistics.meanDiffAbs((ImageFloat64)inputA,(ImageFloat64)inputB);
		} else {
			throw new IllegalArgumentException("Unknown image Type");
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
	public static void histogram( ImageSingleBand input , int minValue , int histogram[] ) {
		if( ImageUInt8.class == input.getClass() ) {
			ImageStatistics.histogram((ImageUInt8)input,histogram);
		} else if( ImageSInt8.class == input.getClass() ) {
			ImageStatistics.histogram((ImageSInt8)input,minValue,histogram);
		} else if( ImageUInt16.class == input.getClass() ) {
			ImageStatistics.histogram((ImageUInt16)input,histogram);
		} else if( ImageSInt16.class == input.getClass() ) {
			ImageStatistics.histogram((ImageSInt16)input,minValue,histogram);
		} else if( ImageSInt32.class == input.getClass() ) {
			ImageStatistics.histogram((ImageSInt32)input,minValue,histogram);
		} else if( ImageSInt64.class == input.getClass() ) {
			ImageStatistics.histogram((ImageSInt64)input,minValue,histogram);
		} else if( ImageFloat32.class == input.getClass() ) {
			ImageStatistics.histogram((ImageFloat32)input,minValue,histogram);
		} else if( ImageFloat64.class == input.getClass() ) {
			ImageStatistics.histogram((ImageFloat64)input,minValue,histogram);
		} else {
			throw new IllegalArgumentException("Unknown image Type");
		}
	}
}
