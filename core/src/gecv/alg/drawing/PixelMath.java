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
import gecv.alg.drawing.impl.PixelMath_Signed_I16;
import gecv.alg.drawing.impl.PixelMath_Unsigned_I16;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt16;

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

	public static int maxAbs( ImageInt16 image ) {
		if( image.isSigned() )
			return PixelMath_Signed_I16.maxAbs(image);
		else
			return PixelMath_Unsigned_I16.max(image);
	}

	public static void abs( ImageFloat32 image ) {
		PixelMath_F32.abs(image);
	}

	public static void abs( ImageInt16 image ) {
		if( image.isSigned() )
			PixelMath_Signed_I16.abs(image);
	}

	public static void divide( ImageFloat32 image , float denominator ) {
		PixelMath_F32.divide(image,denominator);
	}

	public static void divide( ImageInt16 image , int denominator ) {
		if( image.isSigned() )
			PixelMath_Signed_I16.divide(image,denominator);
		else
			PixelMath_Unsigned_I16.divide(image,denominator);
	}

	public static void mult( ImageFloat32 image , float scale ) {
		PixelMath_F32.mult(image,scale);
	}

	public static void mult( ImageInt16 image , int scale ) {
		if( image.isSigned() )
			PixelMath_Signed_I16.mult(image,scale);
		else
			PixelMath_Unsigned_I16.mult(image,scale);
	}

	public static void plus( ImageFloat32 image , float value ) {
		PixelMath_F32.plus(image,value);
	}

	public static void plus( ImageInt16 image , int value ) {
		if( image.isSigned() )
			PixelMath_Signed_I16.plus(image,value);
		else
			PixelMath_Unsigned_I16.plus(image,value);
	}
}
