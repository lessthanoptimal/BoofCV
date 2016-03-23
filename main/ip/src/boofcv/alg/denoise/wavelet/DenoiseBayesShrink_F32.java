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

package boofcv.alg.denoise.wavelet;

import boofcv.alg.denoise.ShrinkThresholdRule;
import boofcv.alg.misc.ImageStatistics;
import boofcv.struct.image.GrayF32;


/**
 * <p>
 * Denoises images using an adaptive soft-threshold in each sub-band computed using Bayesian statistics.
 * </p>
 *
 * <p>
 * Wavelet coefficients are modified using a standard soft-thresholding technique.  The threshold
 * is computing using an adaptively for each sub-band, as follows:<br>
 * T = &sigma;<sup>2</sup>/&sigma;<sub>X</sub><br>
 * where &sigma; is the noise standard deviation and &sigma;<sub>X</sub> is the signal standard deviation.
 * </p>
 *
 * <p>
 * S. Change, B. Yu, M. Vetterli, "Adaptive Wavelet Thresholding for Image Denoising and Compression"
 * IEEE Tran. Image Processing, Vol 9, No. 9, Sept. 2000
 * </p>
 *
 * @author Peter Abeles
 */
public class DenoiseBayesShrink_F32 extends SubbandShrink<GrayF32> {

	float noiseVariance;

	public DenoiseBayesShrink_F32( ShrinkThresholdRule<GrayF32> rule ) {
		super(rule);
	}

	@Override
	protected Number computeThreshold( GrayF32 subband )
	{
		// the maximum magnitude coefficient is used to normalize all the other coefficients
		// and reduce numerical round-off error
		float max = ImageStatistics.maxAbs(subband);
		float varianceY = 0;
		for( int y = 0; y < subband.height; y++ ) {
			int index = subband.startIndex + subband.stride*y;
			int end = index + subband.width;

			for( ;index < end; index++ ) {
				float v = subband.data[index]/max;
				varianceY += v*v;
			}
		}
		// undo normalization.
		// these coefficients are modeled as being zero mean, so the variance can be computed this way
		varianceY = (varianceY/(subband.width*subband.height))*max*max;

		// signal standard deviation
		float inner = varianceY-noiseVariance;

		if( inner < 0 )
			return Float.POSITIVE_INFINITY;
		else
			return noiseVariance/(float)Math.sqrt(inner);
	}

	@Override
	public void denoise(GrayF32 transform , int numLevels ) {

		int w = transform.width;
		int h = transform.height;

		// compute the noise variance using the HH_1 subband
		noiseVariance = UtilDenoiseWavelet.estimateNoiseStdDev(transform.subimage(w/2,h/2,w,h, null),null);
		noiseVariance *= noiseVariance;

//		System.out.println("Noise Variance: "+noiseVariance);

		performShrinkage(transform,numLevels);
	}
}
