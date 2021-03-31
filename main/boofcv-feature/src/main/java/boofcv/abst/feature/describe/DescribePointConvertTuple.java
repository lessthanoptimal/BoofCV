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

import boofcv.abst.feature.convert.ConvertTupleDesc;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

/**
 * Converts the region descriptor type from the {@link DescribePointRadiusAngle} into the desired output using a
 * {@link ConvertTupleDesc}.
 *
 * @author Peter Abeles
 */
public class DescribePointConvertTuple<T extends ImageGray<T>, In extends TupleDesc<In>, Out extends TupleDesc<Out>>
		implements DescribePoint<T, Out> {
	// Computers the description
	DescribePoint<T, In> original;
	// Change the description type
	ConvertTupleDesc<In, Out> converter;

	// internal storage for the original descriptor
	In storage;

	public DescribePointConvertTuple( DescribePoint<T, In> original, ConvertTupleDesc<In, Out> converter ) {
		this.original = original;
		this.converter = converter;

		storage = original.createDescription();
	}

	@Override public void setImage( T image ) {original.setImage(image);}

	@Override public boolean process( double x, double y, Out description ) {
		if (!original.process(x, y, this.storage))
			return false;
		converter.convert(this.storage, description);
		return true;
	}

	@Override public Out createDescription() {
		return converter.createOutput();
	}

	@Override public ImageType<T> getImageType() {
		return original.getImageType();
	}

	@Override public Class<Out> getDescriptionType() {
		return converter.getOutputType();
	}
}
