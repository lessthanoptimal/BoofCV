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

import boofcv.abst.transform.fft.DiscreteFourierTransform;
import boofcv.abst.transform.fft.GeneralFft_to_DiscreteFourierTransform_F32;
import boofcv.abst.transform.fft.GeneralFft_to_DiscreteFourierTransform_F64;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageFloat64;

/**
 * @author Peter Abeles
 */
public class DiscreteFourierTransformOps {

	public static DiscreteFourierTransform<ImageFloat32>  createTransformF32() {
		return new GeneralFft_to_DiscreteFourierTransform_F32();
	}

	public static DiscreteFourierTransform<ImageFloat64>  createTransformF64() {
		return new GeneralFft_to_DiscreteFourierTransform_F64();
	}

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
	public static void checkImageArguments( ImageBase image , ImageBase transform ) {
		if( image.width*2 != transform.width )
			throw new IllegalArgumentException("Transform must be twice the width of the input image");
		if( image.height != transform.height )
			throw new IllegalArgumentException("Transform have the same height of the input image");
	}

	public static boolean isOptimalSize( ImageFloat32 image ) {
		return false;
	}

	public static int optimalSize( int x ) {
		return 0;
	}

	// TODO will this work for even and odd length images?
	// TODO do in a single pass
	public static void centerZeroFrequency( ImageFloat32 transform ) {

		if( transform.width%2 != 0 || transform.height%2 != 0 )
			throw new IllegalArgumentException("Not uspported et");

		int hw = transform.width/2;
		int hh = transform.height/2;

		for( int y = 0; y < transform.height; y++ ) {

			int indexTran = transform.startIndex + y*transform.stride;

			for( int x = 0; x < hw; x += 2, indexTran += 2 ) {
				float ra = transform.data[indexTran];
				float ia = transform.data[indexTran+1];

				transform.data[indexTran] = transform.data[indexTran+hw];
				transform.data[indexTran+1] = transform.data[indexTran+1+hw];

				transform.data[indexTran+hw] = ra;
				transform.data[indexTran+hw+1] = ia;
			}
		}

		int stepY = hh*transform.stride;

		for( int x = 0; x < transform.width; x += 2 ) {

			int indexTran = transform.startIndex + x;

			for( int y = 0; y < hh; y++ , indexTran += transform.stride ) {
				float ra = transform.data[indexTran];
				float ia = transform.data[indexTran+1];

				transform.data[indexTran] = transform.data[indexTran+stepY];
				transform.data[indexTran+1] = transform.data[indexTran+1+stepY];

				transform.data[indexTran+stepY] = ra;
				transform.data[indexTran+stepY+1] = ia;
			}
		}
	}

	public static void resizeOptimal( ImageFloat32 input , ImageFloat32 output , FftBorderType type , float value )
	{

	}

	public static void cropFromOptimal( ImageFloat32 input , ImageFloat32 output ) {

	}

	public static void split( ImageFloat32 transform , ImageFloat32 real , ImageFloat32 imaginary ) {
		for( int y = 0; y < transform.height; y++ ) {

			int indexTran = transform.startIndex + y*transform.stride;
			int indexReal = real.startIndex + y*real.stride;
			int indexImg = imaginary.startIndex + y*imaginary.stride;

			for( int x = 0; x < transform.width; x += 2, indexTran += 2 ) {

				real.data[indexReal++] = transform.data[indexTran];
				imaginary.data[indexImg++] = transform.data[indexTran+1];
			}
		}
	}

	public static void merge( ImageFloat32 real , ImageFloat32 imaginary , ImageFloat32 transform ) {
		for( int y = 0; y < transform.height; y++ ) {

			int indexTran = transform.startIndex + y*transform.stride;
			int indexReal = real.startIndex + y*real.stride;
			int indexImg = imaginary.startIndex + y*imaginary.stride;

			for( int x = 0; x < transform.width; x += 2, indexTran += 2 ) {

				transform.data[indexTran] = real.data[indexReal++];
				transform.data[indexTran+1] = imaginary.data[indexImg++];
			}
		}
	}

	public static void magnitude( ImageFloat32 transform , ImageFloat32 magnitude ) {
		for( int y = 0; y < transform.height; y++ ) {

			int indexTran = transform.startIndex + y*transform.stride;
			int indexMag = magnitude.startIndex + y*magnitude.stride;

			for( int x = 0; x < transform.width; x += 2, indexTran += 2 ) {

				float real = transform.data[indexTran];
				float img = transform.data[indexTran+1];

				magnitude.data[indexMag++] = (float)Math.sqrt(real*real + img*img);
			}
		}
	}

	public static void phase( ImageFloat32 transform , ImageFloat32 phase ) {
		for( int y = 0; y < transform.height; y++ ) {

			int indexTran = transform.startIndex + y*transform.stride;
			int indexPhase = phase.startIndex + y*phase.stride;

			for( int x = 0; x < transform.width; x += 2, indexTran += 2 ) {

				float real = transform.data[indexTran];
				float img = transform.data[indexTran+1];

				phase.data[indexPhase++] = (float)Math.atan2(img, real);
			}
		}
	}

	public static void realToComplex( ImageFloat32 real , ImageFloat32 complex ) {
		for( int y = 0; y < complex.height; y++ ) {

			int indexReal = real.startIndex + y*real.stride;
			int indexComplex = complex.startIndex + y*complex.stride;

			for( int x = 0; x < real.width; x++, indexReal++ , indexComplex += 2 ) {
				complex.data[indexComplex] = real.data[indexReal];
				complex.data[indexComplex+1] = 0;
			}
		}
	}

	public static void multiplyRealComplex( ImageFloat32 realA , ImageFloat32 complexB , ImageFloat32 complexC ) {

		if( realA.width*2 != complexB.width) {
			throw new IllegalArgumentException("First needs to be real");
		}

		for( int y = 0; y < realA.height; y++ ) {

			int indexA = realA.startIndex + y*realA.stride;
			int indexB = complexB.startIndex + y*complexB.stride;
			int indexC = complexC.startIndex + y*complexC.stride;

			for( int x = 0; x < realA.width; x++, indexA++ , indexB += 2  ,indexC += 2 ) {

				float real = realA.data[indexA];
				float realB = complexB.data[indexB];
				float imgB = complexB.data[indexB+1];

				complexC.data[indexC] = real*realB;
				complexC.data[indexC+1] = real*imgB;
			}
		}
	}

	public static void multiplyComplex( ImageFloat32 complexA , ImageFloat32 complexB , ImageFloat32 complexC ) {

		for( int y = 0; y < complexA.height; y++ ) {

			int indexA = complexA.startIndex + y*complexA.stride;
			int indexB = complexB.startIndex + y*complexB.stride;
			int indexC = complexC.startIndex + y*complexC.stride;

			for( int x = 0; x < complexA.width; x += 2, indexA += 2 , indexB += 2  ,indexC += 2 ) {

				float realA = complexA.data[indexA];
				float imgA = complexA.data[indexA+1];
				float realB = complexB.data[indexB];
				float imgB = complexB.data[indexB+1];

				complexC.data[indexC] = realA*realB - imgA*imgB;
				complexC.data[indexC+1] = realA*imgB + imgA*realB;
			}
		}
	}
}
