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

import boofcv.struct.image.ImageSingleBand;

/**
 * <p>
 * Selects the best disparity using a winner takes all strategy.  Then optionally can employ several different
 * techniques to filter out bad disparity values.  This is a base class which allows the output image type
 * to be specified by a child.
 * </p>
 *
 * <p>
 * Filters:<br>
 * <b>MaxError</b> is the largest error value the selected region can have.<br>
 * <b>right To Left</b> validates the disparity by seeing if the matched region on the right has the same region on
 * the left as its optimal solution, within tolerance.<br>
 * <b>texture</b> Tolerance for how similar the best region is to the second best. Lower values indicate greater
 * tolerance.  textureTol < (C2-C1)/C1, where C2 = second best region score and C1 = best region score
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class SelectRectStandardBase_S32<T extends ImageSingleBand> implements DisparitySelect_S32<T>
{
	// output containing disparity
	T imageDisparity;
	// maximum disparity being checked
	int maxDisparity;
	// max allowed disparity at the current pixel
	int localMax;
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

	// texture threshold, use an integer value for speed.
	int textureThreshold;
	int discretizer = 10000;

	/**
	 * Configures tolerances
	 *
	 * @param maxError The maximum allowed error.  Note this is sum error and not per pixel error.
	 *                    Try (region width*height)*30.
	 * @param rightToLeftTolerance Tolerance for how difference the left to right associated values can be.  Try 6
	 * @param texture Tolerance for how similar optimal region is to other region.  Closer to zero is more tolerant.
	 *                Try 0.1
	 */
	public SelectRectStandardBase_S32(int maxError, int rightToLeftTolerance, double texture) {
		this.maxError = maxError <= 0 ? Integer.MAX_VALUE : maxError;
		this.rightToLeftTolerance = rightToLeftTolerance;
		this.textureThreshold = (int)(discretizer *texture);
	}

	@Override
	public void configure(T imageDisparity, int maxDisparity , int radiusX ) {
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

		int indexDisparity = imageDisparity.startIndex + row*imageDisparity.stride + radiusX;

		for( int col = 0; col <= imageWidth-regionWidth; col++ ) {
			// make sure the disparity search doesn't go outside the image border
			localMax = maxDisparityAtColumnL2R(col);

			// index of the element being examined in the score array
			int indexScore = col;

			// select the best disparity
			int bestDisparity = 0;
			int scoreBest = columnScore[0] = scores[indexScore];
			indexScore += imageWidth;

			for( int i = 1; i < localMax; i++ ,indexScore += imageWidth) {
				int s = scores[indexScore];
				columnScore[i] = s;
				if( s < scoreBest ) {
					scoreBest = s;
					bestDisparity = i;
				}
			}

			// detect bad matches
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
			// test to see if the region lacks sufficient texture
			if( bestDisparity != 0 && textureThreshold > 0 ) {
				// find the second best disparity value and exclude its neighbors
				int secondBest = Integer.MAX_VALUE;
				for( int i = 0; i < bestDisparity-1; i++ ) {
					if( columnScore[i] < secondBest ) {
						secondBest = columnScore[i];
					}
				}
				for( int i = bestDisparity+2; i < localMax-1; i++ ) {
					if( columnScore[i] < secondBest ) {
						secondBest = columnScore[i];
					}
				}

				// similar scores indicate lack of texture
				// C = (C2-C1)/C1
				if( discretizer *(secondBest-scoreBest) <= textureThreshold *scoreBest )
					bestDisparity = 0;
			}

			setDisparity(indexDisparity++ , bestDisparity );
		}
	}

	/**
	 * Sets the output to the specified disparity value.
	 *
	 * @param index Image pixel that is being set
	 * @param disparityValue disparity value
	 */
	protected abstract void setDisparity( int index , int disparityValue );

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
