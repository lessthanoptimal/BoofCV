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

package boofcv.abst.disparity;

import boofcv.alg.disparity.sgm.SgmStereoDisparity;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import org.jetbrains.annotations.Nullable;

public class WrapDisparitySgm<DI extends ImageGray<DI>> implements StereoDisparity<GrayU8, DI> {

	SgmStereoDisparity<GrayU8,?> sgm;
	@Nullable GrayF32 subpixel;

	public WrapDisparitySgm( SgmStereoDisparity<GrayU8,?> sgm, boolean subPixel) {
		this.sgm = sgm;
		this.subpixel = subPixel ? new GrayF32(1,1) : null;
	}

	@Override
	public void process(GrayU8 imageLeft, GrayU8 imageRight) {
		sgm.process(imageLeft,imageRight);
		if( subpixel != null ) {
			sgm.subpixel(sgm.getDisparity(), subpixel);
		}
	}

	@Override
	public DI getDisparity() {
		if( subpixel != null ) {
			return (DI)subpixel;
		} else {
			return (DI)sgm.getDisparity();
		}
	}

	@Override
	public int getDisparityMin() {
		return sgm.getDisparityMin();
	}

	@Override
	public int getDisparityRange() {
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
		return ImageType.SB_U8;
	}

	@Override
	public Class<DI> getDisparityType() {
		return (Class)(subpixel == null ? GrayF32.class : GrayU8.class);
	}

	public SgmStereoDisparity<GrayU8,?> getAlgorithm() {
		return sgm;
	}
}
