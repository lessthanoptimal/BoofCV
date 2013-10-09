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

package boofcv.alg.transform.fft;

import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.image.ImageFloat32;

/**
 * @author Peter Abeles
 */
public class DiscreteFourierTransformOps {
	/**
	 * true if the number provided is a power of two
	 * @param x number
	 * @return true if it is a power of two
	 */
	public static boolean isPowerOf2(int x) {
		if (x <= 0)
			return false;
		else
			return (x & (x - 1)) == 0;
	}

	/**
	 * Returns the closest power-of-two number greater than or equal to x.
	 *
	 * @param x
	 * @return the closest power-of-two number greater than or equal to x
	 */
	public static int nextPow2(int x) {
		if (x < 1)
			throw new IllegalArgumentException("x must be greater or equal 1");
		if ((x & (x - 1)) == 0) {
			return x; // x is already a power-of-two number
		}
		x |= (x >>> 1);
		x |= (x >>> 2);
		x |= (x >>> 4);
		x |= (x >>> 8);
		x |= (x >>> 16);
		x |= (x >>> 32);
		return x + 1;
	}

	/**
	 * Checks to see if the image and its transform are appropriate sizes .  The transform should have
	 * twice the width and twice the height as the image.
	 *
	 * @param image Storage for an image
	 * @param transform Storage for a Fourier Transform
	 */
	public static void checkImageArguments( ImageFloat32 image , ImageFloat32 transform ) {
		if( image.width*2 != transform.width )
			throw new IllegalArgumentException("Transform must be twice the width of the input image");
		if( image.height*2 != transform.height )
			throw new IllegalArgumentException("Transform must be twice the height of the input image");
	}

	public static boolean isOptimalSize( ImageFloat32 image ) {
		return false;
	}

	public static int optimalSize( int x ) {
		return 0;
	}

	public static void centerZeroFrequency( ImageFloat32 transform ) {

	}

	public static void undoCenterZeroFrequency( ImageFloat32 transform ) {

	}


	public static void resizeOptimal( ImageFloat32 input , ImageFloat32 output , FftBorderType type , float value )
	{

	}

	public static void cropFromOptimal( ImageFloat32 input , ImageFloat32 output ) {

	}

	public static void insertKernel( Kernel2D_F32 kenel , ImageFloat32 image ) {

	}

	public static void split( ImageFloat32 complex , ImageFloat32 real , ImageFloat32 imaginary ) {

	}

	public static void merge( ImageFloat32 real , ImageFloat32 imaginary , ImageFloat32 complex ) {

	}

	public static void magnitude( ImageFloat32 transform , ImageFloat32 magnitude ) {

	}
}
