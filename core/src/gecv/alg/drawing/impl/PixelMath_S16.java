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

import gecv.struct.image.ImageSInt16;

/**
 * @author Peter Abeles
 */
public class PixelMath_S16 {

	public static int maxAbs( ImageSInt16 image ) {

		int max = 0;

		for( int y = 0; y < image.height; y++ ) {
			int index = image.startIndex + y*image.stride;
			int end = index + image.width;

			for( ; index < end; index++ ) {
				int v = Math.abs(image.data[index]);
				if( v > max )
					max = v;
			}
		}
		return max;
	}

	public static void abs( ImageSInt16 image ) {
		for( int y = 0; y < image.height; y++ ) {
			int index = image.startIndex + y*image.stride;
			int end = index + image.width;

			for( ; index < end; index++ ) {
				image.data[index] = (short)Math.abs(image.data[index]);
			}
		}
	}

	public static void divide( ImageSInt16 image , int denominator ) {

		for( int y = 0; y < image.height; y++ ) {
			int index = image.startIndex + y*image.stride;
			int end = index + image.width;

			for( ; index < end; index++ ) {
				image.data[index] /= denominator;
			}
		}
	}

	public static void mult( ImageSInt16 image , int scale ) {

		for( int y = 0; y < image.height; y++ ) {
			int index = image.startIndex + y*image.stride;
			int end = index + image.width;

			for( ; index < end; index++ ) {
				int val = image.data[index] * scale;
				if( val < Short.MIN_VALUE ) val = Short.MIN_VALUE;
				else if( val > Short.MAX_VALUE ) val = Short.MAX_VALUE;

				image.data[index] = (short)val;
			}
		}
	}

	public static void plus( ImageSInt16 image , int value )
	{
		for( int y = 0; y < image.height; y++ ) {
			int index = image.startIndex + y*image.stride;
			int end = index + image.width;

			for( ; index < end; index++ ) {
				int val = image.data[index] + value;
				if( val < Short.MIN_VALUE ) val = Short.MIN_VALUE;
				else if( val > Short.MAX_VALUE ) val = Short.MAX_VALUE;

				image.data[index] = (short)val;
			}
		}
	}

}
