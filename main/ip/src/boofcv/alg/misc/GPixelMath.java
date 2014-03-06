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

package boofcv.alg.misc;

import boofcv.struct.image.*;

/**
 * Generalized version of {@link PixelMath}.  Type checking is performed at runtime instead of at compile type.
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
			PixelMath.abs((ImageSInt8) input, (ImageSInt8) output);
		} else if( ImageSInt16.class == input.getClass() ) {
			PixelMath.abs((ImageSInt16) input, (ImageSInt16) output);
		} else if( ImageSInt32.class == input.getClass() ) {
			PixelMath.abs((ImageSInt32) input, (ImageSInt32) output);
		} else if( ImageSInt64.class == input.getClass() ) {
			PixelMath.abs((ImageSInt64) input, (ImageSInt64) output);
		} else if( ImageFloat32.class == input.getClass() ) {
			PixelMath.abs((ImageFloat32) input, (ImageFloat32) output);
		} else if( ImageFloat64.class == input.getClass() ) {
			PixelMath.abs((ImageFloat64) input, (ImageFloat64) output);
		}
		// otherwise assume it is an unsigned image type
	}

	/**
	 * Changes the sign of every pixel in the image: output[x,y] = -input[x,y]
	 * Can only be used on signed images.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the inverted image is written to. Modified.
	 */
	public static <T extends ImageSingleBand> void invert( T input , T output )
	{
		if( ImageSInt8.class == input.getClass() ) {
			PixelMath.invert((ImageSInt8) input, (ImageSInt8) output);
		} else if( ImageSInt16.class == input.getClass() ) {
			PixelMath.invert((ImageSInt16) input, (ImageSInt16) output);
		} else if( ImageSInt32.class == input.getClass() ) {
			PixelMath.invert((ImageSInt32) input, (ImageSInt32) output);
		} else if( ImageSInt64.class == input.getClass() ) {
			PixelMath.invert((ImageSInt64) input, (ImageSInt64) output);
		} else if( ImageFloat32.class == input.getClass() ) {
			PixelMath.invert((ImageFloat32) input, (ImageFloat32) output);
		} else if( ImageFloat64.class == input.getClass() ) {
			PixelMath.invert((ImageFloat64) input, (ImageFloat64) output);
		} else {
			throw new IllegalArgumentException("Unsupported image type.  Input image must be signed");
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static <T extends ImageSingleBand> void divide(T input, double denominator, T output) {

		if( ImageUInt8.class == input.getClass() ) {
			PixelMath.divide((ImageUInt8)input,denominator,(ImageUInt8)output);
		} else if( ImageSInt8.class == input.getClass() ) {
			PixelMath.divide((ImageSInt8)input,denominator,(ImageSInt8)output);
		} else if( ImageUInt16.class == input.getClass() ) {
			PixelMath.divide((ImageUInt16)input,denominator,(ImageUInt16)output);
		} else if( ImageSInt16.class == input.getClass() ) {
			PixelMath.divide((ImageSInt16)input,denominator,(ImageSInt16)output);
		} else if( ImageSInt32.class == input.getClass() ) {
			PixelMath.divide((ImageSInt32)input,denominator,(ImageSInt32)output);
		} else if( ImageSInt64.class == input.getClass() ) {
			PixelMath.divide((ImageSInt64)input,denominator,(ImageSInt64)output);
		} else if( ImageFloat32.class == input.getClass() ) {
			PixelMath.divide((ImageFloat32)input,(float)denominator,(ImageFloat32)output);
		} else if( ImageFloat64.class == input.getClass() ) {
			PixelMath.divide((ImageFloat64)input,denominator,(ImageFloat64)output);
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
		}
	}

	/**
	 * Divide each element by a scalar value and bounds the result. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param lower Lower bound on output
	 * @param upper Upper bound on output
	 * @param output The output image. Modified.
	 */
	public static <T extends ImageSingleBand> void divide(T input, double denominator,
														  double lower , double upper  , T output)
	{
		if( ImageUInt8.class == input.getClass() ) {
			PixelMath.divide((ImageUInt8)input,denominator,(int)lower,(int)upper,(ImageUInt8)output);
		} else if( ImageSInt8.class == input.getClass() ) {
			PixelMath.divide((ImageSInt8)input,denominator,(int)lower,(int)upper,(ImageSInt8)output);
		} else if( ImageUInt16.class == input.getClass() ) {
			PixelMath.divide((ImageUInt16)input,denominator,(int)lower,(int)upper,(ImageUInt16)output);
		} else if( ImageSInt16.class == input.getClass() ) {
			PixelMath.divide((ImageSInt16)input,denominator,(int)lower,(int)upper,(ImageSInt16)output);
		} else if( ImageSInt32.class == input.getClass() ) {
			PixelMath.divide((ImageSInt32)input,denominator,(int)lower,(int)upper,(ImageSInt32)output);
		} else if( ImageSInt64.class == input.getClass() ) {
			PixelMath.divide((ImageSInt64)input,denominator,(long)lower,(long)upper,(ImageSInt64)output);
		} else if( ImageFloat32.class == input.getClass() ) {
			PixelMath.divide((ImageFloat32)input,(float)denominator,(float)lower,(float)upper,(ImageFloat32)output);
		} else if( ImageFloat64.class == input.getClass() ) {
			PixelMath.divide((ImageFloat64)input,denominator,lower,upper,(ImageFloat64)output);
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
		}
	}

	/**
	 * <p>
	 * Performs pixel-wise division<br>
	 * output(x,y) = imgA(x,y) / imgB(x,y)
	 * </p>
	 * Only floating point images are supported.
	 *
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Modified.
	 */
	public static <T extends ImageSingleBand> void divide(T imgA, T imgB , T output)
	{
		if( ImageFloat32.class == imgA.getClass() ) {
			PixelMath.divide((ImageFloat32)imgA,(ImageFloat32)imgB,(ImageFloat32)output);
		} else if( ImageFloat64.class == imgA.getClass() ) {
			PixelMath.divide((ImageFloat64)imgA,(ImageFloat64)imgB,(ImageFloat64)output);
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+imgA.getClass().getSimpleName());
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param output The output image. Modified.
	 */
	public static <T extends ImageSingleBand> void multiply(T input, double value, T output) {

		if( ImageUInt8.class == input.getClass() ) {
			PixelMath.multiply((ImageUInt8) input, value, (ImageUInt8) output);
		} else if( ImageSInt8.class == input.getClass() ) {
			PixelMath.multiply((ImageSInt8) input, value, (ImageSInt8) output);
		} else if( ImageUInt16.class == input.getClass() ) {
			PixelMath.multiply((ImageUInt16) input, value, (ImageUInt16) output);
		} else if( ImageSInt16.class == input.getClass() ) {
			PixelMath.multiply((ImageSInt16) input, value, (ImageSInt16) output);
		} else if( ImageSInt32.class == input.getClass() ) {
			PixelMath.multiply((ImageSInt32) input, value, (ImageSInt32) output);
		} else if( ImageSInt64.class == input.getClass() ) {
			PixelMath.multiply((ImageSInt64) input, value, (ImageSInt64) output);
		} else if( ImageFloat32.class == input.getClass() ) {
			PixelMath.multiply((ImageFloat32) input, (float) value, (ImageFloat32) output);
		} else if( ImageFloat64.class == input.getClass() ) {
			PixelMath.multiply((ImageFloat64) input, value, (ImageFloat64) output);
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
		}
	}

	/**
	 * Multiply each element by a scalar value and bounds the result. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param lower Lower bound on output
	 * @param upper Upper bound on output
	 * @param output The output image. Modified.
	 */
	public static <T extends ImageSingleBand> void multiply(T input, double value,
														  double lower , double upper , T output)
	{
		if( ImageUInt8.class == input.getClass() ) {
			PixelMath.multiply((ImageUInt8)input,value,(int)lower,(int)upper,(ImageUInt8)output);
		} else if( ImageSInt8.class == input.getClass() ) {
			PixelMath.multiply((ImageSInt8)input,value,(int)lower,(int)upper,(ImageSInt8)output);
		} else if( ImageUInt16.class == input.getClass() ) {
			PixelMath.multiply((ImageUInt16)input,value,(int)lower,(int)upper,(ImageUInt16)output);
		} else if( ImageSInt16.class == input.getClass() ) {
			PixelMath.multiply((ImageSInt16)input,value,(int)lower,(int)upper,(ImageSInt16)output);
		} else if( ImageSInt32.class == input.getClass() ) {
			PixelMath.multiply((ImageSInt32)input,value,(int)lower,(int)upper,(ImageSInt32)output);
		} else if( ImageSInt64.class == input.getClass() ) {
			PixelMath.multiply((ImageSInt64)input,value,(long)lower,(long)upper,(ImageSInt64)output);
		} else if( ImageFloat32.class == input.getClass() ) {
			PixelMath.multiply((ImageFloat32)input,(float)value,(float)lower,(float)upper,(ImageFloat32)output);
		} else if( ImageFloat64.class == input.getClass() ) {
			PixelMath.multiply((ImageFloat64)input,value,lower,upper,(ImageFloat64)output);
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
		}
	}

	/**
	 * <p>
	 * Performs pixel-wise multiplication<br>
	 * output(x,y) = imgA(x,y) * imgB(x,y)
	 * </p>
	 * Only floating point images are supported.
	 *
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Modified.
	 */
	public static <T extends ImageSingleBand> void multiply(T imgA, T imgB , T output)
	{
		if( ImageFloat32.class == imgA.getClass() ) {
			PixelMath.multiply((ImageFloat32)imgA,(ImageFloat32)imgB,(ImageFloat32)output);
		} else if( ImageFloat64.class == imgA.getClass() ) {
			PixelMath.multiply((ImageFloat64)imgA,(ImageFloat64)imgB,(ImageFloat64)output);
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+imgA.getClass().getSimpleName());
		}
	}

	/**
	 * Sets each pixel in the output image to log( 1 + input(x,y)) of the input image.
	 * Both the input and output image can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the log image is written to. Modified.
	 */
	public static <T extends ImageSingleBand> void log( T input , T output ) {
		if( ImageFloat32.class == input.getClass() ) {
			PixelMath.log((ImageFloat32) input, (ImageFloat32) output);
		} else if( ImageFloat64.class == input.getClass() ) {
			PixelMath.log((ImageFloat64) input, (ImageFloat64) output);
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
		}
	}

	/**
	 * Each element has the specified number added to it. Both input and output images can
	 * be the same.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param output The output image. Modified.
	 */
	public static <T extends ImageSingleBand> void plus(T input, double value, T output) {
		if( ImageUInt8.class == input.getClass() ) {
			PixelMath.plus((ImageUInt8) input, (int)value, (ImageUInt8) output);
		} else if( ImageSInt8.class == input.getClass() ) {
			PixelMath.plus((ImageSInt8) input, (int)value, (ImageSInt8) output);
		} else if( ImageUInt16.class == input.getClass() ) {
			PixelMath.plus((ImageUInt16) input, (int)value, (ImageUInt16) output);
		} else if( ImageSInt16.class == input.getClass() ) {
			PixelMath.plus((ImageSInt16) input, (int)value, (ImageSInt16) output);
		} else if( ImageSInt32.class == input.getClass() ) {
			PixelMath.plus((ImageSInt32) input, (int)value, (ImageSInt32) output);
		} else if( ImageSInt64.class == input.getClass() ) {
			PixelMath.plus((ImageSInt64) input, (int)value, (ImageSInt64) output);
		} else if( ImageFloat32.class == input.getClass() ) {
			PixelMath.plus((ImageFloat32) input, (float) value, (ImageFloat32) output);
		} else if( ImageFloat64.class == input.getClass() ) {
			PixelMath.plus((ImageFloat64) input, value, (ImageFloat64) output);
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
		}
	}

	/**
	 * Add a scalar value to each element and bounds the result. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is added to each element.
	 * @param lower Lower bound on output
	 * @param upper Upper bound on output
	 * @param output The output image. Modified.
	 */
	public static <T extends ImageSingleBand> void plus(T input, double value,
															double lower , double upper , T output)
	{
		if( ImageUInt8.class == input.getClass() ) {
			PixelMath.plus((ImageUInt8)input,(int)value,(int)lower,(int)upper,(ImageUInt8)output);
		} else if( ImageSInt8.class == input.getClass() ) {
			PixelMath.plus((ImageSInt8)input,(int)value,(int)lower,(int)upper,(ImageSInt8)output);
		} else if( ImageUInt16.class == input.getClass() ) {
			PixelMath.plus((ImageUInt16)input,(int)value,(int)lower,(int)upper,(ImageUInt16)output);
		} else if( ImageSInt16.class == input.getClass() ) {
			PixelMath.plus((ImageSInt16)input,(int)value,(int)lower,(int)upper,(ImageSInt16)output);
		} else if( ImageSInt32.class == input.getClass() ) {
			PixelMath.plus((ImageSInt32)input,(int)value,(int)lower,(int)upper,(ImageSInt32)output);
		} else if( ImageSInt64.class == input.getClass() ) {
			PixelMath.plus((ImageSInt64)input,(int)value,(long)lower,(long)upper,(ImageSInt64)output);
		} else if( ImageFloat32.class == input.getClass() ) {
			PixelMath.plus((ImageFloat32)input,(float)value,(float)lower,(float)upper,(ImageFloat32)output);
		} else if( ImageFloat64.class == input.getClass() ) {
			PixelMath.plus((ImageFloat64)input,value,lower,upper,(ImageFloat64)output);
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
		}
	}

	/**
	 * <p>
	 * Performs pixel-wise addition<br>
	 * d(x,y) = inputA(x,y) + inputB(x,y)
	 * </p>
	 * @param inputA Input image. Not modified.
	 * @param inputB Input image. Not modified.
	 * @param output Output image. Modified.
	 */
	public static <T extends ImageSingleBand, O extends ImageSingleBand>
	void add(T inputA, T inputB, O output) {
		if( ImageUInt8.class == inputA.getClass() ) {
			PixelMath.add((ImageUInt8) inputA, (ImageUInt8)inputB, (ImageUInt16) output);
		} else if( ImageSInt8.class == inputA.getClass() ) {
			PixelMath.add((ImageSInt8) inputA, (ImageSInt8)inputB, (ImageSInt16) output);
		} else if( ImageUInt16.class == inputA.getClass() ) {
			PixelMath.add((ImageUInt16) inputA, (ImageUInt16)inputB, (ImageSInt32) output);
		} else if( ImageSInt16.class == inputA.getClass() ) {
			PixelMath.add((ImageSInt16) inputA, (ImageSInt16)inputB, (ImageSInt32) output);
		} else if( ImageSInt32.class == inputA.getClass() ) {
			PixelMath.add((ImageSInt32) inputA, (ImageSInt32)inputB, (ImageSInt32) output);
		} else if( ImageSInt64.class == inputA.getClass() ) {
			PixelMath.add((ImageSInt64) inputA, (ImageSInt64)inputB, (ImageSInt64) output);
		} else if( ImageFloat32.class == inputA.getClass() ) {
			PixelMath.add((ImageFloat32) inputA, (ImageFloat32)inputB, (ImageFloat32) output);
		} else if( ImageFloat64.class == inputA.getClass() ) {
			PixelMath.add((ImageFloat64) inputA, (ImageFloat64)inputB, (ImageFloat64) output);
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+inputA.getClass().getSimpleName());
		}
	}

	/**
	 * <p>
	 * Performs pixel-wise subtraction, but ensures the result is between two bounds.<br>
	 * d(x,y) = imgA(x,y) - imgB(x,y)
	 * </p>
	 * @param inputA Input image. Not modified.
	 * @param inputB Input image. Not modified.
	 * @param output Output image. Modified.
	 */
	public static <T extends ImageBase, O extends ImageBase>
	void subtract(T inputA, T inputB, O output) {
		if( inputA instanceof ImageSingleBand ){
			if( ImageUInt8.class == inputA.getClass() ) {
				PixelMath.subtract((ImageUInt8) inputA, (ImageUInt8)inputB, (ImageInt16) output);
			} else if( ImageSInt8.class == inputA.getClass() ) {
				PixelMath.subtract((ImageSInt8) inputA, (ImageSInt8)inputB, (ImageSInt16) output);
			} else if( ImageUInt16.class == inputA.getClass() ) {
				PixelMath.subtract((ImageUInt16) inputA, (ImageUInt16)inputB, (ImageSInt32) output);
			} else if( ImageSInt16.class == inputA.getClass() ) {
				PixelMath.subtract((ImageSInt16) inputA, (ImageSInt16)inputB, (ImageSInt32) output);
			} else if( ImageSInt32.class == inputA.getClass() ) {
				PixelMath.subtract((ImageSInt32) inputA, (ImageSInt32)inputB, (ImageSInt32) output);
			} else if( ImageSInt64.class == inputA.getClass() ) {
				PixelMath.subtract((ImageSInt64) inputA, (ImageSInt64)inputB, (ImageSInt64) output);
			} else if( ImageFloat32.class == inputA.getClass() ) {
				PixelMath.subtract((ImageFloat32) inputA, (ImageFloat32)inputB, (ImageFloat32) output);
			} else if( ImageFloat64.class == inputA.getClass() ) {
				PixelMath.subtract((ImageFloat64) inputA, (ImageFloat64)inputB, (ImageFloat64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: "+inputA.getClass().getSimpleName());
			}
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+inputA.getClass().getSimpleName());
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
		} else if( ImageSInt64.class == input.getClass() ) {
			PixelMath.boundImage((ImageSInt64)input,(long)min,(long)max);
		} else if( ImageFloat32.class == input.getClass() ) {
			PixelMath.boundImage((ImageFloat32)input,(float)min,(float)max);
		} else if( ImageFloat64.class == input.getClass() ) {
			PixelMath.boundImage((ImageFloat64)input,min,max);
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
		}
	}

	/**
	 * <p>
	 * Computes the absolute value of the difference between each pixel in the two images.<br>
	 * d(x,y) = |img1(x,y) - img2(x,y)|
	 * </p>
	 * @param inputA Input image. Not modified.
	 * @param inputB Input image. Not modified.
	 * @param output Absolute value of difference image. Modified.
	 */
	public static <T extends ImageSingleBand> void diffAbs( T inputA , T inputB , T output) {

		if( ImageUInt8.class == inputA.getClass() ) {
			PixelMath.diffAbs((ImageUInt8) inputA, (ImageUInt8) inputB, (ImageUInt8) output);
		} else if( ImageSInt8.class == inputA.getClass() ) {
			PixelMath.diffAbs((ImageSInt8) inputA, (ImageSInt8) inputB, (ImageSInt8) output);
		} else if( ImageUInt16.class == inputA.getClass() ) {
			PixelMath.diffAbs((ImageUInt16) inputA, (ImageUInt16) inputB, (ImageUInt16) output);
		} else if( ImageSInt16.class == inputA.getClass() ) {
			PixelMath.diffAbs((ImageSInt16) inputA, (ImageSInt16) inputB, (ImageSInt16) output);
		} else if( ImageSInt32.class == inputA.getClass() ) {
			PixelMath.diffAbs((ImageSInt32) inputA, (ImageSInt32) inputB, (ImageSInt32) output);
		} else if( ImageSInt64.class == inputA.getClass() ) {
			PixelMath.diffAbs((ImageSInt64) inputA, (ImageSInt64) inputB, (ImageSInt64) output);
		} else if( ImageFloat32.class == inputA.getClass() ) {
			PixelMath.diffAbs((ImageFloat32) inputA, (ImageFloat32) inputB, (ImageFloat32) output);
		} else if( ImageFloat64.class == inputA.getClass() ) {
			PixelMath.diffAbs((ImageFloat64) inputA, (ImageFloat64) inputB, (ImageFloat64) output);
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+inputA.getClass().getSimpleName());
		}
	}

	/**
	 * Computes the average for each pixel across all bands in the {@link MultiSpectral} image.
	 *
	 * @param input MultiSpectral image
	 * @param output Gray scale image containing average pixel values
	 */
	public static <T extends ImageSingleBand> void averageBand(MultiSpectral<T> input, T output) {

		if( ImageUInt8.class == input.getType() ) {
			PixelMath.averageBand((MultiSpectral<ImageUInt8>) input, (ImageUInt8) output);
		} else if( ImageSInt8.class == input.getType() ) {
			PixelMath.averageBand((MultiSpectral<ImageSInt8>) input, (ImageSInt8) output);
		} else if( ImageUInt16.class == input.getType() ) {
			PixelMath.averageBand((MultiSpectral<ImageUInt16>) input, (ImageUInt16) output);
		} else if( ImageSInt16.class == input.getType() ) {
			PixelMath.averageBand((MultiSpectral<ImageSInt16>) input, (ImageSInt16) output);
		} else if( ImageSInt32.class == input.getType() ) {
			PixelMath.averageBand((MultiSpectral<ImageSInt32>) input, (ImageSInt32) output);
		} else if( ImageSInt64.class == input.getType() ) {
			PixelMath.averageBand((MultiSpectral<ImageSInt64>) input, (ImageSInt64) output);
		} else if( ImageFloat32.class == input.getType() ) {
			PixelMath.averageBand((MultiSpectral<ImageFloat32>) input, (ImageFloat32) output);
		} else if( ImageFloat64.class == input.getType() ) {
			PixelMath.averageBand((MultiSpectral<ImageFloat64>) input, (ImageFloat64) output);
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+input.getType().getSimpleName());
		}
	}
}
