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
import gecv.core.image.border.BorderIndex1D;
import gecv.struct.image.ImageFloat32;
import gecv.struct.wavelet.WaveletDesc_F32;


/**
 * <p>
 * Standard algorithm for forward and inverse wavelet transform which has been optimized to only
 * process the inner portion of the image by excluding the border.
 * </p>
 *
 * @author Peter Abeles
 */
public class ImplWaveletTransformInner {

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

		final int offsetA = coefficients.offsetScaling;
		final int offsetB = coefficients.offsetWavelet;
		final float[] alpha = coefficients.scaling;
		final float[] beta = coefficients.wavelet;

		float []trends = new float[ output.width ];
		float []details = new float[ output.width ];

		final int width = input.width;
		final int height = output.height;
		final int widthD2 = width/2;
		final int startX = UtilWavelet.computeBorderStart(coefficients);
		final int endX = input.width - UtilWavelet.computeBorderEnd(coefficients,output.width);
		final int zeroStart = startX+Math.min(offsetA,offsetB);
		final int zeroEnd = endX+Math.min(offsetA,offsetB);

		BorderIndex1D border = coefficients.getBorder();
		border.setLength(input.width);

		for( int y = 0; y < height; y++ ) {

			// initialize details and trends arrays
			for( int i = 0; i < zeroStart; i++ ) {
				details[i] = 0; trends[i] = 0;
			}

			int indexSrc = input.startIndex + y*input.stride+startX/2;
			for( int x = startX; x < endX; x += 2 , indexSrc++ ) {
				float a = input.data[ indexSrc ];
				float d = input.data[ indexSrc + widthD2 ];

				// add the trend
				for( int i = 0; i < 2; i++ )
					trends[i+x+offsetA] = a*alpha[i];

				// add the detail signal
				for( int i = 0; i < 2; i++ )
					details[i+x+offsetB] = d*beta[i];
			}

			for( int i = zeroEnd; i < output.width; i++ ) {
				details[i] = 0; trends[i] = 0;
			}

			// perform the normal inverse transform
			indexSrc = input.startIndex + y*input.stride+startX/2;
			for( int x = startX; x < endX; x += 2 , indexSrc++ ) {
				float a = input.data[ indexSrc ];
				float d = input.data[ indexSrc + widthD2 ];

				// add the trend
				for( int i = 2; i < alpha.length; i++ ) {
					trends[i+x+offsetA] += a*alpha[i];
				}

				// add the detail signal
				for( int i = 2; i < beta.length; i++ ) {
					details[i+x+offsetB] += d*beta[i];
				}
			}

			int indexDst = output.startIndex + y*output.stride;
			for( int x = 0; x < output.width; x++ ) {
				output.data[ indexDst++ ] = trends[x] + details[x];
			}
		}
	}

	public static void verticalInverse( WaveletDesc_F32 coefficients , ImageFloat32 input , ImageFloat32 output ) {
		UtilWavelet.checkShape(output,input);

		final int offsetA = coefficients.offsetScaling;
		final int offsetB = coefficients.offsetWavelet;
		final float[] alpha = coefficients.scaling;
		final float[] beta = coefficients.wavelet;

		float []trends = new float[ output.height ];
		float []details = new float[ output.height ];

		final int width = output.width;
		final int height = input.height;
		final int heightD2 = (height/2)*input.stride;
		final int startY = UtilWavelet.computeBorderStart(coefficients);
		final int endY = input.height - UtilWavelet.computeBorderEnd(coefficients,output.height);
		final int zeroStart = startY+Math.min(offsetA,offsetB);
		final int zeroEnd = endY+Math.min(offsetA,offsetB);

		for( int x = 0; x < width; x++) {

			// initialize details and trends arrays
			for( int i = 0; i < zeroStart; i++ ) {
				details[i] = 0; trends[i] = 0;
			}

			int indexSrc = input.startIndex + (startY/2)*input.stride + x;
			for( int y = startY; y < endY; y += 2 , indexSrc += input.stride ) {
				float a = input.data[ indexSrc ];
				float d = input.data[ indexSrc + heightD2 ];

				// add the trend
				for( int i = 0; i < 2; i++ )
					trends[i+y+offsetA] = a*alpha[i];

				// add the detail signal
				for( int i = 0; i < 2; i++ )
					details[i+y+offsetB] = d*beta[i];
			}

			for( int i = zeroEnd; i < output.height; i++ ) {
				details[i] = 0; trends[i] = 0;
			}

			// perform the normal inverse transform
			indexSrc = input.startIndex + (startY/2)*input.stride + x;

			for( int y = startY; y < endY; y += 2 , indexSrc += input.stride ) {
				float a = input.data[indexSrc];
				float d = input.data[indexSrc+heightD2];

				// add the 'average' signal
				for( int i = 2; i < alpha.length; i++ ) {
					trends[y+offsetA+i] += a*alpha[i];
				}

				// add the detail signal
				for( int i = 2; i < beta.length; i++ ) {
					details[y+offsetB+i] += d*beta[i];
				}
			}

			int indexDst = output.startIndex + x;
			for( int y = 0; y < output.height; y++ , indexDst += output.stride ) {
				output.data[indexDst] = trends[y] + details[y];
			}
		}
	}

}
