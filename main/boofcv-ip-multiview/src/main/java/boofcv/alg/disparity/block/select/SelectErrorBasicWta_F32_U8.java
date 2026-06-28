/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

/// Implementation of [SelectDisparityBasicWta] for scores of type F32.
public class SelectErrorBasicWta_F32_U8 extends SelectDisparityBasicWta<float[], GrayU8> implements Compare_F32 {
	@Override
	public void process( int row, float[] scores ) {

		int indexDisparity = imageDisparity.startIndex + row*imageDisparity.stride;

		// first and last columns it will consider
		int col0 = Math.max(0, disparityMin);
		int col1 = imageWidth + Math.min(0, disparityMax);

		// Mark all pixels as invalid which can't be estimate due to disparityMin
		for (int col = 0; col < col0; col++) {
			imageDisparity.data[indexDisparity++] = (byte)disparityRange;
		}

		for (int col = col0; col < col1; col++) {
			int localDisparityMin = disparityMinAtColumnL2R(col) - disparityMin;
			int localRange = disparityMaxAtColumnL2R(col) - disparityMin + 1;
			int indexScore = col + localDisparityMin*imageWidth;

			int bestDisparity = localDisparityMin;
			float scoreBest = scores[indexScore];
			indexScore += imageWidth;

			for (int disparity = localDisparityMin + 1; disparity < localRange; disparity++, indexScore += imageWidth) {
				float s = scores[indexScore];
				if (s < scoreBest) {
					scoreBest = s;
					bestDisparity = disparity;
				}
			}

			imageDisparity.data[indexDisparity++] = (byte)bestDisparity;
		}

		// mark pixels on the upper end as invalid if it can't reach them
		for (int col = col1; col < imageWidth; col++) {
			imageDisparity.data[indexDisparity++] = (byte)disparityRange;
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
		return Float.compare(-scoreA, -scoreB);
	}
}
