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

package boofcv.alg.feature.disparity.sgm.cost;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.feature.disparity.DisparityBlockMatchRowFormat;
import boofcv.alg.feature.disparity.block.DisparitySelect;
import boofcv.alg.feature.disparity.sgm.SgmDisparityCost;
import boofcv.misc.Compare_S32;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.Planar;

/**
 * Computes the error for SGM using {@link boofcv.alg.feature.disparity.block.BlockRowScore block matching}.
 * It's a little bit of a hack since it grabs the error by implementing {@link DisparitySelect} which is normally
 * used to select a disparity instead here it copies it into the SGM cost tensor.
 *
 * @author Peter Abeles
 */
public class SgmCostFromBlocks<T extends ImageBase<T>>
		implements SgmDisparityCost<T> , DisparitySelect<int[], GrayU8> , Compare_S32
{
	protected Planar<GrayU16> costYXD;
	protected DisparityBlockMatchRowFormat<T,GrayU8> blockScore;
	private GrayU8 dummy=null;
	private int maxRegionError = 0;

	@Override
	public void process(T left, T right, int disparityMin, int disparityRange, Planar<GrayU16> costYXD) {
		InputSanityCheck.checkSameShape(left,right);
		costYXD.reshape(disparityRange,left.width,left.height);

		// disparity min + range should be configured only once at construction and fixed after that
		this.costYXD = costYXD;
		maxRegionError = blockScore.getMaxRegionError();
		blockScore.process(left,right,dummy);
	}

	@Override
	public void configure(GrayU8 imageDisparity, int minDisparity, int maxDisparity, int radiusX) {}

	@Override
	public void process(int row, int[] scoresArray) {
		GrayU16 costXD = costYXD.getBand(row);
		final int lengthX = costXD.height;
		final int lengthD = costXD.width;

		for (int x = 0; x < lengthX; x++) {
			int dstIdx = x*lengthD; // TODO find the localLengthD
			for (int d = 0; d < lengthD; d++) {
				// copy the error and range it's range
				int srcIdx = d*lengthX + x;
				costXD.data[dstIdx++] = (short)(SgmDisparityCost.MAX_COST*scoresArray[srcIdx]/maxRegionError);

				if( scoresArray[srcIdx] > maxRegionError || scoresArray[srcIdx] < 0 ) {
					throw new RuntimeException("score is out of bounds. "+scoresArray[srcIdx]+
							" / "+maxRegionError);
				}
			}
		}
	}

	@Override
	public DisparitySelect<int[], GrayU8> concurrentCopy() {
		return this;
	}

	@Override
	public Class<GrayU8> getDisparityType() {return null;}

	@Override
	public int compare(int scoreA, int scoreB) {
		return Integer.compare(scoreA,scoreB);
	}

	public void setBlockScore(DisparityBlockMatchRowFormat<T, GrayU8> blockScore) {
		this.blockScore = blockScore;
	}
}
