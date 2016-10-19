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

package boofcv.alg.background.moving;

import boofcv.alg.background.BackgroundAlgorithmBasic;
import boofcv.alg.background.BackgroundModelMoving;
import boofcv.struct.distort.Point2Transform2Model_F32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.InvertibleTransform;

/**
 * <p>Implementation of {@link BackgroundAlgorithmBasic} for moving images.</p>
 *
 * @see BackgroundAlgorithmBasic
 * @see BackgroundModelMoving
 *
 * @author Peter Abeles
 */
public abstract class BackgroundMovingBasic<T extends ImageBase, Motion extends InvertibleTransform<Motion>>
		extends BackgroundModelMoving<T,Motion> implements BackgroundAlgorithmBasic {

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
	 * @param transform Point transform
	 * @param imageType Type of input image
	 */
	public BackgroundMovingBasic(float learnRate , float threshold,
								 Point2Transform2Model_F32<Motion> transform, ImageType<T> imageType) {
		super(transform, imageType);

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
