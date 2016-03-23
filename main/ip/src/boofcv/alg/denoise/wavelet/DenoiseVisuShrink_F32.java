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

import boofcv.alg.denoise.DenoiseWavelet;
import boofcv.alg.denoise.ShrinkThresholdRule;
import boofcv.alg.transform.wavelet.UtilWavelet;
import boofcv.struct.image.GrayF32;


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
public class DenoiseVisuShrink_F32 implements DenoiseWavelet<GrayF32> {

	ShrinkThresholdRule<GrayF32> rule = new ShrinkThresholdSoft_F32();

	/**
	 * Applies VisuShrink denoising to the provided multilevel wavelet transform using
	 * the provided threshold.
	 *
	 * @param transform Mult-level wavelet transform.  Modified.
	 * @param numLevels Number of levels in the transform.
	 */
	@Override
	public void denoise(GrayF32 transform , int numLevels ) {
		int scale = UtilWavelet.computeScale(numLevels);

		final int h = transform.height;
		final int w = transform.width;

		// width and height of scaling image
		final int innerWidth = w/scale;
		final int innerHeight = h/scale;

		GrayF32 subbandHH = transform.subimage(w/2,h/2,w,h, null);
		float sigma = UtilDenoiseWavelet.estimateNoiseStdDev(subbandHH,null);
		float threshold = (float) UtilDenoiseWavelet.universalThreshold(subbandHH,sigma);

		// apply same threshold to all wavelet coefficients
		rule.process(transform.subimage(innerWidth,0,w,h, null),threshold);
		rule.process(transform.subimage(0,innerHeight,innerWidth,h, null),threshold);
	}
}
