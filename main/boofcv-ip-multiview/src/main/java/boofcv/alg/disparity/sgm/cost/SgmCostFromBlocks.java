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

package boofcv.alg.disparity.sgm.cost;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.disparity.DisparityBlockMatchRowFormat;
import boofcv.alg.disparity.block.BlockRowScore;
import boofcv.alg.disparity.block.DisparitySelect;
import boofcv.alg.disparity.sgm.SgmDisparityCost;
import boofcv.misc.Compare_S32;
import boofcv.struct.image.*;
import org.jetbrains.annotations.Nullable;

/// Computes the error for SGM using [`block matching`][BlockRowScore].
/// It's a little bit of a hack since it grabs the error by implementing [DisparitySelect] which is normally
/// used to select a disparity instead here it copies it into the SGM cost tensor.
@SuppressWarnings({"NullAway.Init"})
public class SgmCostFromBlocks<T extends ImageBase<T>>
		implements SgmDisparityCost<T>, DisparitySelect<int[], GrayU8>, Compare_S32 {
	protected Planar<GrayU16> costYXD;
	protected DisparityBlockMatchRowFormat<T, GrayU8> blockScore;
	private final GrayU8 dummy = new GrayU8(0, 0);
	private int maxRegionError = 0;
	private int disparityMin;
	private int disparityRange;

	@Override public void configure( int disparityMin, int disparityRange ) {
		blockScore.configure(disparityMin, disparityRange);
		this.disparityMin = disparityMin;
		this.disparityRange = disparityRange;
	}

	@Override public void process( T left, T right, Planar<GrayU16> costYXD ) {
		InputSanityCheck.checkSameShape(left, right);
		this.costYXD = costYXD;
		costYXD.reshape(/* width= */disparityRange, /* height= */left.width, /* numberOfBands= */left.height);
		maxRegionError = blockScore.getMaxRegionError();
		blockScore.process(left, right, dummy, null);
	}

	@Override public void configure(
			GrayU8 imageDisparity, @Nullable GrayF32 score, int minDisparity, int maxDisparity, int radiusX ) {}

	@Override public void process( int row, int[] scoresArray ) {
		GrayU16 costXD = costYXD.getBand(row);
		final int lengthX = costXD.height;

		// first stored column; the cost tensor's x-coordinate is relative to this (disparityMin if non-negative, else 0)
		int xOffset = Math.max(0, disparityMin);

		for (int x = xOffset; x < lengthX; x++) {
			// Per-column disparity window.
			int loD = Math.min(disparityRange, Math.max(0, x - disparityMin - (lengthX - 1)));
			int hiD = Math.min(disparityRange, x - disparityMin + 1);
			int dstIdx = (x - xOffset)*disparityRange;

			// Fill disparities that fall off the right border of the right image with max cost
			for (int d = 0; d < loD; d++) {
				costXD.data[dstIdx + d] = SgmDisparityCost.MAX_COST;
			}
			for (int d = loD; d < hiD; d++) {
				// copy the error and its range. Block scores are stored at the absolute column within each row.
				int srcIdx = d*lengthX + x;
				costXD.data[dstIdx + d] = (short)(SgmDisparityCost.MAX_COST*scoresArray[srcIdx]/maxRegionError);

//				if( scoresArray[srcIdx] > maxRegionError || scoresArray[srcIdx] < 0 ) {
//					throw new RuntimeException("score is out of bounds. "+scoresArray[srcIdx]+
//							" / "+maxRegionError);
//				}
			}
			// Fill disparities that fall off the left border of the right image with max cost
			for (int d = hiD; d < disparityRange; d++) {
				costXD.data[dstIdx + d] = SgmDisparityCost.MAX_COST;
			}
		}
	}

	@Override public DisparitySelect<int[], GrayU8> concurrentCopy() {
		return this;
	}

	@Override public Class<GrayU8> getDisparityType() {throw new RuntimeException("Not supported");}

	@Override public int compare( int scoreA, int scoreB ) {
		return Integer.compare(scoreA, scoreB);
	}

	public void setBlockScore( DisparityBlockMatchRowFormat<T, GrayU8> blockScore ) {
		this.blockScore = blockScore;
	}
}
