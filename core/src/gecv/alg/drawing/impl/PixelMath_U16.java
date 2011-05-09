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

package gecv.alg.drawing.impl;

import gecv.struct.image.ImageUInt16;

/**
 * @author Peter Abeles
 */
public class PixelMath_U16 {

	public static int max( ImageUInt16 image ) {

		int max = 0;

		for( int y = 0; y < image.height; y++ ) {
			int index = image.startIndex + y*image.stride;
			int end = index + image.width;

			for( ; index < end; index++ ) {
				int v = image.data[index] & 0xFFFF;
				if( v > max )
					max = v;
			}
		}
		return max;
	}

	public static int min( ImageUInt16 image ) {

		int min = Integer.MAX_VALUE;

		for( int y = 0; y < image.height; y++ ) {
			int index = image.startIndex + y*image.stride;
			int end = index + image.width;

			for( ; index < end; index++ ) {
				int v = image.data[index] & 0xFFFF;
				if( v < min )
					min = v;
			}
		}
		return min;
	}

	public static void divide( ImageUInt16 image , int denominator ) {

		if( denominator < 0 )
			throw new IllegalArgumentException("Can't divide an unsigned image by a negative number.");

		for( int y = 0; y < image.height; y++ ) {
			int index = image.startIndex + y*image.stride;
			int end = index + image.width;

			for( ; index < end; index++ ) {
				int r = (image.data[index] & 0xFFFF) / denominator;
				image.data[index] = (short)r;
			}
		}
	}

	public static void mult( ImageUInt16 image , int scale ) {

		if( scale < 0 )
			throw new IllegalArgumentException("Can't scale an unsigned image by a negative number.");

		for( int y = 0; y < image.height; y++ ) {
			int index = image.startIndex + y*image.stride;
			int end = index + image.width;

			for( ; index < end; index++ ) {
				int val = (image.data[index] & 0xFFFF) * scale;
				if( val > 0xFFFF ) val = 0xFFFF;

				image.data[index] = (short)val;
			}
		}
	}

	public static void plus( ImageUInt16 image , int value )
	{
		for( int y = 0; y < image.height; y++ ) {
			int index = image.startIndex + y*image.stride;
			int end = index + image.width;

			for( ; index < end; index++ ) {
				int val = (image.data[index] & 0xFFFF) + value;
				if( val < 0 ) val = 0;
				else if( val > 0xFFFF ) val = 0xFFFF;

				image.data[index] = (short)val;
			}
		}
	}

}
