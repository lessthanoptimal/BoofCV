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

import boofcv.alg.background.BackgroundAlgorithmBasic;
import boofcv.alg.background.BackgroundModelMoving;
import boofcv.struct.distort.Point2Transform2Model_F32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.InvertibleTransform;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>Implementation of {@link BackgroundAlgorithmBasic} for moving images.</p>
 *
 * @author Peter Abeles
 * @see BackgroundAlgorithmBasic
 * @see BackgroundModelMoving
 */
@SuppressWarnings("MissingOverride")
public abstract class BackgroundMovingBasic<T extends ImageBase<T>, Motion extends InvertibleTransform<Motion>>
		extends BackgroundModelMoving<T, Motion> implements BackgroundAlgorithmBasic {
	/**
	 * Specifies how fast it will adapt. 0 to 1, inclusive. 0 = static  1.0 = instant.
	 */
	@Getter @Setter protected float learnRate;

	/**
	 * Threshold for classifying a pixel as background or not. If euclidean distance less than or equal to this value
	 * it is background.
	 */
	@Getter @Setter protected float threshold;

	/**
	 * Configures background model
	 *
	 * @param learnRate learning rate, 0 to 1. 0 = fastest and 1 = slowest.
	 * @param threshold Euclidean distance threshold. Background is &le; this value
	 * @param transform Point transform
	 * @param imageType Type of input image
	 */
	protected BackgroundMovingBasic( float learnRate, float threshold,
									 Point2Transform2Model_F32<Motion> transform, ImageType<T> imageType ) {
		super(transform, imageType);

		if (learnRate < 0 || learnRate > 1f)
			throw new IllegalArgumentException("LearnRate must be 0 <= rate <= 1.0f");

		this.learnRate = learnRate;
		this.threshold = threshold;
	}
}
