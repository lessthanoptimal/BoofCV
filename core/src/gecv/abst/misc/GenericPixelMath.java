/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.abst.misc;

import gecv.alg.misc.PixelMath;
import gecv.struct.image.*;

/**
 * Image type agnostic implementation of {@link PixelMath}.
 *
 * @author Peter Abeles
 */
public class GenericPixelMath {

	/**
	 * Sets each pixel in the output image to be the absolute value of the input image.
	 * Both the input and output image can be the same instance.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the absolute value image is written to. Modified.
	 */
	public static <T extends ImageBase> void abs( T input , T output )
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
	public static double maxAbs( ImageBase input ) {
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
	 * Divides each element by the denominator. Both input and output images can
	 * be the same.
	 *
	 * @param input The input image. Not modified.
	 * @param output The output image. Modified.
	 * @param denominator What each element is divided by.
	 */
	public static <T extends ImageBase> void divide( T input , T output , double denominator ) {

		if( ImageInteger.class.isAssignableFrom(input.getClass())) {
			int denominatorI = (int)denominator;

			if( ImageUInt8.class == input.getClass() ) {
				PixelMath.divide((ImageUInt8)input,(ImageUInt8)output, denominatorI);
			} else if( ImageSInt8.class == input.getClass() ) {
				PixelMath.divide((ImageSInt8)input,(ImageSInt8)output, denominatorI);
			} else if( ImageUInt16.class == input.getClass() ) {
				PixelMath.divide((ImageUInt16)input,(ImageUInt16)output, denominatorI);
			} else if( ImageSInt16.class == input.getClass() ) {
				PixelMath.divide((ImageSInt16)input,(ImageSInt16)output, denominatorI);
			} else if( ImageSInt32.class == input.getClass() ) {
				PixelMath.divide((ImageSInt32)input,(ImageSInt32)output, denominatorI);
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
	public static <T extends ImageBase> void multiply( T input , T output , double scale ) {

		if( ImageInteger.class.isAssignableFrom(input.getClass())) {
			int scaleI = (int)scale;

			if( ImageUInt8.class == input.getClass() ) {
				PixelMath.multiply((ImageUInt8)input,(ImageUInt8)output, scaleI);
			} else if( ImageSInt8.class == input.getClass() ) {
				PixelMath.multiply((ImageSInt8)input,(ImageSInt8)output, scaleI);
			} else if( ImageUInt16.class == input.getClass() ) {
				PixelMath.multiply((ImageUInt16)input,(ImageUInt16)output, scaleI);
			} else if( ImageSInt16.class == input.getClass() ) {
				PixelMath.multiply((ImageSInt16)input,(ImageSInt16)output, scaleI);
			} else if( ImageSInt32.class == input.getClass() ) {
				PixelMath.multiply((ImageSInt32)input,(ImageSInt32)output, scaleI);
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
	public static <T extends ImageBase> void plus( T input , T output, double value ) {
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
}
