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

import boofcv.alg.background.BackgroundAlgorithmGmm;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageMultiBand;
import boofcv.struct.image.ImageType;
import org.jetbrains.annotations.Nullable;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * Implementation of {@link BackgroundAlgorithmGmm} for {@link ImageMultiBand}.
 *
 * @author Peter Abeles
 */
public class BackgroundStationaryGmm_MB<T extends ImageMultiBand<T>> extends BackgroundStationaryGmm<T> {
	/**
	 * @param learningPeriod Specifies how fast it will adjust to changes in the image. Must be greater than zero.
	 * @param decayCoef Determines how quickly a Gaussian is forgotten
	 * @param maxGaussians Maximum number of Gaussians in a mixture for a pixel
	 * @param imageType Type of image it's processing.
	 */
	public BackgroundStationaryGmm_MB( float learningPeriod, float decayCoef,
									   int maxGaussians, ImageType<T> imageType ) {
		super(learningPeriod, decayCoef, maxGaussians, imageType);
	}

	/**
	 * @param mask If null then the background mask is ignored
	 */
	@Override public void updateBackground( T frame, @Nullable GrayU8 mask ) {
		super.updateBackground(frame, mask);

		common.inputWrapperMB.wrap(frame);
		final int pixelStride = common.inputWrapperMB.getPixelStride();

		common.storagePixels.reset(); //CONCURRENT_REMOVE_LINE

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0, frame.height, 20, common.storagePixels, (inputPixel, idx0, idx1) -> {
		final int idx0 = 0, idx1 = frame.height; final float[] inputPixel = common.storagePixels.grow();
		for (int y = idx0; y < idx1; y++) {
			int inputIndex = frame.getIndex(0, y);
			float[] dataRow = common.model.data[y];

			if (mask == null) {
				for (int col = 0; col < common.imageWidth; col++, inputIndex += pixelStride) {
					common.inputWrapperMB.getF(inputIndex, inputPixel);
					int modelIndex = col*common.modelStride;

					common.updateMixture(inputPixel, dataRow, modelIndex);
				}
			} else {
				int indexMask = mask.startIndex + y*mask.stride;
				for (int col = 0; col < common.imageWidth; col++, inputIndex += pixelStride) {
					common.inputWrapperMB.getF(inputIndex, inputPixel);
					int modelIndex = col*common.modelStride;

					mask.data[indexMask++] = (byte)common.updateMixture(inputPixel, dataRow, modelIndex);
				}
			}
		}
		//CONCURRENT_ABOVE }});
	}

	@Override public void segment( T frame, GrayU8 segmented ) {
		segmented.reshape(frame.width, frame.height);
		if (common.imageWidth != frame.width || common.imageHeight != frame.height) {
			ImageMiscOps.fill(segmented, unknownValue);
			return;
		}

		common.unknownValue = unknownValue;
		common.inputWrapperMB.wrap(frame);
		final int pixelStride = common.inputWrapperMB.getPixelStride();

		common.storagePixels.reset(); //CONCURRENT_REMOVE_LINE

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0, frame.height, 20, common.storagePixels, (inputPixel, idx0, idx1) -> {
		final int idx0 = 0, idx1 = frame.height; final float[] inputPixel = common.storagePixels.grow();
		for (int y = idx0; y < idx1; y++) {
			int indexIn = frame.getIndex(0, y);
			int indexOut = segmented.startIndex + y*segmented.stride;
			float[] dataRow = common.model.data[y];

			for (int col = 0; col < common.imageWidth; col++, indexIn += pixelStride) {
				common.inputWrapperMB.getF(indexIn, inputPixel);
				int modelIndex = col*common.modelStride;

				segmented.data[indexOut++] = (byte)common.checkBackground(inputPixel, dataRow, modelIndex);
			}
		}
		//CONCURRENT_ABOVE }});
	}
}
