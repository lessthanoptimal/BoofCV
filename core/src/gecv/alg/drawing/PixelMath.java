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

package gecv.alg.drawing;

import gecv.alg.drawing.impl.PixelMath_F32;
import gecv.alg.drawing.impl.PixelMath_S16;
import gecv.alg.drawing.impl.PixelMath_U16;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt16;

/**
 * Standard mathematical operations performed on a per-pixel basis or computed across the whole image.
 *
 * @author Peter Abeles
 */
// todo make sure an image can be rescaled from 16bit to 8bit 
public class PixelMath {

	public static float maxAbs( ImageFloat32 image ) {
		return PixelMath_F32.maxAbs(image);
	}

	public static int maxAbs( ImageSInt16 image ) {
		return PixelMath_S16.maxAbs(image);
	}

	public static int maxAbs( ImageUInt16 image ) {
		return PixelMath_U16.max(image);
	}

	public static void abs( ImageFloat32 image ) {
		PixelMath_F32.abs(image);
	}

	public static void abs( ImageSInt16 image ) {
		PixelMath_S16.abs(image);
	}

	public static void divide( ImageFloat32 image , float denominator ) {
		PixelMath_F32.divide(image,denominator);
	}

	public static void divide( ImageSInt16 image , int denominator ) {
		PixelMath_S16.divide(image,denominator);
	}

	public static void divide( ImageUInt16 image , int denominator ) {
		PixelMath_U16.divide(image,denominator);
	}

	public static void mult( ImageFloat32 image , float scale ) {
		PixelMath_F32.mult(image,scale);
	}

	public static void mult( ImageSInt16 image , int scale ) {
		PixelMath_S16.mult(image,scale);
	}

	public static void mult( ImageUInt16 image , int scale ) {
		PixelMath_U16.mult(image,scale);
	}

	public static void plus( ImageFloat32 image , float value ) {
		PixelMath_F32.plus(image,value);
	}

	public static void plus( ImageSInt16 image , int value ) {
		PixelMath_S16.plus(image,value);
	}

	public static void plus( ImageUInt16 image , int value ) {
		PixelMath_U16.plus(image,value);
	}
}
