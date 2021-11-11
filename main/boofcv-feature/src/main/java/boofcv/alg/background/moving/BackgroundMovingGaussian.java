/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.background.moving;

import boofcv.alg.background.BackgroundAlgorithmGaussian;
import boofcv.alg.background.BackgroundModelMoving;
import boofcv.struct.distort.Point2Transform2Model_F32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.InvertibleTransform;

/**
 * <p>Implementation of {@link BackgroundAlgorithmGaussian} for moving images.</p>
 *
 * @author Peter Abeles
 * @see BackgroundAlgorithmGaussian
 * @see BackgroundModelMoving
 */
public abstract class BackgroundMovingGaussian<T extends ImageBase<T>, Motion extends InvertibleTransform<Motion>>
		extends BackgroundModelMoving<T, Motion> implements BackgroundAlgorithmGaussian {
	/**
	 * Specifies how fast it will adapt. 0 to 1, inclusive. 0 = static  1.0 = instant.
	 */
	protected float learnRate;

	/**
	 * Threshold for classifying a pixel as background or not. This threshold is applied to the
	 * computed Mahalanobis from the distribution.
	 */
	protected float threshold;

	/**
	 * The initial variance assigned to a new pixel. Larger values to reduce false positives due to
	 * under sampling
	 */
	protected float initialVariance = Float.MIN_VALUE;

	protected float minimumDifference = 0;

	/**
	 * See class documentation for parameters definitions.
	 *
	 * @param learnRate Specifies how quickly the background is updated Try 0.05
	 * @param threshold Threshold for background. &ge; 0. Try 10
	 * @param transform Used to convert pixel coordinates
	 * @param imageType Type of input image
	 */
	protected BackgroundMovingGaussian( float learnRate, float threshold,
										Point2Transform2Model_F32<Motion> transform, ImageType<T> imageType ) {
		super(transform, imageType);

		if (threshold < 0)
			throw new IllegalArgumentException("Threshold must be more than 0");

		this.learnRate = learnRate;
		this.threshold = threshold;
	}

	@Override public float getInitialVariance() {
		return initialVariance;
	}

	@Override public void setInitialVariance( float initialVariance ) {
		this.initialVariance = initialVariance;
	}

	@Override public float getLearnRate() {
		return learnRate;
	}

	@Override public void setLearnRate( float learnRate ) {
		this.learnRate = learnRate;
	}

	@Override public float getThreshold() {
		return threshold;
	}

	@Override public void setThreshold( float threshold ) {
		this.threshold = threshold;
	}

	@Override public float getMinimumDifference() {
		return minimumDifference;
	}

	@Override public void setMinimumDifference( float minimumDifference ) {
		this.minimumDifference = minimumDifference;
	}
}
