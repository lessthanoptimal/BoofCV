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

package boofcv.alg.feature.disparity.block.select;

import boofcv.alg.feature.disparity.block.DisparitySelect;
import boofcv.struct.image.GrayU8;

/**
 * <p>
 * Implementation of {@link SelectErrorWithChecks_F32} for {@link GrayU8}.
 * </p>
 *
 * @author Peter Abeles
 */
public class SelectCorrelationChecksWta_F32_U8 extends SelectCorrelationChecksBase_F32<GrayU8>
{
	public SelectCorrelationChecksWta_F32_U8(int rightToLeftTolerance, double texture) {
		super(rightToLeftTolerance, texture);
	}

	public SelectCorrelationChecksWta_F32_U8(SelectCorrelationChecksWta_F32_U8 original) {
		super(original);
	}

	@Override
	public void configure(GrayU8 imageDisparity, int minDisparity, int maxDisparity, int radiusX) {
		super.configure(imageDisparity, minDisparity, maxDisparity, radiusX);

		if( rangeDisparity > 254 )
			throw new IllegalArgumentException("(max - min) disparity must be <= 254");
	}

	@Override
	public DisparitySelect<float[], GrayU8> concurrentCopy() {
		return new SelectCorrelationChecksWta_F32_U8(this);
	}

	protected void setDisparity( int index , int value ) {
		imageDisparity.data[index] = (byte)value;
	}

	@Override
	public Class<GrayU8> getDisparityType() {
		return GrayU8.class;
	}
}
