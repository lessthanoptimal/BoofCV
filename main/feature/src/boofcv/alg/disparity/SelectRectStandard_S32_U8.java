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

package boofcv.alg.disparity;

import boofcv.struct.image.ImageUInt8;

/**
 * TODO Comment Up
 *
 * @author Peter Abeles
 */
// TODO multiple peak filter
// TODO flat region filter
public class SelectRectStandard_S32_U8 implements DisparitySelectRect_S32<ImageUInt8>
{
	// output containing disparity
	ImageUInt8 imageDisparity;
	// maximum disparity being checked
	int maxDisparity;
	// radius and width of the region being compared
	int radiusX;
	int regionWidth;

	// maximum allowed error
	int maxError;
	// tolerance for right to left validation. if < 0 then it's disabled
	int rightToLeftTolerance;

	// scores organized for more efficient processing
	int columnScore[] = new int[1];
	int imageWidth;

	// TODO comment that error grows with region size
	public SelectRectStandard_S32_U8(int maxError, int rightToLeftTolerance) {
		this.maxError = maxError <= 0 ? Integer.MAX_VALUE : maxError;
		this.rightToLeftTolerance = rightToLeftTolerance;
	}

	@Override
	public void configure(ImageUInt8 imageDisparity, int maxDisparity , int radiusX ) {
		this.imageDisparity = imageDisparity;
		this.maxDisparity = maxDisparity;
		this.radiusX = radiusX;

		regionWidth = radiusX*2+1;

		if( columnScore.length < maxDisparity )
			columnScore = new int[maxDisparity];
		imageWidth = imageDisparity.width;

	}

	@Override
	public void process(int row, int[] scores) {
		for( int col = 0; col <= imageWidth-regionWidth; col++ ) {
			// make sure the disparity search doesn't go outside the image border
			int localMax = maxDisparityAtColumnL2R(col);

			int indexScore = col;

//			System.out.println("----  "+localMax+"  col = "+col);
			int bestDisparity = 0;
			int scoreBest = columnScore[0] = scores[indexScore];
			indexScore += imageWidth;

//			System.out.printf("%d ",scoreBest);
			for( int i = 1; i < localMax; i++ ,indexScore += imageWidth) {
				int s = scores[indexScore];
				columnScore[i] = s;
//				System.out.printf("%d ",verticalScore[indexScore]);
				if( s < scoreBest ) {
					scoreBest = s;
					bestDisparity = i;
				}
			}

			if( scoreBest > maxError ) {
				// make sure the error isn't too large
				bestDisparity = 0;
			} else if( rightToLeftTolerance >= 0 ) {
				// if the associate is different going the other direction it is probably
				// noise

				int disparityRtoL = selectRightToLeft(col-bestDisparity,scores);

				if( !(Math.abs(disparityRtoL-bestDisparity) <= rightToLeftTolerance) ) {
					bestDisparity = 0;
				}
			}

			imageDisparity.set(col + radiusX, row, bestDisparity);
		}
	}

	/**
	 * Finds the best disparity going from right to left image.
	 *
	 */
	private int selectRightToLeft( int col , int[] scores ) {
		// see how far it can search
		int localMax = Math.min(imageWidth-regionWidth,col+maxDisparity)-col;


		int indexBest = 0;
		int scoreBest = scores[col];
		int indexScore = col + imageWidth + 1;

		for( int i = 1; i < localMax; i++ ,indexScore += imageWidth+1) {
			int s = scores[indexScore];

			if( s < scoreBest ) {
				scoreBest = s;
				indexBest = i;
			}
		}

		return indexBest;
	}

	/**
	 * Returns the maximum allowed disparity for a particular column in left to right direction,
	 * as limited by the image border.
	 */
	private int maxDisparityAtColumnL2R( int col) {
		return 1+col-Math.max(0,col-maxDisparity+1);
	}
}
