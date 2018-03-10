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
import boofcv.alg.background.BackgroundModelMoving;
import boofcv.struct.RArray2D_F32;
import boofcv.struct.distort.Point2Transform2Model_F32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.InvertibleTransform;

import javax.annotation.Nullable;

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
	// Storage for estimated models
	//
	// [2*i+0] = weight for gaussian i
	// [2*i+1] = variance for gaussian i. variance <= 0 means the Gaussian is unused
	// [2*i+2] = mean for gaussian i
	// The first N gaussians are always in use
	protected RArray2D_F32 model = new RArray2D_F32(1, 1);

	// Shape of expected input image
	protected int imageWidth, imageHeight, numBands;

	// number of elements needed to describe a pixel's model. 3*number of gaussians
	protected int modelStride;
	// number of elements in a single gausian description
	protected int gaussianStride;

	// Determines how quickly it learns/accepts changes to a model
	protected float learningRate; // \alpha in the paper
	// decay subtracted from weights to prune old models
	protected float decay; // \alpha_{CT} in the paper
	// The maximum number of gaussian models for a pixel
	protected int maxGaussians;

	// Maximum Mahanolobis distance
	protected float maxDistance = 3 * 3; // standard deviations squared away

	// if the weight of the best fit Gaussian is more than this value it is considered to belong to the background
	// the foreground could have small values as it moves around, which is why simply matching a model
	// isn't enough
	protected float significantWeight;

	// initial variance assigned to a new Gaussian
	protected float initialVariance = 100;


	public BackgroundMovingGmm(float learningPeriod, float decayCoef, int maxGaussians,
							   Point2Transform2Model_F32<Motion> transformImageType, ImageType<T> imageType) {
		super(transformImageType,imageType);
		if (learningPeriod <= 0)
			throw new IllegalArgumentException("Must be greater than zero");
		if (maxGaussians >= 256 || maxGaussians <= 0)
			throw new IllegalArgumentException("Maximum number of gaussians per pixel is 255");

		setLearningPeriod(learningPeriod);
		this.decay = decayCoef;
		this.maxGaussians = maxGaussians;

		this.significantWeight = Math.min(0.2f, 100 * learningRate);
	}

	@Override
	public void reset() {
		model.reshape(0, 0);
		imageWidth = imageHeight = 0;
	}


	public void updateBackground( T frame , @Nullable GrayU8 mask ) {

		int channels = frame.getImageType().getNumBands();
		this.gaussianStride = 2 + channels; // 1 weight, 1 variance, N means
		this.modelStride = maxGaussians * gaussianStride;


		// if the image size has changed it's safe to assume it needs to be re-initialized
		if( imageWidth != frame.width || imageHeight != frame.height || numBands != channels ) {
			numBands = channels;
			imageWidth = frame.width;
			imageHeight = frame.height;

			model.reshape(frame.height, frame.width* modelStride);
			model.zero();
		}

		if( mask != null ) {
			mask.reshape(frame.width,frame.height);
		}
	}


	@Override
	public float getInitialVariance() {
		return initialVariance;
	}

	@Override
	public void setInitialVariance(float initialVariance) {
		this.initialVariance = initialVariance;
	}

	@Override
	public float getLearningPeriod() {
		return 1.0f / learningRate;
	}

	@Override
	public void setLearningPeriod(float period) {
		learningRate = 1.0f / period;
	}

	@Override
	public void setSignificantWeight(float value) {
		significantWeight = value;
	}

	public float getMaxDistance() {
		return maxDistance;
	}

	public void setMaxDistance(float maxDistance) {
		this.maxDistance = maxDistance;
	}
}
