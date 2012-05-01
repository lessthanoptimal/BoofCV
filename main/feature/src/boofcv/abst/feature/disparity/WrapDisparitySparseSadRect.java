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

package boofcv.abst.feature.disparity;

import boofcv.alg.feature.disparity.DisparitySparseScoreSadRect_U8;
import boofcv.alg.feature.disparity.DisparitySparseSelect_S32;
import boofcv.struct.image.ImageUInt8;

/**
 * @author Peter Abeles
 */
public class WrapDisparitySparseSadRect implements StereoDisparitySparse<ImageUInt8>
{
	DisparitySparseScoreSadRect_U8 computeScore;
	DisparitySparseSelect_S32 select;

	public WrapDisparitySparseSadRect(DisparitySparseScoreSadRect_U8 computeScore,
									  DisparitySparseSelect_S32 select ) {
		this.computeScore = computeScore;
		this.select = select;
	}

	@Override
	public void setImages(ImageUInt8 imageLeft, ImageUInt8 imageRight ) {
		computeScore.setImages(imageLeft,imageRight);
	}

	@Override
	public double getDisparity() {
		return select.getDisparity();
	}

	@Override
	public boolean process(int x, int y) {
		computeScore.process(x,y);
		return select.select(computeScore.getScore(),computeScore.getLocalMaxDisparity());
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
	public int getMaxDisparity() {
		return computeScore.getMaxDisparity();
	}

	@Override
	public Class<ImageUInt8> getInputType() {
		return ImageUInt8.class;
	}
}
