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

package boofcv.alg.background;

import boofcv.struct.image.GrayF32;

/**
 * <p>
 * Background model in which each pixel is modeled as an independent Guassian distribution.  For computational
 * efficiency each band is modeled as having a diagonal covariance matrix with off diagonal terms set to zero,
 * i.e. each band is independent. See [1] for a summary.  This is an approximation but according to several
 * papers it doesn't hurt performance much but simplifies computations significantly.
 * </p>
 * <p>
 * Internally background model is represented by two images; mean and variance, which are stored in
 * {@link GrayF32} images.  This allows for the mean and variance of each pixel to be interpolated,
 * reducing artifacts along the border of objects.
 * </p>
 *
 * <p>Tuning Parameters:</p>
 * <ul>
 * <li><b>learnRate:</b>  Specifies how fast it will adapt. 0 to 1, inclusive.  0 = static  1.0 = instant. Try 0.05</li>
 * <li><b>threshold:</b>  Pixel's with a Mahalanobis distance &le; threshold are assumed to be background. Consult
 * a Chi-Squared table for theoretical values.  1-band try 10.  3-bands try 20. </li>
 * <li><b>initial variance</b> The initial variance assigned to pixels when they are first observed.  By default this is
 * Float.MIN_VALUE.
 * </ul>
 *
 * <p>
 * [1] Benezeth, Y., Jodoin, P. M., Emile, B., Laurent, H., & Rosenberger, C. (2010).
 * Comparative study of background subtraction algorithms. Journal of Electronic Imaging, 19(3), 033003-033003.
 * </p>
 *
 * @author Peter Abeles
 */
public interface BackgroundAlgorithmGaussian {

	/**
	 * Returns the initial variance assigned to a pixel
	 * @return initial variance
	 */
	float getInitialVariance();

	/**
	 * Sets the initial variance assigned to a pixel
	 * @param initialVariance initial variance
	 */
	void setInitialVariance(float initialVariance);

	/**
	 * Returns the learning rate.
	 * @return 0 (slow) to 1 (fast)
	 */
	float getLearnRate();

	/**
	 * Specifies the learning rate
	 * @param learnRate 0 (slow) to 1 (fast)
	 */
	void setLearnRate(float learnRate);

	float getThreshold();

	void setThreshold(float threshold);

	public float getMinimumDifference();

	public void setMinimumDifference(float minimumDifference);
}
