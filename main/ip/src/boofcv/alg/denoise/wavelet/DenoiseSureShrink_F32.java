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

import boofcv.struct.image.GrayF32;

import java.util.Arrays;


/**
 * <p>
 * SureShrink denoises wavelets using a threshold computed by minimizing Stein's Unbiased Risk
 * Estimate (SURE).  In practice a hybrid approach was found to work best where either the Stein
 * threshold or the universal threshold proposed by VisuShrink is used.
 * </p>
 *
 * <p>
 * This implementation computes a threshold for each subband.
 * </p>
 *
 * <p>
 * D. Donoho, L. Johnstone, "Adapting to Unknown Smoothness via Wavelet Shrinkage"
 * Journal of the American Statistical Association, Vol. 90, No. 432, December 1995, pp. 1200-1224
 * </p>
 *
 * @author Peter Abeles
 */
public class DenoiseSureShrink_F32 extends SubbandShrink<GrayF32> {

	float noiseSigma;

	public DenoiseSureShrink_F32() {
		super(new ShrinkThresholdSoft_F32());
	}

	@Override
	protected Number computeThreshold( GrayF32 subband  )
	{
		float coef[] = new float[ subband.width*subband.height ];
		UtilDenoiseWavelet.subbandAbsVal(subband,coef);
		Arrays.sort(coef);

		float maxThreshold =(float) UtilDenoiseWavelet.universalThreshold(subband,1.0);

		float N = coef.length;

		float threshold = maxThreshold;
		float bestRisk = Float.MAX_VALUE;
		float sumW = 0;
		float right = N-2.0f;
		for( int i = 0; i < coef.length; i++ , right -= 2.0f) {
			float c = coef[i]/noiseSigma;
			if( c > maxThreshold ) {
				break;
			}

			float cc = c*c;
			sumW += cc;
			float risk = sumW + cc*(N-i-1.0f) + right;
			if( risk < bestRisk ) {
				threshold = c;
				bestRisk = risk;
			}
		}

		return noiseSigma*threshold;
	}

	@Override
	public void denoise(GrayF32 transform , int numLevels ) {

		int w = transform.width;
		int h = transform.height;

		// compute the noise variance using the HH_1 subband
		noiseSigma = UtilDenoiseWavelet.estimateNoiseStdDev(transform.subimage(w/2,h/2,w,h, null),null);

//		System.out.println("Noise sigma: "+noiseSigma);

		performShrinkage(transform,numLevels);
	}
}
