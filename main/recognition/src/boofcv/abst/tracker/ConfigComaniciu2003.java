/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.struct.image.ImageMultiBand;
import boofcv.struct.image.ImageType;

/**
 * Configuration for {@link Comaniciu2003_to_TrackObjectQuad}.
 *
 * @author Peter Abeles
 */
public class ConfigComaniciu2003<T extends ImageMultiBand> {
	/**
	 * Number of points it samples along each axis of the rectangle.  Default is 40.
	 */
	public int numSamples = 30;
	/**
	 * Used to compute weights. Number of standard deviations away the sides will be from the center. Try 3.
	 */
	public double numSigmas = 3;
	/**
	 * Number of histogram bins for each band.  Try 5
	 */
	public int numHistogramBins = 5;
	/**
	 * Largest value a pixel can have + 1.  For 8-bit images this is 256. Floating point images are some times normalized
	 * to 1.
	 */
	public float maxPixelValue = 256f;
	/**
	 * If true the histogram will be updated using the most recent image. Try false.
	 */
	public boolean updateHistogram = false;
	/**
	 * Maximum number of mean-shift iterations.  Try 30
	 */
	public int meanShiftMaxIterations = 30;
	/**
	 * Mean-shift will stop when the change is below this threshold.  Try 1e-4f
	 */
	public float meanShiftMinimumChange = 1e-4f;
	/**
	 * Weighting factor which limits the amount it will change the scale.  Value from 0 to 1.
	 * Closer to 0 the more it will prefer the most recent estimate.  Try 0.1
	 */
	public float scaleWeight = 0.1f;
	/**
	 * Which interpolation method should it use.
	 */
	public TypeInterpolate interpolation = TypeInterpolate.BILINEAR;
	/**
	 * Which type of input image will it process.
	 */
	public ImageType<T> imageType;

	public ConfigComaniciu2003(int numSamples, float maxPixelValue, ImageType<T> imageType) {
		this.numSamples = numSamples;
		this.maxPixelValue = maxPixelValue;
		this.imageType = imageType;
	}

	public ConfigComaniciu2003(ImageType<T> imageType) {
		this.imageType = imageType;
	}

	public ConfigComaniciu2003() {
	}
}
