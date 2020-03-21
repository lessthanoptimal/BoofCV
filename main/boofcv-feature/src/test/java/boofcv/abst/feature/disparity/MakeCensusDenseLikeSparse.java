/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.transform.census.FilterCensusTransform;
import boofcv.alg.feature.disparity.DisparityBlockMatchRowFormat;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.border.ImageBorderWrapped;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

/**
 * Dense and sparse implementations of Census transform will not produce identical results because of how the image
 * border is handled. This hacks the dense algorithm to act like the sparse at image borders.
 *
 * @author Peter Abeles
 */
public class MakeCensusDenseLikeSparse
		<T extends ImageGray<T>, C extends ImageGray<C>,DI extends ImageGray<DI>>
		extends WrapBaseBlockMatch<T,C,DI>
{

	FilterCensusTransform<T, C> censusTran;

	// same border that's applied during the census transform
	ImageBorder<T> censusBorder;

	// Expand the input image at the border
	T expandedLeft;
	T expandedRight;

	// Storage for census transform of left and right expanded images
	C expandCLeft;
	C expandCRight;

	// Storage for census on regular images
	C cleft;
	C cright;

	public MakeCensusDenseLikeSparse(FilterCensusTransform<T, C> censusTran,
									 ImageBorder<T> censusBorder,
									 DisparityBlockMatchRowFormat<C, DI> alg)
	{
		super(alg);
		this.censusTran = censusTran;
		this.censusBorder = censusBorder;
		this.alg = alg;
		disparity = GeneralizedImageOps.createSingleBand(alg.getDisparityType(),1,1);
		expandedLeft = censusTran.getInputType().createImage(1,1);
		expandedRight = censusTran.getInputType().createImage(1,1);
		cleft = censusTran.getOutputType().createImage(1,1);
		cright = censusTran.getOutputType().createImage(1,1);
		expandCLeft = censusTran.getOutputType().createImage(1,1);
		expandCRight = censusTran.getOutputType().createImage(1,1);
	}

	@Override
	public void _process(T imageLeft, T imageRight) {
		cleft.reshape(imageLeft);
		cright.reshape(imageRight);

		int rx = alg.getRadiusX() + censusTran.getRadiusX();
		int ry = alg.getRadiusY() + censusTran.getRadiusY();

		// expand the input images
		expandedLeft.reshape( cleft.width+2*rx,cleft.height+2*ry);
		expandedRight.reshape(expandedLeft);

		GImageMiscOps.copy(-rx,-ry,0,0,expandedLeft.width,expandedLeft.height,imageLeft,censusBorder,expandedLeft);
		GImageMiscOps.copy(-rx,-ry,0,0,expandedRight.width,expandedRight.height,imageRight,censusBorder,expandedRight);

		// Apply Census Transform to the expanded input images
		censusTran.process(expandedLeft,expandCLeft);
		censusTran.process(expandedRight,expandCRight);

		// copy just the inner portion
		GImageMiscOps.copy(rx,ry,0,0,imageLeft.width,imageLeft.height,expandCLeft,cleft);
		GImageMiscOps.copy(rx,ry,0,0,imageRight.width,imageRight.height,expandCRight,cright);

		// set up the special border image
		alg.getGrowBorderL().setBorder(ImageBorderWrapped.wrap(expandCLeft,cleft));
		alg.getGrowBorderR().setBorder(ImageBorderWrapped.wrap(expandCRight,cright));

		// Now compute the disparity
		alg.process(cleft,cright,disparity);
	}

	@Override
	public ImageType<T> getInputType() {
		return censusTran.getInputType();
	}
}
