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

import gecv.struct.image.ImageFloat32;

/**
 * @author Peter Abeles
 */
public class PixelMath_F32 {

	public static float maxAbs( ImageFloat32 image ) {

		float max = 0f;

		for( int y = 0; y < image.height; y++ ) {
			int index = image.startIndex + y*image.stride;
			int end = index + image.width;

			for( ; index < end; index++ ) {
				float v = Math.abs(image.data[index]);
				if( v > max )
					max = v;
			}
		}
		return max;
	}

	public static void abs( ImageFloat32 image ) {

		for( int y = 0; y < image.height; y++ ) {
			int index = image.startIndex + y*image.stride;
			int end = index + image.width;

			for( ; index < end; index++ ) {
				image.data[index] = Math.abs(image.data[index]);
			}
		}
	}

	public static void divide( ImageFloat32 image , float denominator ) {

		for( int y = 0; y < image.height; y++ ) {
			int index = image.startIndex + y*image.stride;
			int end = index + image.width;

			for( ; index < end; index++ ) {
				image.data[index] /= denominator;
			}
		}
	}

	public static void mult( ImageFloat32 image , float scale ) {

		for( int y = 0; y < image.height; y++ ) {
			int index = image.startIndex + y*image.stride;
			int end = index + image.width;

			for( ; index < end; index++ ) {
				image.data[index] *= scale;
			}
		}
	}

	public static void plus( ImageFloat32 image , float value )
	{
		for( int y = 0; y < image.height; y++ ) {
			int index = image.startIndex + y*image.stride;
			int end = index + image.width;

			for( ; index < end; index++ ) {
				image.data[index] += value;
			}
		}
	}

	public static void minus( ImageFloat32 image , float value )
	{
		for( int y = 0; y < image.height; y++ ) {
			int index = image.startIndex + y*image.stride;
			int end = index + image.width;

			for( ; index < end; index++ ) {
				image.data[index] -= value;
			}
		}
	}

}
