/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

import boofcv.core.image.GConvertImage;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * Used to automatically convert the input image type to use that's usable.
 *
 * @author Peter Abeles
 */
public class DescribePointRadiusAngleConvertImage
		<In extends ImageBase<In>, Mod extends ImageBase<Mod>, Desc extends TupleDesc<Desc>>
		implements DescribePointRadiusAngle<In, Desc> {
	// Computers the description
	DescribePointRadiusAngle<Mod, Desc> original;

	ImageType<In> inputType;

	// Image after it has been converted and modified
	Mod modified;

	public DescribePointRadiusAngleConvertImage( DescribePointRadiusAngle<Mod, Desc> original,
												 ImageType<In> inputType ) {
		this.original = original;
		this.inputType = inputType;

		modified = original.getImageType().createImage(1, 1);
	}

	@Override public void setImage( In image ) {
		GConvertImage.convert(image, modified);
		original.setImage(modified);
	}

	@Override public Desc createDescription() {
		return original.createDescription();
	}

	@Override public boolean process( double x, double y, double orientation, double radius, Desc output ) {
		return original.process(x, y, orientation, radius, output);
	}

	@Override public boolean isScalable() {
		return original.isScalable();
	}

	@Override public boolean isOriented() {
		return original.isOriented();
	}

	@Override public ImageType<In> getImageType() {
		return inputType;
	}

	@Override public double getCanonicalWidth() {
		return original.getCanonicalWidth();
	}

	@Override public Class<Desc> getDescriptionType() {
		return original.getDescriptionType();
	}
}
