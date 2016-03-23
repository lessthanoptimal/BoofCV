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
import boofcv.struct.image.ImageGray;


/**
 * Performs an adaptive threshold based wavelet shrinkage across each of the wavelet subbands in each
 * layer of the transformed image.
 *
 * @author Peter Abeles
 */
public abstract class SubbandShrink<I extends ImageGray<I>> implements DenoiseWavelet<I> {

	// specifies how the threshold is applied to each pixel in the image
	protected ShrinkThresholdRule<I> rule;

	protected SubbandShrink(ShrinkThresholdRule<I> rule) {
		this.rule = rule;
	}

	/**
	 * Compute the threshold for the specified subband.
	 *
	 * @param subband Subband whose threshold is being computed.
	 * @return
	 */
	protected abstract Number computeThreshold( I subband );

	/**
	 * Performs wavelet shrinking using the specified rule and by computing a threshold
	 * for each subband.
	 *
	 * @param transform The image being transformed.
	 * @param numLevels Number of levels in the transform.
	 */
	protected void performShrinkage( I transform , int numLevels ) {

		// step through each layer in the pyramid.
		for( int i = 0; i < numLevels; i++ ) {
			int w = transform.width;
			int h = transform.height;
			int ww = w/2;
			int hh = h/2;
			Number threshold;
			I subband;

			// HL
			subband = transform.subimage(ww,0,w,hh, null);
			threshold = computeThreshold(subband);
			rule.process(subband,threshold);

//			System.out.print("HL = "+threshold);

			// LH
			subband = transform.subimage(0,hh,ww,h, null);
			threshold = computeThreshold(subband);
			rule.process(subband,threshold);

//			System.out.print("  LH = "+threshold);

			// HH
			subband = transform.subimage(ww,hh,w,h, null);
			threshold = computeThreshold(subband);
			rule.process(subband,threshold);

//			System.out.println("  HH = "+threshold);

			transform = transform.subimage(0,0,ww,hh, null);
		}

	}
}
