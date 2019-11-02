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

import boofcv.alg.descriptor.DescriptorDistance;
import boofcv.struct.image.*;

/**
 * Contains common functions for computing disparity scores.
 *
 * @author Peter Abeles
 */
public class UtilDisparityScore { // TODO rename?

	/**
	 * Computes the SAD disparity for an entire row at all disparies.
	 *
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
	public static void computeSadDispRow(GrayU8 left, GrayU8 right, int row, int[] scores,
										 int minDisparity , int maxDisparity , int regionWidth ,
										 int[] elementScore ) {

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
			scoreSad(left, right, colMax, indexLeft, indexRight, elementScore);

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
	 * is in its own function so that it can be overridden and have different cost functions
	 * inserted easily.
	 */
	public static void scoreSad(GrayU8 left, GrayU8 right,
								int elementMax, int indexLeft, int indexRight,
								int[] elementScore)
	{
		for( int rCol = 0; rCol < elementMax; rCol++ ) {
			int diff = (left.data[ indexLeft++ ]& 0xFF) - (right.data[ indexRight++ ]& 0xFF);

			elementScore[rCol] = Math.abs(diff);
		}
	}

	/**
	 * Computes the SAD disparity for an entire row at all disparities.
	 *
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
	public static void dispRowCensus(GrayU8 left, GrayU8 right, int row, int[] scores,
									 int minDisparity , int maxDisparity , int regionWidth ,
									 int[] elementScore ) {

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
			scoreCensus(left, right, colMax, indexLeft, indexRight, elementScore);

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
	 * is in its own function so that it can be overridden and have different cost functions
	 * inserted easily.
	 */
	public static void scoreCensus(GrayU8 left, GrayU8 right,
								   int elementMax, int indexLeft, int indexRight,
								   int[] elementScore)
	{
		for( int rCol = 0; rCol < elementMax; rCol++ ) {
			final int a = left.data[ indexLeft++ ]& 0xFF;
			final int b = right.data[ indexRight++ ]& 0xFF;
			elementScore[rCol] = DescriptorDistance.hamming(a^b);
		}
	}

	/**
	 * Computes the SAD disparity for an entire row at all disparies.
	 *
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
	public static void dispRowCensus(GrayS32 left, GrayS32 right, int row, int[] scores,
									 int minDisparity , int maxDisparity , int regionWidth ,
									 int[] elementScore ) {

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
			scoreCensus(left, right, colMax, indexLeft, indexRight, elementScore);

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
	 * is in its own function so that it can be overridden and have different cost functions
	 * inserted easily.
	 */
	public static void scoreCensus(GrayS32 left, GrayS32 right,
								   int elementMax, int indexLeft, int indexRight,
								   int[] elementScore)
	{
		for( int rCol = 0; rCol < elementMax; rCol++ ) {
			final int a = left.data[ indexLeft++ ];
			final int b = right.data[ indexRight++ ];
			elementScore[rCol] = DescriptorDistance.hamming(a^b);
		}
	}

	public static void dispRowCensus(GrayS64 left, GrayS64 right, int row, int[] scores,
									 int minDisparity , int maxDisparity , int regionWidth ,
									 int[] elementScore ) {

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
			scoreCensus(left, right, colMax, indexLeft, indexRight, elementScore);

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
	 * is in its own function so that it can be overridden and have different cost functions
	 * inserted easily.
	 */
	public static void scoreCensus(GrayS64 left, GrayS64 right,
								   int elementMax, int indexLeft, int indexRight,
								   int[] elementScore)
	{
		for( int rCol = 0; rCol < elementMax; rCol++ ) {
			final long a = left.data[ indexLeft++ ];
			final long b = right.data[ indexRight++ ];
			elementScore[rCol] = DescriptorDistance.hamming(a^b);
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
	 * @param minDisparity Minimum disparity to consider
	 * @param maxDisparity Maximum disparity to consider
	 * @param regionWidth Size of the sample region's width
	 * @param elementScore Storage for scores of individual pixels
	 */
	public static void computeSadDispRow(GrayU16 left, GrayU16 right, int row, int[] scores,
										 int minDisparity , int maxDisparity , int regionWidth ,
										 int[] elementScore ) {

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
			scoreSad(left, right, colMax, indexLeft, indexRight, elementScore);

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
	 * is in its own function so that it can be overridden and have different cost functions
	 * inserted easily.
	 */
	public static void scoreSad(GrayU16 left, GrayU16 right,
								int elementMax, int indexLeft, int indexRight,
								int[] elementScore)
	{
		for( int rCol = 0; rCol < elementMax; rCol++ ) {
			int diff = (left.data[ indexLeft++ ]& 0xFFFF) - (right.data[ indexRight++ ]& 0xFFFF);

			elementScore[rCol] = Math.abs(diff);
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
	 * @param minDisparity Minimum disparity to consider
	 * @param maxDisparity Maximum disparity to consider
	 * @param regionWidth Size of the sample region's width
	 * @param elementScore Storage for scores of individual pixels
	 */
	public static void computeSadDispRow(GrayS16 left, GrayS16 right, int row, int[] scores,
										 int minDisparity , int maxDisparity , int regionWidth ,
										 int[] elementScore ) {

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
			scoreSad(left, right, colMax, indexLeft, indexRight, elementScore);

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
	 * is in its own function so that it can be overridden and have different cost functions
	 * inserted easily.
	 */
	public static void scoreSad(GrayS16 left, GrayS16 right,
								int elementMax, int indexLeft, int indexRight,
								int[] elementScore)
	{
		for( int rCol = 0; rCol < elementMax; rCol++ ) {
			int diff = left.data[ indexLeft++ ] - right.data[ indexRight++ ];

			elementScore[rCol] = Math.abs(diff);
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
	 * @param minDisparity Minimum disparity to consider
	 * @param maxDisparity Maximum disparity to consider
	 * @param regionWidth Size of the sample region's width
	 * @param elementScore Storage for scores of individual pixels
	 */
	public static void computeSadDispRow(GrayF32 left, GrayF32 right, int row, float[] scores,
										 int minDisparity , int maxDisparity , int regionWidth ,
										 float[] elementScore ) {

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
			scoreSad(left, right, colMax, indexLeft, indexRight, elementScore);

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

	/**
	 * compute the score for each element all at once to encourage the JVM to optimize and
	 * encourage the JVM to optimize this section of code.
	 *
	 * Was original inline, but was actually slightly slower by about 3% consistently,  It
	 * is in its own function so that it can be overridden and have different cost functions
	 * inserted easily.
	 */
	public static void scoreSad(GrayF32 left, GrayF32 right,
								int elementMax, int indexLeft, int indexRight,
								float[] elementScore)
	{
		for( int rCol = 0; rCol < elementMax; rCol++ ) {
			float diff = (left.data[ indexLeft++ ]) - (right.data[ indexRight++ ]);

			elementScore[rCol] = Math.abs(diff);
		}
	}
}
