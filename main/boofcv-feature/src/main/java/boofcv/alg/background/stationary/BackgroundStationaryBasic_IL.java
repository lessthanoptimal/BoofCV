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

import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.FactoryGImageMultiBand;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.GImageMultiBand;
import boofcv.struct.image.*;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * Implementation of {@link BackgroundStationaryBasic} for {@link ImageGray}.
 *
 * @author Peter Abeles
 */
public class BackgroundStationaryBasic_IL<T extends ImageInterleaved<T>> extends BackgroundStationaryBasic<T> {
	// storage for background image
	protected InterleavedF32 background;

	// wrapper which provides abstraction across image types
	protected GImageMultiBand inputWrapper;

	public BackgroundStationaryBasic_IL( float learnRate, float threshold, ImageType<T> imageType ) {
		super(learnRate, threshold, imageType);

		int numBands = imageType.getNumBands();

		background = new InterleavedF32(0, 0, numBands);

		inputWrapper = FactoryGImageMultiBand.create(imageType);
	}

	/**
	 * Returns the background image.
	 *
	 * @return background image.
	 */
	public InterleavedF32 getBackground() {
		return background;
	}

	@Override public void reset() {
		background.reshape(0, 0);
	}

	@Override public void updateBackground( T frame ) {
		if (background.width != frame.width || background.height != frame.height) {
			background.reshape(frame.width, frame.height);
			GConvertImage.convert(frame, background);
			return;
		}

		inputWrapper.wrap(frame);

		int numBands = background.getNumBands();
		float minusLearn = 1.0f - learnRate;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, frame.height, y -> {
		for (int y = 0; y < frame.height; y++) {
			int indexBG = y*frame.width*numBands;
			int indexInput = frame.startIndex + y*frame.stride;
			int end = indexInput + frame.width*numBands;
			while (indexInput < end) {
				int endIndexBG = indexBG + numBands;
				while (indexBG < endIndexBG) {
					float valueBG = background.data[indexBG];
					background.data[indexBG] = minusLearn*valueBG + learnRate*inputWrapper.getF(indexInput);
					indexBG++;
					indexInput++;
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	@Override public void segment( T frame, GrayU8 segmented ) {
		segmented.reshape(frame.width, frame.height);
		if (background.width != frame.width || background.height != frame.height) {
			ImageMiscOps.fill(segmented, unknownValue);
			return;
		}
		inputWrapper.wrap(frame);

		int numBands = background.getNumBands();
		float thresholdSq = numBands*threshold*threshold;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, frame.height, y -> {
		for (int y = 0; y < frame.height; y++) {
			int indexBG = y*frame.width*numBands;
			int indexInput = frame.startIndex + y*frame.stride;
			int indexSegmented = segmented.startIndex + y*segmented.stride;

			int end = indexInput + frame.width*numBands;
			while (indexInput < end) {
				float sumErrorSq = 0;
				int endIndexBG = indexBG + numBands;
				while (indexBG < endIndexBG) {
					float diff = background.data[indexBG++] - inputWrapper.getF(indexInput++);
					sumErrorSq += diff*diff;
				}

				segmented.data[indexSegmented++] = (byte)(sumErrorSq <= thresholdSq ? 0 : 1);
			}
		}
		//CONCURRENT_ABOVE });
	}
}
