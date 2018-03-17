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

package boofcv.alg.background;

import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.FactoryGImageMultiBand;
import boofcv.core.image.GImageGray;
import boofcv.core.image.GImageMultiBand;
import boofcv.struct.RArray2D_F32;
import boofcv.struct.image.ImageType;

/**
 * Common code for all implementations of {@link BackgroundAlgorithmGmm}. This is where most of the important
 * mathematics is contained.
 *
 * @author Peter Abeles
 */
public class BackgroundGmmCommon {
	// Storage for estimated models
	//
	// [2*i+0] = weight for gaussian i
	// [2*i+1] = variance for gaussian i. variance <= 0 means the Gaussian is unused
	// [2*i+2] = mean for gaussian i
	// The first N gaussians are always in use
	public RArray2D_F32 model = new RArray2D_F32(1, 1);

	// Shape of expected input image
	public int imageWidth, imageHeight, numBands;

	// number of elements needed to describe a pixel's model. 3*number of gaussians
	public int modelStride;
	// number of elements in a single gausian description
	public int gaussianStride;

	// Determines how quickly it learns/accepts changes to a model
	public float learningRate; // \alpha in the paper
	// decay subtracted from weights to prune old models
	public float decay; // \alpha_{CT} in the paper
	// The maximum number of gaussian models for a pixel
	public int maxGaussians;

	// Maximum Mahanolobis distance
	public float maxDistance = 3 * 3; // standard deviations squared away

	// if the weight of the best fit Gaussian is more than this value it is considered to belong to the background
	// the foreground could have small values as it moves around, which is why simply matching a model
	// isn't enough
	public float significantWeight;

	// initial variance assigned to a new Gaussian
	public float initialVariance = 100;

	// value returned when there's no information
	public int unknownValue = 0;

	// For multiband images only
	public GImageMultiBand inputWrapperMB;
	public float inputPixel[];

	public GImageGray inputWrapperG;

	public BackgroundGmmCommon(float learningPeriod, float decayCoef, int maxGaussians, ImageType imageType) {

		if (learningPeriod <= 0)
			throw new IllegalArgumentException("Must be greater than zero");
		if (maxGaussians >= 256 || maxGaussians <= 0)
			throw new IllegalArgumentException("Maximum number of gaussians per pixel is 255");

		setLearningPeriod(learningPeriod);
		this.decay = decayCoef;
		this.maxGaussians = maxGaussians;

		this.significantWeight = Math.min(0.2f, 100 * learningRate);

		switch( imageType.getFamily() ) {
			case GRAY:
				inputWrapperG = FactoryGImageGray.create(imageType.getImageClass());
				break;

			default:
				inputWrapperMB = FactoryGImageMultiBand.create(imageType);
				inputPixel = new float[imageType.numBands];
				break;
		}

		this.numBands = imageType.numBands;
		this.gaussianStride = 2 + numBands; // 1 weight, 1 variance, N means
		this.modelStride = maxGaussians * gaussianStride;
	}


	/**
	 * Updates the mixtures of gaussian and determines if the pixel matches the background model
	 * @return true if it matches the background or false if not
	 */
	public int updateMixture( float[] pixelValue , float[] dataRow , int modelIndex ) {

		// see which gaussian is the best fit based on Mahalanobis distance
		int index = modelIndex;
		float bestDistance = maxDistance*numBands;
		int bestIndex=-1;

		int ng; // number of gaussians in use
		for (ng = 0; ng < maxGaussians; ng++, index += gaussianStride) {
			float variance = dataRow[index+1];
			if( variance <= 0 ) {
				break;
			}

			float mahalanobis = 0;
			for (int i = 0; i < numBands; i++) {
				float mean = dataRow[index+2+i];
				float delta = pixelValue[i]-mean;
				mahalanobis += delta*delta/variance;
			}

			if( mahalanobis < bestDistance ) {
				bestDistance = mahalanobis;
				bestIndex = index;
			}
		}

		// Update the model for the best gaussian
		if( bestIndex != -1 ) {
			// If there is a good fit update the model
			float weight = dataRow[bestIndex];
			float variance = dataRow[bestIndex+1];

			weight += learningRate*(1f-weight);
			dataRow[bestIndex]   = 1; // set to one so that it can't possible go negative

			float sumDeltaSq = 0;
			for (int i = 0; i < numBands; i++) {
				float mean = dataRow[bestIndex+2+i];
				float delta = pixelValue[i]-mean;
				dataRow[bestIndex+2+i] = mean + delta*learningRate/weight;
				sumDeltaSq += delta*delta;
			}
			sumDeltaSq /= numBands;
			dataRow[bestIndex+1] = variance + (learningRate /weight)*(sumDeltaSq*1.2F - variance);
			// 1.2f is a fudge factor. Empirical testing shows that the above equation is biased. Can't be bothered
			// to verify the derivation and see if there's a mistake

			// Update Gaussian weights and prune models
			updateWeightAndPrune(dataRow, modelIndex, ng, bestIndex, weight);

			return weight >= significantWeight ? 0 : 1;
		} else if( ng < maxGaussians ) {
			// if there is no good fit then create a new model, if there is room

			bestIndex = modelIndex + ng*gaussianStride;
			dataRow[bestIndex]   = 1; // weight is changed later or it's the only model
			dataRow[bestIndex+1] = initialVariance;
			for (int i = 0; i < numBands; i++) {
				dataRow[bestIndex+2+i] = pixelValue[i];
			}
			// There are no models. Return unknown
			if( ng == 0 )
				return unknownValue;

			updateWeightAndPrune(dataRow, modelIndex, ng+1, bestIndex, learningRate);
			return 1;
		} else {
			// didn't match any models and can't create a new model
			return 1;
		}
	}

	/**
	 * Updates the weight of each Gaussian and prunes one which have a negative weight after the update.
	 */
	public void updateWeightAndPrune(float[] dataRow, int modelIndex, int ng, int bestIndex, float bestWeight) {
		int index = modelIndex;
		float weightTotal = 0;
		for (int i = 0; i < ng;  ) {
			float weight = dataRow[index];
//			if( ng > 1 )
//				System.out.println("["+i+"] = "+ng+"  weight "+weight);
			weight = weight - learningRate*(weight + decay); // <-- original equation
//			weight = weight - learningRate*decay;
			if( weight <= 0 ) {
				// copy the last Gaussian into this location
				int indexLast = modelIndex + (ng-1)*gaussianStride;
				for (int j = 0; j < gaussianStride; j++) {
					dataRow[index+j] = dataRow[indexLast+j];
				}

				// see if the best Gaussian just got moved to here
				if( indexLast == bestIndex )
					bestIndex = index;

				// mark it as unused by setting variance to zero
				dataRow[indexLast+1] = 0;

				// decrease the number of gaussians
				ng -= 1;
			} else {
				dataRow[index] = weight;
				weightTotal += weight;
				index += gaussianStride;
				i++;
			}
		}

		// undo the change to the best model. It was done in the for loop to avoid an if statement which would
		// have slowed it down
		if( bestIndex != -1 ) {
			weightTotal -= dataRow[bestIndex];
			weightTotal += bestWeight;
			dataRow[bestIndex] = bestWeight;
		}

		// Normalize the weight so that it sums up to one
		index = modelIndex;
		for (int i = 0; i < ng; i++, index += gaussianStride) {
			dataRow[index] /= weightTotal;
		}
	}

	/**
	 * Updates the mixtures of gaussian and determines if the pixel matches the background model
	 * @return true if it matches the background or false if not
	 */
	public int updateMixture( float pixelValue , float[] dataRow , int modelIndex ) {

		// see which gaussian is the best fit based on Mahalanobis distance
		int index = modelIndex;
		float bestDistance = maxDistance;
		int bestIndex=-1;

		int ng; // number of gaussians in use
		for (ng = 0; ng < maxGaussians; ng++, index += 3) {
			float variance = dataRow[index+1];
			float mean = dataRow[index+2];

			if( variance <= 0 ) {
				break;
			}

			float delta = pixelValue-mean;
			float mahalanobis = delta*delta/variance;
			if( mahalanobis < bestDistance ) {
				bestDistance = mahalanobis;
				bestIndex = index;
			}
		}

		// Update the model for the best gaussian
		if( bestDistance != maxDistance ) {
			// If there is a good fit update the model
			float weight = dataRow[bestIndex];
			float variance = dataRow[bestIndex+1];
			float mean = dataRow[bestIndex+2];

			float delta = pixelValue-mean;

			weight += learningRate*(1f-weight);
			dataRow[bestIndex]   = 1; // set to one so that it can't possible go negative. changed later
			dataRow[bestIndex+1] = variance + (learningRate /weight)*(delta*delta*1.2F - variance);
			// 1.2f is a fudge factor. Empirical testing shows that the above equation is biased. Can't be bothered
			// to verify the derivation and see if there's a mistake
			dataRow[bestIndex+2] = mean + delta* learningRate /weight;

			// Update Gaussian weights and prune models
			updateWeightAndPrune(dataRow, modelIndex, ng, bestIndex, weight);

			return weight >= significantWeight ? 0 : 1;
		} else if( ng < maxGaussians ) {
			// if there is no good fit then create a new model, if there is room
			bestIndex = modelIndex + ng*3;
			dataRow[bestIndex]   = 1; // weight is changed later or it's the only model
			dataRow[bestIndex+1] = initialVariance;
			dataRow[bestIndex+2] = pixelValue;

			// There are no models. Return unknown
			// weight was set to one above, so that's correct since there's only one model
			if( ng == 0 )
				return unknownValue;

			// Update Gaussian weights and prune models
			updateWeightAndPrune(dataRow, modelIndex, ng+1, bestIndex, learningRate);

			return 1; // must be foreground since it didn't match any background
		} else {
			// didn't match any models and can't create a new model
			return 1;
		}
	}

	/**
	 * Checks to see if the the pivel value refers to the background or foreground
	 *
	 * @return true for background or false for foreground
	 */
	public int checkBackground( float[] pixelValue , float[] dataRow , int modelIndex ) {

		// see which gaussian is the best fit based on Mahalanobis distance
		int index = modelIndex;
		float bestDistance = maxDistance*numBands;
		float bestWeight = 0;

		int ng; // number of gaussians in use
		for (ng = 0; ng < maxGaussians; ng++, index += gaussianStride) {
			float variance = dataRow[index + 1];
			if (variance <= 0) {
				break;
			}

			float mahalanobis = 0;
			for (int i = 0; i < numBands; i++) {
				float mean = dataRow[index + 2+i];
				float delta = pixelValue[i] - mean;
				mahalanobis += delta * delta / variance;
			}

			if (mahalanobis < bestDistance) {
				bestDistance = mahalanobis;
				bestWeight = dataRow[index];
			}
		}

		if( ng == 0 ) // There are no models. Return unknown
			return unknownValue;
		return bestWeight >= significantWeight ? 0 : 1;
	}

	/**
	 * Checks to see if the the pivel value refers to the background or foreground
	 *
	 * @return true for background or false for foreground
	 */
	public int checkBackground( float pixelValue , float[] dataRow , int modelIndex ) {

		// see which gaussian is the best fit based on Mahalanobis distance
		int index = modelIndex;
		float bestDistance = maxDistance;
		float bestWeight = 0;

		int ng; // number of gaussians in use
		for (ng = 0; ng < maxGaussians; ng++, index += 3) {
			float variance = dataRow[index + 1];
			float mean = dataRow[index + 2];

			if (variance <= 0) {
				break;
			}

			float delta = pixelValue - mean;
			float mahalanobis = delta * delta / variance;
			if (mahalanobis < bestDistance) {
				bestDistance = mahalanobis;
				bestWeight = dataRow[index];
			}
		}

		if( ng == 0 ) // There are no models. Return unknown
			return unknownValue;
		return bestWeight >= significantWeight ? 0 : 1;
	}

	public void setLearningPeriod(float period) {
		learningRate = 1.0f / period;
	}

	public float getMaxDistance() {
		return maxDistance;
	}

	public void setMaxDistance(float maxDistance) {
		this.maxDistance = maxDistance;
	}

	public float getSignificantWeight() {
		return significantWeight;
	}

	public void setSignificantWeight(float significantWeight) {
		this.significantWeight = significantWeight;
	}

	public float getInitialVariance() {
		return initialVariance;
	}

	public void setInitialVariance(float initialVariance) {
		this.initialVariance = initialVariance;
	}
}
