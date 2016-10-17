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

package boofcv.alg.background.stationary;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.FactoryGImageMultiBand;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.GImageMultiBand;
import boofcv.struct.image.*;

/**
 * Implementation of {@link BackgroundStationaryGaussian} for {@link Planar}.
 *
 * @author Peter Abeles
 */
public class BackgroundStationaryGaussian_PL<T extends ImageGray>
		extends BackgroundStationaryGaussian<Planar<T>>
{

	// wrappers which provide abstraction across image types
	protected GImageMultiBand inputWrapper;
	protected GImageMultiBand bgWrapper;

	// storage for multi-band pixel values
	protected float[] inputPixel;

	// background is composed of bands*2 channels.  even = mean, odd = variance
	Planar<GrayF32> background;

	/**
	 * Configurations background removal.
	 *
	 * @param learnRate Specifies how quickly the background is updated.  0 = static  1.0 = instant.  Try 0.05
	 * @param threshold Threshold for background.  Consult a chi-square table for reasonably values.
	 *                  10 to 16 for 1 to 3 bands.
	 * @param imageType Type of input image.
	 */
	public BackgroundStationaryGaussian_PL(float learnRate, float threshold,
										   ImageType<Planar<T>> imageType)
	{
		super(learnRate, threshold, imageType);

		int numBands = imageType.getNumBands();

		background = new Planar<>(GrayF32.class,1,1,2*numBands);
		bgWrapper = FactoryGImageMultiBand.create(background.getImageType());
		bgWrapper.wrap(background);

		inputWrapper = FactoryGImageMultiBand.create(imageType);

		inputPixel = new float[numBands];
	}

	@Override
	public void reset() {
		background.reshape(1,1);
	}

	@Override
	public void updateBackground( Planar<T> frame) {
		if( background.width == 1 ) {
			background.reshape(frame.width, frame.height);
			// initialize the mean to the current image and the initial variance is whatever it is set to
			for (int band = 0; band < background.getNumBands(); band += 2) {
				GConvertImage.convert(frame.getBand(band / 2), background.getBand(band));
				GImageMiscOps.fill(background.getBand(band + 1), initialVariance);
			}
			return;
		} else {
			InputSanityCheck.checkSameShape(background, frame);
		}

		inputWrapper.wrap(frame);

		int numBands = background.getNumBands()/2;
		float minusLearn = 1.0f - learnRate;


		int indexBG = 0;
		for (int y = 0; y < background.height; y++) {
			int indexInput = frame.startIndex + y*frame.stride;
			int end = indexInput + frame.width;
			while( indexInput < end ) {
				inputWrapper.getF(indexInput, inputPixel);

				for (int band = 0; band < numBands; band++) {
					GrayF32 backgroundMean = background.getBand(band*2);
					GrayF32 backgroundVar = background.getBand(band*2+1);

					float inputValue = inputPixel[band];
					float meanBG = backgroundMean.data[indexBG];
					float varianceBG = backgroundVar.data[indexBG];

					float diff = meanBG-inputValue;
					backgroundMean.data[indexBG] = minusLearn*meanBG + learnRate*inputValue;
					backgroundVar.data[indexBG] = minusLearn*varianceBG + learnRate*diff*diff;
				}

				indexInput++;
				indexBG++;
			}
		}
	}

	@Override
	public void segment(Planar<T> frame, GrayU8 segmented) {
		if( background.width == 1 ) {
			ImageMiscOps.fill(segmented, unknownValue);
			return;
		}
		inputWrapper.wrap(frame);

		final int numBands = background.getNumBands()/2;

		float adjustedMinimumDifference = minimumDifference*numBands;

		int indexBG = 0;
		for (int y = 0; y < frame.height; y++) {
			int indexInput = frame.startIndex + y*frame.stride;
			int indexSegmented = segmented.startIndex + y*segmented.stride;

			int end = indexInput + frame.width;
			while( indexInput < end ) {
				inputWrapper.getF(indexInput, inputPixel);

				float mahalanobis = 0;
				for (int band = 0; band < numBands; band++) {

					GrayF32 backgroundMean = background.getBand(band*2);
					GrayF32 backgroundVar = background.getBand(band*2+1);

					float meanBG = backgroundMean.data[indexBG];
					float varBG = backgroundVar.data[indexBG];

					float diff = meanBG - inputPixel[band];
					mahalanobis += diff * diff / varBG;
				}

				if (mahalanobis <= threshold) {
					segmented.data[indexSegmented] = 0;
				} else {
					if( minimumDifference == 0) {
						segmented.data[indexSegmented] = 1;
					} else {
						float sumAbsDiff = 0;
						for (int band = 0; band < numBands; band++) {
							GrayF32 backgroundMean = background.getBand(band*2);
							sumAbsDiff += Math.abs(backgroundMean.data[indexBG] - inputPixel[band]);
						}
						if (sumAbsDiff >= adjustedMinimumDifference)
							segmented.data[indexSegmented] = 1;
						else
							segmented.data[indexSegmented] = 0;
					}
				}

				indexInput++;
				indexSegmented++;
				indexBG++;
			}
		}
	}
}
