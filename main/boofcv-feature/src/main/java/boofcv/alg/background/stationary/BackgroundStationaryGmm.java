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

package boofcv.alg.background.stationary;

import boofcv.alg.background.BackgroundAlgorithmGmm;
import boofcv.alg.background.BackgroundGmmCommon;
import boofcv.alg.background.BackgroundModelStationary;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import javax.annotation.Nullable;

/**
 * <p>Implementation of {@link BackgroundAlgorithmGmm} for stationary images.</p>
 *
 * @see BackgroundModelStationary
 *
 * @author Peter Abeles
 */
public abstract class BackgroundStationaryGmm< T extends ImageBase<T>>
		extends BackgroundModelStationary<T> implements BackgroundAlgorithmGmm
{
	BackgroundGmmCommon common;

	public BackgroundStationaryGmm(float learningPeriod, float decayCoef,
								   int maxGaussians, ImageType<T> imageType) {
		super(imageType);
		common = new BackgroundGmmCommon(learningPeriod,decayCoef,maxGaussians,imageType);
	}

	@Override
	public void reset() {
		common.model.reshape(0, 0);
		common.imageWidth = common.imageHeight = 0;
	}

	@Override
	public void updateBackground( T frame ) {
		updateBackground(frame,null);
	}

	/**
	 *
	 * @param mask If null then the background mask is ignored
	 */
	@Override
	public void updateBackground( T frame , @Nullable GrayU8 mask ) {

		// if the image size has changed it's safe to assume it needs to be re-initialized
		if( common.imageWidth != frame.width || common.imageHeight != frame.height ) {
			common.imageWidth = frame.width;
			common.imageHeight = frame.height;

			common.model.reshape(frame.height, frame.width*common.modelStride);
			common.model.zero();
		}

		if( mask != null ) {
			mask.reshape(frame.width,frame.height);
		}
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
}
