/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.background.BackgroundAlgorithmGmm;
import boofcv.alg.background.BackgroundGmmCommon;
import boofcv.alg.background.BackgroundModelMoving;
import boofcv.struct.distort.Point2Transform2Model_F32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.InvertibleTransform;

/**
 * <p>Implementation of {@link BackgroundAlgorithmGmm} for moving images.</p>
 *
 * @see BackgroundModelMoving
 *
 * @author Peter Abeles
 */
public abstract class BackgroundMovingGmm<T extends ImageBase<T>, Motion extends InvertibleTransform<Motion>>
		extends BackgroundModelMoving<T,Motion> implements BackgroundAlgorithmGmm
{
	BackgroundGmmCommon common;

	public BackgroundMovingGmm(float learningPeriod, float decayCoef, int maxGaussians,
							   Point2Transform2Model_F32<Motion> transformImageType, ImageType<T> imageType) {
		super(transformImageType,imageType);

		common = new BackgroundGmmCommon(learningPeriod,decayCoef,maxGaussians,imageType);
	}

	@Override
	public void reset() {
		common.model.zero();
	}

	@Override
	public void initialize(int backgroundWidth, int backgroundHeight, Motion homeToWorld) {
		common.model.reshape(backgroundHeight,backgroundWidth*common.modelStride);
		common.model.zero();

		this.homeToWorld.set(homeToWorld);
		this.homeToWorld.invert(worldToHome);

		this.backgroundWidth = backgroundWidth;
		this.backgroundHeight = backgroundHeight;
	}


	@Override
	public float getInitialVariance() {
		return common.initialVariance;
	}

	@Override
	public void setInitialVariance(float initialVariance) {
		common.initialVariance = initialVariance;
	}

	@Override
	public float getLearningPeriod() {
		return 1.0f / common.learningRate;
	}

	@Override
	public void setLearningPeriod(float period) {
		common.learningRate = 1.0f / period;
	}

	@Override
	public void setSignificantWeight(float value) {
		common.significantWeight = value;
	}

	public float getMaxDistance() {
		return common.maxDistance;
	}

	public void setMaxDistance(float maxDistance) {
		common.maxDistance = maxDistance;
	}

	public BackgroundGmmCommon getCommon() {
		return common;
	}
}
