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
import boofcv.alg.background.moving.BackgroundMovingGaussian;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.GImageGray;
import boofcv.struct.image.*;

/**
 * Implementation of {@link BackgroundMovingGaussian} for {@link ImageGray}.
 *
 * @author Peter Abeles
 */
public class BackgroundStationaryGaussian_SB<T extends ImageGray>
		extends BackgroundStationaryGaussian<T>
{
	// wrappers which provide abstraction across image types
	protected GImageGray inputWrapper;

	// background is composed of two channels.  0 = mean, 1 = variance
	Planar<GrayF32> background = new Planar<>(GrayF32.class,1,1,2);

	/**
	 * Configurations background removal.
	 *
	 * @param learnRate Specifies how quickly the background is updated.  0 = static  1.0 = instant.  Try 0.05
	 * @param threshold Threshold for background.  Try 10.
	 * @param imageType Type of input image.
	 */
	public BackgroundStationaryGaussian_SB(float learnRate, float threshold, Class<T> imageType)
	{
		super(learnRate, threshold, ImageType.single(imageType));

		inputWrapper = FactoryGImageGray.create(imageType);
	}

	@Override
	public void reset() {
		background.reshape(1,1);
	}

	@Override
	public void updateBackground( T frame) {
		if( background.width == 1 ) {
			background.reshape(frame.width, frame.height);
			GConvertImage.convert(frame, background.getBand(0));
			GImageMiscOps.fill(background.getBand(1),initialVariance);
			return;
		} else {
			InputSanityCheck.checkSameShape(background, frame);
		}

		inputWrapper.wrap(frame);

		float minusLearn = 1.0f - learnRate;

		GrayF32 backgroundMean = background.getBand(0);
		GrayF32 backgroundVar = background.getBand(1);

		int indexBG = 0;
		for (int y = 0; y < background.height; y++) {
			int indexInput = frame.startIndex + y*frame.stride;

			int end = indexInput + frame.width;
			while( indexInput < end ) {
				float inputValue = inputWrapper.getF(indexInput);
				float meanBG = backgroundMean.data[indexBG];
				float varianceBG = backgroundVar.data[indexBG];

				float diff = meanBG-inputValue;
				backgroundMean.data[indexBG] = minusLearn*meanBG + learnRate*inputValue;
				backgroundVar.data[indexBG] = minusLearn*varianceBG + learnRate*diff*diff;

				indexBG++;
				indexInput++;
			}
		}
	}

	@Override
	public void segment( T frame, GrayU8 segmented) {
		if( background.width == 1 ) {
			ImageMiscOps.fill(segmented, unknownValue);
			return;
		}
		InputSanityCheck.checkSameShape(background,frame,segmented);
		inputWrapper.wrap(frame);

		GrayF32 backgroundMean = background.getBand(0);
		GrayF32 backgroundVar = background.getBand(1);

		int indexBG = 0;
		for (int y = 0; y < frame.height; y++) {
			int indexInput = frame.startIndex + y*frame.stride;
			int indexSegmented = segmented.startIndex + y*segmented.stride;

			int end = indexInput + frame.width;
			while( indexInput < end ) {
				float pixelFrame = inputWrapper.getF(indexInput);

				float meanBG = backgroundMean.data[indexBG];
				float varBG = backgroundVar.data[indexBG];

				float diff = meanBG - pixelFrame;
				float chisq = diff*diff/varBG;

				if (chisq <= threshold) {
					segmented.data[indexSegmented] = 0;
				} else {
					if( diff >= minimumDifference || -diff >= minimumDifference )
						segmented.data[indexSegmented] = 1;
					else
						segmented.data[indexSegmented] = 0;
				}

				indexInput++;
				indexSegmented++;
				indexBG++;
			}
		}
	}
}
