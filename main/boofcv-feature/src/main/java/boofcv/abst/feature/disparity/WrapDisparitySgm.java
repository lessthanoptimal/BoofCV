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

package boofcv.abst.feature.disparity;

import boofcv.alg.feature.disparity.sgm.SgmDisparitySelector;
import boofcv.alg.feature.disparity.sgm.SgmStereoDisparityHmi;
import boofcv.alg.feature.disparity.sgm.StereoMutualInformation;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;

public class WrapDisparitySgm implements StereoDisparity<GrayU8, GrayF32> {

	SgmStereoDisparityHmi sgm;

	public WrapDisparitySgm( int disparityRange ) {
		StereoMutualInformation stereoMI = new StereoMutualInformation();
		stereoMI.configureSmoothing(3);
		stereoMI.configureHistogram(255,255);
		SgmDisparitySelector selector = new SgmDisparitySelector();
		selector.setRightToLeftTolerance(0);
		selector.setMaxError(10000); //TODO make relative to max possible error
		sgm = new SgmStereoDisparityHmi(50,stereoMI,selector);
		sgm.setDisparityMin(0);
		sgm.setDisparityRange(disparityRange);
		sgm.getAggregation().setMaxPathsConsidered(8);
		sgm.getAggregation().setPenalty1(50);
		sgm.getAggregation().setPenalty2(1500);
	}

	@Override
	public void process(GrayU8 imageLeft, GrayU8 imageRight) {
		sgm.processHmi(imageLeft,imageRight);
	}

	@Override
	public GrayF32 getDisparity() {
		GrayU8 pixel = sgm.getDisparity();
		GrayF32 subpixel = new GrayF32(pixel.width,pixel.height);
		sgm.subpixel(pixel,subpixel);
		return subpixel;
	}

	@Override
	public int getMinDisparity() {
		return sgm.getDisparityMin();
	}

	@Override
	public int getRangeDisparity() {
		return sgm.getDisparityRange();
	}

	@Override
	public int getInvalidValue() {
		return sgm.getInvalidDisparity();
	}

	@Override
	public int getBorderX() {
		return 0;
	}

	@Override
	public int getBorderY() {
		return 0;
	}

	@Override
	public ImageType<GrayU8> getInputType() {
		return ImageType.single(GrayU8.class);
	}

	@Override
	public Class<GrayF32> getDisparityType() {
		return GrayF32.class;
	}
}
