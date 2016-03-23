/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

/**
 * Converts the region descriptor type from the {@link DescribeRegionPoint} into the desired output using a
 * {@link ConvertTupleDesc}.
 *
 * @author Peter Abeles
 */
public class DescribeRegionPointConvert<T extends ImageGray,In extends TupleDesc,Out extends TupleDesc>
	implements DescribeRegionPoint<T,Out>
{
	// Computers the description
	DescribeRegionPoint<T,In> original;
	// Change the description type
	ConvertTupleDesc<In,Out> converter;

	// internal storage for the original descriptor
	In storage;

	public DescribeRegionPointConvert(DescribeRegionPoint<T, In> original,
									  ConvertTupleDesc<In, Out> converter) {
		this.original = original;
		this.converter = converter;

		storage = original.createDescription();
	}

	@Override
	public void setImage(T image) {
		original.setImage(image);
	}

	@Override
	public Out createDescription() {
		return converter.createOutput();
	}

	@Override
	public boolean process(double x, double y, double orientation, double radius, Out storage) {

		if( !original.process(x,y,orientation, radius, this.storage) )
			return false;
		converter.convert(this.storage, storage);

		return true;
	}

	@Override
	public boolean requiresRadius() {
		return original.requiresRadius();
	}

	@Override
	public boolean requiresOrientation() {
		return original.requiresOrientation();
	}

	@Override
	public ImageType<T> getImageType() {
		return original.getImageType();
	}

	@Override
	public double getCanonicalWidth() {
		return original.getCanonicalWidth();
	}

	@Override
	public Class<Out> getDescriptionType() {
		return converter.getOutputType();
	}
}
