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

package gecv.alg.wavelet.impl;

import gecv.alg.wavelet.UtilWavelet;
import gecv.alg.wavelet.WaveletDesc_F32;
import gecv.struct.image.ImageFloat32;


/**
 * Standard algorithm for forward and inverse wavelet transform.  Does not handle image borders.
 *
 * @author Peter Abeles
 */
// todo add other image types
public class LevelWaveletTransformInner {

	/**
	 * Performs a single level wavelet transform along the horizontal axis.
	 *
	 * @param coefficients Description of wavelet coefficients.
	 * @param input Input image which is being transform. Not modified.
	 * @param output where the output is written to. Modified
	 */
	public static void horizontal( WaveletDesc_F32 coefficients ,
								   ImageFloat32 input , ImageFloat32 output ) {

		UtilWavelet.checkShape(input,output);

		final int offsetA = coefficients.offsetScaling;
		final int offsetB = coefficients.offsetWavelet;
		final float[] alpha = coefficients.scaling;
		final float[] beta = coefficients.wavelet;

		final float dataIn[] = input.data;
		final float dataOut[] = output.data;

		final int width = output.width;
		final int height = input.height;
		final int widthD2 = width/2;
		final int startX = UtilWavelet.computeBorderStart(coefficients);
		final int endOffsetX = output.width - UtilWavelet.computeBorderEnd(coefficients,input.width) - startX;

		for( int y = 0; y < height; y++ ) {

			int indexIn = input.startIndex + input.stride*y + startX;
			int indexOut = output.startIndex + output.stride*y + startX/2;

			int end = indexIn + endOffsetX;

			for( ; indexIn < end; indexIn += 2 ) {

				float scale = 0f;
				int index = indexIn+offsetA;
				for( int i = 0; i < alpha.length; i++ ) {
					scale += dataIn[index++]*alpha[i];
				}

				float wavelet = 0f;
				index = indexIn+offsetB;
				for( int i = 0; i < beta.length; i++ ) {
					wavelet += dataIn[index++]*beta[i];
				}

				dataOut[ indexOut+widthD2] = wavelet;
				dataOut[ indexOut++ ] = scale;
			}
		}
	}

	public static void vertical( WaveletDesc_F32 coefficients ,
								 ImageFloat32 input , ImageFloat32 output ) {

		UtilWavelet.checkShape(input,output);

		final int offsetA = coefficients.offsetScaling*input.stride;
		final int offsetB = coefficients.offsetWavelet*input.stride;
		final float[] alpha = coefficients.scaling;
		final float[] beta = coefficients.wavelet;

		final float dataIn[] = input.data;
		final float dataOut[] = output.data;

		final int width = input.width;
		final int height = output.height;
		final int heightD2 = (height/2)*output.stride;
		final int startY = UtilWavelet.computeBorderStart(coefficients);
		final int endY = output.height - UtilWavelet.computeBorderEnd(coefficients,input.height);

		for( int y = startY; y < endY; y += 2 ) {

			int indexIn = input.startIndex + input.stride*y;
			int indexOut = output.startIndex + output.stride*(y/2);

			for( int x = 0; x < width; x++, indexIn++) {

				float scale = 0f;
				int index = indexIn + offsetA;
				for( int i = 0; i < alpha.length; i++ ) {
					scale += dataIn[index]*alpha[i];
					index += input.stride;
				}

				float wavelet = 0f;
				index = indexIn + offsetB;
				for( int i = 0; i < beta.length; i++ ) {
					wavelet += dataIn[index]*beta[i];
					index += input.stride;
				}

				dataOut[indexOut+heightD2] = wavelet;
				dataOut[indexOut++] = scale;

			}
		}
	}

	public static void horizontalInverse( WaveletDesc_F32 coefficients , ImageFloat32 input , ImageFloat32 output ) {

		UtilWavelet.checkShape(output,input);

		// NOTE it is assumed the 'A' begins and ends at the same time or before 'B'
		final int lengthA_minus1 = coefficients.scaling.length-1;
		final int offsetA = coefficients.offsetScaling;
		final int offsetB = coefficients.offsetWavelet;
		final float[] alpha = coefficients.scaling;
		final float[] beta = coefficients.wavelet;

		final float dataIn[] = input.data;
		final float dataOut[] = output.data;

		final int width = input.width;
		final int height = output.height;
		final int widthD2 = width/2;
		final int startX = UtilWavelet.computeBorderStart(coefficients);
		final int endOffsetX = input.width - UtilWavelet.computeBorderEnd(coefficients,output.width) - startX;

		final int numZerosEnd = input.width-endOffsetX-startX-offsetA-(output.width%2);

		int initLength = Math.min(2,alpha.length);

		for( int y = 0; y < height; y++ ) {

			int indexIn = input.startIndex + input.stride*y + startX/2;
			int indexOut = output.startIndex + output.stride*y + startX;

			int end = indexOut + endOffsetX;

			// ---------- Do initial assignment
			int index = indexOut-startX;
			for( int i = 0; i < startX+offsetA; i++ ) {
				dataOut[index++] = 0;
			}
			for( ; indexOut < end; indexIn++ , indexOut += 2) {
				// add the 'average' signal
				float a = dataIn[indexIn];
				index = indexOut+offsetA;
				for( int i = 0; i < initLength; i++ ) {
					dataOut[index++] = a*alpha[i];
				}
			}
			
			for( int i = 0; i < numZerosEnd; i++ ) {
				dataOut[index++] = 0;
			}

			indexIn = input.startIndex + input.stride*y + startX/2;
			indexOut = output.startIndex + output.stride*y + startX;

			for( ; indexOut < end; indexIn++ , indexOut += 2) {

				// add the 'average' signal
				float a = dataIn[indexIn];
				index = indexOut+offsetA+2;
				for( int i = 2; i < alpha.length; i++ ) {
					dataOut[index++] += a*alpha[i];
				}

				// add the detail signal
				float d = dataIn[indexIn+widthD2];
				index = indexOut+offsetB;
				for( int i = 0; i < beta.length; i++ ) {
					dataOut[index++] += d*beta[i];
				}
			}
		}
	}

}
