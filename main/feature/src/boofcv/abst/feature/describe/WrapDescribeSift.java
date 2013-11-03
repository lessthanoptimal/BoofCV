/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.describe;

import boofcv.alg.feature.describe.DescribePointSift;
import boofcv.alg.feature.detect.interest.SiftImageScaleSpace;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;

/**
 * Wrapper around {@link DescribePointSift} for {@link DescribeRegionPoint}.  Orientation is optionally
 * also estimated, however only one orientation hypothesis is considered.
 *
 * @author Peter Abeles
 */
public class WrapDescribeSift
		implements DescribeRegionPoint<ImageFloat32,SurfFeature>
{
	DescribePointSift alg;
	SiftImageScaleSpace ss;
	ImageType<ImageFloat32> imageType;

	public WrapDescribeSift(DescribePointSift alg,
							SiftImageScaleSpace ss) {
		this.alg = alg;
		this.ss = ss;
		imageType = ImageType.single(ImageFloat32.class);
	}

	@Override
	public void setImage(ImageFloat32 image) {
		ss.constructPyramid(image);
		ss.computeDerivatives();
		alg.setScaleSpace(ss);
	}

	@Override
	public SurfFeature createDescription() {
		return new SurfFeature(alg.getDescriptorLength());
	}

	@Override
	public boolean process(double x, double y, double orientation, double scale, SurfFeature storage)
	{
		alg.process(x,y,scale,orientation,storage);

		return true;
	}

	@Override
	public boolean requiresScale() {
		return true;
	}

	@Override
	public boolean requiresOrientation() {
		return true;
	}

	@Override
	public ImageType<ImageFloat32> getImageType() {
		return imageType;
	}

	@Override
	public Class<SurfFeature> getDescriptionType() {
		return SurfFeature.class;
	}
}
