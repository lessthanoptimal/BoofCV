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
import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.GImageGray;
import boofcv.struct.image.*;
import lombok.Getter;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * Implementation of {@link BackgroundStationaryBasic} for {@link Planar}.
 *
 * @author Peter Abeles
 */
public class BackgroundStationaryBasic_SB<T extends ImageGray<T>> extends BackgroundStationaryBasic<T> {
	/** Background model image */
	@Getter protected GrayF32 background = new GrayF32(0, 0);

	// wrapper which provides abstraction across image types
	protected GImageGray inputWrapper;

	public BackgroundStationaryBasic_SB( float learnRate, float threshold,
										 Class<T> imageType ) {
		super(learnRate, threshold, ImageType.single(imageType));

		inputWrapper = FactoryGImageGray.create(imageType);
	}

	@Override public void reset() {
		background.reshape(0, 0);
	}

	@Override public void updateBackground( final T frame ) {
		if (background.width != frame.width) {
			background.reshape(frame.width, frame.height);
			GConvertImage.convert(frame, background);
			return;
		}

		InputSanityCheck.checkSameShape(background, frame);
		inputWrapper.wrap(frame);

		final float minusLearn = 1.0f - learnRate;
		final float[] backgroundData = background.data;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, frame.height, y -> {
		for (int y = 0; y < frame.height; y++) {
			int indexBG = y*frame.width;
			int indexInput = frame.startIndex + y*frame.stride;
			final int end = indexInput + frame.width;
			while (indexInput < end) {
				final float value = inputWrapper.getF(indexInput++);
				final float bg = backgroundData[indexBG];

				backgroundData[indexBG++] = minusLearn*bg + learnRate*value;
			}
		}
		//CONCURRENT_ABOVE });
	}

	@Override public void segment( final T frame, final GrayU8 segmented ) {
		segmented.reshape(frame.width, frame.height);
		if (background.width != frame.width || background.height != frame.height) {
			ImageMiscOps.fill(segmented, unknownValue);
			return;
		}
		inputWrapper.wrap(frame);

		final float thresholdSq = threshold*threshold;
		final byte[] segmentedData = segmented.data;
		final float[] backgroundData = background.data;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, frame.height, y -> {
		for (int y = 0; y < frame.height; y++) {
			int indexBG = y*frame.width;
			int indexInput = frame.startIndex + y*frame.stride;
			int indexSegmented = segmented.startIndex + y*segmented.stride;

			final int end = indexInput + frame.width;
			while (indexInput < end) {
				final float bg = backgroundData[indexBG++];
				final float pixelFrame = inputWrapper.getF(indexInput++);

				final float diff = bg - pixelFrame;
				segmentedData[indexSegmented++] = (byte)(diff*diff <= thresholdSq ? 0 : 1);
			}
		}
		//CONCURRENT_ABOVE });
	}
}
