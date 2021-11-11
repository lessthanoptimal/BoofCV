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
import pabeles.concurrency.GrowArray;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * Implementation of {@link BackgroundStationaryBasic} for {@link ImageGray}.
 *
 * @author Peter Abeles
 */
public class BackgroundStationaryBasic_PL<T extends ImageGray<T>> extends BackgroundStationaryBasic<Planar<T>> {
	// storage for background image
	protected Planar<GrayF32> background;

	// wrapper which provides abstraction across image types
	protected GImageMultiBand inputWrapper;

	GrowArray<float[]> storagePixels;

	public BackgroundStationaryBasic_PL( float learnRate, float threshold, ImageType<Planar<T>> imageType ) {
		super(learnRate, threshold, imageType);

		int numBands = imageType.getNumBands();

		background = new Planar<>(GrayF32.class, 0, 0, numBands);

		inputWrapper = FactoryGImageMultiBand.create(imageType);

		storagePixels = new GrowArray<>(() -> new float[numBands]);
	}

	/**
	 * Returns the background image.
	 *
	 * @return background image.
	 */
	public Planar<GrayF32> getBackground() {
		return background;
	}

	@Override public void reset() {
		background.reshape(0, 0);
	}

	@Override public void updateBackground( Planar<T> frame ) {
		if (background.width != frame.width || background.height != frame.height) {
			background.reshape(frame.width, frame.height);
			GConvertImage.convert(frame, background);
			return;
		}

		inputWrapper.wrap(frame);

		final int numBands = background.getNumBands();
		final float minusLearn = 1.0f - learnRate;
		storagePixels.reset(); //CONCURRENT_REMOVE_LINE

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0, frame.height, 20, storagePixels, (inputPixels, idx0, idx1) -> {
		final int idx0 = 0, idx1 = frame.height; final float[] inputPixels = storagePixels.grow();
		for (int y = idx0; y < idx1; y++) {
			int indexBG = y*frame.width;
			int indexInput = frame.startIndex + y*frame.stride;
			final int end = indexInput + frame.width;
			while (indexInput < end) {
				inputWrapper.getF(indexInput, inputPixels);

				for (int band = 0; band < numBands; band++) {
					GrayF32 backgroundBand = background.getBand(band);
					backgroundBand.data[indexBG] = minusLearn*backgroundBand.data[indexBG] + learnRate*inputPixels[band];
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

		final int numBands = background.getNumBands();
		final float thresholdSq = numBands*threshold*threshold;
		storagePixels.reset(); //CONCURRENT_REMOVE_LINE

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0, frame.height, 20, storagePixels, (inputPixels, idx0, idx1) -> {
		final int idx0 = 0, idx1 = frame.height; final float[] inputPixels = storagePixels.grow();
		for (int y = idx0; y < idx1; y++) {
			int indexBG = y*frame.width;
			int indexInput = frame.startIndex + y*frame.stride;
			int indexSegmented = segmented.startIndex + y*segmented.stride;

			final int end = indexInput + frame.width;
			while (indexInput < end) {
				inputWrapper.getF(indexInput, inputPixels);

				double sumErrorSq = 0;
				for (int band = 0; band < numBands; band++) {
					float diff = background.getBand(band).data[indexBG] - inputPixels[band];
					sumErrorSq += diff*diff;
				}

				segmented.data[indexSegmented++] = (byte)(sumErrorSq <= thresholdSq ? 0 : 1);

				indexInput++;
				indexBG++;
			}
		}
		//CONCURRENT_ABOVE }});
	}
}
