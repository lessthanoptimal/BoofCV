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
import boofcv.alg.disparity.block.SelectDisparityWithChecksWta;
import boofcv.misc.Compare_F32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import org.jetbrains.annotations.Nullable;

/// Implementation of [SelectDisparityWithChecksWta] as a base class for arrays of type F32 are a correlation
/// score. Extend for different output image types.
public abstract class SelectCorrelationWithChecks_F32<DI extends ImageGray<DI>>
		extends SelectDisparityWithChecksWta<float[], DI> implements Compare_F32 {
	// scores organized for more efficient processing
	float[] columnScore = new float[1];
	int imageWidth;

	// texture threshold, use an integer value for speed.
	protected float textureThreshold;

	protected SelectCorrelationWithChecks_F32( int rightToLeftTolerance, double texture, Class<DI> disparityType ) {
		super(-1, rightToLeftTolerance, texture, disparityType);
	}

	protected SelectCorrelationWithChecks_F32( SelectCorrelationWithChecks_F32<DI> original ) {
		this(original.rightToLeftTolerance, original.textureThreshold, original.disparityType);
	}

	@Override public void setTexture( double threshold ) {
		textureThreshold = (float)threshold;
	}

	@Override public void configure( DI imageDisparity, @Nullable GrayF32 imageScore,
									 int disparityMin, int disparityMax, int radiusX ) {
		super.configure(imageDisparity, imageScore, disparityMin, disparityMax, radiusX);

		columnScore = new float[disparityRange];
		imageWidth = imageDisparity.width;
	}

	@Override public void process( int row, float[] scores ) {
		int indexDisparity = imageDisparity.startIndex + row*imageDisparity.stride;

		// first and last columns it will consider
		int col0 = Math.max(0, disparityMin);
		int col1 = imageWidth + Math.min(0, disparityMax);

		// Mark all pixels as invalid which can't be estimate due to disparityMin
		for (int col = 0; col < col0; col++) {
			setDisparity(indexDisparity++, invalidDisparity, Float.NaN);
		}

		for (int col = col0; col < col1; col++) {
			// first valid disparity, relative to disparityMin
			localDisparityMin = disparityMinAtColumnL2R(col) - disparityMin;
			// Number of disparities considered at this column
			localRange = disparityMaxAtColumnL2R(col) - disparityMin + 1;

			// index of the element being examined in the score array
			int indexScore = col + localDisparityMin*imageWidth;

			// select the best disparity. columnScore is indexed by disparity relative to disparityMin.
			int bestDisparity = localDisparityMin;
			float scoreBest = columnScore[localDisparityMin] = scores[indexScore];
			float scoreWorst = scoreBest;
			indexScore += imageWidth;

			for (int disparity = localDisparityMin + 1; disparity < localRange; disparity++, indexScore += imageWidth) {
				float s = scores[indexScore];
				columnScore[disparity] = s;
				if (s > scoreBest) {
					scoreBest = s;
					bestDisparity = disparity;
				} else if (s < scoreWorst) {
					scoreWorst = s;
				}
			}

			// detect bad matches
			if (rightToLeftTolerance >= 0) {
				// if the associate is different going the other direction it is probably noise
				int disparityRtoL = selectRightToLeft(col - bestDisparity - disparityMin, scores);

				if (Math.abs(disparityRtoL - bestDisparity) > rightToLeftTolerance) {
					bestDisparity = invalidDisparity;
				}
			}
			// test to see if the region lacks sufficient texture if:
			// 1) not already eliminated 2) sufficient disparities to check, 3) it's activated
			if (textureThreshold > 0 && bestDisparity != invalidDisparity && localRange - localDisparityMin >= 3) {
				// find the second-best disparity value and exclude its neighbors
				float secondBest = scoreWorst;
				for (int i = localDisparityMin; i < bestDisparity - 1; i++) {
					if (columnScore[i] > secondBest) {
						secondBest = columnScore[i];
					}
				}
				for (int i = bestDisparity + 2; i < localRange; i++) {
					if (columnScore[i] > secondBest) {
						secondBest = columnScore[i];
					}
				}

				// Make the score relative to the worst score
				scoreBest -= scoreWorst;
				secondBest -= scoreWorst;

				// similar scores indicate lack of texture
				// C = (C2-C1)/C1
				if (scoreBest - secondBest <= textureThreshold*secondBest)
					bestDisparity = invalidDisparity;
			}

			setDisparity(indexDisparity++, bestDisparity, scoreBest);
		}

		// mark pixels on the upper end as invalid if it can't reach them
		for (int col = col1; col < imageWidth; col++) {
			setDisparity(indexDisparity++, invalidDisparity, Float.NaN);
		}
	}

	/// Finds the best disparity going from right to left image.
	private int selectRightToLeft( int col, float[] scores ) {
		// 'col' is a right image column. Search disparities whose matched left column (col+d) stays in the image.
		// Disparity indices (relative to disparityMin) are clamped against both image borders.
		int localMin = Math.max(0, -col - disparityMin);
		int endDisparity = Math.min(disparityRange, imageWidth - col - disparityMin);

		int indexBest = localMin;
		int indexScore = (col + disparityMin) + localMin*(imageWidth + 1);
		float scoreBest = scores[indexScore];

		for (int disparity = localMin + 1; disparity < endDisparity; disparity++) {
			indexScore += imageWidth + 1;
			float s = scores[indexScore];

			if (s > scoreBest) {
				scoreBest = s;
				indexBest = disparity;
			}
		}

		return indexBest;
	}

	@Override public int compare( float scoreA, float scoreB ) {
		return Float.compare(scoreA, scoreB);
	}

	/// Implementation for disparity images of type GrayU8
	public static class DispU8 extends SelectCorrelationWithChecks_F32<GrayU8> {
		public DispU8( int rightToLeftTolerance, double texture ) {
			super(rightToLeftTolerance, texture, GrayU8.class);
		}

		public DispU8( DispU8 original ) {
			super(original);
		}

		@Override public DisparitySelect<float[], GrayU8> concurrentCopy() {
			return new DispU8(this);
		}

		@Override protected void setDisparity( int index, int value, float score ) {
			imageDisparity.data[index] = (byte)value;
			funcSaveScore.saveScore(index, score);
		}

		@Override protected void setDisparityInvalid( int index ) {
			imageDisparity.data[index] = (byte)invalidDisparity;
		}
	}
}
