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

import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.FactoryGImageMultiBand;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.GImageMultiBand;
import boofcv.struct.image.*;
import pabeles.concurrency.GrowArray;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * Implementation of {@link BackgroundStationaryGaussian} for {@link Planar}.
 *
 * @author Peter Abeles
 */
public class BackgroundStationaryGaussian_PL<T extends ImageGray<T>> extends BackgroundStationaryGaussian<Planar<T>> {

	// wrappers which provide abstraction across image types
	protected GImageMultiBand inputWrapper;
	protected GImageMultiBand bgWrapper;

	// storage for multi-band pixel values
	GrowArray<float[]> storageInput;

	// background is composed of bands*2 channels. even = mean, odd = variance
	Planar<GrayF32> background;

	/**
	 * Configurations background removal.
	 *
	 * @param learnRate Specifies how quickly the background is updated. 0 = static  1.0 = instant. Try 0.05
	 * @param threshold Threshold for background. Consult a chi-square table for reasonably values.
	 * 10 to 16 for 1 to 3 bands.
	 * @param imageType Type of input image.
	 */
	public BackgroundStationaryGaussian_PL( float learnRate, float threshold,
											ImageType<Planar<T>> imageType ) {
		super(learnRate, threshold, imageType);

		int numBands = imageType.getNumBands();

		background = new Planar<>(GrayF32.class, 0, 0, 2*numBands);
		bgWrapper = FactoryGImageMultiBand.create(background.getImageType());
		bgWrapper.wrap(background);

		inputWrapper = FactoryGImageMultiBand.create(imageType);

		storageInput = new GrowArray<>(() -> new float[numBands]);
	}

	@Override public void reset() {
		background.reshape(0, 0);
	}

	@Override public void updateBackground( Planar<T> frame ) {

		// Fill in background model with the mean and initial variance of each pixel
		if (background.width != frame.width || background.height != frame.height) {
			background.reshape(frame.width, frame.height);
			// initialize the mean to the current image and the initial variance is whatever it is set to
			for (int band = 0; band < background.getNumBands(); band += 2) {
				GConvertImage.convert(frame.getBand(band/2), background.getBand(band));
				GImageMiscOps.fill(background.getBand(band + 1), initialVariance);
			}
			return;
		}

		inputWrapper.wrap(frame);

		final int numBands = background.getNumBands()/2;
		final float minusLearn = 1.0f - learnRate;

		storageInput.reset(); //CONCURRENT_REMOVE_LINE

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0, frame.height, 20, storageInput, (inputPixel, idx0, idx1) -> {
		final int idx0 = 0, idx1 = frame.height; final float[] inputPixel = storageInput.grow();
		for (int y = idx0; y < idx1; y++) {
			int indexBG = y*background.width;
			int indexInput = frame.startIndex + y*frame.stride;
			int end = indexInput + frame.width;
			while (indexInput < end) {
				inputWrapper.getF(indexInput, inputPixel);

				for (int band = 0; band < numBands; band++) {
					GrayF32 backgroundMean = background.getBand(band*2);
					GrayF32 backgroundVar = background.getBand(band*2 + 1);

					float inputValue = inputPixel[band];
					float meanBG = backgroundMean.data[indexBG];
					float varianceBG = backgroundVar.data[indexBG];

					float diff = meanBG - inputValue;
					backgroundMean.data[indexBG] = minusLearn*meanBG + learnRate*inputValue;
					backgroundVar.data[indexBG] = minusLearn*varianceBG + learnRate*diff*diff;
				}

				indexInput++;
				indexBG++;
			}
		}
		//CONCURRENT_ABOVE }});
	}

	@Override public void segment( Planar<T> frame, GrayU8 segmented ) {
		segmented.reshape(frame.width, frame.height);
		if (background.width != frame.width || background.height != frame.height) {
			ImageMiscOps.fill(segmented, unknownValue);
			return;
		}
		inputWrapper.wrap(frame);

		final int numBands = background.getNumBands()/2;

		final float adjustedMinimumDifference = minimumDifference*numBands;

		storageInput.reset(); //CONCURRENT_REMOVE_LINE

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0, frame.height, 20, storageInput, (inputPixel, idx0, idx1) -> {
		final int idx0 = 0, idx1 = frame.height; final float[] inputPixel = storageInput.grow();
		for (int y = idx0; y < idx1; y++) {
			int indexBG = y*background.width;
			int indexInput = frame.startIndex + y*frame.stride;
			int indexSegmented = segmented.startIndex + y*segmented.stride;

			final int end = indexInput + frame.width;
			while (indexInput < end) {
				inputWrapper.getF(indexInput, inputPixel);

				float mahalanobis = 0;
				for (int band = 0; band < numBands; band++) {

					GrayF32 backgroundMean = background.getBand(band*2);
					GrayF32 backgroundVar = background.getBand(band*2 + 1);

					float meanBG = backgroundMean.data[indexBG];
					float varBG = backgroundVar.data[indexBG];

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
							GrayF32 backgroundMean = background.getBand(band*2);
							sumAbsDiff += Math.abs(backgroundMean.data[indexBG] - inputPixel[band]);
						}

						segmented.data[indexSegmented] = (byte)(sumAbsDiff >= adjustedMinimumDifference ? 1 : 0);
					}
				}

				indexInput++;
				indexSegmented++;
				indexBG++;
			}
		}
		//CONCURRENT_ABOVE }});
	}
}
