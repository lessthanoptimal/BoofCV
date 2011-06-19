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

import gecv.alg.misc.ImageTestingOps;
import gecv.alg.wavelet.UtilWavelet;
import gecv.alg.wavelet.WaveletDesc_F32;
import gecv.core.image.border.ImageBorder_F32;
import gecv.struct.image.ImageFloat32;


/**
 * Unoptimized and simplistic implementation of a forward and inverse wavelet transform across one
 * level.  Primary used for validation testing.
 *
 * @author Peter Abeles
 */
public class LevelWaveletTransformNaive {

	/**
	 * Performs a single level wavelet transform along the horizontal axis.
	 *
	 * @param border How image borders are handled.
	 * @param coefficients Description of wavelet coefficients.
	 * @param input Input image which is being transform. Not modified.
	 * @param output where the output is written to. Modified
	 */
	public static void horizontal( ImageBorder_F32 border , WaveletDesc_F32 coefficients ,
								   ImageFloat32 input , ImageFloat32 output ) {

		UtilWavelet.checkShape(input,output);

		final int offsetA = coefficients.offsetScaling;
		final int offsetB = coefficients.offsetWavelet;
		final float[] alpha = coefficients.scaling;
		final float[] beta = coefficients.wavelet;

		border.setImage(input);

		final int width = output.width;
		final int height = input.height;

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x += 2 ) {

				float scale = 0f;
				float wavelet = 0f;

				for( int i = 0; i < alpha.length; i++ ) {
					scale += border.get(x+i+offsetA,y)*alpha[i];
				}
				for( int i = 0; i < beta.length; i++ ) {
					wavelet += border.get(x+i+offsetB,y)*beta[i];
				}

				int outX = x/2;

				output.set(outX,y,scale);
				output.set(width/2 + outX , y , wavelet );
			}
		}
	}

	/**
	 * Performs a single level wavelet transform along the vertical axis.
	 *
	 * @param border How image borders are handled.
	 * @param coefficients Description of wavelet coefficients.
	 * @param input Input image which is being transform. Not modified.
	 * @param output where the output is written to. Modified
	 */
	public static void vertical( ImageBorder_F32 border , WaveletDesc_F32 coefficients ,
								 ImageFloat32 input , ImageFloat32 output ) {

		UtilWavelet.checkShape(input,output);

		final int offsetA = coefficients.offsetScaling;
		final int offsetB = coefficients.offsetWavelet;
		final float[] alpha = coefficients.scaling;
		final float[] beta = coefficients.wavelet;

		border.setImage(input);

		final int width = input.width;
		final int height = output.height;

		for( int x = 0; x < width; x++) {
			for( int y = 0; y < height; y += 2 ) {
				float scale = 0f;
				float wavelet = 0f;

				for( int i = 0; i < alpha.length; i++ ) {
					scale += border.get(x,y+i+offsetA)*alpha[i];
				}
				for( int i = 0; i < beta.length; i++ ) {
					wavelet += border.get(x,y+i+offsetB)*beta[i];
				}

				int outY = y/2;

				output.set(x , outY,scale);
				output.set(x , height/2 + outY , wavelet );
			}
		}
	}

	/**
	 * Performs a single level inverse wavelet transform along the horizontal axis.
	 *
	 * @param coefficients Description of wavelet coefficients.
	 * @param input Transformed image. Not modified.
	 * @param output Reconstruction of original image. Modified
	 */
	public static void horizontalInverse( WaveletDesc_F32 coefficients , ImageFloat32 input , ImageFloat32 output ) {

		UtilWavelet.checkShape(output,input);

		final int offsetA = coefficients.offsetScaling;
		final int offsetB = coefficients.offsetWavelet;
		final float[] alpha = coefficients.scaling;
		final float[] beta = coefficients.wavelet;

		ImageTestingOps.fill(output,0);

		final int width = input.width;
		final int height = output.height;

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x += 2 ) {
				float a = input.get(x/2,y);
				float d = input.get(width/2+x/2,y);

				// add the 'average' signal
				for( int i = 0; i < alpha.length; i++ ) {
					int xx = x+offsetA+i;
					if( xx < 0 || xx >= output.width )
						continue;
					float pixel = output.get(xx,y);
					output.set(xx,y,pixel+a*alpha[i]);
				}

				// add the detail signal
				for( int i = 0; i < beta.length; i++ ) {
					int xx = x+offsetB+i;
					if( xx < 0 || xx >= output.width )
						continue;
					float pixel = output.get(xx,y);
					output.set(xx,y,pixel+d*beta[i]);
				}
			}
		}
	}

	/**
	/**
	 * Performs a single level inverse wavelet transform along the vertical axis.
	 *
	 * @param coefficients Description of wavelet coefficients.
	 * @param input Transformed image. Not modified.
	 * @param output Reconstruction of original image. Modified
	 */
	public static void verticalInverse( WaveletDesc_F32 coefficients , ImageFloat32 input , ImageFloat32 output ) {

		UtilWavelet.checkShape(output,input);

		final int offsetA = coefficients.offsetScaling;
		final int offsetB = coefficients.offsetWavelet;
		final float[] alpha = coefficients.scaling;
		final float[] beta = coefficients.wavelet;

		ImageTestingOps.fill(output,0);

		final int width = output.width;
		final int height = input.height;

		for( int x = 0; x < width; x++) {
			for( int y = 0; y < height; y += 2 ) {
				float a = input.get(x,y/2);
				float d = input.get(x,y/2+height/2);

				// add the 'average' signal
				for( int i = 0; i < alpha.length; i++ ) {
					int yy = y+offsetA+i;
					if( yy < 0 || yy >= output.height )
						continue;
					float pixel = output.get(x,yy);
					output.set(x,yy,pixel+a*alpha[i]);
				}

				// add the detail signal
				for( int i = 0; i < beta.length; i++ ) {
					int yy = y+offsetB+i;
					if( yy < 0 || yy >= output.height )
						continue;
					float pixel = output.get(x,yy);
					output.set(x,yy,pixel+d*beta[i]);
				}
			}
		}
	}

}
