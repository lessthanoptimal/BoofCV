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
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.FactoryGImageMultiBand;
import boofcv.core.image.GImageMultiBand;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageMultiBand;
import boofcv.struct.image.ImageType;

import javax.annotation.Nullable;

/**
 * Implementation of {@link BackgroundAlgorithmGmm} for {@link ImageMultiBand}.
 *
 * @author Peter Abeles
 */
public class BackgroundStationaryGmm_MB<T extends ImageMultiBand<T>>
		extends BackgroundStationaryGmm<T>
{
	// wrappers which provide abstraction across image types
	protected GImageMultiBand inputWrapper;

	private float inputPixel[];

	/**
	 *
	 * @param learningPeriod Specifies how fast it will adjust to changes in the image. Must be greater than zero.
	 * @param decayCoef Determines how quickly a Gaussian is forgotten
	 * @param maxGaussians Maximum number of Gaussians in a mixture for a pixel
	 * @param imageType Type of image it's processing.
	 */
	public BackgroundStationaryGmm_MB(float learningPeriod, float decayCoef,
									  int maxGaussians, ImageType<T> imageType )
	{
		super(learningPeriod, decayCoef, maxGaussians, imageType);

		inputWrapper = FactoryGImageMultiBand.create(imageType);

		inputPixel = new float[imageType.numBands];
	}

	/**
	 *
	 * @param mask If null then the background mask is ignored
	 */
	@Override
	public void updateBackground( T frame , @Nullable GrayU8 mask ) {
		super.updateBackground(frame, mask);

		inputWrapper.wrap(frame);
		final int pixelStride = inputWrapper.getPixelStride();
		for (int row = 0; row < imageHeight; row++) {
			int inputIndex = frame.getIndex(0,row);
			float[] dataRow = model.data[row];

			if( mask == null ) {
				for (int col = 0; col < imageWidth; col++, inputIndex += pixelStride) {
					inputWrapper.getF(inputIndex,inputPixel);
					int modelIndex = col * modelStride;

					updateMixture(inputPixel, dataRow, modelIndex);
				}
			} else {
				int indexMask = mask.startIndex + row*mask.stride;
				for (int col = 0; col < imageWidth; col++, inputIndex += pixelStride) {
					inputWrapper.getF(inputIndex,inputPixel);
					int modelIndex = col * modelStride;

					mask.data[indexMask++] = updateMixture(inputPixel, dataRow, modelIndex) ? (byte)0 : (byte)1;
				}
			}
		}
	}
	/**
	 * Updates the mixtures of gaussian and determines if the pixel matches the background model
	 * @return true if it matches the background or false if not
	 */
	boolean updateMixture( float[] pixelValue , float[] dataRow , int modelIndex ) {

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
		float bestWeight = 0;
		if( bestIndex != -1 ) {
			// If there is a good fit update the model
			float weight = dataRow[bestIndex];
			float variance = dataRow[bestIndex+1];

			bestWeight = weight + learningRate*(1f-weight-decay);
			dataRow[bestIndex]   = 1; // set to one so that it can't possible go negative

			float sumDeltaSq = 0;
			for (int i = 0; i < numBands; i++) {
				float mean = dataRow[bestIndex+2+i];
				float delta = pixelValue[i]-mean;
				dataRow[bestIndex+2+i] = mean + delta*learningRate/bestWeight;
				sumDeltaSq += delta*delta;
			}
			sumDeltaSq /= numBands;
			dataRow[bestIndex+1] = variance + (learningRate /bestWeight)*(sumDeltaSq - variance);


		} else if( ng < maxGaussians ) {
			// if there is no good fit then create a new model, if there is room

			bestWeight = learningRate;
			bestIndex = modelIndex + ng*gaussianStride;
			dataRow[bestIndex]   = 1;
			dataRow[bestIndex+1] = initialVariance;
			for (int i = 0; i < numBands; i++) {
				dataRow[bestIndex+2+i] = pixelValue[i];
			}
			ng += 1;
		}

		// Update Gaussian weights and prune models
		updateWeightAndPrune(dataRow, modelIndex, ng, bestIndex, bestWeight);

		return bestWeight >= significantWeight;
	}

	/**
	 * Updates the weight of each Gaussian and prunes one which have a negative weight after the update.
	 */
	void updateWeightAndPrune(float[] dataRow, int modelIndex, int ng, int bestIndex, float bestWeight) {
		int index = modelIndex;
		float weightTotal = 0;
		for (int i = 0; i < ng;  ) {
			float weight = dataRow[index];
			weight = weight - learningRate*(weight + decay);
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

	@Override
	public void segment(T frame, GrayU8 segmented) {
		if( imageWidth != frame.width || imageHeight != frame.height ) {
			segmented.reshape(frame.width,frame.height);
			ImageMiscOps.fill(segmented,unknownValue);
			return;
		}

		inputWrapper.wrap(frame);
		final int pixelStride = inputWrapper.getPixelStride();
		for (int row = 0; row < imageHeight; row++) {
			int indexIn = frame.getIndex(0,row);
			int indexOut = segmented.startIndex + row*segmented.stride;
			float[] dataRow = model.data[row];

			for (int col = 0; col < imageWidth; col++, indexIn += pixelStride) {
				inputWrapper.getF(indexIn,inputPixel);
				int modelIndex = col * modelStride;

				segmented.data[indexOut++] = checkBackground(inputPixel, dataRow, modelIndex) ? (byte)0 : (byte)1;
			}
		}
	}

	/**
	 * Checks to see if the the pivel value refers to the background or foreground
	 *
	 * @return true for background or false for foreground
	 */
	boolean checkBackground( float[] pixelValue , float[] dataRow , int modelIndex ) {

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

		return bestWeight >= significantWeight;
	}
}
