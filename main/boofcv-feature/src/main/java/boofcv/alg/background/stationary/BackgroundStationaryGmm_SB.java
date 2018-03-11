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
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

import javax.annotation.Nullable;

/**
 * Implementation of {@link BackgroundAlgorithmGmm} for {@link ImageGray}.
 *
 * @author Peter Abeles
 */
public class BackgroundStationaryGmm_SB<T extends ImageGray<T>>
		extends BackgroundStationaryGmm<T>
{
	/**
	 *
	 * @param learningPeriod Specifies how fast it will adjust to changes in the image. Must be greater than zero.
	 * @param decayCoef Determines how quickly a Gaussian is forgotten
	 * @param maxGaussians Maximum number of Gaussians in a mixture for a pixel
	 * @param imageType Type of image it's processing.
	 */
	public BackgroundStationaryGmm_SB(float learningPeriod, float decayCoef,
									  int maxGaussians, ImageType<T> imageType )
	{
		super(learningPeriod, decayCoef, maxGaussians, imageType);
	}

	@Override
	public void updateBackground( T frame , @Nullable GrayU8 mask ) {
		super.updateBackground(frame, mask);

		common.inputWrapperG.wrap(frame);
		for (int row = 0; row < common.imageHeight; row++) {
			int inputIndex = frame.startIndex + row*frame.stride;
			float[] dataRow = common.model.data[row];

			if( mask == null ) {
				for (int col = 0; col < common.imageWidth; col++) {
					float pixelValue = common.inputWrapperG.getF(inputIndex++);
					int modelIndex = col * common.modelStride;

					common.updateMixture(pixelValue, dataRow, modelIndex);
				}
			} else {
				int indexMask = mask.startIndex + row*mask.stride;
				for (int col = 0; col < common.imageWidth; col++) {
					float pixelValue = common.inputWrapperG.getF(inputIndex++);
					int modelIndex = col * common.modelStride;

					mask.data[indexMask++] = (byte)common.updateMixture(pixelValue, dataRow, modelIndex);
				}
			}
		}
	}

	@Override
	public void segment(T frame, GrayU8 segmented) {
		if( common.imageWidth != frame.width || common.imageHeight != frame.height ) {
			segmented.reshape(frame.width,frame.height);
			ImageMiscOps.fill(segmented,unknownValue);
			return;
		}

		common.unknownValue = unknownValue;
		common.inputWrapperG.wrap(frame);
		for (int row = 0; row < common.imageHeight; row++) {
			int indexIn = frame.startIndex + row*frame.stride;
			int indexOut = segmented.startIndex + row*segmented.stride;
			float[] dataRow = common.model.data[row];

			for (int col = 0; col < common.imageWidth; col++) {
				float pixelValue = common.inputWrapperG.getF(indexIn++);
				int modelIndex = col * common.modelStride;

				segmented.data[indexOut++] = (byte)common.checkBackground(pixelValue, dataRow, modelIndex);
			}
		}
	}
}
