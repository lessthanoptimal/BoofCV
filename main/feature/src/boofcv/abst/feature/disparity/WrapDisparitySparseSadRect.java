/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.disparity;

import boofcv.alg.feature.disparity.DisparitySparseScoreSadRect;
import boofcv.alg.feature.disparity.DisparitySparseSelect;
import boofcv.struct.image.ImageGray;

/**
 * @author Peter Abeles
 */
public class WrapDisparitySparseSadRect <ArrayData,T extends ImageGray>
		implements StereoDisparitySparse<T>
{
	DisparitySparseScoreSadRect<ArrayData,T> computeScore;
	DisparitySparseSelect<ArrayData> select;

	// for an insignificant speed boost save this constant as a floating point number
	double minDisparityFloat;

	public WrapDisparitySparseSadRect(DisparitySparseScoreSadRect<ArrayData,T> computeScore,
									  DisparitySparseSelect<ArrayData> select ) {
		this.computeScore = computeScore;
		this.select = select;
	}

	@Override
	public void setImages(T imageLeft, T imageRight ) {
		computeScore.setImages(imageLeft,imageRight);
		minDisparityFloat = computeScore.getMinDisparity();
	}

	@Override
	public double getDisparity() {
		return minDisparityFloat+select.getDisparity();
	}

	@Override
	public boolean process(int x, int y) {
		if( computeScore.process(x,y) ) {
			return select.select(computeScore.getScore(), computeScore.getLocalMaxDisparity());
		}
		return false;
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
		return computeScore.getMinDisparity();
	}

	@Override
	public int getMaxDisparity() {
		return computeScore.getMaxDisparity();
	}

	@Override
	public Class<T> getInputType() {
		return computeScore.getImageType();
	}
}
