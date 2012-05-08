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

import boofcv.alg.InputSanityCheck;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;

/**
 * <p>
 * Scores the disparity for a point using multiple rectangular regions in an effort to reduce errors at object borders,
 * based off te 5 region algorithm described in [1].  Five overlapping regions are considered and the error at each
 * disparity is the center region plus the two regions with the smallest error.  The idea is that only errors for
 * common elements in each image are considered.
 * </p>
 *
 * <p>
 * [1] Heiko Hirschmuller, Peter R. Innocent, and Jon Garibaldi. "Real-Time Correlation-Based Stereo Vision with
 * Reduced Border Errors." Int. J. Comput. Vision 47, 1-3 2002
 * </p>
 *
 * @author Peter Abeles
 */
public class DisparityScoreWindowFive <Disparity extends ImageSingleBand>
		extends DisparityScoreSadRect<ImageUInt8,Disparity> {


	// Computes disparity from scores
	DisparitySelect<int[],Disparity> computeDisparity;

	// stores the local scores for the width of the region
	int elementScore[];
	// scores along horizontal axis for current block
	// To allow right to left validation all disparity scores are stored for the entire row
	// size = num columns * maxDisparity
	// disparity for column i is stored in elements i*maxDisparity to (i+1)*maxDisparity
	int horizontalScore[][];
	// summed scores along vertical axis
	// This is simply the sum of like elements in horizontal score
	int verticalScore[][];
	// which set of scores is currently active
	int activeScore;

	/**
	 * Configures disparity calculation.
	 *
	 * @param minDisparity Minimum disparity that it will check. Must be >= 0 and < maxDisparity
	 * @param maxDisparity Maximum disparity that it will calculate. Must be > 0
	 * @param regionRadiusX Radius of the rectangular region along x-axis.
	 * @param regionRadiusY Radius of the rectangular region along y-axis.
	 */
	public DisparityScoreWindowFive(int minDisparity, int maxDisparity,
									int regionRadiusX, int regionRadiusY ,
									DisparitySelect<int[],Disparity> computeDisparity) {
		super(minDisparity,maxDisparity,regionRadiusX,regionRadiusY);
		this.computeDisparity = computeDisparity;
	}

	/**
	 * Computes disparity between two stereo images
	 *
	 * @param left Left rectified stereo image. Input
	 * @param right Right rectified stereo image. Input
	 * @param disparity Disparity between the two images. Output
	 */
	@Override
	public void process( ImageUInt8 left , ImageUInt8 right , Disparity disparity ) {
		// initialize data structures
		InputSanityCheck.checkSameShape(left, right, disparity);

		lengthHorizontal = left.width*rangeDisparity;
		if( horizontalScore == null || verticalScore.length < lengthHorizontal ) {
			horizontalScore = new int[regionHeight][lengthHorizontal];
			verticalScore = new int[regionHeight][lengthHorizontal];
			elementScore = new int[ left.width ];
		}

		computeDisparity.configure(disparity,minDisparity,maxDisparity,radiusX*2);

		// initialize computation
		computeFirstRow(left, right);
		// efficiently compute rest of the rows using previous results to avoid repeat computations
		computeRemainingRows(left, right);
	}

	/**
	 * Initializes disparity calculation by finding the scores for the initial block of horizontal
	 * rows.
	 */
	private void computeFirstRow(ImageUInt8 left, ImageUInt8 right ) {
		int firstRow[] = verticalScore[0];
		activeScore = 1;

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
			firstRow[i] = sum;
		}
	}

	/**
	 * Using previously computed results it efficiently finds the disparity in the remaining rows.
	 * When a new block is processes the last row/column is subtracted and the new row/column is
	 * added.
	 */
	private void computeRemainingRows( ImageUInt8 left, ImageUInt8 right )
	{
		for( int row = regionHeight; row < left.height; row++ , activeScore++) {
			int oldRow = row%regionHeight;
			int previous[] = verticalScore[ (activeScore-1) % regionHeight ];
			int active[] = verticalScore[ activeScore % regionHeight ];

			// subtract first row from vertical score
			int scores[] = horizontalScore[oldRow];
			for( int i = 0; i < lengthHorizontal; i++ ) {
				active[i] = previous[i] - scores[i];
			}

			computeScoreRow(left, right, row, scores);

			// add the new score
			for( int i = 0; i < lengthHorizontal; i++ ) {
				active[i] += scores[i];
			}

			if( activeScore >= regionHeight-1 ) {
				int top[] = verticalScore[ (activeScore-2*radiusY) % regionHeight ];
				int middle[] = verticalScore[ (activeScore-radiusY) % regionHeight ];
				int bottom[] = verticalScore[ activeScore % regionHeight ];

				computeScoreFive(top,middle,bottom,scores,left.width);
				computeDisparity.process(row - (1 + 4*radiusY) + 2*radiusY+1, scores );
			}
		}
	}

	/**
	 * Computes disparity score for an entire row.
	 *
	 * For a given disparity, the score for each region on the left share many components in common.
	 * Because of this the scores are computed with disparity being the outer most loop
	 *
	 * @param left left image
	 * @param right Right image
	 * @param row Image row being examined
	 * @param scores Storage for disparity scores.
	 */
	protected void computeScoreRow(ImageUInt8 left, ImageUInt8 right, int row, int[] scores) {

		// disparity as the outer loop to maximize common elements in inner loops, reducing redundant calculations
		for( int d = minDisparity; d < maxDisparity; d++ ) {
			int dispFromMin = d - minDisparity;

			// number of individual columns the error is computed in
			final int colMax = left.width-d;
			// number of regions that a score/error is computed in
			final int scoreMax = colMax-regionWidth;

			// indexes that data is read to/from for different data structures
			int indexScore = left.width*dispFromMin + dispFromMin;
			int indexLeft = left.startIndex + left.stride*row + d;
			int indexRight = right.startIndex + right.stride*row;

			// Fill elementScore with scores for individual elements for this row at disparity d
			computeScoreRow(left, right, colMax, indexLeft, indexRight);

			// score at the first column
			int score = 0;
			for( int i = 0; i < regionWidth; i++ )
				score += elementScore[i];

			scores[indexScore++] = score;

			// scores for the remaining columns
			for( int col = 0; col < scoreMax; col++ , indexScore++ ) {
				scores[indexScore] = score += elementScore[col+regionWidth] - elementScore[col];
			}
		}
	}

	protected void computeScoreFive( int top[] , int middle[] , int bottom[] , int score[] , int width ) {
		// disparity as the outer loop to maximize common elements in inner loops, reducing redundant calculations
		for( int d = minDisparity; d < maxDisparity; d++ ) {


			int index = (d-minDisparity)*width + d + 2*radiusX;
			for( int col = d + 2*radiusX; col < width - 2*radiusX; col++ ) {
				int s = 0;

				int val0 = top[index-radiusX];
				int val1 = top[index+radiusX];
				int val2 = bottom[index-radiusX];
				int val3 = bottom[index+radiusX];

				if( val1 < val0 ) {
					int temp = val0;
					val0 = val1;
					val1 = temp;
				}

				if( val3 < val2 ) {
					int temp = val2;
					val2 = val3;
					val3 = temp;
				}

				if( val3 < val1 ) {
					s += val2;
					s += val3;
				} else if( val2 < val1 ) {
					s += val2;
					s += val0;
				} else {
					s += val0;
					s += val1;
				}

				score[index] = s + middle[index];
			}
		}
	}

	/**
	 * compute the score for each element all at once to encourage the JVM to optimize and
	 * encourage the JVM to optimize this section of code.
	 *
	 * Was original inline, but was actually slightly slower by about 3% consistently,  It
	 * is in its own function so that it can be overridden and have different cost functions
	 * inserted easily.
	 */
	protected void computeScoreRow(ImageUInt8 left, ImageUInt8 right,
								   int elementMax, int indexLeft, int indexRight)
	{
		for( int rCol = 0; rCol < elementMax; rCol++ ) {
			int diff = (left.data[ indexLeft++ ]& 0xFF) - (right.data[ indexRight++ ]& 0xFF);

			elementScore[rCol] = Math.abs(diff);
		}
	}

	@Override
	public Class<ImageUInt8> getInputType() {
		return ImageUInt8.class;
	}

	@Override
	public Class<Disparity> getDisparityType() {
		return computeDisparity.getDisparityType();
	}

	@Override
	public int getBorderX() {
		return radiusX*2;
	}

	@Override
	public int getBorderY() {
		return radiusY*2;
	}
}
