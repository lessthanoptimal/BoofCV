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

package boofcv.alg.background.stationary;

import boofcv.alg.background.BackgroundAlgorithmBasic;
import boofcv.alg.background.BackgroundModelStationary;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * <p>Implementation of {@link BackgroundAlgorithmBasic} for stationary images.</p>
 *
 * @see BackgroundAlgorithmBasic
 * @see BackgroundModelStationary
 *
 * @author Peter Abeles
 */
public abstract class BackgroundStationaryBasic<T extends ImageBase>
		extends BackgroundModelStationary<T> implements BackgroundAlgorithmBasic {

	/**
	 * Specifies how fast it will adapt. 0 to 1, inclusive.  0 = static  1.0 = instant.
	 */
	protected float learnRate;

	/**
	 * Threshold for classifying a pixel as background or not.  If euclidean distance less than or equal to this value
	 * it is background.
	 */
	protected float threshold;

	/**
	 * Configures background model
	 *
	 * @param learnRate learning rate, 0 to 1.  0 = fastest and 1 = slowest.
	 * @param threshold Euclidean distance threshold.  Background is &le; this value
	 * @param imageType Type of input image
	 */
	public BackgroundStationaryBasic(float learnRate, float threshold, ImageType<T> imageType) {
		super(imageType);

		if( learnRate < 0 || learnRate > 1f )
			throw new IllegalArgumentException("LearnRate must be 0 <= rate <= 1.0f");

		this.learnRate = learnRate;
		this.threshold = threshold;
	}

	@Override
	public float getLearnRate() {
		return learnRate;
	}

	@Override
	public void setLearnRate(float learnRate) {
		this.learnRate = learnRate;
	}

	@Override
	public float getThreshold() {
		return threshold;
	}

	@Override
	public void setThreshold(float threshold) {
		this.threshold = threshold;
	}
}
