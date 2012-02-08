/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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
 * Image type agnostic implementation of {@link PixelMath}.
 *
 * @author Peter Abeles
 */
public class GPixelMath {

	/**
	 * Sets each pixel in the output image to be the absolute value of the input image.
	 * Both the input and output image can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the absolute value image is written to. Modified.
	 */
	public static <T extends ImageSingleBand> void abs( T input , T output )
	{
		if( ImageSInt8.class == input.getClass() ) {
			PixelMath.abs((ImageSInt8)input,(ImageSInt8)output);
		} else if( ImageSInt16.class == input.getClass() ) {
			PixelMath.abs((ImageSInt16)input,(ImageSInt16)output);
		} else if( ImageSInt32.class == input.getClass() ) {
			PixelMath.abs((ImageSInt32)input,(ImageSInt32)output);
		} else if( ImageFloat32.class == input.getClass() ) {
			PixelMath.abs((ImageFloat32)input,(ImageFloat32)output);
		} else {
			throw new IllegalArgumentException("Unknown Image Type");
		}
	}

	/**
	 * Returns the absolute value of the element with the largest absolute value.
	 *
	 * @param input Input image. Not modified.
	 * @return Largest pixel absolute value.
	 */
	public static double maxAbs( ImageSingleBand input ) {
		if( ImageUInt8.class == input.getClass() ) {
			return PixelMath.maxAbs((ImageUInt8)input);
		} else if( ImageSInt8.class == input.getClass() ) {
			return PixelMath.maxAbs((ImageSInt8)input);
		} else if( ImageUInt16.class == input.getClass() ) {
			return PixelMath.maxAbs((ImageUInt16)input);
		} else if( ImageSInt16.class == input.getClass() ) {
			return PixelMath.maxAbs((ImageSInt16)input);
		} else if( ImageSInt32.class == input.getClass() ) {
			return PixelMath.maxAbs((ImageSInt32)input);
		} else if( ImageFloat32.class == input.getClass() ) {
			return PixelMath.maxAbs((ImageFloat32)input);
		} else {
			throw new IllegalArgumentException("Unknown Image Type");
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
			return PixelMath.max((ImageUInt8) input);
		} else if( ImageSInt8.class == input.getClass() ) {
			return PixelMath.max((ImageSInt8) input);
		} else if( ImageUInt16.class == input.getClass() ) {
			return PixelMath.max((ImageUInt16) input);
		} else if( ImageSInt16.class == input.getClass() ) {
			return PixelMath.max((ImageSInt16) input);
		} else if( ImageSInt32.class == input.getClass() ) {
			return PixelMath.max((ImageSInt32) input);
		} else if( ImageFloat32.class == input.getClass() ) {
			return PixelMath.max((ImageFloat32) input);
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
			return PixelMath.min((ImageUInt8) input);
		} else if( ImageSInt8.class == input.getClass() ) {
			return PixelMath.min((ImageSInt8) input);
		} else if( ImageUInt16.class == input.getClass() ) {
			return PixelMath.min((ImageUInt16) input);
		} else if( ImageSInt16.class == input.getClass() ) {
			return PixelMath.min((ImageSInt16) input);
		} else if( ImageSInt32.class == input.getClass() ) {
			return PixelMath.min((ImageSInt32) input);
		} else if( ImageFloat32.class == input.getClass() ) {
			return PixelMath.min((ImageFloat32) input);
		} else {
			throw new IllegalArgumentException("Unknown Image Type");
		}
	}

	/**
	 * Divides each element by the denominator. Both input and output images can
	 * be the same.
	 *
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 * @param denominator What each element is divided by.
	 */
	public static <T extends ImageSingleBand> void divide( T input , T output , double denominator ) {

		if( ImageInteger.class.isAssignableFrom(input.getClass())) {
			if( ImageUInt8.class == input.getClass() ) {
				PixelMath.divide((ImageUInt8)input,(ImageUInt8)output, denominator);
			} else if( ImageSInt8.class == input.getClass() ) {
				PixelMath.divide((ImageSInt8)input,(ImageSInt8)output, denominator);
			} else if( ImageUInt16.class == input.getClass() ) {
				PixelMath.divide((ImageUInt16)input,(ImageUInt16)output, denominator);
			} else if( ImageSInt16.class == input.getClass() ) {
				PixelMath.divide((ImageSInt16)input,(ImageSInt16)output, denominator);
			} else if( ImageSInt32.class == input.getClass() ) {
				PixelMath.divide((ImageSInt32)input,(ImageSInt32)output, denominator);
			} else {
				throw new IllegalArgumentException("Unknown integer image Type");
			}
		} else {
			if( ImageFloat32.class == input.getClass() ) {
				PixelMath.divide((ImageFloat32)input,(ImageFloat32)output, (float)denominator);
			} else {
				throw new IllegalArgumentException("Unknown integer image Type");
			}
		}
	}

	/**
	 * Multiplied each element by the scale factor. Both input and output images can
	 * be the same.
	 *
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 * @param scale What each element is divided by.
	 */
	public static <T extends ImageSingleBand> void multiply( T input , T output , double scale ) {

		if( ImageInteger.class.isAssignableFrom(input.getClass())) {
			if( ImageUInt8.class == input.getClass() ) {
				PixelMath.multiply((ImageUInt8)input,(ImageUInt8)output, scale);
			} else if( ImageSInt8.class == input.getClass() ) {
				PixelMath.multiply((ImageSInt8)input,(ImageSInt8)output, scale);
			} else if( ImageUInt16.class == input.getClass() ) {
				PixelMath.multiply((ImageUInt16)input,(ImageUInt16)output, scale);
			} else if( ImageSInt16.class == input.getClass() ) {
				PixelMath.multiply((ImageSInt16)input,(ImageSInt16)output, scale);
			} else if( ImageSInt32.class == input.getClass() ) {
				PixelMath.multiply((ImageSInt32)input,(ImageSInt32)output, scale);
			} else {
				throw new IllegalArgumentException("Unknown integer image Type");
			}
		} else {
			if( ImageFloat32.class == input.getClass() ) {
				PixelMath.multiply((ImageFloat32)input,(ImageFloat32)output, (float)scale);
			} else {
				throw new IllegalArgumentException("Unknown integer image Type");
			}
		}
	}

	/**
	 * Each element has the specified number added to it. Both input and output images can
	 * be the same.
	 *
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 * @param value What is added to each element.
	 */
	public static <T extends ImageSingleBand> void plus( T input , T output, double value ) {
		if( ImageInteger.class.isAssignableFrom(input.getClass())) {
			int scaleI = (int)value;

			if( ImageUInt8.class == input.getClass() ) {
				PixelMath.plus((ImageUInt8)input,(ImageUInt8)output, scaleI);
			} else if( ImageSInt8.class == input.getClass() ) {
				PixelMath.plus((ImageSInt8)input,(ImageSInt8)output, scaleI);
			} else if( ImageUInt16.class == input.getClass() ) {
				PixelMath.plus((ImageUInt16)input,(ImageUInt16)output, scaleI);
			} else if( ImageSInt16.class == input.getClass() ) {
				PixelMath.plus((ImageSInt16)input,(ImageSInt16)output, scaleI);
			} else if( ImageSInt32.class == input.getClass() ) {
				PixelMath.plus((ImageSInt32)input,(ImageSInt32)output, scaleI);
			} else {
				throw new IllegalArgumentException("Unknown integer image Type");
			}
		} else {
			if( ImageFloat32.class == input.getClass() ) {
				PixelMath.plus((ImageFloat32)input,(ImageFloat32)output, (float)value);
			} else {
				throw new IllegalArgumentException("Unknown integer image Type");
			}
		}
	}

	/**
	 * <p>
	 * Returns the sum of all the pixels in the image.
	 * </p>
	 *
	 * @param input Input image. Not modified.
	 */
	public static <T extends ImageSingleBand> double sum( T input ) {

		if( ImageUInt8.class == input.getClass() ) {
			return PixelMath.sum((ImageUInt8)input);
		} else if( ImageSInt8.class == input.getClass() ) {
			return PixelMath.sum((ImageSInt8)input);
		} else if( ImageUInt16.class == input.getClass() ) {
			return PixelMath.sum((ImageUInt16)input);
		} else if( ImageSInt16.class == input.getClass() ) {
			return PixelMath.sum((ImageSInt16)input);
		} else if( ImageSInt32.class == input.getClass() ) {
			return PixelMath.sum((ImageSInt32)input);
		} else if( ImageFloat32.class == input.getClass() ) {
			return PixelMath.sum((ImageFloat32)input);
		} else if( ImageFloat64.class == input.getClass() ) {
			return PixelMath.sum((ImageFloat64)input);
		} else {
			throw new IllegalArgumentException("Unknown integer image Type");
		}
	}

	/**
	 * Bounds image pixels to be between these two values.
	 *
	 * @param input Input image.
	 * @param min minimum value.
	 * @param max maximum value.
	 */
	public static <T extends ImageSingleBand> void boundImage( T input , double min , double max ) {

		if( ImageUInt8.class == input.getClass() ) {
			PixelMath.boundImage((ImageUInt8)input,(int)min,(int)max);
		} else if( ImageSInt8.class == input.getClass() ) {
			PixelMath.boundImage((ImageSInt8)input,(int)min,(int)max);
		} else if( ImageUInt16.class == input.getClass() ) {
			PixelMath.boundImage((ImageUInt16)input,(int)min,(int)max);
		} else if( ImageSInt16.class == input.getClass() ) {
			PixelMath.boundImage((ImageSInt16)input,(int)min,(int)max);
		} else if( ImageSInt32.class == input.getClass() ) {
			PixelMath.boundImage((ImageSInt32)input,(int)min,(int)max);
		} else if( ImageFloat32.class == input.getClass() ) {
			PixelMath.boundImage((ImageFloat32)input,(float)min,(float)max);
		} else if( ImageFloat64.class == input.getClass() ) {
			PixelMath.boundImage((ImageFloat64)input,(int)min,(int)max);
		} else {
			throw new IllegalArgumentException("Unknown integer image Type");
		}
	}

	/**
	 * Computes the average for each pixel across all bands in the {@link MultiSpectral} image.
	 *
	 * @param input MultiSpectral image
	 * @param output Gray scale image containing average pixel values
	 */
	public static <T extends ImageSingleBand> void bandAve( MultiSpectral<T> input , T output) {

		if( ImageUInt8.class == input.getType() ) {
			PixelMath.bandAve((MultiSpectral<ImageUInt8>)input,(ImageUInt8)output);
		} else if( ImageSInt8.class == input.getType() ) {
			PixelMath.bandAve((MultiSpectral<ImageSInt8>)input,(ImageSInt8)output);
		} else if( ImageUInt16.class == input.getType() ) {
			PixelMath.bandAve((MultiSpectral<ImageUInt16>)input,(ImageUInt16)output);
		} else if( ImageSInt16.class == input.getType() ) {
			PixelMath.bandAve((MultiSpectral<ImageSInt16>)input,(ImageSInt16)output);
		} else if( ImageSInt32.class == input.getType() ) {
			PixelMath.bandAve((MultiSpectral<ImageSInt32>)input,(ImageSInt32)output);
		} else if( ImageFloat32.class == input.getType() ) {
			PixelMath.bandAve((MultiSpectral<ImageFloat32>)input,(ImageFloat32)output);
		} else if( ImageFloat64.class == input.getType() ) {
			PixelMath.bandAve((MultiSpectral<ImageFloat64>)input,(ImageFloat64)output);
		} else {
			throw new IllegalArgumentException("Unknown integer image Type");
		}
	}
}
