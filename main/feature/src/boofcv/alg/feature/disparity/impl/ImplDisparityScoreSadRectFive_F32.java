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

package boofcv.alg.feature.disparity.impl;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.feature.disparity.DisparityScoreWindowFive;
import boofcv.alg.feature.disparity.DisparitySelect;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;

/**
 * <p>
 * Implementation of {@link boofcv.alg.feature.disparity.DisparityScoreWindowFive} which
 * processes {@limk ImageFloat32} as input images.
 * </p>
 *
 * <p>
 * DO NOT MODIFY. Generated by {@link GenerateDisparityScoreSadRectFive}.
 * </p>
 *
 * @author Peter Abeles
 */
public class ImplDisparityScoreSadRectFive_F32<Disparity extends ImageSingleBand>
		extends DisparityScoreWindowFive<ImageFloat32,Disparity>
{

	// Computes disparity from scores
	DisparitySelect<float[],Disparity> computeDisparity;

	// stores the local scores for the width of the region
	float elementScore[];
	// scores along horizontal axis for current block
	float horizontalScore[][];
	// summed scores along vertical axis
	// Save the last regionHeight scores in a rolling window
	float verticalScore[][];
	// In the rolling verticalScore window, which one is the active one
	int activeVerticalScore;
	// Where the final score it stored that has been computed from five regions
	float fiveScore[];

	public ImplDisparityScoreSadRectFive_F32(int minDisparity, int maxDisparity,
											int regionRadiusX, int regionRadiusY,
											DisparitySelect<float[], Disparity> computeDisparity) {
		super(minDisparity,maxDisparity,regionRadiusX,regionRadiusY);
		this.computeDisparity = computeDisparity;
	}

	@Override
	public void process( ImageFloat32 left , ImageFloat32 right , Disparity disparity ) {
		// initialize data structures
		InputSanityCheck.checkSameShape(left, right, disparity);

		lengthHorizontal = left.width*rangeDisparity;
		if( horizontalScore == null || verticalScore.length < lengthHorizontal ) {
			horizontalScore = new float[regionHeight][lengthHorizontal];
			verticalScore = new float[regionHeight][lengthHorizontal];
			elementScore = new float[ left.width ];
			fiveScore = new float[ lengthHorizontal ];
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
	private void computeFirstRow( ImageFloat32 left, ImageFloat32 right ) {
		float firstRow[] = verticalScore[0];
		activeVerticalScore = 1;

		// compute horizontal scores for first row block
		for( int row = 0; row < regionHeight; row++ ) {

			float scores[] = horizontalScore[row];

			UtilDisparityScore.computeScoreRow(left, right, row, scores,
					minDisparity, maxDisparity, regionWidth, elementScore);
		}

		// compute score for the top possible row
		for( int i = 0; i < lengthHorizontal; i++ ) {
			float sum = 0;
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
	private void computeRemainingRows( ImageFloat32 left, ImageFloat32 right )
	{
		for( int row = regionHeight; row < left.height; row++ , activeVerticalScore++) {
			int oldRow = row%regionHeight;
			float previous[] = verticalScore[ (activeVerticalScore -1) % regionHeight ];
			float active[] = verticalScore[ activeVerticalScore % regionHeight ];

			// subtract first row from vertical score
			float scores[] = horizontalScore[oldRow];
			for( int i = 0; i < lengthHorizontal; i++ ) {
				active[i] = previous[i] - scores[i];
			}

			UtilDisparityScore.computeScoreRow(left, right, row, scores,
					minDisparity,maxDisparity,regionWidth,elementScore);

			// add the new score
			for( int i = 0; i < lengthHorizontal; i++ ) {
				active[i] += scores[i];
			}

			if( activeVerticalScore >= regionHeight-1 ) {
				float top[] = verticalScore[ (activeVerticalScore -2*radiusY) % regionHeight ];
				float middle[] = verticalScore[ (activeVerticalScore -radiusY) % regionHeight ];
				float bottom[] = verticalScore[ activeVerticalScore % regionHeight ];

				computeScoreFive(top,middle,bottom,fiveScore,left.width);
				computeDisparity.process(row - (1 + 4*radiusY) + 2*radiusY+1, fiveScore );
			}
		}
	}

	/**
	 * Compute the final score by sampling the 5 regions.  Four regions are sampled around the center
	 * region.  Out of those four only the two with the smallest score are used.
	 */
	protected void computeScoreFive( float top[] , float middle[] , float bottom[] , float score[] , int width ) {

		// disparity as the outer loop to maximize common elements in inner loops, reducing redundant calculations
		for( int d = minDisparity; d < maxDisparity; d++ ) {

			// take in account the different in image border between the sub-regions and the effective region
			int indexSrc = (d-minDisparity)*width + (d-minDisparity) + radiusX;
			int indexDst = (d-minDisparity)*width + (d-minDisparity);
			for( int col = d + 2*radiusX; col < width - 2*radiusX; col++ ) {
				int s = 0;

				// sample four outer regions at the corners around the center region
				float val0 = top[indexSrc-radiusX];
				float val1 = top[indexSrc+radiusX];
				float val2 = bottom[indexSrc-radiusX];
				float val3 = bottom[indexSrc+radiusX];

				// select the two best scores from outer for regions
				if( val1 < val0 ) {
					float temp = val0;
					val0 = val1;
					val1 = temp;
				}

				if( val3 < val2 ) {
					float temp = val2;
					val2 = val3;
					val3 = temp;
				}

				if( val3 < val0 ) {
					s += val2;
					s += val3;
				} else if( val2 < val1 ) {
					s += val2;
					s += val0;
				} else {
					s += val0;
					s += val1;
				}

				score[indexDst++] = s + middle[indexSrc++];
			}
		}
	}

	@Override
	public Class<ImageFloat32> getInputType() {
		return ImageFloat32.class;
	}

	@Override
	public Class<Disparity> getDisparityType() {
		return computeDisparity.getDisparityType();
	}

}
