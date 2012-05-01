/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.disparity;

import boofcv.struct.image.ImageUInt8;

/**
 * The simplest winner take all (WTA) disparity algorithm which performs no additional filtering.
 * Primarily for testing and benchmarking purposes.
 *
 * @author Peter Abeles
 */
public class SelectRectBasicWta_S32_U8 implements DisparitySelect_S32<ImageUInt8>
{
	ImageUInt8 imageDisparity;
	int maxDisparity;
	int radiusX;
	int regionWidth;

	int imageWidth;

	@Override
	public void configure(ImageUInt8 imageDisparity, int maxDisparity , int radiusX ) {
		this.imageDisparity = imageDisparity;
		this.maxDisparity = maxDisparity;
		this.radiusX = radiusX;

		regionWidth = radiusX*2+1;
		imageWidth = imageDisparity.width;

	}

	@Override
	public void process(int row, int[] scores) {
		for( int col = 0; col <= imageWidth-regionWidth; col++ ) {
			// make sure the disparity search doesn't go outside the image border
			int localMax = maxDisparityAtColumnL2R(col);

			int indexScore = col;

			int bestDisparity = 0;
			int scoreBest = scores[indexScore];
			indexScore += imageWidth;

//			System.out.printf("%d ",scoreBest);
			for( int i = 1; i < localMax; i++ ,indexScore += imageWidth) {
				int s = scores[indexScore];
				if( s < scoreBest ) {
					scoreBest = s;
					bestDisparity = i;
				}
			}

			imageDisparity.set(col + radiusX, row, bestDisparity);
		}
	}

	/**
	 * Returns the maximum allowed disparity for a particular column in left to right direction,
	 * as limited by the image border.
	 */
	private int maxDisparityAtColumnL2R( int col) {
		return 1+col-Math.max(0,col-maxDisparity+1);
	}
}
