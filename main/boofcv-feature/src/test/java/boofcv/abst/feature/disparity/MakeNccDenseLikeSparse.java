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

import boofcv.alg.feature.disparity.DisparityBlockMatchRowFormat;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.border.ImageBorderWrapped;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

/**
 * Dense and sparse implementations of NCC transform will not produce identical results because of how the image
 * border is handled. This hacks the dense algorithm to act like the sparse at image borders.
 *
 * @author Peter Abeles
 */
public class MakeNccDenseLikeSparse
		<T extends ImageGray<T>, DI extends ImageGray<DI>>
		extends WrapBaseBlockMatch<T,T,DI>
{
	// Border that's applied to the input image
	ImageBorder<T> border;

	// Expand the input image at the border
	T expandedLeft;
	T expandedRight;

	public MakeNccDenseLikeSparse(ImageBorder<T> border, DisparityBlockMatchRowFormat<T, DI> alg)
	{
		super(alg);
		this.border = border;
		this.alg = alg;
		disparity = GeneralizedImageOps.createSingleBand(alg.getDisparityType(),1,1);
		expandedLeft = alg.getInputType().createImage(1,1);
		expandedRight = alg.getInputType().createImage(1,1);
	}

	@Override
	public void _process(T imageLeft, T imageRight) {
		int rx = alg.getRadiusX();
		int ry = alg.getRadiusY();

		// expand the input images
		expandedLeft.reshape( imageLeft.width+2*rx,imageLeft.height+2*ry);
		expandedRight.reshape(expandedLeft);

		GImageMiscOps.copy(-rx,-ry,0,0,expandedLeft.width,expandedLeft.height,imageLeft,border,expandedLeft);
		GImageMiscOps.copy(-rx,-ry,0,0,expandedRight.width,expandedRight.height,imageRight,border,expandedRight);

		// set up the special border image
		alg.getGrowBorderL().setBorder(ImageBorderWrapped.wrap(expandedLeft, imageLeft));
		alg.getGrowBorderR().setBorder(ImageBorderWrapped.wrap(expandedRight,imageRight));

		// Now compute the disparity
		alg.process(imageLeft,imageRight,disparity);
	}

	@Override
	public ImageType<T> getInputType() {
		return alg.getInputType();
	}
}
