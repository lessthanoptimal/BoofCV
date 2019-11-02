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

import boofcv.abst.filter.FilterImageInterface;
import boofcv.alg.feature.disparity.DisparityBlockMatchRowFormat;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

/**
 * Wrapper around Block Matching disparity which uses Census as an error measure.
 *
 * @author Peter Abeles
 */
public class WrapDisparityBlockMatchCensus
		<T extends ImageGray<T>, C extends ImageGray<C>,DI extends ImageGray<DI>>
		implements StereoDisparity<T, DI>
{
	DisparityBlockMatchRowFormat<C, DI> alg;
	DI disparity;

	FilterImageInterface<T, C> censusTran;

	// Storage for census transform of left and right images
	C cleft;
	C cright;

	public WrapDisparityBlockMatchCensus(FilterImageInterface<T, C> censusTran,
										 DisparityBlockMatchRowFormat<C, DI> alg)
	{
		this.censusTran = censusTran;
		this.alg = alg;
		disparity = GeneralizedImageOps.createSingleBand(alg.getDisparityType(),1,1);
		cleft = censusTran.getOutputType().createImage(1,1);
		cright = censusTran.getOutputType().createImage(1,1);
	}

	@Override
	public void process(T imageLeft, T imageRight) {

		if( disparity.width != imageLeft.width || disparity.height != imageLeft.height )  {
			disparity.reshape(imageLeft.width,imageLeft.height);
			GImageMiscOps.fill(disparity, getMaxDisparity() - getMinDisparity() + 1);
		}

		// Apply Census Transform to input images
		censusTran.process(imageLeft,cleft);
		censusTran.process(imageRight,cright);

		// Now compute the disparity
		alg.process(cleft,cright,disparity);
	}

	@Override
	public DI getDisparity() {
		return disparity;
	}

	public C getCLeft() {
		return cleft;
	}

	public C getCRight() {
		return cright;
	}

	@Override
	public int getBorderX() {
		return alg.getBorderX();
	}

	@Override
	public int getBorderY() {
		return alg.getBorderY();
	}

	@Override
	public int getMinDisparity() {
		return alg.getMinDisparity();
	}

	@Override
	public int getMaxDisparity() {
		return alg.getMaxDisparity();
	}

	@Override
	public ImageType<T> getInputType() {
		return censusTran.getInputType();
	}

	@Override
	public Class<DI> getDisparityType() {
		return alg.getDisparityType();
	}

	public DisparityBlockMatchRowFormat<C, DI> getDisparityAlg() {
		return alg;
	}

	public FilterImageInterface<T, C> getCensusTran() {
		return censusTran;
	}
}
