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

package boofcv.abst.tracker;

import boofcv.alg.interpolate.InterpolationType;

/**
 * Configuration for {@link Comaniciu2003_to_TrackerObjectQuad}.
 *
 * @author Peter Abeles
 */
public class ConfigComaniciu2003 {
	/**
	 * Number of points it samples along each axis of the rectangle.  Default is 30.
	 */
	public int numSamples = 30;
	/**
	 * Used to compute weights. Number of standard deviations away the sides will be from the center. Shouldn't
	 * need to tune this.  Try 3.
	 */
	public double numSigmas = 3;
	/**
	 * Number of histogram bins for each band.  Try 5
	 */
	public int numHistogramBins = 5;
	/**
	 * Largest value a pixel can have.  For 8-bit images this is 255. Floating point images are some times normalized
	 * to 1.
	 */
	public float maxPixelValue = 255f;
	/**
	 * If true the histogram will be updated using the most recent image. Try false.
	 */
	public boolean updateHistogram = false;
	/**
	 * Maximum number of mean-shift iterations.  Try 30
	 */
	public int meanShiftMaxIterations = 15;
	/**
	 * Mean-shift will stop when the change is below this threshold.  Try 1e-4f
	 */
	public float meanShiftMinimumChange = 1e-3f;
	/**
	 * Weighting factor which limits the amount it will change the scale.  Value from 0 to 1.
	 * Closer to 0 the more it will prefer the most recent estimate.  Try 0.1
	 */
	public float scaleWeight = 0.1f;
	/**
	 * Specifies how much it will scale the region up and down by when testing for a scale change.  Allowed
	 * values are from 0 to 1, inclusive.  0 means no scale change and 1 is 100% increase and decrease.
	 *
	 * If no scale change is considered it can run 3x faster. If the target doesn't change scale then the tracker
	 * is much more robust.  The paper recommends 0.1.  By default scale change is set to 0.
	 */
	public float scaleChange = 0;

	/**
	 * The scale is allowed to be reduced by this much from the original region which is selected.  Default
	 * is 0.25
	 */
	public float minimumSizeRatio = 0.25f;

	/**
	 * Which interpolation method should it use.
	 */
	public InterpolationType interpolation = InterpolationType.BILINEAR;

	public ConfigComaniciu2003(int numSamples, int numHistogramBins, float scaleWeight ) {
		this.numSamples = numSamples;
		this.numHistogramBins = numHistogramBins;
		this.scaleWeight = scaleWeight;
	}

	public ConfigComaniciu2003( boolean estimateScale ) {
		if( estimateScale )
			scaleChange = 0.1f;
	}

	public ConfigComaniciu2003() {
	}
}
