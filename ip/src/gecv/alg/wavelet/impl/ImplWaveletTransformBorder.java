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
import gecv.struct.wavelet.WaveletCoefficient_F32;


/**
 * <p>
 * Performs the wavelet transform just around the image border.  Should be called in conjunction
 * with {@link ImplWaveletTransformInner} or similar functions.  Must be called after the inner
 * portion has been computed because the "inner" functions modify the border during the inverse
 * transform.
 * </p>
 *
 * <p>
 * For the inverse transform the inner transform must be called before the border is computed.
 * Due to how the inverse is computed some of the output values will be added to border.  The values
 * computed in these inverse functions add to that.
 * </p>
 *
 * @author Peter Abeles
 */
public class ImplWaveletTransformBorder {

	public static void horizontal( WaveletCoefficient_F32 coefficients , ImageFloat32 input , ImageFloat32 output )
	{
		final int offsetA = coefficients.offsetScaling;
		final int offsetB = coefficients.offsetWavelet;
		final float[] alpha = coefficients.scaling;
		final float[] beta = coefficients.wavelet;

		final BorderIndex1D border = coefficients.getBorder();
		border.setLength(output.width);

		final boolean isLarger = output.width > input.width;
		final int width = output.width;
		final int height = input.height;
		final int leftBorder = UtilWavelet.computeBorderStart(coefficients);
		final int rightBorder = output.width - UtilWavelet.computeBorderEnd(coefficients,input.width,output.width);

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < leftBorder; x += 2 ) {
				float scale = 0;
				float wavelet = 0;

				for( int i = 0; i < alpha.length; i++ ) {
					int xx = border.getIndex(x+i+offsetA);
					if( isLarger && xx >= input.width )
						continue;
					scale += input.get(xx,y)*alpha[i];
				}
				for( int i = 0; i < beta.length; i++ ) {
					int xx = border.getIndex(x+i+offsetB);
					if( isLarger && xx >= input.width )
						continue;
					wavelet += input.get(xx,y)*beta[i];
				}

				int outX = x/2;

				output.set(outX,y,scale);
				output.set(width/2 + outX , y , wavelet );
			}
			for( int x = rightBorder; x < width; x += 2 ) {
				float scale = 0;
				float wavelet = 0;

				for( int i = 0; i < alpha.length; i++ ) {
					int xx = border.getIndex(x+i+offsetA);
					if( isLarger && xx >= input.width )
						continue;
					scale += input.get(xx,y)*alpha[i];
				}
				for( int i = 0; i < beta.length; i++ ) {
					int xx = border.getIndex(x+i+offsetB);
					if( isLarger && xx >= input.width )
						continue;
					wavelet += input.get(xx,y)*beta[i];
				}

				int outX = x/2;

				output.set(outX,y,scale);
				output.set(width/2 + outX , y , wavelet );
			}
		}
	}

	public static void vertical( WaveletCoefficient_F32 coefficients , ImageFloat32 input , ImageFloat32 output )
	{
		final int offsetA = coefficients.offsetScaling;
		final int offsetB = coefficients.offsetWavelet;
		final float[] alpha = coefficients.scaling;
		final float[] beta = coefficients.wavelet;

		final BorderIndex1D border = coefficients.getBorder();
		border.setLength(output.height);

		final boolean isLarger = output.height > input.height;
		final int width = input.width;
		final int height = output.height;
		final int lowerBorder = UtilWavelet.computeBorderStart(coefficients);
		final int upperBorder = output.height - UtilWavelet.computeBorderEnd(coefficients,input.height,output.height);

		for( int x = 0; x < width; x++) {
			for( int y = 0; y < lowerBorder; y += 2 ) {
				float scale = 0;
				float wavelet = 0;

				for( int i = 0; i < alpha.length; i++ ) {
					int yy = border.getIndex(y+i+offsetA);
					if( isLarger && yy >= input.height )
						continue;
					scale += input.get(x,yy)*alpha[i];
				}
				for( int i = 0; i < beta.length; i++ ) {
					int yy = border.getIndex(y+i+offsetB);
					if( isLarger && yy >= input.height )
						continue;
					wavelet += input.get(x,yy)*beta[i];
				}

				int outY = y/2;

				output.set(x , outY,scale);
				output.set(x , height/2 + outY , wavelet );
			}

			for( int y = upperBorder; y < height; y += 2 ) {
				float scale = 0;
				float wavelet = 0;

				for( int i = 0; i < alpha.length; i++ ) {
					int yy = border.getIndex(y+i+offsetA);
					if( isLarger && yy >= input.height )
						continue;
					scale += input.get(x,yy)*alpha[i];
				}
				for( int i = 0; i < beta.length; i++ ) {
					int yy = border.getIndex(y+i+offsetB);
					if( isLarger && yy >= input.height )
						continue;
					wavelet += input.get(x,yy)*beta[i];
				}

				int outY = y/2;

				output.set(x , outY,scale);
				output.set(x , height/2 + outY , wavelet );
			}
		}
	}

	public static void horizontalInverse( WaveletCoefficient_F32 coefficients , ImageFloat32 input , ImageFloat32 output )
	{
		final int offsetA = coefficients.offsetScaling;
		final int offsetB = coefficients.offsetWavelet;
		final float[] alpha = coefficients.scaling;
		final float[] beta = coefficients.wavelet;

		final float []trends = new float[ input.width ];
		final float []details = new float[ input.width ];

		final boolean isLarger = input.width > output.width;
		final int width = input.width;
		final int height = output.height;
		final int lowerBorder = UtilWavelet.computeBorderStart(coefficients);
		final int upperBorder = input.width - UtilWavelet.computeBorderEnd(coefficients,output.width,input.width);

		BorderIndex1D border = coefficients.getBorder();
		border.setLength(input.width);

		// need to take in account the possibility of a border wrapping around
		int zeroLower = lowerBorder+Math.max(alpha.length,beta.length);
		int zeroUpper = upperBorder-Math.max(alpha.length,beta.length);

		if( zeroLower > output.width ) zeroLower = output.width;
		if( zeroUpper < 0 ) zeroUpper = 0;

		for( int y = 0; y < height; y++ ) {

			for( int i = 0; i < zeroLower; i++ ) {
				details[i] = 0; trends[i] = 0;
			}

			for( int i = zeroUpper; i < width; i++ ) {
				details[i] = 0; trends[i] = 0;
			}

			for( int x = 0; x < lowerBorder; x += 2 ) {
				float a = input.get(x/2,y);
				float d = input.get(width/2+x/2,y);

				// add the trend
				for( int i = 0; i < alpha.length; i++ ) {
					// if an odd image don't update the outer edge
					int xx = border.getIndex(x+offsetA+i);
					if( isLarger && xx >= output.width )
						continue;
					trends[xx] += a*alpha[i];
				}

				// add the detail signal
				for( int i = 0; i < beta.length; i++ ) {
					int xx = border.getIndex(x+offsetB+i);
					if( isLarger && xx >= output.width )
						continue;
					details[xx] += d*beta[i];
				}
			}

			for( int x = upperBorder; x < width; x += 2 ) {
				float a = input.get(x/2,y);
				float d = input.get(width/2+x/2,y);

				// add the trend
				for( int i = 0; i < alpha.length; i++ ) {
					// if an odd image don't update the outer edge
					int xx = border.getIndex(x+offsetA+i);
					if( isLarger && xx == output.width )
						continue;
					trends[xx] += a*alpha[i];
				}

				// add the detail signal
				for( int i = 0; i < beta.length; i++ ) {
					int xx = border.getIndex(x+offsetB+i);
					if( isLarger && xx >= output.width )
						continue;
					details[xx] += d*beta[i];
				}
			}

			// some of the values from the "inner" transformation bleed into the border
			int index = output.startIndex + y*output.stride;
			for( int x = 0; x < zeroLower; x++ ) {
				output.data[ index++ ] += trends[x] + details[x];
			}
			index = output.startIndex + y*output.stride + zeroUpper;
			for( int x = zeroUpper; x < output.width; x++ ) {
				output.data[ index++ ] += trends[x] + details[x];
			}
		}
	}

	public static void verticalInverse( WaveletCoefficient_F32 coefficients , ImageFloat32 input , ImageFloat32 output )
	{
		UtilWavelet.checkShape(output,input);

		final int offsetA = coefficients.offsetScaling;
		final int offsetB = coefficients.offsetWavelet;
		final float[] alpha = coefficients.scaling;
		final float[] beta = coefficients.wavelet;

		final float []trends = new float[ output.height ];
		final float []details = new float[ output.height ];

		final boolean isLarger = input.height > output.height;
		final int width = output.width;
		final int height = input.height;
		final int lowerBorder = UtilWavelet.computeBorderStart(coefficients);
		final int upperBorder = input.height - UtilWavelet.computeBorderEnd(coefficients,output.height,input.height);

		// need to take in account the possibility of a border wrapping around
		int zeroLower = lowerBorder+Math.max(alpha.length,beta.length);
		int zeroUpper = upperBorder-Math.max(alpha.length,beta.length);

		if( zeroLower > output.height ) zeroLower = output.height;
		if( zeroUpper < 0 ) zeroUpper = 0;

		BorderIndex1D border = coefficients.getBorder();
		border.setLength(input.height);

		for( int x = 0; x < width; x++) {

			for( int i = 0; i < zeroLower; i++ ) {
				details[i] = 0; trends[i] = 0;
			}

			for( int i = zeroUpper; i < output.height; i++ ) {
				details[i] = 0; trends[i] = 0;
			}

			for( int y = 0; y < lowerBorder; y += 2 ) {
				float a = input.get(x,y/2);
				float d = input.get(x,y/2+height/2);

				// add the 'average' signal
				for( int i = 0; i < alpha.length; i++ ) {
					// if an odd image don't update the outer edge
					int yy = border.getIndex(y+offsetA+i);
					if( isLarger && yy >= output.height )
						continue;
					trends[yy] += a*alpha[i];
				}

				// add the detail signal
				for( int i = 0; i < beta.length; i++ ) {
					int yy = border.getIndex(y+offsetB+i);
					if( isLarger && yy >= output.height )
						continue;
					details[yy] += d*beta[i];
				}
			}

			for( int y = upperBorder; y < height; y += 2 ) {
				float a = input.get(x,y/2);
				float d = input.get(x,y/2+height/2);

				// add the 'average' signal
				for( int i = 0; i < alpha.length; i++ ) {
					// if an odd image don't update the outer edge
					int yy = border.getIndex(y+offsetA+i);
					if( isLarger && yy >= output.height )
						continue;
					trends[yy] += a*alpha[i];
				}

				// add the detail signal
				for( int i = 0; i < beta.length; i++ ) {
					int yy = border.getIndex(y+offsetB+i);
					if( isLarger && yy >= output.height )
						continue;
					details[yy] += d*beta[i];
				}
			}

			// some of the values from the "inner" transformation bleed into the border
			int index = output.startIndex + x;
			for( int y = 0; y < zeroLower; y++ , index += output.stride ) {
				output.data[ index ] +=  trends[y] + details[y];
			}
			index = output.startIndex + zeroUpper*output.stride + x;
			for( int y = zeroUpper; y < output.height; y++ , index += output.stride ) {
				output.data[ index ] +=  trends[y] + details[y];
			}
		}
	}

}
