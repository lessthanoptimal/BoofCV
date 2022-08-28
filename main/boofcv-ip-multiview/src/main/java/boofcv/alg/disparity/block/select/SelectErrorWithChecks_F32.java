/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

/**
 * <p>
 * Implementation of {@link SelectDisparityWithChecksWta} as a base class for arrays of type F32.
 * Extend for different output image types.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class SelectErrorWithChecks_F32<DI extends ImageGray<DI>>
		extends SelectDisparityWithChecksWta<float[], DI> implements Compare_F32 {
	// scores organized for more efficient processing
	float[] columnScore = new float[1];
	int imageWidth;

	// texture threshold, use an integer value for speed.
	protected float textureThreshold;

	protected SelectErrorWithChecks_F32( int maxError,
										 int rightToLeftTolerance,
										 double texture,
										 Class<DI> disparityType ) {
		super(maxError, rightToLeftTolerance, texture, disparityType);
		this.disparityType = disparityType;
	}

	protected SelectErrorWithChecks_F32( SelectErrorWithChecks_F32<DI> original ) {
		this(original.maxError, original.rightToLeftTolerance, original.textureThreshold, original.disparityType);
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

		// Mark all pixels as invalid which can't be estimate due to disparityMin
		for (int col = 0; col < disparityMin; col++) {
			setDisparityInvalid(indexDisparity++);
		}

		// Select the best disparity from all the rest
		for (int col = disparityMin; col < imageWidth; col++) {
			// Determine the number of disparities that can be considered at this column
			localRange = disparityMaxAtColumnL2R(col) - disparityMin + 1;

			// index of the element being examined in the score array
			int indexScore = col - disparityMin;

			// select the best disparity
			int bestDisparity = 0;
			float scoreBest = columnScore[0] = scores[indexScore];
			indexScore += imageWidth;

			for (int i = 1; i < localRange; i++, indexScore += imageWidth) {
				float s = scores[indexScore];
				columnScore[i] = s;
				if (s < scoreBest) {
					scoreBest = s;
					bestDisparity = i;
				}
			}

			// detect bad matches
			if (scoreBest > maxError) {
				// make sure the error isn't too large
				bestDisparity = invalidDisparity;
			} else if (rightToLeftTolerance >= 0) {
				// if the associate is different going the other direction it is probably noise

				int disparityRtoL = selectRightToLeft(col - bestDisparity - disparityMin, scores);

				if (Math.abs(disparityRtoL - bestDisparity) > rightToLeftTolerance) {
					bestDisparity = invalidDisparity;
				}
			}
			// test to see if the region lacks sufficient texture if:
			// 1) not already eliminated 2) sufficient disparities to check, 3) it's activated
			if (textureThreshold > 0 && bestDisparity != invalidDisparity && localRange >= 3) {
				// find the second best disparity value and exclude its neighbors
				float secondBest = Float.MAX_VALUE;
				for (int i = 0; i < bestDisparity - 1; i++) {
					if (columnScore[i] < secondBest) {
						secondBest = columnScore[i];
					}
				}
				for (int i = bestDisparity + 2; i < localRange; i++) {
					if (columnScore[i] < secondBest) {
						secondBest = columnScore[i];
					}
				}

				// similar scores indicate lack of texture
				// C = (C2-C1)/C1
				if (secondBest - scoreBest <= textureThreshold*scoreBest)
					bestDisparity = invalidDisparity;
			}

			setDisparity(indexDisparity++, bestDisparity, scoreBest);
		}
	}

	/**
	 * Finds the best disparity going from right to left image.
	 */
	private int selectRightToLeft( int col, float[] scores ) {
		// The range of disparities it can search
		int maxLocalDisparity = Math.min(imageWidth, col + disparityMax + 1) - col - disparityMin;

		int indexBest = 0;
		int indexScore = col;
		float scoreBest = scores[col];
		indexScore += imageWidth + 1;

		for (int i = 1; i < maxLocalDisparity; i++, indexScore += imageWidth + 1) {
			float s = scores[indexScore];

			if (s < scoreBest) {
				scoreBest = s;
				indexBest = i;
			}
		}

		return indexBest;
	}

	@Override public int compare( float scoreA, float scoreB ) {
		return Float.compare(-scoreA, -scoreB);
	}

	/**
	 * Implementation for disparity images of type GrayU8
	 */
	public static class DispU8 extends SelectErrorWithChecks_F32<GrayU8> {
		public DispU8( int maxError, int rightToLeftTolerance, double texture ) {
			super(maxError, rightToLeftTolerance, texture, GrayU8.class);
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
