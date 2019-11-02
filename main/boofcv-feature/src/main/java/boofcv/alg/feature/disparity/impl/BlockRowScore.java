/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * Interface for computing disparity scores across an entire row
 *
 * <p>MUST BE THREAD SAFE</p>
 *
 * @author Peter Abeles
 */
public interface BlockRowScore<T extends ImageBase<T>,Array> {
	/**
	 * For a given disparity, the score for each region on the left share many components in common.
	 * Because of this the scores are computed with disparity being the outer most loop
	 *
	 * @param left left image
	 * @param right Right image
	 * @param row Image row being examined
	 * @param scores Storage for disparity scores.
	 * @param minDisparity Minimum disparity to consider
	 * @param maxDisparity Maximum disparity to consider
	 * @param regionWidth Size of the sample region's width
	 * @param elementScore Storage for scores of individual pixels
	 */
	void scoreRow(T left, T right, int row, Array scores,
				  int minDisparity , int maxDisparity , int regionWidth ,
				  Array elementScore);

	void score(T left, T right,
			   int elementMax, int indexLeft, int indexRight,
			   Array elementScore);

	ImageType<T> getImageType();

	abstract class ArrayS32<T extends ImageBase<T>> extends BlockRowScoreSad<T,int[]> {
		@Override
		public void scoreRow(T left, T right, int row, int[] scores,
							 int minDisparity, int maxDisparity, int regionWidth,
							 int[] elementScore) {
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
				score(left, right, colMax, indexLeft, indexRight, elementScore);

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
	}

	abstract class ArrayF32<T extends ImageBase<T>> extends BlockRowScoreSad<T,float[]> {
		@Override
		public void scoreRow(T left, T right, int row, float[] scores,
							 int minDisparity, int maxDisparity, int regionWidth,
							 float[] elementScore) {
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
				score(left, right, colMax, indexLeft, indexRight, elementScore);

				// score at the first column
				float score = 0;
				for( int i = 0; i < regionWidth; i++ )
					score += elementScore[i];

				scores[indexScore++] = score;

				// scores for the remaining columns
				for( int col = 0; col < scoreMax; col++ , indexScore++ ) {
					scores[indexScore] = score += elementScore[col+regionWidth] - elementScore[col];
				}
			}
		}
	}
}
