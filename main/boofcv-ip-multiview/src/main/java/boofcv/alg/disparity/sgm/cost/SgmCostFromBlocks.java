/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.Planar;

/**
 * Computes the error for SGM using {@link BlockRowScore block matching}.
 * It's a little bit of a hack since it grabs the error by implementing {@link DisparitySelect} which is normally
 * used to select a disparity instead here it copies it into the SGM cost tensor.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class SgmCostFromBlocks<T extends ImageBase<T>>
		implements SgmDisparityCost<T>, DisparitySelect<int[], GrayU8>, Compare_S32 {
	protected Planar<GrayU16> costYXD;
	protected DisparityBlockMatchRowFormat<T, GrayU8> blockScore;
	private final GrayU8 dummy = new GrayU8(0, 0);
	private int maxRegionError = 0;
	private int disparityMin;
	private int disparityRange;

	@Override
	public void configure( int disparityMin, int disparityRange ) {
		blockScore.configure(disparityMin, disparityRange);
		this.disparityMin = disparityMin;
		this.disparityRange = disparityRange;
	}

	@Override
	public void process( T left, T right, Planar<GrayU16> costYXD ) {
		InputSanityCheck.checkSameShape(left, right);
		this.costYXD = costYXD;
		costYXD.reshape(/* width= */disparityRange, /* height= */left.width, /* numberOfBands= */left.height);
		maxRegionError = blockScore.getMaxRegionError();
		blockScore.process(left, right, dummy);
	}

	@Override
	public void configure( GrayU8 imageDisparity, int minDisparity, int maxDisparity, int radiusX ) {}

	@Override
	public void process( int row, int[] scoresArray ) {
		GrayU16 costXD = costYXD.getBand(row);
		final int lengthX = costXD.height;

		for (int x = disparityMin; x < lengthX; x++) {
			int localRangeD = Math.min(disparityRange, x - disparityMin + 1);
			int dstIdx = (x - disparityMin)*disparityRange;
			for (int d = 0; d < localRangeD; d++) {
				// copy the error and range it's range
				int srcIdx = d*lengthX + x - disparityMin;
				costXD.data[dstIdx++] = (short)(SgmDisparityCost.MAX_COST*scoresArray[srcIdx]/maxRegionError);

//				if( scoresArray[srcIdx] > maxRegionError || scoresArray[srcIdx] < 0 ) {
//					throw new RuntimeException("score is out of bounds. "+scoresArray[srcIdx]+
//							" / "+maxRegionError);
//				}
			}
			for (int d = localRangeD; d < disparityRange; d++) {
				costXD.data[dstIdx++] = SgmDisparityCost.MAX_COST;
			}
		}
	}

	@Override
	public DisparitySelect<int[], GrayU8> concurrentCopy() {
		return this;
	}

	@Override
	public Class<GrayU8> getDisparityType() {throw new RuntimeException("Not supported");}

	@Override
	public int compare( int scoreA, int scoreB ) {
		return Integer.compare(scoreA, scoreB);
	}

	public void setBlockScore( DisparityBlockMatchRowFormat<T, GrayU8> blockScore ) {
		this.blockScore = blockScore;
	}
}
