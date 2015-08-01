/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

/**
 * <p>
 * Performs background subtraction on a moving image by modeling each pixel as an independent Guassian
 * distribution.  For computational efficiency each band is modeled as having a diagonal covariance
 * matrix with off diagonal terms set to zero.  This model is susceptible to
 * </p>
 * <p>
 * By assuming that off diagonal terms are zero that's the same as assuming that all bands are
 * independent distribution.  This is an approximation but according to several papers it doesn't hurt
 * performance much but simplifies computations significantly.
 * </p>
 * <p>
 * Internally the mean and variance are stored in ImageFloat32 images.  This allows for the mean and variance
 * of each pixel to be interpolated, reducing artifacts along the border of objects.
 * </p>
 *
 * <p>Tuning Parameters:</p>
 * <ul>
 * <li><b>learnRate:</b>  Specifies how fast it will adapt. 0 to 1, inclusive.  0 = static  1.0 = instant. Try 0.05</li>
 * <li><b>threshold:</b>  Pixel's with a Mahalanobis distance <= threshold are assumed to be background. Consult
 * a Chi-Squared table for theoretical values.  1-band try 10.  3-bands try 20. </li>
 * <li><b>initial variance</b> The initial variance assigned to pixels when they are first observed.  By default this is
 * Float.MIN_VALUE.
 * </ul>
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
