/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.InputSanityCheck;
import boofcv.struct.image.*;

/**
 * Various functions related to {@link DiscreteFourierTransform}.
 *
 * @author Peter Abeles
 */
public class DiscreteFourierTransformOps {

	/**
	 * Creates a {@link DiscreteFourierTransform} for images of type {@link GrayF32}.
	 *
	 * @see GeneralPurposeFFT_F32_2D
	 *
	 * @return {@link DiscreteFourierTransform}
	 */
	public static DiscreteFourierTransform<GrayF32,InterleavedF32>  createTransformF32() {
		return new GeneralFft_to_DiscreteFourierTransform_F32();
	}

	/**
	 * Creates a {@link DiscreteFourierTransform} for images of type {@link GrayF64}.
	 *
	 * @see GeneralPurposeFFT_F64_2D
	 *
	 * @return {@link DiscreteFourierTransform}
	 */
	public static DiscreteFourierTransform<GrayF64,InterleavedF64>  createTransformF64() {
		return new GeneralFft_to_DiscreteFourierTransform_F64();
	}

	/**
	 * true if the number provided is a power of two
	 * @param x number
	 * @return true if it is a power of two
	 */
	public static boolean isPowerOf2(int x) {
		if (x <= 1)
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
			if( x == 1 )
				return 2;
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
	public static void checkImageArguments( ImageBase image , ImageInterleaved transform ) {
		InputSanityCheck.checkSameShape(image,transform);
		if( 2 != transform.getNumBands() )
			throw new IllegalArgumentException("The transform must have two bands");
	}

	/**
	 * Moves the zero-frequency component into the image center (width/2,height/2).   This function can
	 * be called to undo the transform.
	 *
	 * @param transform the DFT which is to be shifted.
	 * @param forward If true then it does the shift in the forward direction.  If false then it undoes the transforms.
	 */
	public static void shiftZeroFrequency(InterleavedF32 transform, boolean forward ) {

		int hw = transform.width/2;
		int hh = transform.height/2;

		if( transform.width%2 == 0 && transform.height%2 == 0 ) {
			// if both sides are even then a simple transform can be done
			for( int y = 0; y < hh; y++ ) {
				for( int x = 0; x < hw; x++ ) {
					float ra = transform.getBand(x,y,0);
					float ia = transform.getBand(x,y,1);

					float rb = transform.getBand(x+hw,y+hh,0);
					float ib = transform.getBand(x+hw,y+hh,1);

					transform.setBand(x,y,0,rb);
					transform.setBand(x,y,1,ib);
					transform.setBand(x+hw,y+hh,0,ra);
					transform.setBand(x+hw,y+hh,1,ia);

					ra = transform.getBand(x+hw,y,0);
					ia = transform.getBand(x+hw,y,1);

					rb = transform.getBand(x,y+hh,0);
					ib = transform.getBand(x,y+hh,1);

					transform.setBand(x+hw,y,0,rb);
					transform.setBand(x+hw,y,1,ib);
					transform.setBand(x,y+hh,0,ra);
					transform.setBand(x,y+hh,1,ia);
				}
			}
		} else {
			// with odd images, the easiest way to do it is by copying the regions
			int w = transform.width;
			int h = transform.height;

			int hw1 = hw + w%2;
			int hh1 = hh + h%2;

			if( forward ) {
				InterleavedF32 storageTL = new InterleavedF32(hw1,hh1,2);
				storageTL.setTo(transform.subimage(0, 0, hw1, hh1, null));

				InterleavedF32 storageTR = new InterleavedF32(hw,hh1,2);
				storageTR.setTo(transform.subimage(hw1, 0, w, hh1, null));

				transform.subimage(0,0,hw,hh, null).setTo(transform.subimage(hw1,hh1,w,h, null));
				transform.subimage(hw,0,w, hh, null).setTo(transform.subimage(0,hh1,hw1,h, null));
				transform.subimage(hw,hh,w,h, null).setTo(storageTL);
				transform.subimage(0,hh,hw,h, null).setTo(storageTR);
			} else {
				InterleavedF32 storageBL = new InterleavedF32(hw,hh1,2);
				storageBL.setTo(transform.subimage(0, hh, hw, h, null));

				InterleavedF32 storageBR = new InterleavedF32(hw1,hh1,2);
				storageBR.setTo(transform.subimage(hw, hh, w, h, null));


				transform.subimage(hw1,hh1,w,h, null).setTo(transform.subimage(0,0,hw,hh, null));
				transform.subimage(0,hh1,hw1,h, null).setTo(transform.subimage(hw,0,w, hh, null));
				transform.subimage(hw1,0,w,hh1, null).setTo(storageBL);
				transform.subimage(0,0,hw1,hh1, null).setTo(storageBR);
			}
		}
	}

	/**
	 * Moves the zero-frequency component into the image center (width/2,height/2).   This function can
	 * be called to undo the transform.
	 *
	 * @param transform the DFT which is to be shifted.
	 * @param forward If true then it does the shift in the forward direction.  If false then it undoes the transforms.
	 */
	public static void shiftZeroFrequency(InterleavedF64 transform, boolean forward ) {

		int hw = transform.width/2;
		int hh = transform.height/2;

		if( transform.width%2 == 0 && transform.height%2 == 0 ) {
			// if both sides are even then a simple transform can be done
			for( int y = 0; y < hh; y++ ) {
				for( int x = 0; x < hw; x++ ) {
					double ra = transform.getBand(x,y,0);
					double ia = transform.getBand(x,y,1);

					double rb = transform.getBand(x+hw,y+hh,0);
					double ib = transform.getBand(x+hw,y+hh,1);

					transform.setBand(x,y,0,rb);
					transform.setBand(x,y,1,ib);
					transform.setBand(x+hw,y+hh,0,ra);
					transform.setBand(x+hw,y+hh,1,ia);

					ra = transform.getBand(x+hw,y,0);
					ia = transform.getBand(x+hw,y,1);

					rb = transform.getBand(x,y+hh,0);
					ib = transform.getBand(x,y+hh,1);

					transform.setBand(x+hw,y,0,rb);
					transform.setBand(x+hw,y,1,ib);
					transform.setBand(x,y+hh,0,ra);
					transform.setBand(x,y+hh,1,ia);
				}
			}
		} else {
			// with odd images, the easiest way to do it is by copying the regions
			int w = transform.width;
			int h = transform.height;

			int hw1 = hw + w%2;
			int hh1 = hh + h%2;

			if( forward ) {
				InterleavedF64 storageTL = new InterleavedF64(hw1,hh1,2);
				storageTL.setTo(transform.subimage(0, 0, hw1, hh1, null));

				InterleavedF64 storageTR = new InterleavedF64(hw,hh1,2);
				storageTR.setTo(transform.subimage(hw1, 0, w, hh1, null));

				transform.subimage(0,0,hw,hh, null).setTo(transform.subimage(hw1,hh1,w,h, null));
				transform.subimage(hw,0,w, hh, null).setTo(transform.subimage(0,hh1,hw1,h, null));
				transform.subimage(hw,hh,w,h, null).setTo(storageTL);
				transform.subimage(0,hh,hw,h, null).setTo(storageTR);
			} else {
				InterleavedF64 storageBL = new InterleavedF64(hw,hh1,2);
				storageBL.setTo(transform.subimage(0, hh, hw, h, null));

				InterleavedF64 storageBR = new InterleavedF64(hw1,hh1,2);
				storageBR.setTo(transform.subimage(hw, hh, w, h, null));


				transform.subimage(hw1,hh1,w,h, null).setTo(transform.subimage(0,0,hw,hh, null));
				transform.subimage(0,hh1,hw1,h, null).setTo(transform.subimage(hw,0,w, hh, null));
				transform.subimage(hw1,0,w,hh1, null).setTo(storageBL);
				transform.subimage(0,0,hw1,hh1, null).setTo(storageBR);
			}
		}
	}

	/**
	 * Computes the magnitude of the complex image:<br>
	 * magnitude = sqrt( real<sup>2</sup> + imaginary<sup>2</sup> )
	 * @param transform (Input)  Complex interleaved image
	 * @param magnitude (Output) Magnitude of image
	 */
	public static void magnitude( InterleavedF32 transform , GrayF32 magnitude ) {
		checkImageArguments(magnitude,transform);

		for( int y = 0; y < transform.height; y++ ) {

			int indexTran = transform.startIndex + y*transform.stride;
			int indexMag = magnitude.startIndex + y*magnitude.stride;

			for( int x = 0; x < transform.width; x++, indexTran += 2 ) {

				float real = transform.data[indexTran];
				float img = transform.data[indexTran+1];

				magnitude.data[indexMag++] = (float)Math.sqrt(real * real + img * img);
			}
		}
	}

	/**
	 * Computes the magnitude of the complex image:<br>
	 * magnitude = sqrt( real<sup>2</sup> + imaginary<sup>2</sup> )
	 * @param transform (Input)  Complex interleaved image
	 * @param magnitude (Output) Magnitude of image
	 */
	public static void magnitude( InterleavedF64 transform , GrayF64 magnitude ) {
		checkImageArguments(magnitude,transform);

		for( int y = 0; y < transform.height; y++ ) {

			int indexTran = transform.startIndex + y*transform.stride;
			int indexMag = magnitude.startIndex + y*magnitude.stride;

			for( int x = 0; x < transform.width; x++, indexTran += 2 ) {

				double real = transform.data[indexTran];
				double img = transform.data[indexTran+1];

				magnitude.data[indexMag++] = Math.sqrt(real * real + img * img);
			}
		}
	}

	/**
	 * Computes the phase of the complex image:<br>
	 * phase = atan2( imaginary , real )
	 * @param transform (Input) Complex interleaved image
	 * @param phase (output) Phase of image
	 */
	public static void phase( InterleavedF32 transform , GrayF32 phase ) {
		checkImageArguments(phase,transform);

		for( int y = 0; y < transform.height; y++ ) {

			int indexTran = transform.startIndex + y*transform.stride;
			int indexPhase = phase.startIndex + y*phase.stride;

			for( int x = 0; x < transform.width; x++, indexTran += 2 ) {

				float real = transform.data[indexTran];
				float img = transform.data[indexTran+1];

				phase.data[indexPhase++] = (float)Math.atan2(img, real);
			}
		}
	}

	/**
	 * Computes the phase of the complex image:<br>
	 * phase = atan2( imaginary , real )
	 * @param transform (Input) Complex interleaved image
	 * @param phase (output) Phase of image
	 */
	public static void phase( InterleavedF64 transform , GrayF64 phase ) {
		checkImageArguments(phase,transform);

		for( int y = 0; y < transform.height; y++ ) {

			int indexTran = transform.startIndex + y*transform.stride;
			int indexPhase = phase.startIndex + y*phase.stride;

			for( int x = 0; x < transform.width; x++, indexTran += 2 ) {

				double real = transform.data[indexTran];
				double img = transform.data[indexTran+1];

				phase.data[indexPhase++] = Math.atan2(img, real);
			}
		}
	}

	/**
	 * Converts a regular image into a complex interleaved image with the imaginary component set to zero.
	 *
	 * @param real (Input) Regular image.
	 * @param complex (Output) Equivalent complex image.
	 */
	public static void realToComplex(GrayF32 real , InterleavedF32 complex ) {
		checkImageArguments(real,complex);
		for( int y = 0; y < complex.height; y++ ) {

			int indexReal = real.startIndex + y*real.stride;
			int indexComplex = complex.startIndex + y*complex.stride;

			for( int x = 0; x < real.width; x++, indexReal++ , indexComplex += 2 ) {
				complex.data[indexComplex] = real.data[indexReal];
				complex.data[indexComplex+1] = 0;
			}
		}
	}

	/**
	 * Converts a regular image into a complex interleaved image with the imaginary component set to zero.
	 *
	 * @param real (Input) Regular image.
	 * @param complex (Output) Equivalent complex image.
	 */
	public static void realToComplex(GrayF64 real , InterleavedF64 complex ) {
		checkImageArguments(real,complex);
		for( int y = 0; y < complex.height; y++ ) {

			int indexReal = real.startIndex + y*real.stride;
			int indexComplex = complex.startIndex + y*complex.stride;

			for( int x = 0; x < real.width; x++, indexReal++ , indexComplex += 2 ) {
				complex.data[indexComplex] = real.data[indexReal];
				complex.data[indexComplex+1] = 0;
			}
		}
	}

	/**
	 * Performs element-wise complex multiplication between a real image and a complex image.
	 *
	 * @param realA (Input) Regular image
	 * @param complexB (Input) Complex image
	 * @param complexC (Output) Complex image
	 */
	public static void multiplyRealComplex( GrayF32 realA ,
											InterleavedF32 complexB , InterleavedF32 complexC ) {

		checkImageArguments(realA,complexB);

		InputSanityCheck.checkSameShape( complexB,complexC);

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

	/**
	 * Performs element-wise complex multiplication between a real image and a complex image.
	 *
	 * @param realA (Input) Regular image
	 * @param complexB (Input) Complex image
	 * @param complexC (Output) Complex image
	 */
	public static void multiplyRealComplex( GrayF64 realA ,
											InterleavedF64 complexB , InterleavedF64 complexC ) {

		checkImageArguments(realA,complexB);

		InputSanityCheck.checkSameShape( complexB,complexC);

		for( int y = 0; y < realA.height; y++ ) {

			int indexA = realA.startIndex + y*realA.stride;
			int indexB = complexB.startIndex + y*complexB.stride;
			int indexC = complexC.startIndex + y*complexC.stride;

			for( int x = 0; x < realA.width; x++, indexA++ , indexB += 2  ,indexC += 2 ) {

				double real = realA.data[indexA];
				double realB = complexB.data[indexB];
				double imgB = complexB.data[indexB+1];

				complexC.data[indexC] = real*realB;
				complexC.data[indexC+1] = real*imgB;
			}
		}
	}

	/**
	 * Performs element-wise complex multiplication between two complex images.
	 *
	 * @param complexA (Input) Complex image
	 * @param complexB (Input) Complex image
	 * @param complexC (Output) Complex image
	 */
	public static void multiplyComplex( InterleavedF32 complexA , InterleavedF32 complexB , InterleavedF32 complexC ) {

		InputSanityCheck.checkSameShape(complexA, complexB,complexC);

		for( int y = 0; y < complexA.height; y++ ) {

			int indexA = complexA.startIndex + y*complexA.stride;
			int indexB = complexB.startIndex + y*complexB.stride;
			int indexC = complexC.startIndex + y*complexC.stride;

			for( int x = 0; x < complexA.width; x++, indexA += 2 , indexB += 2  ,indexC += 2 ) {

				float realA = complexA.data[indexA];
				float imgA = complexA.data[indexA+1];
				float realB = complexB.data[indexB];
				float imgB = complexB.data[indexB+1];

				complexC.data[indexC] = realA*realB - imgA*imgB;
				complexC.data[indexC+1] = realA*imgB + imgA*realB;
			}
		}
	}

	/**
	 * Performs element-wise complex multiplication between two complex images.
	 *
	 * @param complexA (Input) Complex image
	 * @param complexB (Input) Complex image
	 * @param complexC (Output) Complex image
	 */
	public static void multiplyComplex( InterleavedF64 complexA , InterleavedF64 complexB , InterleavedF64 complexC ) {

		InputSanityCheck.checkSameShape(complexA, complexB,complexC);

		for( int y = 0; y < complexA.height; y++ ) {

			int indexA = complexA.startIndex + y*complexA.stride;
			int indexB = complexB.startIndex + y*complexB.stride;
			int indexC = complexC.startIndex + y*complexC.stride;

			for( int x = 0; x < complexA.width; x++, indexA += 2 , indexB += 2  ,indexC += 2 ) {

				double realA = complexA.data[indexA];
				double imgA = complexA.data[indexA+1];
				double realB = complexB.data[indexB];
				double imgB = complexB.data[indexB+1];

				complexC.data[indexC] = realA*realB - imgA*imgB;
				complexC.data[indexC+1] = realA*imgB + imgA*realB;
			}
		}
	}
}
