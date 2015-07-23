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

package boofcv.alg.background.models;

import boofcv.alg.background.BackgroundModelMoving;
import boofcv.struct.distort.PointTransformModel_F32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.InvertibleTransform;

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
 * <li><b>learnRate:</b>  Specifies how fast it will adapt. 0 to 1, inclusive.  0 = static  1.0 = instant.</li>
 * <li><b>threshold:</b>  Pixel's with a Mahalanobis distance <= threshold are assumed to be background. Consult
 * a Chi-Squared table for theoretical values.  1-band try 10.  3-bands try 20. </li>
 * </ul>
 *
 * @author Peter Abeles
 */
public abstract class BackgroundMovingGaussian<T extends ImageBase, Motion extends InvertibleTransform<Motion>>
		extends BackgroundModelMoving<T,Motion>
{
	/**
	 * Specifies how fast it will adapt. 0 to 1, inclusive.  0 = static  1.0 = instant.
	 */
	protected float learnRate;

	/**
	 * Threshold for classifying a pixel as background or not. This threshold is applied to the
	 * computed Mahalanobis from the distribution.
	 */
	protected float threshold;

	/**
	 * The initial variance assigned to a new pixel.  Larger values to reduce false positives due to
	 * under sampling
	 */
	protected float initialVariance = 0;

	/**
	 * See class documentation for parameters definitions.
	 * @param learnRate Specifies how quickly the background is updated
	 * @param threshold Threshold for background.  >= 0
	 * @param transform Used to convert pixel coordinates
	 * @param imageType Type of input image
	 */
	public BackgroundMovingGaussian(float learnRate, float threshold,
									PointTransformModel_F32<Motion> transform, ImageType<T> imageType) {
		super(transform, imageType);

		this.learnRate = learnRate;
		this.threshold = threshold;
	}

	public float getInitialVariance() {
		return initialVariance;
	}

	public void setInitialVariance(float initialVariance) {
		this.initialVariance = initialVariance;
	}

	public float getLearnRate() {
		return learnRate;
	}

	public void setLearnRate(float learnRate) {
		this.learnRate = learnRate;
	}

	public float getThreshold() {
		return threshold;
	}

	public void setThreshold(float threshold) {
		this.threshold = threshold;
	}
}
