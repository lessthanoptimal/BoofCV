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

package boofcv.factory.background;

import boofcv.alg.interpolate.InterpolationType;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link ConfigBackgroundGaussian}.
 *
 * @author Peter Abeles
 */
public class ConfigBackgroundGaussian implements Configuration {

	/**
	 * Specifies how fast it will adapt to changes in the background. From 0 to 1, inclusive.  0 = static  1.0 = instant.
	 */
	public float learnRate = 0.05f;

	/**
	 * Threshold for classifying a pixel as background or not. This threshold is applied to the
	 * computed Mahalanobis from the distribution.  An appropriate threshold will vary depending on the number
	 * of bands in the image.
	 */
	public float threshold;

	/**
	 * The initial variance assigned to a new pixel.  Larger values to reduce false positives due to
	 * under sampling.  Don't set to zero since that can cause divided by zero errors.
	 */
	public float initialVariance = Float.MIN_VALUE;

	/**
	 * Minimum Euclidean distance between the mean background and observed pixel value for it to be considered moving.
	 * This value is automatically scaled for the number of bands in the image.
	 */
	public float minimumDifference = 0;

	/**
	 * Specifies which interpolation it will use.  {@link InterpolationType#BILINEAR} or
	 * {@link InterpolationType#NEAREST_NEIGHBOR} recommended.
	 *
	 * <p>ONLY USED FOR MOVING BACKGROUNDS!</p>
	 */
	public InterpolationType interpolation = InterpolationType.BILINEAR;

	public ConfigBackgroundGaussian(float threshold) {
		this.threshold = threshold;
	}

	public ConfigBackgroundGaussian(float threshold, float learnRate) {
		this.threshold = threshold;
		this.learnRate = learnRate;
	}

	@Override
	public void checkValidity() {
		if( learnRate < 0 || learnRate > 1 )
			throw new IllegalArgumentException("Learn rate must be 0 <= rate <= 1");
		if( threshold <= 0 )
			throw new IllegalArgumentException("threshold must be > 0");
		if( initialVariance == 0 )
			throw new IllegalArgumentException("Don't set initialVariance to zero, set it to Float.MIN_VALUE instead");
		if( initialVariance < 0 )
			throw new IllegalArgumentException("Variance must be set to a value larger than zero");
		if( initialVariance < 0 )
			throw new IllegalArgumentException("minimumDifference must be >= 0");
	}
}
