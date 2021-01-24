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

package boofcv.io.image;

import boofcv.core.image.GConvertImage;
import boofcv.misc.BoofMiscOps;
import boofcv.misc.LookUpImages;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageDimension;
import lombok.Setter;

import java.util.List;

/**
 * Implementation of {@link LookUpImages} that converts the name into an integer and grabs images from memory.
 *
 * @author Peter Abeles
 */
public class LookUpImageListByIndex<Image extends ImageBase<Image>> implements LookUpImages {
	List<Image> images;

	/** Function for converting images from the input to output type */
	@Setter ConvertImage<Image> convert = GConvertImage::convert;

	public LookUpImageListByIndex( List<Image> images ) {
		BoofMiscOps.checkTrue(images.size() > 0);
		this.images = images;
	}

	@Override public boolean loadShape( String name, ImageDimension shape ) {
		int index = Integer.parseInt(name);
		if (index < 0 || index >= images.size())
			return false;

		Image img = images.get(index);
		shape.setTo(img.width, img.height);
		return true;
	}

	@Override public <LT extends ImageBase<LT>> boolean loadImage( String name, LT output ) {
		int index = Integer.parseInt(name);
		if (index < 0 || index >= images.size())
			return false;

		convert.convert(images.get(index), output);
		return true;
	}

	/**
	 * Converts the src image data type into the dst image type. Can be used to provide a custom conversion.
	 * E.g. weighted rgb to gray.
	 */
	@SuppressWarnings("rawtypes")
	@FunctionalInterface
	interface ConvertImage<A> {
		void convert(A src, ImageBase dst);
	}
}
