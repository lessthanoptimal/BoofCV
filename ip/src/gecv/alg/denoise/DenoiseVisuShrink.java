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

package gecv.alg.denoise;

import gecv.alg.wavelet.UtilWavelet;
import gecv.struct.image.ImageFloat32;
import pja.sorting.QuickSelectArray;


/**
 * <p>
 * Classic algorithm for wavelet noise reduction by shrinkage with a universal threshold. Noise
 * is reduced by applying a soft threshold to wavelet coefficients. A method is provided for
 * automatically selecting a reasonable threshold based upon the coefficients statistics.
 * </p>
 *
 * <p>
 * D. Donoho and I. Johnstone, "Ideal spatial adaption via wavelet shrinkage," Biometrics, Vol. 81, 425-455, 1994
 * </p>
 *
 * @author Peter Abeles
 */
public class DenoiseVisuShrink {


	/**
	 * Automatically computes a threshold using wavelet coefficient's median statistic.  See cited
	 * paper for an explanation of this algorithm.
	 *
	 * @param transform Image containing wavelet transform.
	 * @param numLevels Number of levels in the wavelet transform.
	 * @param coefs Temporary storage for coefficients.  Should be large enough to contain each pixel in the image.
	 * @return Computed threshold.
	 */
	public static float computeThreshold( ImageFloat32 transform , int numLevels , float coefs[]) {
		int scale = UtilWavelet.computeScale(numLevels);

		final int h = transform.height;
		final int w = transform.width;

		// width and height of scaling image
		final int innerWidth = w/scale;
		final int innerHeight = h/scale;

		// copy wavelet coefficients into the coefficients array
		int index = 0;
		for( int y = innerHeight; y < h; y++ ) {
			int indexTran = transform.startIndex + transform.stride*y;
			int indexEnd = indexTran + transform.width;

			for( ; indexTran < indexEnd; indexTran++ ) {
				coefs[index++] = Math.abs(transform.data[indexTran]);
			}
		}
		for( int y = 0; y < innerHeight; y++ ) {
			int indexTran = transform.startIndex + transform.stride*y+innerWidth;
			int indexEnd = indexTran + transform.width-innerWidth;

			for( ; indexTran < indexEnd; indexTran++ ) {
				coefs[index++] = Math.abs(transform.data[indexTran]);
			}
		}

		// select the median value
		float median = QuickSelectArray.select(coefs,index/2,index);

		// estimate the sigma of the distribution
		float sigma = median/0.6475f;

		// return the threshold
		return sigma*(float)Math.sqrt(2*Math.log(Math.max(w,h)));
	}

	/**
	 * Applies VisuShrink denoising to the provided multilevel wavelet transform using
	 * the provided threshold.
	 *
	 * @param transform Mult-level wavelet transform.  Modified.
	 * @param numLevels Number of levels in the transform.
	 * @param threshold Threshold used by VisuaShrink.
	 */
	public static void process( ImageFloat32 transform , int numLevels , float threshold  ) {
		int scale = UtilWavelet.computeScale(numLevels);

		final int h = transform.height;
		final int w = transform.width;

		// width and height of scaling image
		final int innerWidth = w/scale;
		final int innerHeight = h/scale;

		// Threshold wavelets using a soft threshold
		for( int y = innerHeight; y < h; y++ ) {
			int indexTran = transform.startIndex + transform.stride*y;
			int indexEnd = indexTran + transform.width;

			for( ; indexTran < indexEnd; indexTran++ ) {
				float wc = transform.data[indexTran];
				
				if( Math.abs(wc) < threshold ) {
					transform.data[indexTran] = 0;
				} else if( wc < 0 ) {
					transform.data[indexTran] = wc + threshold;
				} else {
					transform.data[indexTran] = wc - threshold;
				}
			}
		}
		for( int y = 0; y < innerHeight; y++ ) {
			int indexTran = transform.startIndex + transform.stride*y+innerWidth;
			int indexEnd = indexTran + transform.width-innerWidth;

			for( ; indexTran < indexEnd; indexTran++ ) {
				float wc = transform.data[indexTran];
				
				if( Math.abs(wc) < threshold ) {
					transform.data[indexTran] = 0;
				} else if( wc < 0 ) {
					transform.data[indexTran] = wc + threshold;
				} else {
					transform.data[indexTran] = wc - threshold;
				}
			}
		}
	}
}
