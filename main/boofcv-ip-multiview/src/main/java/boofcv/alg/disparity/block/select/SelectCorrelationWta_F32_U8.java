/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.disparity.block.select;

import boofcv.alg.disparity.block.DisparitySelect;
import boofcv.alg.disparity.block.SelectDisparityBasicWta;
import boofcv.misc.Compare_F32;
import boofcv.struct.image.GrayU8;

/**
 * <p>
 * Implementation of {@link SelectDisparityBasicWta} for scores of type F32 and correlation. Since it's correlation
 * it pixels the score with the highest value.
 * </p>
 *
 * @author Peter Abeles
 */
public class SelectCorrelationWta_F32_U8 extends SelectDisparityBasicWta<float[], GrayU8>
		implements Compare_F32 {
	@Override
	public void configure( GrayU8 imageDisparity, int disparityMin, int disparityMax, int radiusX ) {
		super.configure(imageDisparity, disparityMin, disparityMax, radiusX);
	}

	@Override
	public void process( int row, float[] blockOfScores ) {
		int indexDisparity = imageDisparity.startIndex + row*imageDisparity.stride;

		// Mark all pixels as invalid which can't be estimate due to disparityMin
		for (int col = 0; col < disparityMin; col++) {
			imageDisparity.data[indexDisparity++] = (byte)disparityRange;
		}

		// Select the best disparity from all the rest
		for (int col = disparityMin; col < imageWidth; col++) {
			// make sure the disparity search doesn't go outside the image border
			int localMaxRange = disparityMaxAtColumnL2R(col) - disparityMin + 1;

			// Find the disparity with the best score, which is the largest score for correlation
			int indexScore = col - disparityMin;
			int maxIndex = 0;
			float maxValue = blockOfScores[indexScore];
			indexScore += imageWidth;
			for (int i = 1; i < localMaxRange; i++, indexScore += imageWidth) {
				float v = blockOfScores[indexScore];
				if (v > maxValue) {
					maxValue = v;
					maxIndex = i;
				}
			}

			imageDisparity.data[indexDisparity++] = (byte)maxIndex;
		}
	}

	@Override
	public DisparitySelect<float[], GrayU8> concurrentCopy() {
		return this;
	}

	@Override
	public Class<GrayU8> getDisparityType() {
		return GrayU8.class;
	}

	@Override
	public int compare( float scoreA, float scoreB ) {
		return Float.compare(scoreA, scoreB);
	}
}
