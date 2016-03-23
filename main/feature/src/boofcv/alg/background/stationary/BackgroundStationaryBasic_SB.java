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
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.GImageGray;
import boofcv.struct.image.*;

/**
 * Implementation of {@link BackgroundStationaryBasic} for {@link Planar}.
 *
 * @author Peter Abeles
 */
public class BackgroundStationaryBasic_SB<T extends ImageGray>
	extends BackgroundStationaryBasic<T>
{
	// storage for background image
	protected GrayF32 background = new GrayF32(1,1);

	// wrapper which provides abstraction across image types
	protected GImageGray inputWrapper;

	public BackgroundStationaryBasic_SB(float learnRate, float threshold,
										Class<T> imageType) {
		super(learnRate, threshold, ImageType.single(imageType));

		inputWrapper = FactoryGImageGray.create(imageType);
	}

	/**
	 * Returns the background image.
	 *
	 * @return background image.
	 */
	public GrayF32 getBackground() {
		return background;
	}

	@Override
	public void reset() {
		background.reshape(1,1);
	}

	@Override
	public void updateBackground( T frame) {
		if( background.width == 1 ) {
			background.reshape(frame.width, frame.height);
			GConvertImage.convert(frame, background);
			return;
		} else {
			InputSanityCheck.checkSameShape(background,frame);
		}

		inputWrapper.wrap(frame);

		float minusLearn = 1.0f - learnRate;

		int indexBG = 0;
		for (int y = 0; y < frame.height; y++) {
			int indexInput = frame.startIndex + y*frame.stride;
			int end = indexInput + frame.width;
			while( indexInput < end ) {
				float value = inputWrapper.getF(indexInput++);
				float bg = background.data[indexBG];

				background.data[indexBG++] = minusLearn*bg + learnRate*value;
			}
		}
	}

	@Override
	public void segment(T frame, GrayU8 segmented) {
		if( background.width == 1 ) {
			ImageMiscOps.fill(segmented,unknownValue);
			return;
		}
		InputSanityCheck.checkSameShape(background,frame,segmented);
		inputWrapper.wrap(frame);

		float thresholdSq = threshold*threshold;

		int indexBG = 0;
		for (int y = 0; y < frame.height; y++) {
			int indexInput = frame.startIndex + y*frame.stride;
			int indexSegmented = segmented.startIndex + y*segmented.stride;

			int end = indexInput + frame.width;
			while( indexInput < end ) {
				float bg = background.data[indexBG];
				float pixelFrame = inputWrapper.getF(indexInput);

				float diff = bg - pixelFrame;
				if (diff * diff <= thresholdSq) {
					segmented.data[indexSegmented] = 0;
				} else {
					segmented.data[indexSegmented] = 1;
				}

				indexInput++;
				indexSegmented++;
				indexBG++;
			}
		}
	}


}
