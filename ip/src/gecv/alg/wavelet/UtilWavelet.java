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

package gecv.alg.wavelet;

import gecv.struct.image.ImageBase;
import gecv.struct.wavelet.WaveletDesc;


/**
 * @author Peter Abeles
 */
public class UtilWavelet {

	/**
	 * The original image can have an even or odd number of width/height.  While the transformed
	 * image must have an even number of pixels.  If the original image is even then the sames
	 * are the same, otherwise the transformed image's shape is rounded up.
	 *
	 * @param original Original input image.
	 * @param transformed Image which has been transformed.
	 */
	public static void checkShape( ImageBase original , ImageBase transformed )
	{
		if( transformed.width % 2 == 1 )
			throw new IllegalArgumentException("Transformed image must have an even width.");
		if( transformed.height % 2 == 1 )
			throw new IllegalArgumentException("Transformed image must have an even height.");

		int w = original.width + original.width%2;
		int h = original.height + original.height%2;

		if( w != transformed.width || h != transformed.height )
			throw new IllegalArgumentException("Input images do not have compatible shapes.");
	}

	/**
	 * <p>
	 * Compute the energy of the specified array.
	 * </p>
	 *
	 * <p>
	 * E = sum( i=1..N , a[i]*a[i] )
	 * </p>
	 */
	public static double computeEnergy( float []array  ) {
		double total = 0;

		for( int i = 0; i < array.length; i++ ) {
			total += array[i]*array[i];
		}

		return total;
	}

	/**
	 * <p>
	 * Compute the energy of the specified array.
	 * </p>
	 *
	 * <p>
	 * E = sum( i=1..N , a[i]*a[i] ) / (N*d*d)
	 * </p>
	 */
	public static double computeEnergy( int []array  , int denominator) {
		double total = 0;

		for( int i = 0; i < array.length; i++ ) {
			total += array[i]*array[i];
		}

		total /= denominator*denominator;

		return total;
	}

	public static double sumCoefficients( float []array  ) {
		double total = 0;

		for( int i = 0; i < array.length; i++ ) {
			total += array[i];
		}

		return total;
	}

	public static int sumCoefficients( int []array  ) {
		int total = 0;

		for( int i = 0; i < array.length; i++ ) {
			total += array[i];
		}

		return total;
	}

	public static int computeBorderStart( WaveletDesc desc ) {
		int ret =  -Math.min(desc.offsetScaling,desc.offsetWavelet);

		return ret + (ret % 2);
	}

	public static int computeBorderEnd( WaveletDesc desc , int sideLength ) {
		int w = Math.max( desc.offsetScaling+desc.getScalingLength() , desc.offsetWavelet+desc.getWaveletLength());
		w += sideLength % 2;

		return Math.max((w + (w%2))-2,0);
	}
}
