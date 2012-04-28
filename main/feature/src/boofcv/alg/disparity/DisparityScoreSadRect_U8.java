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
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;

/**
 * <p>
 * Computes the disparity SAD score efficiently for a single rectangular region while minimizing CPU cache misses.
 * After the score has been computed for an entire row it is passed onto another algorithm to compute the actual
 * disparity. Provides support for fast right to left validation.  First the sad score is computed horizontally
 * then summed up vertically while minimizing redundant calculations that naive implementation would have.
 * </p>
 *
 * <p>
 * Memory usage is minimized by only saving disparity scores for the row being considered.  The more
 * straight forward implementation is to compute the disparity score for the whole image at once,
 * which can be quite expensive.
 * </p>
 *
 * <p>
 * Score Format:  The index of the score for column i at disparity d is 'index = imgWidth*d + i'.  The
 * first score element refers to column radiusX in the image.<br>
 * Format Comment:<br>
 * This ordering is a bit unnatural when searching for the best disparity, but reduces cache misses
 * when writing.  Performance boost is about 20%-30% depending on max disparity and image size.
 * </p>
 *
 * @see DisparitySelectRect_S32
 *
 * @author Peter Abeles
 */
public class DisparityScoreSadRect_U8 <Disparity extends ImageSingleBand> {

	// Computes disparity from scores
	DisparitySelectRect_S32<Disparity> computeDisparity;

	// maximum allowed image disparity
	int maxDisparity;

	// number of score elements: (image width - regionWidth)*maxDisparity
	int lengthHorizontal;

	// stores the local scores for the width of the region
	int elementScore[];
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

	/**
	 * Configures disparity calculation.
	 *
	 * @param maxDisparity Maximum disparity that it will calculate. Must be > 0
	 * @param regionRadiusX Radius of the rectangular region along x-axis.
	 * @param regionRadiusY Radius of the rectangular region along y-axis.
	 * @param computeDisparity Algorithm which computes the disparity from the score.
	 */
	public DisparityScoreSadRect_U8(int maxDisparity,
									int regionRadiusX, int regionRadiusY,
									DisparitySelectRect_S32<Disparity> computeDisparity ) {
		if( maxDisparity <= 0 )
			throw new IllegalArgumentException("Max disparity must be greater than zero");

		this.maxDisparity = maxDisparity;
		this.radiusX = regionRadiusX;
		this.radiusY = regionRadiusY;
		this.computeDisparity = computeDisparity;

		this.regionWidth = regionRadiusX*2+1;
		this.regionHeight = regionRadiusY*2+1;
	}

	/**
	 * Computes disparity between two stereo images
	 *
	 * @param left Left rectified stereo image. Input
	 * @param right Right rectified stereo image. Input
	 * @param disparity Disparity between the two images. Output
	 */
	public void process( ImageUInt8 left , ImageUInt8 right , Disparity disparity ) {
		// initialize data structures
		InputSanityCheck.checkSameShape(left,right,disparity);

		lengthHorizontal = left.width*maxDisparity;
		if( horizontalScore == null || verticalScore.length < lengthHorizontal ) {
			horizontalScore = new int[regionHeight][lengthHorizontal];
			verticalScore = new int[lengthHorizontal];
			elementScore = new int[ left.width ];
		}

		computeDisparity.configure(disparity,maxDisparity,radiusX);

		// initialize computation
		computeFirstRow(left, right, disparity);
		// efficiently compute rest of the rows using previous results to avoid repeat computations
		computeRemainingRows(left, right, disparity);
	}

	/**
	 * Initializes disparity calculation by finding the scores for the initial block of horizontal
	 * rows.
	 */
	private void computeFirstRow(ImageUInt8 left, ImageUInt8 right , Disparity disparity ) {
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
		computeDisparity.process(radiusY, verticalScore);
	}

	/**
	 * Using previously computed results it efficiently finds the disparity in the remaining rows.
	 * When a new block is processes the last row/column is subtracted and the new row/column is
	 * added.
	 */
	private void computeRemainingRows( ImageUInt8 left, ImageUInt8 right , Disparity disparity )
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
			computeDisparity.process(row - regionHeight + 1 + radiusY, verticalScore);
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
		for( int d = 0; d < maxDisparity; d++ ) {
			final int elementMax = left.width-d;
			final int scoreMax = elementMax-regionWidth;
			int indexScore = left.width*d+d;

			int indexLeft = left.startIndex + left.stride*row + d;
			int indexRight =  right.startIndex + right.stride*row;

			// Fill elementScore with all the scores for this row at disparity d
			compoteScoreRow(left, right, elementMax, indexLeft, indexRight);

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

	/**
	 * compute the score for each element all at once to encourage the JVM to optimize and
	 * encourage the JVM to optimize this section of code.
	 *
	 * Was original inline, but was actually slightly slower by about 3% consistently,  It
	 * is in its own function so that it can be overiden and have different cost functions
	 * inserted easily.
	 */
	protected void compoteScoreRow(ImageUInt8 left, ImageUInt8 right,
								   int elementMax, int indexLeft, int indexRight)
	{
		for( int rCol = 0; rCol < elementMax; rCol++ ) {
			int diff = (left.data[ indexLeft++ ] & 0xFF) - (right.data[ indexRight++ ] & 0xFF);

//			elementScore[rCol] = diff*diff;
			elementScore[rCol] = Math.abs(diff);
		}
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
