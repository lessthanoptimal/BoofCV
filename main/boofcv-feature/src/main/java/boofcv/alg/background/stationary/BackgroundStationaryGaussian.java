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

package boofcv.alg.background.stationary;

import boofcv.alg.background.BackgroundAlgorithmGaussian;
import boofcv.alg.background.BackgroundModelStationary;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>Implementation of {@link BackgroundAlgorithmGaussian} for stationary images.</p>
 *
 * @author Peter Abeles
 * @see BackgroundAlgorithmGaussian
 * @see BackgroundModelStationary
 */
@SuppressWarnings("MissingOverride")
public abstract class BackgroundStationaryGaussian<T extends ImageBase<T>>
		extends BackgroundModelStationary<T> implements BackgroundAlgorithmGaussian {
	/**
	 * Specifies how fast it will adapt. 0 to 1, inclusive. 0 = static  1.0 = instant.
	 */
	@Getter @Setter protected float learnRate;

	/**
	 * Threshold for classifying a pixel as background or not. This threshold is applied to the
	 * computed Mahalanobis from the distribution.
	 */
	@Getter @Setter protected float threshold;

	/**
	 * The initial variance assigned to a new pixel. Larger values to reduce false positives due to
	 * under sampling
	 */
	@Getter @Setter protected float initialVariance = Float.MIN_VALUE;

	@Getter @Setter protected float minimumDifference = 0;

	/**
	 * See class documentation for parameters definitions.
	 *
	 * @param learnRate Specifies how quickly the background is updated Try 0.05
	 * @param threshold Threshold for background. &ge; 0. Try 10
	 * @param imageType Type of input image
	 */
	protected BackgroundStationaryGaussian( float learnRate, float threshold, ImageType<T> imageType ) {
		super(imageType);

		if (threshold < 0)
			throw new IllegalArgumentException("Threshold must be more than 0");

		this.learnRate = learnRate;
		this.threshold = threshold;
	}
}
