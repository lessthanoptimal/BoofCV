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

package boofcv.abst.feature.describe;

import boofcv.alg.feature.describe.DescribePointRawPixels;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.feature.TupleDesc_F32;
import boofcv.struct.feature.TupleDesc_U8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

/**
 * Wrapper around {@link DescribePointRawPixels} for {@link DescribePointRadiusAngle}.
 *
 * @author Peter Abeles
 */
public class DescribePointRawPixels_RadiusAngle<T extends ImageGray<T>, TD extends TupleDesc<TD>>
		implements DescribePointRadiusAngle<T, TD> {
	DescribePointRawPixels<T, TD> alg;
	ImageType<T> imageType;

	public DescribePointRawPixels_RadiusAngle( DescribePointRawPixels<T, TD> alg, Class<T> imageType ) {
		this.alg = alg;
		this.imageType = ImageType.single(imageType);
	}

	@Override
	public TD createDescription() {
		if (alg.getDescriptorType() == TupleDesc_F32.class) {
			return (TD)new TupleDesc_F32(alg.getDescriptorLength());
		} else {
			return (TD)new TupleDesc_U8(alg.getDescriptorLength());
		}
	}

	@Override
	public void setImage( T image ) {
		alg.setImage(image);
	}

	@Override
	public boolean process( double x, double y, double orientation, double radius, TD storage ) {
		alg.process((int)x, (int)y, storage);

		return true;
	}

	@Override
	public boolean isScalable() {
		return false;
	}

	@Override
	public boolean isOriented() {
		return false;
	}

	@Override
	public ImageType<T> getImageType() {
		return imageType;
	}

	@Override
	public Class<TD> getDescriptionType() {
		return alg.getDescriptorType();
	}

	@Override
	public double getCanonicalWidth() {
		return (alg.getRegionWidth() + alg.getRegionHeight())/2.0;
	}
}
