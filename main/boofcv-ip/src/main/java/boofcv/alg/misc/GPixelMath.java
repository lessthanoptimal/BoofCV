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

import boofcv.alg.misc.PixelMathLambdas.*;
import boofcv.struct.image.*;

import javax.annotation.Generated;

/**
 * Generalized version of {@link PixelMath}. Type checking is performed at runtime instead of at compile type.
 *
 * <p>DO NOT MODIFY. Automatically generated code created by GenerateGPixelMath</p>
 *
 * @author Peter Abeles
 */
@Generated("boofcv.alg.misc.GenerateGPixelMath")
public class GPixelMath {

	/**
	 * Applies the lambda operation to each element in the input image. output[i] = function(input[i])
	 * Both the input and output image can be the same instance.
	 */
	public static <T extends ImageBase<T>> void operator1( T input, Function1 function, T output )
	{
		if( input instanceof ImageGray) {
			if (GrayI8.class.isAssignableFrom(input.getClass())) {
				PixelMath.operator1((GrayI8) input, (Function1_I8)function, (GrayI8) output);
			} else if (GrayI16.class.isAssignableFrom(input.getClass())) {
				PixelMath.operator1((GrayI16) input, (Function1_I16)function, (GrayI16) output);
			} else if (GrayS32.class.isAssignableFrom(input.getClass())) {
				PixelMath.operator1((GrayS32) input, (Function1_S32)function, (GrayS32) output);
			} else if (GrayS64.class.isAssignableFrom(input.getClass())) {
				PixelMath.operator1((GrayS64) input, (Function1_S64)function, (GrayS64) output);
			} else if (GrayF32.class.isAssignableFrom(input.getClass())) {
				PixelMath.operator1((GrayF32) input, (Function1_F32)function, (GrayF32) output);
			} else if (GrayF64.class.isAssignableFrom(input.getClass())) {
				PixelMath.operator1((GrayF64) input, (Function1_F64)function, (GrayF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
			}
		} else if( input instanceof ImageInterleaved ) {
			if (InterleavedI8.class.isAssignableFrom(input.getClass())) {
				PixelMath.operator1((InterleavedI8) input, (Function1_I8)function, (InterleavedI8) output);
			} else if (InterleavedI16.class.isAssignableFrom(input.getClass())) {
				PixelMath.operator1((InterleavedI16) input, (Function1_I16)function, (InterleavedI16) output);
			} else if (InterleavedS32.class.isAssignableFrom(input.getClass())) {
				PixelMath.operator1((InterleavedS32) input, (Function1_S32)function, (InterleavedS32) output);
			} else if (InterleavedS64.class.isAssignableFrom(input.getClass())) {
				PixelMath.operator1((InterleavedS64) input, (Function1_S64)function, (InterleavedS64) output);
			} else if (InterleavedF32.class.isAssignableFrom(input.getClass())) {
				PixelMath.operator1((InterleavedF32) input, (Function1_F32)function, (InterleavedF32) output);
			} else if (InterleavedF64.class.isAssignableFrom(input.getClass())) {
				PixelMath.operator1((InterleavedF64) input, (Function1_F64)function, (InterleavedF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
			}
		} else if( input instanceof Planar ) {
			Planar in = (Planar)input;
			Planar out = (Planar)output;

			for (int i = 0; i < in.getNumBands(); i++) {
				operator1(in.getBand(i), function, out.getBand(i));
			}
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
		}
	}

	/**
	 * Applies the lambda operation to each element in the two input images. output[i] = function(imA[i],imgB[i])
	 * Both the imgA, imgB, and output images can be the same instance.
	 */
	public static <T extends ImageBase<T>> void operator2( T imgA, Function2 function, T imgB, T output )
	{
		if( imgA instanceof ImageGray) {
			if (GrayI8.class.isAssignableFrom(imgA.getClass())) {
				PixelMath.operator2((GrayI8) imgA, (Function2_I8)function, (GrayI8) imgB, (GrayI8) output);
			} else if (GrayI16.class.isAssignableFrom(imgA.getClass())) {
				PixelMath.operator2((GrayI16) imgA, (Function2_I16)function, (GrayI16) imgB, (GrayI16) output);
			} else if (GrayS32.class.isAssignableFrom(imgA.getClass())) {
				PixelMath.operator2((GrayS32) imgA, (Function2_S32)function, (GrayS32) imgB, (GrayS32) output);
			} else if (GrayS64.class.isAssignableFrom(imgA.getClass())) {
				PixelMath.operator2((GrayS64) imgA, (Function2_S64)function, (GrayS64) imgB, (GrayS64) output);
			} else if (GrayF32.class.isAssignableFrom(imgA.getClass())) {
				PixelMath.operator2((GrayF32) imgA, (Function2_F32)function, (GrayF32) imgB, (GrayF32) output);
			} else if (GrayF64.class.isAssignableFrom(imgA.getClass())) {
				PixelMath.operator2((GrayF64) imgA, (Function2_F64)function, (GrayF64) imgB, (GrayF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + imgA.getClass().getSimpleName());
			}
		} else if( imgA instanceof ImageInterleaved ) {
			if (InterleavedI8.class.isAssignableFrom(imgA.getClass())) {
				PixelMath.operator2((InterleavedI8) imgA, (Function2_I8)function, (InterleavedI8) imgB, (InterleavedI8) output);
			} else if (InterleavedI16.class.isAssignableFrom(imgA.getClass())) {
				PixelMath.operator2((InterleavedI16) imgA, (Function2_I16)function, (InterleavedI16) imgB, (InterleavedI16) output);
			} else if (InterleavedS32.class.isAssignableFrom(imgA.getClass())) {
				PixelMath.operator2((InterleavedS32) imgA, (Function2_S32)function, (InterleavedS32) imgB, (InterleavedS32) output);
			} else if (InterleavedS64.class.isAssignableFrom(imgA.getClass())) {
				PixelMath.operator2((InterleavedS64) imgA, (Function2_S64)function, (InterleavedS64) imgB, (InterleavedS64) output);
			} else if (InterleavedF32.class.isAssignableFrom(imgA.getClass())) {
				PixelMath.operator2((InterleavedF32) imgA, (Function2_F32)function, (InterleavedF32) imgB, (InterleavedF32) output);
			} else if (InterleavedF64.class.isAssignableFrom(imgA.getClass())) {
				PixelMath.operator2((InterleavedF64) imgA, (Function2_F64)function, (InterleavedF64) imgB, (InterleavedF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + imgA.getClass().getSimpleName());
			}
		} else if( imgA instanceof Planar ) {
			Planar _imgA = (Planar)imgA;
			Planar _imgB = (Planar)imgB;
			Planar out = (Planar)output;

			for (int i = 0; i < _imgA.getNumBands(); i++) {
				operator2(_imgA.getBand(i), function, _imgB.getBand(i), out.getBand(i));
			}
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+imgA.getClass().getSimpleName());
		}
	}

	/**
	 * Sets each pixel in the output image to be the absolute value of the input image.
	 * Both the input and output image can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the inverted image is written to. Modified.
	 */
	public static <T extends ImageBase<T>> void abs( T input , T output )
	{
		if( input instanceof ImageGray) {
			if (GrayS8.class == input.getClass()) {
				PixelMath.abs((GrayS8) input, (GrayS8) output);
			} else if (GrayS16.class == input.getClass()) {
				PixelMath.abs((GrayS16) input, (GrayS16) output);
			} else if (GrayS32.class == input.getClass()) {
				PixelMath.abs((GrayS32) input, (GrayS32) output);
			} else if (GrayS64.class == input.getClass()) {
				PixelMath.abs((GrayS64) input, (GrayS64) output);
			} else if (GrayF32.class == input.getClass()) {
				PixelMath.abs((GrayF32) input, (GrayF32) output);
			} else if (GrayF64.class == input.getClass()) {
				PixelMath.abs((GrayF64) input, (GrayF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
			}
		} else if( input instanceof ImageInterleaved ) {
			if (InterleavedS8.class == input.getClass()) {
				PixelMath.abs((InterleavedS8) input, (InterleavedS8) output);
			} else if (InterleavedS16.class == input.getClass()) {
				PixelMath.abs((InterleavedS16) input, (InterleavedS16) output);
			} else if (InterleavedS32.class == input.getClass()) {
				PixelMath.abs((InterleavedS32) input, (InterleavedS32) output);
			} else if (InterleavedS64.class == input.getClass()) {
				PixelMath.abs((InterleavedS64) input, (InterleavedS64) output);
			} else if (InterleavedF32.class == input.getClass()) {
				PixelMath.abs((InterleavedF32) input, (InterleavedF32) output);
			} else if (InterleavedF64.class == input.getClass()) {
				PixelMath.abs((InterleavedF64) input, (InterleavedF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
			}
		} else if( input instanceof Planar ) {
			Planar in = (Planar)input;
			Planar out = (Planar)output;

			for (int i = 0; i < in.getNumBands(); i++) {
				abs(in.getBand(i), out.getBand(i));
			}
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
		}
	}

	/**
	 * Changes the sign of every pixel in the image: output[x,y] = -input[x,y]
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the inverted image is written to. Modified.
	 */
	public static <T extends ImageBase<T>> void negative( T input , T output )
	{
		if( input instanceof ImageGray) {
			if (GrayS8.class == input.getClass()) {
				PixelMath.negative((GrayS8) input, (GrayS8) output);
			} else if (GrayS16.class == input.getClass()) {
				PixelMath.negative((GrayS16) input, (GrayS16) output);
			} else if (GrayS32.class == input.getClass()) {
				PixelMath.negative((GrayS32) input, (GrayS32) output);
			} else if (GrayS64.class == input.getClass()) {
				PixelMath.negative((GrayS64) input, (GrayS64) output);
			} else if (GrayF32.class == input.getClass()) {
				PixelMath.negative((GrayF32) input, (GrayF32) output);
			} else if (GrayF64.class == input.getClass()) {
				PixelMath.negative((GrayF64) input, (GrayF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
			}
		} else if( input instanceof ImageInterleaved ) {
			if (InterleavedS8.class == input.getClass()) {
				PixelMath.negative((InterleavedS8) input, (InterleavedS8) output);
			} else if (InterleavedS16.class == input.getClass()) {
				PixelMath.negative((InterleavedS16) input, (InterleavedS16) output);
			} else if (InterleavedS32.class == input.getClass()) {
				PixelMath.negative((InterleavedS32) input, (InterleavedS32) output);
			} else if (InterleavedS64.class == input.getClass()) {
				PixelMath.negative((InterleavedS64) input, (InterleavedS64) output);
			} else if (InterleavedF32.class == input.getClass()) {
				PixelMath.negative((InterleavedF32) input, (InterleavedF32) output);
			} else if (InterleavedF64.class == input.getClass()) {
				PixelMath.negative((InterleavedF64) input, (InterleavedF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
			}
		} else if( input instanceof Planar ) {
			Planar in = (Planar)input;
			Planar out = (Planar)output;

			for (int i = 0; i < in.getNumBands(); i++) {
				negative(in.getBand(i), out.getBand(i));
			}
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param output The output image. Modified.
	 */
	public static <T extends ImageBase<T>,O extends ImageBase<O>> void divide( T input, double denominator, O output )
	{
		if( input instanceof ImageGray) {
			if( input.getClass() == output.getClass() ) {
				if (GrayU8.class == input.getClass()) {
					PixelMath.divide((GrayU8) input, denominator, (GrayU8) output);
				} else if (GrayS8.class == input.getClass()) {
					PixelMath.divide((GrayS8) input, denominator, (GrayS8) output);
				} else if (GrayU16.class == input.getClass()) {
					PixelMath.divide((GrayU16) input, denominator, (GrayU16) output);
				} else if (GrayS16.class == input.getClass()) {
					PixelMath.divide((GrayS16) input, denominator, (GrayS16) output);
				} else if (GrayS32.class == input.getClass()) {
					PixelMath.divide((GrayS32) input, denominator, (GrayS32) output);
				} else if (GrayS64.class == input.getClass()) {
					PixelMath.divide((GrayS64) input, denominator, (GrayS64) output);
				} else if (GrayF32.class == input.getClass()) {
					PixelMath.divide((GrayF32) input, (float) denominator, (GrayF32) output);
				} else if (GrayF64.class == input.getClass()) {
					PixelMath.divide((GrayF64) input, denominator, (GrayF64) output);
				} else {
					throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
				}
			} else if( GrayF32.class == output.getClass() ) {
				if (GrayU8.class == input.getClass()) {
					PixelMath.divide((GrayU8) input, (float)denominator, (GrayF32) output);
				} else if (GrayS8.class == input.getClass()) {
					PixelMath.divide((GrayS8) input, (float)denominator, (GrayF32) output);
				} else if (GrayU16.class == input.getClass()) {
					PixelMath.divide((GrayU16) input, (float)denominator, (GrayF32) output);
				} else if (GrayS16.class == input.getClass()) {
					PixelMath.divide((GrayS16) input, (float)denominator, (GrayF32) output);
				} else if (GrayS32.class == input.getClass()) {
					PixelMath.divide((GrayS32) input, (float)denominator, (GrayF32) output);
				} else if (GrayS64.class == input.getClass()) {
					PixelMath.divide((GrayS64) input, (float)denominator, (GrayF32) output);
				} else {
					throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
				}
			}
		} else if( input instanceof ImageInterleaved ) {
			if( input.getClass() == output.getClass() ) {
					if (InterleavedU8.class == input.getClass()) {
						PixelMath.divide((InterleavedU8) input, denominator, (InterleavedU8) output);
					} else if (InterleavedS8.class == input.getClass()) {
						PixelMath.divide((InterleavedS8) input, denominator, (InterleavedS8) output);
					} else if (InterleavedU16.class == input.getClass()) {
						PixelMath.divide((InterleavedU16) input, denominator, (InterleavedU16) output);
					} else if (InterleavedS16.class == input.getClass()) {
						PixelMath.divide((InterleavedS16) input, denominator, (InterleavedS16) output);
					} else if (InterleavedS32.class == input.getClass()) {
						PixelMath.divide((InterleavedS32) input, denominator, (InterleavedS32) output);
					} else if (InterleavedS64.class == input.getClass()) {
						PixelMath.divide((InterleavedS64) input, denominator, (InterleavedS64) output);
					} else if (InterleavedF32.class == input.getClass()) {
						PixelMath.divide((InterleavedF32) input, (float) denominator, (InterleavedF32) output);
					} else if (InterleavedF64.class == input.getClass()) {
						PixelMath.divide((InterleavedF64) input, denominator, (InterleavedF64) output);
					} else {
						throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
					}
			} else if( InterleavedF32.class == output.getClass() ) {
				if (InterleavedU8.class == input.getClass()) {
					PixelMath.divide((InterleavedU8) input, (float)denominator, (InterleavedF32) output);
				} else if (InterleavedS8.class == input.getClass()) {
					PixelMath.divide((InterleavedS8) input, (float)denominator, (InterleavedF32) output);
				} else if (InterleavedU16.class == input.getClass()) {
					PixelMath.divide((InterleavedU16) input, (float)denominator, (InterleavedF32) output);
				} else if (InterleavedS16.class == input.getClass()) {
					PixelMath.divide((InterleavedS16) input, (float)denominator, (InterleavedF32) output);
				} else if (InterleavedS32.class == input.getClass()) {
					PixelMath.divide((InterleavedS32) input, (float)denominator, (InterleavedF32) output);
				} else if (InterleavedS64.class == input.getClass()) {
					PixelMath.divide((InterleavedS64) input, (float)denominator, (InterleavedF32) output);
				} else {
					throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
				}
			}
		} else if( input instanceof Planar ) {
			Planar in = (Planar)input;
			Planar out = (Planar)output;

			for (int i = 0; i < in.getNumBands(); i++) {
				divide(in.getBand(i), denominator, out.getBand(i));
			}
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
		}
	}

	/**
	 * Divide each element by a scalar value. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param denominator What each element is divided by.
	 * @param lower Lower bound on output. Inclusive.
	 * @param upper Upper bound on output. Inclusive.
	 * @param output The output image. Modified.
	 */
	public static <T extends ImageBase<T>,O extends ImageBase<O>> void divide( T input, double denominator, double lower, double upper, O output )
	{
		if( input instanceof ImageGray) {
			if (GrayU8.class == input.getClass()) {
				PixelMath.divide((GrayU8) input, denominator, (int) lower, (int) upper, (GrayU8) output);
			} else if (GrayS8.class == input.getClass()) {
				PixelMath.divide((GrayS8) input, denominator, (int) lower, (int) upper, (GrayS8) output);
			} else if (GrayU16.class == input.getClass()) {
				PixelMath.divide((GrayU16) input, denominator, (int) lower, (int) upper, (GrayU16) output);
			} else if (GrayS16.class == input.getClass()) {
				PixelMath.divide((GrayS16) input, denominator, (int) lower, (int) upper, (GrayS16) output);
			} else if (GrayS32.class == input.getClass()) {
				PixelMath.divide((GrayS32) input, denominator, (int) lower, (int) upper, (GrayS32) output);
			} else if (GrayS64.class == input.getClass()) {
				PixelMath.divide((GrayS64) input, denominator, (long) lower, (long) upper, (GrayS64) output);
			} else if (GrayF32.class == input.getClass()) {
				PixelMath.divide((GrayF32) input, (float) denominator, (float) lower, (float) upper, (GrayF32) output);
			} else if (GrayF64.class == input.getClass()) {
				PixelMath.divide((GrayF64) input, denominator,  lower,  upper, (GrayF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
			}
		} else if( input instanceof ImageInterleaved ) {
			if (InterleavedU8.class == input.getClass()) {
				PixelMath.divide((InterleavedU8) input, denominator, (int) lower, (int) upper, (InterleavedU8) output);
			} else if (InterleavedS8.class == input.getClass()) {
				PixelMath.divide((InterleavedS8) input, denominator, (int) lower, (int) upper, (InterleavedS8) output);
			} else if (InterleavedU16.class == input.getClass()) {
				PixelMath.divide((InterleavedU16) input, denominator, (int) lower, (int) upper, (InterleavedU16) output);
			} else if (InterleavedS16.class == input.getClass()) {
				PixelMath.divide((InterleavedS16) input, denominator, (int) lower, (int) upper, (InterleavedS16) output);
			} else if (InterleavedS32.class == input.getClass()) {
				PixelMath.divide((InterleavedS32) input, denominator, (int) lower, (int) upper, (InterleavedS32) output);
			} else if (InterleavedS64.class == input.getClass()) {
				PixelMath.divide((InterleavedS64) input, denominator, (long) lower, (long) upper, (InterleavedS64) output);
			} else if (InterleavedF32.class == input.getClass()) {
				PixelMath.divide((InterleavedF32) input, (float) denominator, (float) lower, (float) upper, (InterleavedF32) output);
			} else if (InterleavedF64.class == input.getClass()) {
				PixelMath.divide((InterleavedF64) input, denominator,  lower,  upper, (InterleavedF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
			}
		} else if( input instanceof Planar ) {
			Planar in = (Planar)input;
			Planar out = (Planar)output;

			for (int i = 0; i < in.getNumBands(); i++) {
				divide(in.getBand(i),denominator,lower,upper,out.getBand(i));
			}
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
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
	public static <T extends ImageBase<T>,O extends ImageBase<O>> void multiply( T input, double value, O output )
	{
		if( input instanceof ImageGray) {
			if( input.getClass() == output.getClass() ) {
				if (GrayU8.class == input.getClass()) {
					PixelMath.multiply((GrayU8) input, value, (GrayU8) output);
				} else if (GrayS8.class == input.getClass()) {
					PixelMath.multiply((GrayS8) input, value, (GrayS8) output);
				} else if (GrayU16.class == input.getClass()) {
					PixelMath.multiply((GrayU16) input, value, (GrayU16) output);
				} else if (GrayS16.class == input.getClass()) {
					PixelMath.multiply((GrayS16) input, value, (GrayS16) output);
				} else if (GrayS32.class == input.getClass()) {
					PixelMath.multiply((GrayS32) input, value, (GrayS32) output);
				} else if (GrayS64.class == input.getClass()) {
					PixelMath.multiply((GrayS64) input, value, (GrayS64) output);
				} else if (GrayF32.class == input.getClass()) {
					PixelMath.multiply((GrayF32) input, (float) value, (GrayF32) output);
				} else if (GrayF64.class == input.getClass()) {
					PixelMath.multiply((GrayF64) input, value, (GrayF64) output);
				} else {
					throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
				}
			} else if( GrayF32.class == output.getClass() ) {
				if (GrayU8.class == input.getClass()) {
					PixelMath.multiply((GrayU8) input, (float)value, (GrayF32) output);
				} else if (GrayS8.class == input.getClass()) {
					PixelMath.multiply((GrayS8) input, (float)value, (GrayF32) output);
				} else if (GrayU16.class == input.getClass()) {
					PixelMath.multiply((GrayU16) input, (float)value, (GrayF32) output);
				} else if (GrayS16.class == input.getClass()) {
					PixelMath.multiply((GrayS16) input, (float)value, (GrayF32) output);
				} else if (GrayS32.class == input.getClass()) {
					PixelMath.multiply((GrayS32) input, (float)value, (GrayF32) output);
				} else if (GrayS64.class == input.getClass()) {
					PixelMath.multiply((GrayS64) input, (float)value, (GrayF32) output);
				} else {
					throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
				}
			}
		} else if( input instanceof ImageInterleaved ) {
			if( input.getClass() == output.getClass() ) {
					if (InterleavedU8.class == input.getClass()) {
						PixelMath.multiply((InterleavedU8) input, value, (InterleavedU8) output);
					} else if (InterleavedS8.class == input.getClass()) {
						PixelMath.multiply((InterleavedS8) input, value, (InterleavedS8) output);
					} else if (InterleavedU16.class == input.getClass()) {
						PixelMath.multiply((InterleavedU16) input, value, (InterleavedU16) output);
					} else if (InterleavedS16.class == input.getClass()) {
						PixelMath.multiply((InterleavedS16) input, value, (InterleavedS16) output);
					} else if (InterleavedS32.class == input.getClass()) {
						PixelMath.multiply((InterleavedS32) input, value, (InterleavedS32) output);
					} else if (InterleavedS64.class == input.getClass()) {
						PixelMath.multiply((InterleavedS64) input, value, (InterleavedS64) output);
					} else if (InterleavedF32.class == input.getClass()) {
						PixelMath.multiply((InterleavedF32) input, (float) value, (InterleavedF32) output);
					} else if (InterleavedF64.class == input.getClass()) {
						PixelMath.multiply((InterleavedF64) input, value, (InterleavedF64) output);
					} else {
						throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
					}
			} else if( InterleavedF32.class == output.getClass() ) {
				if (InterleavedU8.class == input.getClass()) {
					PixelMath.multiply((InterleavedU8) input, (float)value, (InterleavedF32) output);
				} else if (InterleavedS8.class == input.getClass()) {
					PixelMath.multiply((InterleavedS8) input, (float)value, (InterleavedF32) output);
				} else if (InterleavedU16.class == input.getClass()) {
					PixelMath.multiply((InterleavedU16) input, (float)value, (InterleavedF32) output);
				} else if (InterleavedS16.class == input.getClass()) {
					PixelMath.multiply((InterleavedS16) input, (float)value, (InterleavedF32) output);
				} else if (InterleavedS32.class == input.getClass()) {
					PixelMath.multiply((InterleavedS32) input, (float)value, (InterleavedF32) output);
				} else if (InterleavedS64.class == input.getClass()) {
					PixelMath.multiply((InterleavedS64) input, (float)value, (InterleavedF32) output);
				} else {
					throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
				}
			}
		} else if( input instanceof Planar ) {
			Planar in = (Planar)input;
			Planar out = (Planar)output;

			for (int i = 0; i < in.getNumBands(); i++) {
				multiply(in.getBand(i), value, out.getBand(i));
			}
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
		}
	}

	/**
	 * Multiply each element by a scalar value. Both input and output images can
	 * be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What each element is multiplied by.
	 * @param lower Lower bound on output. Inclusive.
	 * @param upper Upper bound on output. Inclusive.
	 * @param output The output image. Modified.
	 */
	public static <T extends ImageBase<T>,O extends ImageBase<O>> void multiply( T input, double value, double lower, double upper, O output )
	{
		if( input instanceof ImageGray) {
			if (GrayU8.class == input.getClass()) {
				PixelMath.multiply((GrayU8) input, value, (int) lower, (int) upper, (GrayU8) output);
			} else if (GrayS8.class == input.getClass()) {
				PixelMath.multiply((GrayS8) input, value, (int) lower, (int) upper, (GrayS8) output);
			} else if (GrayU16.class == input.getClass()) {
				PixelMath.multiply((GrayU16) input, value, (int) lower, (int) upper, (GrayU16) output);
			} else if (GrayS16.class == input.getClass()) {
				PixelMath.multiply((GrayS16) input, value, (int) lower, (int) upper, (GrayS16) output);
			} else if (GrayS32.class == input.getClass()) {
				PixelMath.multiply((GrayS32) input, value, (int) lower, (int) upper, (GrayS32) output);
			} else if (GrayS64.class == input.getClass()) {
				PixelMath.multiply((GrayS64) input, value, (long) lower, (long) upper, (GrayS64) output);
			} else if (GrayF32.class == input.getClass()) {
				PixelMath.multiply((GrayF32) input, (float) value, (float) lower, (float) upper, (GrayF32) output);
			} else if (GrayF64.class == input.getClass()) {
				PixelMath.multiply((GrayF64) input, value,  lower,  upper, (GrayF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
			}
		} else if( input instanceof ImageInterleaved ) {
			if (InterleavedU8.class == input.getClass()) {
				PixelMath.multiply((InterleavedU8) input, value, (int) lower, (int) upper, (InterleavedU8) output);
			} else if (InterleavedS8.class == input.getClass()) {
				PixelMath.multiply((InterleavedS8) input, value, (int) lower, (int) upper, (InterleavedS8) output);
			} else if (InterleavedU16.class == input.getClass()) {
				PixelMath.multiply((InterleavedU16) input, value, (int) lower, (int) upper, (InterleavedU16) output);
			} else if (InterleavedS16.class == input.getClass()) {
				PixelMath.multiply((InterleavedS16) input, value, (int) lower, (int) upper, (InterleavedS16) output);
			} else if (InterleavedS32.class == input.getClass()) {
				PixelMath.multiply((InterleavedS32) input, value, (int) lower, (int) upper, (InterleavedS32) output);
			} else if (InterleavedS64.class == input.getClass()) {
				PixelMath.multiply((InterleavedS64) input, value, (long) lower, (long) upper, (InterleavedS64) output);
			} else if (InterleavedF32.class == input.getClass()) {
				PixelMath.multiply((InterleavedF32) input, (float) value, (float) lower, (float) upper, (InterleavedF32) output);
			} else if (InterleavedF64.class == input.getClass()) {
				PixelMath.multiply((InterleavedF64) input, value,  lower,  upper, (InterleavedF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
			}
		} else if( input instanceof Planar ) {
			Planar in = (Planar)input;
			Planar out = (Planar)output;

			for (int i = 0; i < in.getNumBands(); i++) {
				multiply(in.getBand(i),value,lower,upper,out.getBand(i));
			}
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
	public static <T extends ImageBase<T>,O extends ImageBase<O>> void plus( T input, double value, O output )
	{
		if( input instanceof ImageGray) {
			if( input.getClass() == output.getClass() ) {
				if (GrayU8.class == input.getClass()) {
					PixelMath.plus((GrayU8) input, (int)value, (GrayU8) output);
				} else if (GrayS8.class == input.getClass()) {
					PixelMath.plus((GrayS8) input, (int)value, (GrayS8) output);
				} else if (GrayU16.class == input.getClass()) {
					PixelMath.plus((GrayU16) input, (int)value, (GrayU16) output);
				} else if (GrayS16.class == input.getClass()) {
					PixelMath.plus((GrayS16) input, (int)value, (GrayS16) output);
				} else if (GrayS32.class == input.getClass()) {
					PixelMath.plus((GrayS32) input, (int)value, (GrayS32) output);
				} else if (GrayS64.class == input.getClass()) {
					PixelMath.plus((GrayS64) input, (long)value, (GrayS64) output);
				} else if (GrayF32.class == input.getClass()) {
					PixelMath.plus((GrayF32) input, (float)value, (GrayF32) output);
				} else if (GrayF64.class == input.getClass()) {
					PixelMath.plus((GrayF64) input, value, (GrayF64) output);
				} else {
					throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
				}
			} else if( GrayF32.class == output.getClass() ) {
				if (GrayU8.class == input.getClass()) {
					PixelMath.plus((GrayU8) input, (int)value, (GrayF32) output);
				} else if (GrayS8.class == input.getClass()) {
					PixelMath.plus((GrayS8) input, (int)value, (GrayF32) output);
				} else if (GrayU16.class == input.getClass()) {
					PixelMath.plus((GrayU16) input, (int)value, (GrayF32) output);
				} else if (GrayS16.class == input.getClass()) {
					PixelMath.plus((GrayS16) input, (int)value, (GrayF32) output);
				} else if (GrayS32.class == input.getClass()) {
					PixelMath.plus((GrayS32) input, (int)value, (GrayF32) output);
				} else if (GrayS64.class == input.getClass()) {
					PixelMath.plus((GrayS64) input, (long)value, (GrayF32) output);
				} else {
					throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
				}
			}
		} else if( input instanceof ImageInterleaved ) {
			if( input.getClass() == output.getClass() ) {
					if (InterleavedU8.class == input.getClass()) {
						PixelMath.plus((InterleavedU8) input, (int)value, (InterleavedU8) output);
					} else if (InterleavedS8.class == input.getClass()) {
						PixelMath.plus((InterleavedS8) input, (int)value, (InterleavedS8) output);
					} else if (InterleavedU16.class == input.getClass()) {
						PixelMath.plus((InterleavedU16) input, (int)value, (InterleavedU16) output);
					} else if (InterleavedS16.class == input.getClass()) {
						PixelMath.plus((InterleavedS16) input, (int)value, (InterleavedS16) output);
					} else if (InterleavedS32.class == input.getClass()) {
						PixelMath.plus((InterleavedS32) input, (int)value, (InterleavedS32) output);
					} else if (InterleavedS64.class == input.getClass()) {
						PixelMath.plus((InterleavedS64) input, (long)value, (InterleavedS64) output);
					} else if (InterleavedF32.class == input.getClass()) {
						PixelMath.plus((InterleavedF32) input, (float)value, (InterleavedF32) output);
					} else if (InterleavedF64.class == input.getClass()) {
						PixelMath.plus((InterleavedF64) input, value, (InterleavedF64) output);
					} else {
						throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
					}
			} else if( InterleavedF32.class == output.getClass() ) {
				if (InterleavedU8.class == input.getClass()) {
					PixelMath.plus((InterleavedU8) input, (int)value, (InterleavedF32) output);
				} else if (InterleavedS8.class == input.getClass()) {
					PixelMath.plus((InterleavedS8) input, (int)value, (InterleavedF32) output);
				} else if (InterleavedU16.class == input.getClass()) {
					PixelMath.plus((InterleavedU16) input, (int)value, (InterleavedF32) output);
				} else if (InterleavedS16.class == input.getClass()) {
					PixelMath.plus((InterleavedS16) input, (int)value, (InterleavedF32) output);
				} else if (InterleavedS32.class == input.getClass()) {
					PixelMath.plus((InterleavedS32) input, (int)value, (InterleavedF32) output);
				} else if (InterleavedS64.class == input.getClass()) {
					PixelMath.plus((InterleavedS64) input, (long)value, (InterleavedF32) output);
				} else {
					throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
				}
			}
		} else if( input instanceof Planar ) {
			Planar in = (Planar)input;
			Planar out = (Planar)output;

			for (int i = 0; i < in.getNumBands(); i++) {
				plus(in.getBand(i), value, out.getBand(i));
			}
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
	 * @param lower Lower bound on output. Inclusive.
	 * @param upper Upper bound on output. Inclusive.
	 * @param output The output image. Modified.
	 */
	public static <T extends ImageBase<T>,O extends ImageBase<O>> void plus( T input, double value, double lower, double upper, O output )
	{
		if( input instanceof ImageGray) {
			if (GrayU8.class == input.getClass()) {
				PixelMath.plus((GrayU8) input, (int)value, (int) lower, (int) upper, (GrayU8) output);
			} else if (GrayS8.class == input.getClass()) {
				PixelMath.plus((GrayS8) input, (int)value, (int) lower, (int) upper, (GrayS8) output);
			} else if (GrayU16.class == input.getClass()) {
				PixelMath.plus((GrayU16) input, (int)value, (int) lower, (int) upper, (GrayU16) output);
			} else if (GrayS16.class == input.getClass()) {
				PixelMath.plus((GrayS16) input, (int)value, (int) lower, (int) upper, (GrayS16) output);
			} else if (GrayS32.class == input.getClass()) {
				PixelMath.plus((GrayS32) input, (int)value, (int) lower, (int) upper, (GrayS32) output);
			} else if (GrayS64.class == input.getClass()) {
				PixelMath.plus((GrayS64) input, (long)value, (long) lower, (long) upper, (GrayS64) output);
			} else if (GrayF32.class == input.getClass()) {
				PixelMath.plus((GrayF32) input, (float)value, (float) lower, (float) upper, (GrayF32) output);
			} else if (GrayF64.class == input.getClass()) {
				PixelMath.plus((GrayF64) input, value,  lower,  upper, (GrayF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
			}
		} else if( input instanceof ImageInterleaved ) {
			if (InterleavedU8.class == input.getClass()) {
				PixelMath.plus((InterleavedU8) input, (int)value, (int) lower, (int) upper, (InterleavedU8) output);
			} else if (InterleavedS8.class == input.getClass()) {
				PixelMath.plus((InterleavedS8) input, (int)value, (int) lower, (int) upper, (InterleavedS8) output);
			} else if (InterleavedU16.class == input.getClass()) {
				PixelMath.plus((InterleavedU16) input, (int)value, (int) lower, (int) upper, (InterleavedU16) output);
			} else if (InterleavedS16.class == input.getClass()) {
				PixelMath.plus((InterleavedS16) input, (int)value, (int) lower, (int) upper, (InterleavedS16) output);
			} else if (InterleavedS32.class == input.getClass()) {
				PixelMath.plus((InterleavedS32) input, (int)value, (int) lower, (int) upper, (InterleavedS32) output);
			} else if (InterleavedS64.class == input.getClass()) {
				PixelMath.plus((InterleavedS64) input, (long)value, (long) lower, (long) upper, (InterleavedS64) output);
			} else if (InterleavedF32.class == input.getClass()) {
				PixelMath.plus((InterleavedF32) input, (float)value, (float) lower, (float) upper, (InterleavedF32) output);
			} else if (InterleavedF64.class == input.getClass()) {
				PixelMath.plus((InterleavedF64) input, value,  lower,  upper, (InterleavedF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
			}
		} else if( input instanceof Planar ) {
			Planar in = (Planar)input;
			Planar out = (Planar)output;

			for (int i = 0; i < in.getNumBands(); i++) {
				plus(in.getBand(i),value,lower,upper,out.getBand(i));
			}
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element in input.
	 * @param output The output image. Modified.
	 */
	public static <T extends ImageBase<T>,O extends ImageBase<O>> void minus( T input, double value, O output )
	{
		if( input instanceof ImageGray) {
			if( input.getClass() == output.getClass() ) {
				if (GrayU8.class == input.getClass()) {
					PixelMath.minus((GrayU8) input, (int)value, (GrayU8) output);
				} else if (GrayS8.class == input.getClass()) {
					PixelMath.minus((GrayS8) input, (int)value, (GrayS8) output);
				} else if (GrayU16.class == input.getClass()) {
					PixelMath.minus((GrayU16) input, (int)value, (GrayU16) output);
				} else if (GrayS16.class == input.getClass()) {
					PixelMath.minus((GrayS16) input, (int)value, (GrayS16) output);
				} else if (GrayS32.class == input.getClass()) {
					PixelMath.minus((GrayS32) input, (int)value, (GrayS32) output);
				} else if (GrayS64.class == input.getClass()) {
					PixelMath.minus((GrayS64) input, (long)value, (GrayS64) output);
				} else if (GrayF32.class == input.getClass()) {
					PixelMath.minus((GrayF32) input, (float)value, (GrayF32) output);
				} else if (GrayF64.class == input.getClass()) {
					PixelMath.minus((GrayF64) input, value, (GrayF64) output);
				} else {
					throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
				}
			} else if( GrayF32.class == output.getClass() ) {
				if (GrayU8.class == input.getClass()) {
					PixelMath.minus((GrayU8) input, (int)value, (GrayF32) output);
				} else if (GrayS8.class == input.getClass()) {
					PixelMath.minus((GrayS8) input, (int)value, (GrayF32) output);
				} else if (GrayU16.class == input.getClass()) {
					PixelMath.minus((GrayU16) input, (int)value, (GrayF32) output);
				} else if (GrayS16.class == input.getClass()) {
					PixelMath.minus((GrayS16) input, (int)value, (GrayF32) output);
				} else if (GrayS32.class == input.getClass()) {
					PixelMath.minus((GrayS32) input, (int)value, (GrayF32) output);
				} else if (GrayS64.class == input.getClass()) {
					PixelMath.minus((GrayS64) input, (long)value, (GrayF32) output);
				} else {
					throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
				}
			}
		} else if( input instanceof ImageInterleaved ) {
			if( input.getClass() == output.getClass() ) {
					if (InterleavedU8.class == input.getClass()) {
						PixelMath.minus((InterleavedU8) input, (int)value, (InterleavedU8) output);
					} else if (InterleavedS8.class == input.getClass()) {
						PixelMath.minus((InterleavedS8) input, (int)value, (InterleavedS8) output);
					} else if (InterleavedU16.class == input.getClass()) {
						PixelMath.minus((InterleavedU16) input, (int)value, (InterleavedU16) output);
					} else if (InterleavedS16.class == input.getClass()) {
						PixelMath.minus((InterleavedS16) input, (int)value, (InterleavedS16) output);
					} else if (InterleavedS32.class == input.getClass()) {
						PixelMath.minus((InterleavedS32) input, (int)value, (InterleavedS32) output);
					} else if (InterleavedS64.class == input.getClass()) {
						PixelMath.minus((InterleavedS64) input, (long)value, (InterleavedS64) output);
					} else if (InterleavedF32.class == input.getClass()) {
						PixelMath.minus((InterleavedF32) input, (float)value, (InterleavedF32) output);
					} else if (InterleavedF64.class == input.getClass()) {
						PixelMath.minus((InterleavedF64) input, value, (InterleavedF64) output);
					} else {
						throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
					}
			} else if( InterleavedF32.class == output.getClass() ) {
				if (InterleavedU8.class == input.getClass()) {
					PixelMath.minus((InterleavedU8) input, (int)value, (InterleavedF32) output);
				} else if (InterleavedS8.class == input.getClass()) {
					PixelMath.minus((InterleavedS8) input, (int)value, (InterleavedF32) output);
				} else if (InterleavedU16.class == input.getClass()) {
					PixelMath.minus((InterleavedU16) input, (int)value, (InterleavedF32) output);
				} else if (InterleavedS16.class == input.getClass()) {
					PixelMath.minus((InterleavedS16) input, (int)value, (InterleavedF32) output);
				} else if (InterleavedS32.class == input.getClass()) {
					PixelMath.minus((InterleavedS32) input, (int)value, (InterleavedF32) output);
				} else if (InterleavedS64.class == input.getClass()) {
					PixelMath.minus((InterleavedS64) input, (long)value, (InterleavedF32) output);
				} else {
					throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
				}
			}
		} else if( input instanceof Planar ) {
			Planar in = (Planar)input;
			Planar out = (Planar)output;

			for (int i = 0; i < in.getNumBands(); i++) {
				minus(in.getBand(i), value, out.getBand(i));
			}
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
		}
	}

	/**
	 * Subtracts a scalar value from each element. Both input and output images can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value What is subtracted from each element in input.
	 * @param lower Lower bound on output. Inclusive.
	 * @param upper Upper bound on output. Inclusive.
	 * @param output The output image. Modified.
	 */
	public static <T extends ImageBase<T>,O extends ImageBase<O>> void minus( T input, double value, double lower, double upper, O output )
	{
		if( input instanceof ImageGray) {
			if (GrayU8.class == input.getClass()) {
				PixelMath.minus((GrayU8) input, (int)value, (int) lower, (int) upper, (GrayU8) output);
			} else if (GrayS8.class == input.getClass()) {
				PixelMath.minus((GrayS8) input, (int)value, (int) lower, (int) upper, (GrayS8) output);
			} else if (GrayU16.class == input.getClass()) {
				PixelMath.minus((GrayU16) input, (int)value, (int) lower, (int) upper, (GrayU16) output);
			} else if (GrayS16.class == input.getClass()) {
				PixelMath.minus((GrayS16) input, (int)value, (int) lower, (int) upper, (GrayS16) output);
			} else if (GrayS32.class == input.getClass()) {
				PixelMath.minus((GrayS32) input, (int)value, (int) lower, (int) upper, (GrayS32) output);
			} else if (GrayS64.class == input.getClass()) {
				PixelMath.minus((GrayS64) input, (long)value, (long) lower, (long) upper, (GrayS64) output);
			} else if (GrayF32.class == input.getClass()) {
				PixelMath.minus((GrayF32) input, (float)value, (float) lower, (float) upper, (GrayF32) output);
			} else if (GrayF64.class == input.getClass()) {
				PixelMath.minus((GrayF64) input, value,  lower,  upper, (GrayF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
			}
		} else if( input instanceof ImageInterleaved ) {
			if (InterleavedU8.class == input.getClass()) {
				PixelMath.minus((InterleavedU8) input, (int)value, (int) lower, (int) upper, (InterleavedU8) output);
			} else if (InterleavedS8.class == input.getClass()) {
				PixelMath.minus((InterleavedS8) input, (int)value, (int) lower, (int) upper, (InterleavedS8) output);
			} else if (InterleavedU16.class == input.getClass()) {
				PixelMath.minus((InterleavedU16) input, (int)value, (int) lower, (int) upper, (InterleavedU16) output);
			} else if (InterleavedS16.class == input.getClass()) {
				PixelMath.minus((InterleavedS16) input, (int)value, (int) lower, (int) upper, (InterleavedS16) output);
			} else if (InterleavedS32.class == input.getClass()) {
				PixelMath.minus((InterleavedS32) input, (int)value, (int) lower, (int) upper, (InterleavedS32) output);
			} else if (InterleavedS64.class == input.getClass()) {
				PixelMath.minus((InterleavedS64) input, (long)value, (long) lower, (long) upper, (InterleavedS64) output);
			} else if (InterleavedF32.class == input.getClass()) {
				PixelMath.minus((InterleavedF32) input, (float)value, (float) lower, (float) upper, (InterleavedF32) output);
			} else if (InterleavedF64.class == input.getClass()) {
				PixelMath.minus((InterleavedF64) input, value,  lower,  upper, (InterleavedF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
			}
		} else if( input instanceof Planar ) {
			Planar in = (Planar)input;
			Planar out = (Planar)output;

			for (int i = 0; i < in.getNumBands(); i++) {
				minus(in.getBand(i),value,lower,upper,out.getBand(i));
			}
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
		}
	}

	/**
	 * Subtracts the value of each element from a scalar value. Both input and output images can be the same instance.
	 * output = value - input
	 *
	 * @param value Left side of equation.
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 */
	public static <T extends ImageBase<T>> void minus( double value, T input, T output )
	{
		if( input instanceof ImageGray) {
			if (GrayU8.class == input.getClass()) {
				PixelMath.minus((int) value, (GrayU8) input,  (GrayU8) output);
			} else if (GrayS8.class == input.getClass()) {
				PixelMath.minus((int) value, (GrayS8) input,  (GrayS8) output);
			} else if (GrayU16.class == input.getClass()) {
				PixelMath.minus((int) value, (GrayU16) input,  (GrayU16) output);
			} else if (GrayS16.class == input.getClass()) {
				PixelMath.minus((int) value, (GrayS16) input,  (GrayS16) output);
			} else if (GrayS32.class == input.getClass()) {
				PixelMath.minus((int) value, (GrayS32) input,  (GrayS32) output);
			} else if (GrayS64.class == input.getClass()) {
				PixelMath.minus((long) value, (GrayS64) input,  (GrayS64) output);
			} else if (GrayF32.class == input.getClass()) {
				PixelMath.minus((float) value, (GrayF32) input,  (GrayF32) output);
			} else if (GrayF64.class == input.getClass()) {
				PixelMath.minus( value, (GrayF64) input,  (GrayF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
			}
		} else if( input instanceof ImageInterleaved ) {
			if (InterleavedU8.class == input.getClass()) {
				PixelMath.minus((int) value, (InterleavedU8) input,  (InterleavedU8) output);
			} else if (InterleavedS8.class == input.getClass()) {
				PixelMath.minus((int) value, (InterleavedS8) input,  (InterleavedS8) output);
			} else if (InterleavedU16.class == input.getClass()) {
				PixelMath.minus((int) value, (InterleavedU16) input,  (InterleavedU16) output);
			} else if (InterleavedS16.class == input.getClass()) {
				PixelMath.minus((int) value, (InterleavedS16) input,  (InterleavedS16) output);
			} else if (InterleavedS32.class == input.getClass()) {
				PixelMath.minus((int) value, (InterleavedS32) input,  (InterleavedS32) output);
			} else if (InterleavedS64.class == input.getClass()) {
				PixelMath.minus((long) value, (InterleavedS64) input,  (InterleavedS64) output);
			} else if (InterleavedF32.class == input.getClass()) {
				PixelMath.minus((float) value, (InterleavedF32) input,  (InterleavedF32) output);
			} else if (InterleavedF64.class == input.getClass()) {
				PixelMath.minus( value, (InterleavedF64) input,  (InterleavedF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
			}
		} else if( input instanceof Planar ) {
			Planar in = (Planar)input;
			Planar out = (Planar)output;

			for (int i = 0; i < in.getNumBands(); i++) {
				minus(value, in.getBand(i), out.getBand(i));
			}
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
		}
	}

	/**
	 * Subtracts the value of each element from a scalar value. Both input and output images can be the same instance.
	 * output = value - input
	 *
	 * @param value Left side of equation.
	 * @param input The input image. Not modified.
	 * @param lower Lower bound on output. Inclusive.
	 * @param upper Upper bound on output. Inclusive.
	 * @param output The output image. Modified.
	 */
	public static <T extends ImageBase<T>> void minus( double value, T input, double lower, double upper, T output )
	{
		if( input instanceof ImageGray) {
			if (GrayU8.class == input.getClass()) {
				PixelMath.minus((int) value, (GrayU8) input, (int) lower, (int) upper, (GrayU8) output);
			} else if (GrayS8.class == input.getClass()) {
				PixelMath.minus((int) value, (GrayS8) input, (int) lower, (int) upper, (GrayS8) output);
			} else if (GrayU16.class == input.getClass()) {
				PixelMath.minus((int) value, (GrayU16) input, (int) lower, (int) upper, (GrayU16) output);
			} else if (GrayS16.class == input.getClass()) {
				PixelMath.minus((int) value, (GrayS16) input, (int) lower, (int) upper, (GrayS16) output);
			} else if (GrayS32.class == input.getClass()) {
				PixelMath.minus((int) value, (GrayS32) input, (int) lower, (int) upper, (GrayS32) output);
			} else if (GrayS64.class == input.getClass()) {
				PixelMath.minus((long) value, (GrayS64) input, (long) lower, (long) upper, (GrayS64) output);
			} else if (GrayF32.class == input.getClass()) {
				PixelMath.minus((float) value, (GrayF32) input, (float) lower, (float) upper, (GrayF32) output);
			} else if (GrayF64.class == input.getClass()) {
				PixelMath.minus( value, (GrayF64) input,  lower,  upper, (GrayF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
			}
		} else if( input instanceof ImageInterleaved ) {
			if (InterleavedU8.class == input.getClass()) {
				PixelMath.minus((int) value, (InterleavedU8) input, (int) lower, (int) upper, (InterleavedU8) output);
			} else if (InterleavedS8.class == input.getClass()) {
				PixelMath.minus((int) value, (InterleavedS8) input, (int) lower, (int) upper, (InterleavedS8) output);
			} else if (InterleavedU16.class == input.getClass()) {
				PixelMath.minus((int) value, (InterleavedU16) input, (int) lower, (int) upper, (InterleavedU16) output);
			} else if (InterleavedS16.class == input.getClass()) {
				PixelMath.minus((int) value, (InterleavedS16) input, (int) lower, (int) upper, (InterleavedS16) output);
			} else if (InterleavedS32.class == input.getClass()) {
				PixelMath.minus((int) value, (InterleavedS32) input, (int) lower, (int) upper, (InterleavedS32) output);
			} else if (InterleavedS64.class == input.getClass()) {
				PixelMath.minus((long) value, (InterleavedS64) input, (long) lower, (long) upper, (InterleavedS64) output);
			} else if (InterleavedF32.class == input.getClass()) {
				PixelMath.minus((float) value, (InterleavedF32) input, (float) lower, (float) upper, (InterleavedF32) output);
			} else if (InterleavedF64.class == input.getClass()) {
				PixelMath.minus( value, (InterleavedF64) input,  lower,  upper, (InterleavedF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
			}
		} else if( input instanceof Planar ) {
			Planar in = (Planar)input;
			Planar out = (Planar)output;

			for (int i = 0; i < in.getNumBands(); i++) {
				minus(value, in.getBand(i), lower,upper,out.getBand(i));
			}
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
		}
	}

	/**
	 * Sets each pixel in the output image to log( val + input(x,y)) of the input image.
	 * Both the input and output image can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param value log( value + input[x,y])
	 * @param output The output image. Modified.
	 */
	public static <T extends ImageBase<T>,O extends ImageBase<O>> void log( T input, double value, O output )
	{
		if( input instanceof ImageGray) {
			if (GrayF32.class == input.getClass()) {
				PixelMath.log((GrayF32) input, (float)value, (GrayF32) output);
			} else if (GrayF64.class == input.getClass()) {
				PixelMath.log((GrayF64) input, value, (GrayF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
			}
		} else if( input instanceof ImageInterleaved ) {
			if (InterleavedF32.class == input.getClass()) {
				PixelMath.log((InterleavedF32) input, (float)value, (InterleavedF32) output);
			} else if (InterleavedF64.class == input.getClass()) {
				PixelMath.log((InterleavedF64) input, value, (InterleavedF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
			}
		} else if( input instanceof Planar ) {
			Planar in = (Planar)input;
			Planar out = (Planar)output;

			for (int i = 0; i < in.getNumBands(); i++) {
				log(in.getBand(i), value, out.getBand(i));
			}
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
		}
	}

	/**
	 * Sets each pixel in the output image to sgn*log( val + sgn*input(x,y)) of the input image.
	 * where sng is the sign of input(x,y).
	 *
	 * @param input The input image. Not modified.
	 * @param value sgn*log( value + sgn*input[x,y])
	 * @param output The output image. Modified.
	 */
	public static <T extends ImageBase<T>,O extends ImageBase<O>> void logSign( T input, double value, O output )
	{
		if( input instanceof ImageGray) {
			if (GrayF32.class == input.getClass()) {
				PixelMath.logSign((GrayF32) input, (float)value, (GrayF32) output);
			} else if (GrayF64.class == input.getClass()) {
				PixelMath.logSign((GrayF64) input, value, (GrayF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
			}
		} else if( input instanceof ImageInterleaved ) {
			if (InterleavedF32.class == input.getClass()) {
				PixelMath.logSign((InterleavedF32) input, (float)value, (InterleavedF32) output);
			} else if (InterleavedF64.class == input.getClass()) {
				PixelMath.logSign((InterleavedF64) input, value, (InterleavedF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
			}
		} else if( input instanceof Planar ) {
			Planar in = (Planar)input;
			Planar out = (Planar)output;

			for (int i = 0; i < in.getNumBands(); i++) {
				logSign(in.getBand(i), value, out.getBand(i));
			}
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
		}
	}

	/**
	 * Computes the square root of each pixel in the input image. Both the input and output image can be the
	 * same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the inverted image is written to. Modified.
	 */
	public static <T extends ImageBase<T>> void sqrt( T input , T output )
	{
		if( input instanceof ImageGray) {
			if (GrayF32.class == input.getClass()) {
				PixelMath.sqrt((GrayF32) input, (GrayF32) output);
			} else if (GrayF64.class == input.getClass()) {
				PixelMath.sqrt((GrayF64) input, (GrayF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
			}
		} else if( input instanceof ImageInterleaved ) {
			if (InterleavedF32.class == input.getClass()) {
				PixelMath.sqrt((InterleavedF32) input, (InterleavedF32) output);
			} else if (InterleavedF64.class == input.getClass()) {
				PixelMath.sqrt((InterleavedF64) input, (InterleavedF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
			}
		} else if( input instanceof Planar ) {
			Planar in = (Planar)input;
			Planar out = (Planar)output;

			for (int i = 0; i < in.getNumBands(); i++) {
				sqrt(in.getBand(i), out.getBand(i));
			}
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+input.getClass().getSimpleName());
		}
	}

	/**
	 * <p>
	 * Performs pixel-wise division<br>
	 * output(x,y) = imgA(x,y) / imgB(x,y)
	 * </p>
	 * Only floating point images are supported. If the numerator has multiple bands and the denominator is a single
	 * band then the denominator will divide each band.
	 *
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Modified.
	 */
	public static <N extends ImageBase,D extends ImageBase<D>> void divide(N imgA, D imgB , N output)
	{
		if( imgA instanceof ImageGray && imgB instanceof ImageGray ) {
			if (GrayF32.class == imgA.getClass()) {
				PixelMath.divide((GrayF32) imgA, (GrayF32) imgB, (GrayF32) output);
			} else if (GrayF64.class == imgA.getClass()) {
				PixelMath.divide((GrayF64) imgA, (GrayF64) imgB, (GrayF64) output);
			}
		} else if( imgA instanceof Planar && imgB instanceof ImageGray ) {
			Planar in = (Planar) imgA;
			Planar out = (Planar) output;

			for (int i = 0; i < in.getNumBands(); i++) {
				if (GrayF32.class == imgB.getClass()) {
					PixelMath.divide((GrayF32) in.getBand(i), (GrayF32) imgB, (GrayF32) out.getBand(i));
				} else if (GrayF64.class == imgB.getClass()) {
					PixelMath.divide((GrayF64) in.getBand(i), (GrayF64) imgB, (GrayF64) out.getBand(i));
				}
			}
		} else if( imgA instanceof Planar && imgB instanceof Planar ) {
			Planar inA = (Planar) imgA;
			Planar inB = (Planar) imgB;
			Planar out = (Planar) output;

			for (int i = 0; i < inA.getNumBands(); i++) {
				divide(inA.getBand(i), inB.getBand(i), out.getBand(i));
			}
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+imgA.getClass().getSimpleName());
		}
	}

	/**
	 * <p>
	 * Performs pixel-wise multiplication<br>
	 * output(x,y) = imgA(x,y) * imgB(x,y)
	 * </p>
	 * Only floating point images are supported. If one image has multiple bands and the other is gray then
	 * the gray image will be multiplied by each band in the multiple band image.
	 *
	 * @param imgA Input image. Not modified.
	 * @param imgB Input image. Not modified.
	 * @param output Output image. Modified.
	 */
	public static <N extends ImageBase,D extends ImageBase<D>> void multiply(N imgA, D imgB , N output)
	{
		if( imgA instanceof ImageGray && imgB instanceof ImageGray ) {
			if (GrayF32.class == imgA.getClass()) {
				PixelMath.multiply((GrayF32) imgA, (GrayF32) imgB, (GrayF32) output);
			} else if (GrayF64.class == imgA.getClass()) {
				PixelMath.multiply((GrayF64) imgA, (GrayF64) imgB, (GrayF64) output);
			}
		} else if( imgA instanceof Planar && imgB instanceof Planar ) {
			Planar inA = (Planar) imgA;
			Planar inB = (Planar) imgB;
			Planar out = (Planar) output;

			for (int i = 0; i < inA.getNumBands(); i++) {
				multiply(inA.getBand(i), inB.getBand(i), out.getBand(i));
			}
		} else if( imgA instanceof Planar || imgB instanceof Planar ) {
			Planar in;
			ImageGray gray;
			Planar out = (Planar) output;

			if( imgA instanceof Planar ) {
				in = (Planar)imgA;
				gray = (ImageGray)imgB;
			} else {
				in = (Planar)imgB;
				gray = (ImageGray)imgA;
			}

			for (int i = 0; i < in.getNumBands(); i++) {
				if (GrayF32.class == gray.getClass()) {
					PixelMath.multiply((GrayF32) in.getBand(i), (GrayF32) gray, (GrayF32) out.getBand(i));
				} else if (GrayF64.class == gray.getClass()) {
					PixelMath.multiply((GrayF64) in.getBand(i), (GrayF64) gray, (GrayF64) out.getBand(i));
				}
			}
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+imgA.getClass().getSimpleName());
		}
	}

	/**
	 * Raises each pixel in the input image to the power of two. Both the input and output image can be the same
	 * instance.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the pow2 image is written to. Modified.
	 */
	public static <A extends ImageBase<A>,B extends ImageBase<B>>
	void pow2(A input , B output ) {
		if( input instanceof ImageGray ) {
			if (GrayU8.class == input.getClass()) {
				PixelMath.pow2((GrayU8) input, (GrayU16) output);
			} else if (GrayU16.class == input.getClass()) {
				PixelMath.pow2((GrayU16) input, (GrayS32) output);
			} else if (GrayF32.class == input.getClass()) {
				PixelMath.pow2((GrayF32) input, (GrayF32) output);
			} else if (GrayF64.class == input.getClass()) {
				PixelMath.pow2((GrayF64) input, (GrayF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
			}
		} else if( input instanceof ImageInterleaved ) {
			if (InterleavedU8.class == input.getClass()) {
				PixelMath.pow2((InterleavedU8) input, (InterleavedU16) output);
			} else if (InterleavedU16.class == input.getClass()) {
				PixelMath.pow2((InterleavedU16) input, (InterleavedS32) output);
			} else if (InterleavedF32.class == input.getClass()) {
				PixelMath.pow2((InterleavedF32) input, (InterleavedF32) output);
			} else if (InterleavedF64.class == input.getClass()) {
				PixelMath.pow2((InterleavedF64) input, (InterleavedF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
			}
		} else if( input instanceof Planar ) {
			Planar in = (Planar)input;
			Planar out = (Planar)output;

			for (int i = 0; i < in.getNumBands(); i++) {
				pow2( in.getBand(i), out.getBand(i));
			}
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
	public static <T extends ImageBase<T>, O extends ImageBase>
	void add(T inputA, T inputB, O output) {
		if( inputA instanceof ImageGray) {
			if (GrayU8.class == inputA.getClass()) {
				PixelMath.add((GrayU8) inputA, (GrayU8) inputB, (GrayU16) output);
			} else if (GrayS8.class == inputA.getClass()) {
				PixelMath.add((GrayS8) inputA, (GrayS8) inputB, (GrayS16) output);
			} else if (GrayU16.class == inputA.getClass()) {
				PixelMath.add((GrayU16) inputA, (GrayU16) inputB, (GrayS32) output);
			} else if (GrayS16.class == inputA.getClass()) {
				PixelMath.add((GrayS16) inputA, (GrayS16) inputB, (GrayS32) output);
			} else if (GrayS32.class == inputA.getClass()) {
				PixelMath.add((GrayS32) inputA, (GrayS32) inputB, (GrayS32) output);
			} else if (GrayS64.class == inputA.getClass()) {
				PixelMath.add((GrayS64) inputA, (GrayS64) inputB, (GrayS64) output);
			} else if (GrayF32.class == inputA.getClass()) {
				PixelMath.add((GrayF32) inputA, (GrayF32) inputB, (GrayF32) output);
			} else if (GrayF64.class == inputA.getClass()) {
				PixelMath.add((GrayF64) inputA, (GrayF64) inputB, (GrayF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + inputA.getClass().getSimpleName());
			}
		} else if (inputA instanceof Planar) {
			Planar inA = (Planar)inputA;
			Planar inB = (Planar)inputB;
			Planar out = (Planar)output;

			for (int i = 0; i < inA.getNumBands(); i++) {
				add(inA.getBand(i),inB.getBand(i),out.getBand(i));
			}
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+inputA.getClass().getSimpleName());
		}
	}

	/**
	 * Bounds image pixels to be between these two values.
	 *
	 * @param input Input image.
	 * @param min minimum value. Inclusive.
	 * @param max maximum value. Inclusive.
	 */
	public static <T extends ImageBase<T>> void boundImage(T input , double min , double max ) {
		if( input instanceof ImageGray ) {
			if (GrayU8.class == input.getClass()) {
				PixelMath.boundImage((GrayU8) input, (int) min, (int) max);
			} else if (GrayS8.class == input.getClass()) {
				PixelMath.boundImage((GrayS8) input, (int) min, (int) max);
			} else if (GrayU16.class == input.getClass()) {
				PixelMath.boundImage((GrayU16) input, (int) min, (int) max);
			} else if (GrayS16.class == input.getClass()) {
				PixelMath.boundImage((GrayS16) input, (int) min, (int) max);
			} else if (GrayS32.class == input.getClass()) {
				PixelMath.boundImage((GrayS32) input, (int) min, (int) max);
			} else if (GrayS64.class == input.getClass()) {
				PixelMath.boundImage((GrayS64) input, (long) min, (long) max);
			} else if (GrayF32.class == input.getClass()) {
				PixelMath.boundImage((GrayF32) input, (float) min, (float) max);
			} else if (GrayF64.class == input.getClass()) {
				PixelMath.boundImage((GrayF64) input, min, max);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + input.getClass().getSimpleName());
			}
		} else if( input instanceof Planar ) {
			Planar in = (Planar)input;

			for (int i = 0; i < in.getNumBands(); i++) {
				boundImage( in.getBand(i), min, max);
			}
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
	public static <T extends ImageBase<T>> void diffAbs(T inputA , T inputB , T output) {
		if( inputA instanceof ImageGray ) {
			if (GrayU8.class == inputA.getClass()) {
				PixelMath.diffAbs((GrayU8) inputA, (GrayU8) inputB, (GrayU8) output);
			} else if (GrayS8.class == inputA.getClass()) {
				PixelMath.diffAbs((GrayS8) inputA, (GrayS8) inputB, (GrayS8) output);
			} else if (GrayU16.class == inputA.getClass()) {
				PixelMath.diffAbs((GrayU16) inputA, (GrayU16) inputB, (GrayU16) output);
			} else if (GrayS16.class == inputA.getClass()) {
				PixelMath.diffAbs((GrayS16) inputA, (GrayS16) inputB, (GrayS16) output);
			} else if (GrayS32.class == inputA.getClass()) {
				PixelMath.diffAbs((GrayS32) inputA, (GrayS32) inputB, (GrayS32) output);
			} else if (GrayS64.class == inputA.getClass()) {
				PixelMath.diffAbs((GrayS64) inputA, (GrayS64) inputB, (GrayS64) output);
			} else if (GrayF32.class == inputA.getClass()) {
				PixelMath.diffAbs((GrayF32) inputA, (GrayF32) inputB, (GrayF32) output);
			} else if (GrayF64.class == inputA.getClass()) {
				PixelMath.diffAbs((GrayF64) inputA, (GrayF64) inputB, (GrayF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + inputA.getClass().getSimpleName());
			}
		} else if( inputA instanceof Planar ) {
			Planar inA = (Planar)inputA;
			Planar inB = (Planar)inputB;
			Planar out = (Planar)output;

			for (int i = 0; i < inA.getNumBands(); i++) {
				diffAbs( inA.getBand(i), inB.getBand(i), out.getBand(i));
			}
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+inputA.getClass().getSimpleName());
		}
	}

	/**
	 * Computes the standard deviation of each pixel in a local region.
	 *
	 * @param mean (Input) Image with local mean
	 * @param pow2 (Input) Image with local mean pixel-wise power of 2
	 * @param stdev (Output) standard deviation of each pixel. Can be same instance as either input.
	 */
	public static <T extends ImageBase<T>, B extends ImageBase<B>> void stdev(T mean , B pow2 , T stdev ) {
		if( mean instanceof ImageGray ) {
			if (GrayU8.class == mean.getClass()) {
				PixelMath.stdev((GrayU8) mean, (GrayU16) pow2, (GrayU8) stdev);
			} else if (GrayU16.class == mean.getClass()) {
				PixelMath.stdev((GrayU16) mean, (GrayS32) pow2, (GrayU16) stdev);
			} else if (GrayF32.class == mean.getClass()) {
				PixelMath.stdev((GrayF32) mean, (GrayF32) pow2, (GrayF32) stdev);
			} else if (GrayF64.class == mean.getClass()) {
				PixelMath.stdev((GrayF64) mean, (GrayF64) pow2, (GrayF64) stdev);
			} else {
				throw new IllegalArgumentException("Unknown image Type: " + mean.getClass().getSimpleName());
			}
		} else if( mean instanceof Planar ) {
			Planar inA = (Planar)mean;
			Planar inB = (Planar)pow2;
			Planar out = (Planar)stdev;

			for (int i = 0; i < inA.getNumBands(); i++) {
				stdev( inA.getBand(i), inB.getBand(i), out.getBand(i));
			}
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+mean.getClass().getSimpleName());
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
	public static <T extends ImageBase<T>, O extends ImageBase>
	void subtract(T inputA, T inputB, O output) {
		if( inputA instanceof ImageGray){
			if( GrayU8.class == inputA.getClass() ) {
				PixelMath.subtract((GrayU8) inputA, (GrayU8)inputB, (GrayI16) output);
			} else if( GrayS8.class == inputA.getClass() ) {
				PixelMath.subtract((GrayS8) inputA, (GrayS8)inputB, (GrayS16) output);
			} else if( GrayU16.class == inputA.getClass() ) {
				PixelMath.subtract((GrayU16) inputA, (GrayU16)inputB, (GrayS32) output);
			} else if( GrayS16.class == inputA.getClass() ) {
				PixelMath.subtract((GrayS16) inputA, (GrayS16)inputB, (GrayS32) output);
			} else if( GrayS32.class == inputA.getClass() ) {
				PixelMath.subtract((GrayS32) inputA, (GrayS32)inputB, (GrayS32) output);
			} else if( GrayS64.class == inputA.getClass() ) {
				PixelMath.subtract((GrayS64) inputA, (GrayS64)inputB, (GrayS64) output);
			} else if( GrayF32.class == inputA.getClass() ) {
				PixelMath.subtract((GrayF32) inputA, (GrayF32)inputB, (GrayF32) output);
			} else if( GrayF64.class == inputA.getClass() ) {
				PixelMath.subtract((GrayF64) inputA, (GrayF64)inputB, (GrayF64) output);
			} else {
				throw new IllegalArgumentException("Unknown image Type: "+inputA.getClass().getSimpleName());
			}
		} else if (inputA instanceof Planar) {
			Planar inA = (Planar)inputA;
			Planar inB = (Planar)inputB;
			Planar out = (Planar)output;

			for (int i = 0; i < inA.getNumBands(); i++) {
				subtract(inA.getBand(i),inB.getBand(i),out.getBand(i));
			}
		} else {
			throw new IllegalArgumentException("Unknown image Type: "+inputA.getClass().getSimpleName());
		}
	}


}
