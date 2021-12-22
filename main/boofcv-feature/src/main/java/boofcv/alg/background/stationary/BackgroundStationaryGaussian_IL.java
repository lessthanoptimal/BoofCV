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

import boofcv.alg.InputSanityCheck;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.FactoryGImageMultiBand;
import boofcv.core.image.GImageMultiBand;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageInterleaved;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.InterleavedF32;
import pabeles.concurrency.GrowArray;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * Implementation of {@link BackgroundStationaryGaussian} for {@link ImageInterleaved}.
 *
 * @author Peter Abeles
 */
public class BackgroundStationaryGaussian_IL<T extends ImageInterleaved<T>> extends BackgroundStationaryGaussian<T> {

	// wrappers which provide abstraction across image types
	protected GImageMultiBand inputWrapper;
	protected GImageMultiBand bgWrapper;

	// storage for multi-band pixel values
	Work work;
	GrowArray<Work> workspace;

	// background is composed of bands*2 channels. even = mean, odd = variance
	InterleavedF32 background;

	/**
	 * Configurations background removal.
	 *
	 * @param learnRate Specifies how quickly the background is updated. 0 = static  1.0 = instant. Try 0.05
	 * @param threshold Threshold for background. Consult a chi-square table for reasonably values.
	 * 10 to 16 for 1 to 3 bands.
	 * @param imageType Type of input image.
	 */
	public BackgroundStationaryGaussian_IL( float learnRate, float threshold, ImageType<T> imageType ) {
		super(learnRate, threshold, imageType);

		int numBands = imageType.getNumBands();

		background = new InterleavedF32(0, 0, 2*numBands);
		bgWrapper = FactoryGImageMultiBand.create(background.getImageType());
		bgWrapper.wrap(background);

		inputWrapper = FactoryGImageMultiBand.create(imageType);

		workspace = new GrowArray<>(() -> new Work(numBands));
		work = workspace.grow();
	}

	@Override public void reset() {
		background.reshape(0, 0);
	}

	@Override public void updateBackground( T frame ) {
		inputWrapper.wrap(frame);

		// Fill in background model with the mean and initial variance of each pixel
		if (background.width != frame.width || background.height != frame.height) {
			background.reshape(frame.width, frame.height);

			//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0, frame.height, 20, workspace, (work, idx0, idx1) -> {
			final int idx0 = 0, idx1 = frame.height;
			float[] inputPixel = work.inputPixel;
			float[] bgPixel = work.bgPixel;
			for (int y = idx0; y < idx1; y++) {
				for (int x = 0; x < frame.width; x++) {
					inputWrapper.get(x, y, inputPixel);
					for (int i = 0; i < frame.numBands; i++) {
						bgPixel[i*2] = inputPixel[i];
						bgPixel[i*2 + 1] = initialVariance;
					}
					bgWrapper.set(x, y, bgPixel);
				}
			}
			//CONCURRENT_ABOVE }});
			return;
		}

		InputSanityCheck.checkSameShape(background, frame);

		final int numBands = background.getNumBands()/2;
		final float minusLearn = 1.0f - learnRate;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0, frame.height, 20, workspace, (work, idx0, idx1) -> {
		final int idx0 = 0, idx1 = frame.height;
		float[] inputPixel = work.inputPixel;
		for (int y = idx0; y < idx1; y++) {
			int indexBG = y*background.stride;
			int indexInput = frame.startIndex + y*frame.stride;
			int end = indexInput + frame.width*numBands;
			while (indexInput < end) {
				inputWrapper.getF(indexInput, inputPixel);

				for (int band = 0; band < numBands; band++) {

					float inputValue = inputPixel[band];
					float meanBG = background.data[indexBG];
					float varianceBG = background.data[indexBG + 1];

					float diff = meanBG - inputValue;
					background.data[indexBG++] = minusLearn*meanBG + learnRate*inputValue;
					background.data[indexBG++] = minusLearn*varianceBG + learnRate*diff*diff;
				}

				indexInput += frame.numBands;
			}
		}
		//CONCURRENT_ABOVE }});
	}

	@Override public void segment( T frame, GrayU8 segmented ) {
		segmented.reshape(frame.width, frame.height);
		if (background.width != frame.width || background.height != frame.height) {
			ImageMiscOps.fill(segmented, unknownValue);
			return;
		}

		inputWrapper.wrap(frame);

		final int numBands = background.getNumBands()/2;

		float adjustedMinimumDifference = minimumDifference*numBands;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0, frame.height, 20, workspace, (work, idx0, idx1) -> {
		final int idx0 = 0, idx1 = frame.height;
		float[] inputPixel = work.inputPixel;
		for (int y = idx0; y < idx1; y++) {
			int indexBG = y*background.stride;
			int indexInput = frame.startIndex + y*frame.stride;
			int indexSegmented = segmented.startIndex + y*segmented.stride;

			int end = indexInput + frame.width*frame.numBands;
			while (indexInput < end) {
				inputWrapper.getF(indexInput, inputPixel);

				float mahalanobis = 0;
				for (int band = 0; band < numBands; band++) {
					int indexBG_band = indexBG + band*2;

					float meanBG = background.data[indexBG_band];
					float varBG = background.data[indexBG_band + 1];

					float diff = meanBG - inputPixel[band];
					mahalanobis += diff*diff/varBG;
				}

				if (mahalanobis <= threshold) {
					segmented.data[indexSegmented] = 0;
				} else {
					if (minimumDifference == 0) {
						segmented.data[indexSegmented] = 1;
					} else {
						float sumAbsDiff = 0;
						for (int band = 0; band < numBands; band++) {
							int indexBG_band = indexBG + band*2;
							sumAbsDiff += Math.abs(background.data[indexBG_band] - inputPixel[band]);
						}
						if (sumAbsDiff >= adjustedMinimumDifference)
							segmented.data[indexSegmented] = 1;
						else
							segmented.data[indexSegmented] = 0;
					}
				}

				indexInput += frame.numBands;
				indexSegmented += 1;
				indexBG += background.numBands;
			}
		}
		//CONCURRENT_ABOVE }});
	}

	private static class Work {
		final float[] inputPixel;
		final float[] bgPixel;

		public Work( int numBands ) {
			inputPixel = new float[numBands];
			bgPixel = new float[numBands*2];
		}
	}
}
