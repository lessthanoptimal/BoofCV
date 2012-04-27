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

import boofcv.alg.InputSanityCheck;
import boofcv.struct.image.ImageUInt16;
import boofcv.struct.image.ImageUInt8;

/**
 * <p>
 * Computes the disparity SAD score efficiently while minimizing CPU cache misses.  Provides support
 * for fast right to left validation.  First the sad score is computed horizontally then summed up
 * vertically while minimizing calculations.
 * </p>
 *
 * <p>
 * Memory usage is minimized by only saving disparity scores for the row being considered.  The more
 * straight forward implementation is to compute the disparity score for the whole image at once,
 * which can be quite expensive.
 * </p>
 *
 * <p>
 * Right to left validation is when the match is found going from the right image to the left image 
 * and compared to the result already found going from the left to right image. If the results match
 * then it is more likely to be a good disparity estimate.
 * </p>
 *
 * @author Peter Abeles
 */
public class DisparityEfficientSadValidR2L_U8_U16 {

	// maximum allowed image disparity
	int maxDisparity;

	// number of score elements: (image width - regionWidth)*maxDisparity
	int lengthHorizontal;

	// scores along horizontal axis for current block
	// To allow right to left validation all disparity scores are stored for the entire row
	// size = num columns * maxDisparity
	// disparity for column i is stored in elements i*maxDisparity to (i+1)*maxDisparity
	int horizontalScore[][];
	// summed scores along vertical axis
	// This is simply the sum of like elements in horizontal score
	int verticalScore[];

	// radius of the region along x and y axis
	int radiusX,radiusY;
	// size of the region: radius*2 + 1
	int regionWidth,regionHeight;

	public DisparityEfficientSadValidR2L_U8_U16(int maxDisparity, int regionRadiusX, int regionRadiusY) {
		this.maxDisparity = maxDisparity;
		this.regionWidth = regionRadiusX*2+1;
		this.regionHeight = regionRadiusY*2+1;

		this.radiusX = regionRadiusX;
		this.radiusY = regionRadiusY;
	}

	public void process( ImageUInt8 left , ImageUInt8 right , ImageUInt16 disparity ) {
		// initialize data structures
		InputSanityCheck.checkSameShape(left,right,disparity);

		lengthHorizontal = left.width*maxDisparity;
		if( horizontalScore == null || verticalScore.length < lengthHorizontal ) {
			horizontalScore = new int[regionHeight][lengthHorizontal];
			verticalScore = new int[lengthHorizontal];
		}

		// initialize computation
//		computeFirstRow(left, right,disparity);
		// efficiently compute rest of the rows using previous results to avoid repeat computations
		computeRemainingRows(left, right, disparity);
	}

	private void computeFirstRow(ImageUInt8 left, ImageUInt8 right , ImageUInt16 disparity ) {
		// compute horizontal scores for first row block
		for( int row = 0; row < regionHeight; row++ ) {

			int scores[] = horizontalScore[row];

			computeScoreRow(left, right, row, scores);
		}

		// compute score for the top possible row
		for( int i = 0; i < lengthHorizontal; i++ ) {
			int sum = 0;
			for( int row = 0; row < regionHeight; row++ ) {
				sum += horizontalScore[row][i];
			}
			verticalScore[i] = sum;
		}

		// compute disparity
		selectRightToLeft(0,left.width,disparity);
	}

	private void computeRemainingRows( ImageUInt8 left, ImageUInt8 right , ImageUInt16 disparity )
	{
		for( int row = regionHeight; row < left.height; row++ ) {
			int oldRow = row%regionHeight;

			// subtract first row from vertical score
			int scores[] = horizontalScore[oldRow];
			for( int i = 0; i < lengthHorizontal; i++ ) {
				verticalScore[i] -= scores[i];
			}
			computeScoreRow(left, right, row, scores);

			// add the new score
			for( int i = 0; i < lengthHorizontal; i++ ) {
				verticalScore[i] += scores[i];
			}

			// compute disparity
			selectRightToLeft(row-regionHeight+1,left.width,disparity);
		}
	}

	private void selectRightToLeft( int row , int imageWidth , ImageUInt16 disparity) {
		for( int col = 0; col <= imageWidth-regionWidth; col++ ) {
			// make sure the disparity search doesn't go outside the image border
			int localMax = maxDisparityAtColumnL2R(imageWidth, col);

			int indexScore = col*maxDisparity;

//			System.out.println("----  "+localMax+"  col = "+col);
			int indexBest = 0;
			int scoreBest = verticalScore[indexScore++];
//			System.out.printf("%d ",scoreBest);
			for( int i = 1; i < localMax; i++ ,indexScore++) {
//				System.out.printf("%d ",verticalScore[indexScore]);
				if( verticalScore[indexScore] < scoreBest ) {
					scoreBest = verticalScore[indexScore];
					indexBest = i;
				}
			}
//			System.out.println();

//			if( indexBest == 9 )
//				System.out.println("crap");

			// TODO right to left validation

			disparity.set(col + radiusX, row + radiusY, indexBest);
		}
	}

	/**
	 * Computes disparity score for an entire row
	 *
	 * @param left left image
	 * @param right Right image
	 * @param row Image row being examined
	 * @param scores Storage for disparity scores.
	 */
	protected void computeScoreRow(ImageUInt8 left, ImageUInt8 right, int row, int[] scores) {
		// compute the new row
		for( int col = 0; col <= left.width-regionWidth; col++ ) {
			// make sure the disparity search doesn't go outside the image border
			int localMax = maxDisparityAtColumnL2R(left.width, col);

			int startLeft = left.startIndex + left.stride*row + col;
			int startRight = right.startIndex + right.stride*row + col;
			int indexScores = col*maxDisparity;

			// compute the disparity scores for region in the left camera across
			// all possible disparities
			for( int d = 0; d < localMax; d++ ) {
				int score = 0;
				int indexLeft = startLeft;
				int indexRight = startRight+d;

				int end = indexLeft + regionWidth;
//				for( int rCol = 0; rCol < regionWidth; rCol++ )  {
				while( indexLeft < end )  {
					int diff = (left.data[ indexLeft++ ] & 0xFF) - (right.data[ indexRight++ ] & 0xFF);

					score += diff*diff;
//					score += Math.abs(diff);
				}
				scores[indexScores++] = score;
			}
		}
	}

	/**
	 * Returns the maximum allowed disparity for a particular column in left to right direction,
	 * as limited by the image border.
	 */
	private int maxDisparityAtColumnL2R(int imageWidth, int col) {
		return Math.min(imageWidth-regionWidth+1,col+maxDisparity)-col;
	}

	public int getMaxDisparity() {
		return maxDisparity;
	}

	public int getBorderX() {
		return radiusX;
	}

	public int getBorderY() {
		return radiusY;
	}

}
