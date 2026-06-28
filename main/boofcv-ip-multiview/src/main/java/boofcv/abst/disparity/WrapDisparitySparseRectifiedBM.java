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

package boofcv.abst.disparity;

import boofcv.alg.disparity.block.DisparitySparseSelect;
import boofcv.alg.disparity.block.score.DisparitySparseRectifiedScoreBM;
import boofcv.struct.image.ImageGray;
import lombok.Getter;

/// Wrapper around [DisparitySparseRectifiedScoreBM] for [StereoDisparitySparse]
public class WrapDisparitySparseRectifiedBM<ArrayData, T extends ImageGray<T>>
		implements StereoDisparitySparse<T> {
	@Getter DisparitySparseRectifiedScoreBM<ArrayData, T> computeScore;
	@Getter DisparitySparseSelect<ArrayData> select;

	public WrapDisparitySparseRectifiedBM( DisparitySparseRectifiedScoreBM<ArrayData, T> computeScore,
										   DisparitySparseSelect<ArrayData> select ) {
		this.computeScore = computeScore;
		this.select = select;
	}

	@Override
	public void setImages( T imageLeft, T imageRight ) {
		computeScore.setImages(imageLeft, imageRight);
	}

	@Override
	public double getDisparity() {
		// the score array is indexed relative to the first valid disparity at this pixel, which equals
		// disparityMin except where a negative disparity is clamped by the right image border
		return computeScore.getLocalDisparityMinLtoR() + select.getDisparity();
	}

	@Override
	public boolean process( int x, int y ) {
		return select.select(computeScore, x, y);
	}

	@Override
	public int getBorderX() {
		return computeScore.getRadiusX();
	}

	@Override
	public int getBorderY() {
		return computeScore.getRadiusY();
	}

	@Override
	public int getMinDisparity() {
		return computeScore.getDisparityMin();
	}

	@Override
	public int getMaxDisparity() {
		return computeScore.getDisparityMax();
	}

	@Override
	public Class<T> getInputType() {
		return computeScore.getInputType();
	}
}
